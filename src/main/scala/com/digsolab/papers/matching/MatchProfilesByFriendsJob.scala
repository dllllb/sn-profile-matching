package com.digsolab.papers.matching

import java.io.{DataInput, DataOutput, File}
import java.lang.Iterable

import com.digsolab.papers.util.{TypedJob, RichReducer, RichMapper}
import org.apache.hadoop.io.{LongWritable, NullWritable, Text, Writable}
import org.apache.hadoop.util.StringUtils
import org.apache.lucene.index._
import org.apache.lucene.search._
import org.apache.lucene.search.spell.LevensteinDistance
import org.apache.lucene.store.{Directory, MMapDirectory}

import scala.collection.JavaConverters._

case class MatchData(var rightId: String,
                     var score: Double,
                     var leftInfo: ProfileName,
                     var rightInfo: ProfileName) extends Writable {
  def this() = this("", 0, null, null)

  def write(out: DataOutput) {
    out.writeUTF(rightId)
    out.writeDouble(score)
    out.writeUTF(leftInfo.name)
    out.writeUTF(leftInfo.secName)
    out.writeUTF(rightInfo.name)
    out.writeUTF(rightInfo.secName)
  }

  def readFields(in: DataInput) {
    rightId = in.readUTF()
    score = in.readDouble()
    leftInfo = ProfileName(in.readUTF(), in.readUTF())
    rightInfo = ProfileName(in.readUTF(), in.readUTF())
  }
}

class DistCacheMatchProfilesByFriendsMapper extends RichMapper[LongWritable, Text, Text, MatchData] {
  override def run(context: MapperContext) {
    val paths = StringUtils.stringToPath(context.getConfiguration.getStrings("mapred.cache.localArchives"))
    val path = paths.find(_.getName.contains("index")).getOrElse(
      throw new RuntimeException("index is not found in distributed cache")
    )
    val dir = new MMapDirectory(new File(path.toString))
    new MatchProfilesByFriendsMapper(dir).run(context)
  }
}

class MatchProfilesByFriendsMapper(val dir: Directory)
  extends RichMapper[LongWritable, Text, Text, MatchData] {

  var profileMatcher: LuceneIndexProfileMatcher = _
  var reader: IndexReader = _
  var index: IndexSearcher = _

  override def setup(context: MapperContext) {
    reader = DirectoryReader.open(dir)
    index = new IndexSearcher(reader)
    val nameDistThreshold = context.getConfiguration.getFloat("dist.threshold.name", 0.6F)
    val secNameDistThreshold = context.getConfiguration.getFloat("dist.threshold.secname", 0.8F)
    profileMatcher = new LuceneIndexProfileMatcher(
      reader,
      new LevensteinDistance,
      nameDistThreshold,
      secNameDistThreshold,
      10000,
      3
    )
  }

  override def map(key: LongWritable, value: Text, context: MapperContext) {
    val profile = ProfileRecord.fromJson(value.toString)
    val (matches, foundCandidates) = profileMatcher.findMatches(profile)

    context.getCounter("STAT", "found candidates").increment(foundCandidates)

    matches foreach { case (score, candidate) =>
      context.write(
        new Text(profile._id),
        new MatchData(candidate._id, score, profile.info, candidate.info)
      )
    }
  }

  override def cleanup(context: MapperContext) {
    reader.close()
  }
}

class MatchProfilesByFriendsReducer extends RichReducer[Text, MatchData, NullWritable, Text] {
  override def safeReduce(key: Text, values: Iterable[MatchData], context: ReducerContext) {
    val leftPid = key.toString
    val matches = values.asScala.map(_.copy()).toSeq.sortBy(_.score).reverse.map { (md) =>
      Array(leftPid, md.rightId, md.leftInfo, md.rightInfo, md.score).mkString("\t")
    }.mkString("|")

    context.write(NullWritable.get(), new Text(matches))
  }
}

object MatchProfilesByFriendsJob extends TypedJob[LongWritable, Text, Text, MatchData, NullWritable, Text] {
  override val reducer = classOf[MatchProfilesByFriendsReducer]
  override val mapper  = classOf[DistCacheMatchProfilesByFriendsMapper]
}