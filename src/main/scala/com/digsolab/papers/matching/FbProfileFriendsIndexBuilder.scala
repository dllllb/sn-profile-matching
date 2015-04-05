package com.digsolab.papers.matching

import java.io._
import com.digsolab.papers.util.{Romanization, NameUtils}
import org.apache.lucene.store.{SimpleFSDirectory, Directory}
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.util.Version
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.document._
import org.apache.lucene.index.FieldInfo.IndexOptions
import scala.collection.JavaConverters._

/**
 * @author Dmitri Babaev (dmitri.babaev@gmail.com)
 */
object FbProfileFriendsIndexBuilder {
  def parseProfileLine(line: String) = {
    def nameParser(arg: Array[String]) = {
      val pid = arg.headOption.getOrElse(
        throw new IllegalArgumentException(s"incorrect user line ${arg.mkString(" ")}")
      ).toLong
      val orderedName = NameUtils.parseNameSecNamePair(arg.tail.dropRight(1).mkString(" "),
        arg.lastOption.getOrElse(""))
      val firstName = Romanization.normalize(orderedName._1)
      val secName = Romanization.normalize(orderedName._2)

      Person(pid, firstName, secName)
    }

    try {
      val items = line.split("\t")
      val user = items.headOption.getOrElse(
        throw new IllegalArgumentException(s"incorrect input line $line")
      )
      val friendsStr = items.tail.mkString("")

      val info = nameParser(user.split(" +"))
      val friends = friendsStr.split("\\|").filter(!_.isEmpty).map(_.split(" +")).map(nameParser)

      Some(ProfileFriends(info, friends.toSeq))
    } catch {
      case e: Exception => println(s"incorrect user line: $line; ${e.getMessage}")
      None
    }
  }

  def wallFriendsRecordsIt(reader: BufferedReader) = {
    val it = Iterator.continually(reader.readLine).takeWhile(_ != null)
    it.map { (line) =>
      parseProfileLine(line)
    }.flatten
  }

  def jsonRecordsIt(reader: BufferedReader) = {
    val it = Iterator.continually(reader.readLine).takeWhile(_ != null)
    it.map { (line) =>
      ProfileFriends.fromJson(line)
    }
  }

  def buildIndex(dir: Directory,
                 synonyms: => Reader,
                 source: Iterator[ProfileFriends]) {
    val defaultAnalyzer = new KeywordAnalyzer
    val nameAnalyzer = new SynonymAnalyzer(synonyms)
    val analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer,
      Map("name" -> nameAnalyzer.asInstanceOf[Analyzer]).asJava
    )

    val config = new IndexWriterConfig(Version.LUCENE_43, analyzer)
    config.setOpenMode(OpenMode.CREATE)

    val writer = new IndexWriter(dir, config)

    val nameFT = new FieldType()
    nameFT.setIndexed(true)
    nameFT.setOmitNorms(true)
    nameFT.setIndexOptions(IndexOptions.DOCS_AND_FREQS)
    nameFT.setStored(true)
    nameFT.setTokenized(true)
    nameFT.freeze()

    var processedDocs = 0
    var processedFriends = 0

    for (ProfileFriends(Person(pid, name, secName), friends) <- source) {
      val doc = new Document()
      doc.add(new StringField("id", pid.toString, Field.Store.YES))
      doc.add(new Field("name", name, nameFT))
      doc.add(new Field("2name", secName, nameFT))
      doc.add(new StoredField("friendNames", friends.map { (p) =>
        s"${p.name}/${p.secName}"
      }.mkString("|")))
      writer.addDocument(doc)

      processedDocs += 1
      processedFriends += friends.size
      if (processedDocs % 10000 == 0) {
        println(s"docs processed: $processedDocs")
      }
    }

    writer.forceMerge(1)
    writer.close()

    println(s"total docs processed: $processedDocs")
    println(s"total friends processed: $processedFriends")
  }
}

object FbProfileFriendsWallIndexBuilder {
  import FbProfileFriendsIndexBuilder._
  def main(args: Array[String]) {
    val indexLocation = System.getProperty("profile.index.location")
    val sourceFile = System.getProperty("profile.source.file")

    resource.managed(
      new BufferedReader(
        new InputStreamReader(
          new FileInputStream(sourceFile)
        )
      )
    ).acquireAndGet { reader =>
      buildIndex(
        new SimpleFSDirectory(new File(indexLocation)),
        new BufferedReader(
          new InputStreamReader(
            this.getClass.getResourceAsStream("/name-aliases-known-vb-vk-pairs-vk90k.txt")
          )
        ),
        wallFriendsRecordsIt(reader)
      )
    }
  }
}

object FbProfileFriendsJsonIndexBuilder {
  import FbProfileFriendsIndexBuilder._
  def main(args: Array[String]) {
    val indexLocation = System.getProperty("profile.index.location")
    val sourceFile = System.getProperty("profile.source.file")

    resource.managed(
      new BufferedReader(
        new InputStreamReader(
          new FileInputStream(sourceFile)
        )
      )
    ).acquireAndGet { reader =>
      buildIndex(
        new SimpleFSDirectory(new File(indexLocation)),
        new BufferedReader(
          new InputStreamReader(
            this.getClass.getResourceAsStream("/name-aliases-known-vb-vk-pairs-vk90k.txt")
          )
        ),
        jsonRecordsIt(reader)
      )
    }
  }
}
