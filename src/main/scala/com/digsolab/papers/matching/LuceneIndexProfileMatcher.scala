package com.digsolab.papers.matching

import com.digsolab.papers.util.TermFrequences
import org.apache.lucene.index.{Term, MultiFields, IndexReader}
import org.apache.lucene.search.spell.StringDistance
import org.apache.lucene.search.{BooleanClause, BooleanQuery, FuzzyQuery, IndexSearcher}

trait ActionLogger {
  def logAction(action: => String)
}

object NoOpActionLogger extends ActionLogger {
  def logAction(action: => String) {}
}

class DebugActionLogger extends ActionLogger {
  def logAction(action: => String) {
    println(action)
  }
}

class LuceneIndexProfileMatcher(val reader: IndexReader,
                                val stringDistAlgo: StringDistance,
                                val friendNameDistThreshold: Float,
                                val friendSecNameDistThreshold: Float,
                                val maxCandidates: Int,
                                val maxResults: Int,
                                val actionLogger: ActionLogger = NoOpActionLogger) {

  val (nameFreqs, secNameFreqs) = {
    val fields = MultiFields.getFields(reader)
    (TermFrequences.termFreqs(fields.terms("name")), TermFrequences.termFreqs(fields.terms("2name")))
  }

  val docsCount = reader.numDocs()

  val index = new IndexSearcher(reader)

  def findMatches(profile: ProfileRecord) = {
    actionLogger.logAction(s"profile: ${profile._id}, ${profile.info}")

    val nameFQ = new FuzzyQuery(new Term("name", profile.info.name), 2, 1)
    val snameFQ = new FuzzyQuery(new Term("2name", profile.info.secName), 2, 1)
    val candQ = new BooleanQuery
    candQ.add(nameFQ, BooleanClause.Occur.MUST)
    candQ.add(snameFQ, BooleanClause.Occur.MUST)
    val candTopDocs = index.search(candQ, maxCandidates)
    val hits = candTopDocs.scoreDocs
    val candidates = hits.map { (hit) => index.doc(hit.doc) }.map { (doc) =>
      val friends = doc.get("friendNames").split("\\|").map(_.split("/")).collect {
        case Array(n, sn) => ProfileName(n, sn)
      }

      ProfileRecord(doc.get("id"), ProfileName(doc.get("name"), doc.get("2name")), friends)
    }

    def similarFriend(profFriend: ProfileName,
                      candFriends: Map[String, Seq[ProfileName]]) = {
      val samePrefixCandFriends = candFriends.getOrElse(
        profFriend.secName.take(2), Seq.empty[ProfileName]
      )

      val friendNameDists = samePrefixCandFriends map { candFriend =>
        val frNameDist = stringDistAlgo.getDistance(profFriend.name, candFriend.name)
        val frSecNameDist = stringDistAlgo.getDistance(profFriend.secName, candFriend.secName)

        (frNameDist, frSecNameDist, profFriend, candFriend)
      }

      friendNameDists.find { (e) =>
        val (frNameDist, frSecNameDist, _, _) = e
        val similar = frNameDist > friendNameDistThreshold && frSecNameDist > friendSecNameDistThreshold
        similar
      }
    }

    def calcCandScore(candidate: ProfileRecord) = {
      actionLogger.logAction(s"  candidate: ${candidate._id}, ${candidate.info}")

      val candFriendsBySecNamePrefix = candidate.friends groupBy { (p) =>
        p.secName.take(2)
      }

      val (score, matchCount) = profile.friends.foldLeft((0D,0)) { (scoreAndCount, profFriend) =>
        val (tScore, tMatchCount) = scoreAndCount
        similarFriend(profFriend, candFriendsBySecNamePrefix) match {
          case Some((frNameDist, frSecNameDist, _, candFriend)) => {
            val candNameCount = nameFreqs.getOrElse(candFriend.name, 1L)
            val candSecNameCount = secNameFreqs.getOrElse(candFriend.secName, 1L)
            val intermediateWeight = docsCount.toDouble/candNameCount.toDouble/candSecNameCount.toDouble
            val weight = if (intermediateWeight > 1) 1 else intermediateWeight
            actionLogger.logAction(
              s"    " +
              s"$profFriend ~ $candFriend: " +
              s"ND:$frNameDist, 2ND:$frSecNameDist, W:$weight"
            )
            (tScore + weight, tMatchCount+1)
          }
          case None => (tScore, tMatchCount)
        }
      }

      actionLogger.logAction(s"  match score: $score, matched friends: $matchCount")
      (score, candidate)
    }

    val candScores = candidates.map(calcCandScore)

    (candScores.sortBy(_._1).takeRight(maxResults).toSeq, candTopDocs.totalHits)
  }
}
