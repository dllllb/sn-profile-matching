package com.digsolab.euler.intprof.matching

import com.digsolab.euler.intprof.util.{NameUtils, Romanization}
import java.io.{Reader, BufferedReader}
import org.apache.lucene.store.Directory
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.util.Version
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.document._
import org.apache.lucene.index.FieldInfo.IndexOptions
import scala.Some
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._


object MailruProfileFriendsIndexBuilder {
  def parseProfileLine(line: String) = {
    def nameParser(arg: Array[String]) = {
      if (arg.length < 2) throw new IllegalArgumentException(s"incorrect user line ${arg.mkString(" ")}")
      val pid = arg.head
      val rawName = (arg.last.split(" ").head, arg.last.split(" ").last)
      val orderedName = NameUtils.parseNameSecNamePair(rawName._1, rawName._2)
      val firstName = Romanization.normalize(orderedName._1)
      val secName = Romanization.normalize(orderedName._2)

      MailruPerson(pid, firstName, secName)
    }

    try {
      val items = line.split("\t")
      val user = items.headOption.getOrElse(
        throw new IllegalArgumentException(s"incorrect input line $line")
      )

      val info = nameParser(items.sliding(1, 2).toArray.flatten)
      val friend = nameParser(items.drop(1).sliding(1, 2).toArray.flatten)

      Some(MailruProfileFriend(info, friend))
    } catch {
      case e: Exception => println(s"incorrect user line: $line; ${e.getMessage}")
        None
    }
  }

  def csvFriendsRecordsIt(reader: BufferedReader) = {
    val it = Iterator.continually(reader.readLine).takeWhile(_ != null)
    it.map { (line) =>
      parseProfileLine(line)
    }.flatten
  }

  def flushFriendsBulk(currentProfile: MailruPerson, friendsBulk: ArrayBuffer[MailruPerson],
                       writer: IndexWriter, nameFT: FieldType) = {
    val doc = new Document()
    doc.add(new StringField("id", currentProfile.id.toString, Field.Store.YES))
    doc.add(new Field("name", currentProfile.name, nameFT))
    doc.add(new Field("2name", currentProfile.secName, nameFT))
    doc.add(new StoredField("friendNames", friendsBulk.map { (p) =>
      s"${p.name}/${p.secName}"
    }.mkString("|")))
    writer.addDocument(doc)
    friendsBulk.clear()
  }

  def buildIndex(dir: Directory,
                 synonyms: => Reader,
                 source: Iterator[MailruProfileFriend]) {
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

    val friendsBulk = ArrayBuffer.empty[MailruPerson]
    var currentProfile: Option[MailruPerson] = None

    for (MailruProfileFriend(MailruPerson(pid, name, secName), friend) <- source) {
      currentProfile match {
        case Some(MailruPerson(currentPid, currentName, currentSecName)) =>
          if (pid != currentPid) {
            flushFriendsBulk(MailruPerson(currentPid, currentName, currentSecName), friendsBulk, writer, nameFT)
            currentProfile = Some(MailruPerson(pid, name, secName))
          }
        case None => currentProfile = Some(MailruPerson(pid, name, secName))
      }
      friendsBulk += friend
      processedDocs += 1
      processedFriends += 1
      if (processedDocs % 10000 == 0) {
        println(s"docs processed: $processedDocs")
      }
    }
    if (friendsBulk.nonEmpty) flushFriendsBulk(currentProfile.get, friendsBulk, writer, nameFT)

    writer.forceMerge(1)
    writer.close()

    println(s"total docs processed: $processedDocs")
    println(s"total friends processed: $processedFriends")
  }
}
