package com.digsolab.papers.matching

import java.io.{BufferedReader, StringReader}

import org.apache.hadoop.io.{LongWritable, NullWritable, Text}
import org.apache.hadoop.mrunit.mapreduce.MapReduceDriver
import org.apache.lucene.store.RAMDirectory
import org.scalatest.FunSuite

class MatchProfilesByFriendsJobSuite extends FunSuite {
  val synonyms =
    """
      |ira, irina
      |volodya, vladimir
    """.stripMargin.trim

  val indexSource =
    s"""
      |{"info":{ "id":100000000004370, "name": "Владимир", "secName": "Шеменков"}, "friends":[{"id": 100001400487895, "name": "ира", "secName":"гуда"}, {"id": 100000916647130, "name": "Anna", "secName": "Tilniak"}, {"id": 100000911217004, "name": "Анастасия", "secName": "Звягинцева"}, {"id": 1638165763, "name": "Lilya", "secName": "Zabolotnaya"}]}
      |{"info":{ "id":100000000004371, "name": "Владимир", "secName": "Шеменков"}, "friends":[]}
    """.stripMargin.trim

  val matchSource =
    s"""
       |{"_id":"1","info":{"name":"Володя","secName":"Шеменков"},"friends":[{"name":"Ирина","secName":"Гуда"},{"name":"Анна","secName":"Тильняк"},{"name":"Анастасия","secName":"Звягинцева"},{"name":"Лилия","secName":"Заболотная"}]}
     """.stripMargin.trim

  test("mapreduce") {
    val dir = new RAMDirectory()
    FbProfileFriendsIndexBuilder.buildIndex(
      dir,
      new StringReader(synonyms),
      FbProfileFriendsIndexBuilder.jsonRecordsIt(new BufferedReader(new StringReader(indexSource)))
    )

    val driver = MapReduceDriver.newMapReduceDriver(
      new MatchProfilesByFriendsMapper(dir),
      new MatchProfilesByFriendsReducer
    ).withInput(
      new LongWritable(1),
      new Text(matchSource)
    ).withOutput(
      NullWritable.get(),
      new Text("1\t100000000004370\tvolodya shemenkov\tvladimir shemenkov\t4.0|1\t100000000004371\tvolodya shemenkov\tvladimir shemenkov\t0.0")
    )

    driver.getConfiguration.setFloat("dist.threshold.name", 0.5F)
    driver.getConfiguration.setFloat("dist.threshold.secname", 0.6F)

    driver.runTest()
  }
}
