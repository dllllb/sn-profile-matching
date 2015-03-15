package com.digsolab.euler.intprof.util

import java.io.{InputStreamReader, InputStream, BufferedReader}

import org.apache.lucene.search.spell.LevensteinDistance

object NameUtils {
  val diffThreshold = 1

  private val namesSourceStream = this.getClass.getResourceAsStream("/fb-name-tf-top10k.tsv")
  private val lastNamesSourceStream = this.getClass.getResourceAsStream("/fb-2name-tf-top10k.tsv")

  private lazy val namesFreqDictionary = initFreqDictionary(namesSourceStream)
  private lazy val lastNamesFreqDictionary = initFreqDictionary(lastNamesSourceStream)

  private def initFreqDictionary(inputStream: InputStream) = {
    resource.managed(new BufferedReader(new InputStreamReader(inputStream))).acquireAndGet { reader =>
      val it = Iterator.continually(reader.readLine).takeWhile(_ != null)
      it.map(_.split("\t")).map { case Array(name, freq) =>
        (name, freq.toLong)
      }.toMap
    }
  }

  private def calcCandidateScore(candidate: String, freqDict: Map[String, Long]) = {
    freqDict get Romanization.normalize(candidate) match {
      case Some(freq) ⇒ freq
      case None       ⇒ 0L
    }
  }

  case class ScoreDiff(nameDiff: Long, lNameDiff: Long)

  def parseNameSecNamePair(firstCandidate: String, secondCandidate: String) = {
    val firstNameScore = calcCandidateScore(firstCandidate, namesFreqDictionary)
    val firstLNameScore = calcCandidateScore(firstCandidate, lastNamesFreqDictionary)
    val secondNameScore = calcCandidateScore(secondCandidate, namesFreqDictionary)
    val secondLNameScore = calcCandidateScore(secondCandidate, lastNamesFreqDictionary)
    ScoreDiff(firstNameScore * diffThreshold - secondNameScore,
      secondLNameScore * diffThreshold - firstLNameScore) match {
        case ScoreDiff(nameDiff, lNameDiff) if (nameDiff == 0 && lNameDiff == 0) ⇒
          (firstCandidate, secondCandidate)
        case ScoreDiff(nameDiff, lNameDiff) if (nameDiff <= 0 && lNameDiff <= 0) ⇒
          (secondCandidate, firstCandidate)
        case ScoreDiff(nameDiff, lNameDiff) if (nameDiff <= 0 && lNameDiff > 0) ⇒
          if (nameDiff != 0 && secondNameScore > secondLNameScore * diffThreshold)
            (secondCandidate, firstCandidate)
          else
            (firstCandidate, secondCandidate)
        case ScoreDiff(nameDiff, lNameDiff) if (nameDiff > 0 && lNameDiff >= 0) ⇒
          (firstCandidate, secondCandidate)
        case ScoreDiff(nameDiff, lNameDiff) if (nameDiff > 0 && lNameDiff <= 0) ⇒
          if (nameDiff != 0 && firstLNameScore > firstNameScore * diffThreshold)
            (secondCandidate, firstCandidate)
          else
            (firstCandidate, secondCandidate)
      }
  }

  def isRussianName(name: String) =
    namesFreqDictionary.keys.toSet contains Romanization.normalize(name)

  def isRussianLastName(lastName: String) =
    lastNamesFreqDictionary.keys.toSet contains Romanization.normalize(lastName)

  def matchingLastNames(first: String, second: String) =
    if (first.nonEmpty && second.nonEmpty) {
      val firstRomanized = Romanization.normalize(first)
      val secondRomanized = Romanization.normalize(second)
      (firstRomanized.head == secondRomanized.head) &&
        (new LevensteinDistance().getDistance(firstRomanized, secondRomanized) >= 0.9F)
    } else
      false
}
