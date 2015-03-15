package com.digsolab.euler.intprof.matching

import org.apache.lucene.store.MMapDirectory
import java.io.{FileInputStream, InputStreamReader, BufferedReader, File}
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.spell.LevensteinDistance

object MatchProfileByFriendsTool {
  def main(args: Array[String]) {
    val indexPath = System.getProperty("profile.friends.index")
    val profilesPath = System.getProperty("profile.friends.list")
    val logger = if (System.getProperty("match.debug.output", "false").toBoolean)
      new DebugActionLogger
    else
      NoOpActionLogger
    val dir = new MMapDirectory(new File(indexPath))
    val reader = DirectoryReader.open(dir)
    val profileMatcher = new LuceneIndexProfileMatcher(reader, new LevensteinDistance, 0.6F, 0.8F, 10000, 3, logger)

    resource.managed(
      new BufferedReader(
        new InputStreamReader(
          new FileInputStream(profilesPath)
        )
      )
    ).acquireAndGet { reader =>
      val it = Iterator.continually(reader.readLine).takeWhile(_ != null)
      it.map { (line) =>
        ProfileRecord.fromJson(line)
      }.foreach { (profile) =>
        profileMatcher.findMatches(profile)._1.sortBy(_._1).reverse.foreach { (candidate) =>
          val (score, candProf) = candidate
          println(Array(profile._id, candProf._id, profile.info, candProf.info, score).mkString("\t"))
        }
      }
    }
  }
}
