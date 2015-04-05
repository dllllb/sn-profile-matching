package com.digsolab.papers.matching

import java.io.{BufferedReader, StringReader}

import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.search.spell.LevensteinDistance
import org.apache.lucene.store.RAMDirectory
import org.scalatest.FunSuite

class ProfileMatchSuite extends FunSuite {

  test("close match") {
    val synonyms =
      """
        |ira, irina
        |volodya, vladimir
      """.stripMargin.trim

    val indexSource =
      s"""
        |{"info":{ "id":100000000004370, "name": "Владимир", "secName": "Шеменков"}, "friends":[{"id": 100001400487895, "name": "ира", "secName":"гуда"}, {"id": 100000916647130, "name": "Anna", "secName": "Tilniak"}, {"id": 100000911217004, "name": "Анастасия", "secName": "Звягинцева"}, {"id": 1638165763, "name": "Lilya", "secName": "Zabolotnaya"}]}
      """.stripMargin.trim

    val dir = new RAMDirectory()
    FbProfileFriendsIndexBuilder.buildIndex(
      dir,
      new StringReader(synonyms),
      FbProfileFriendsIndexBuilder.jsonRecordsIt(new BufferedReader(new StringReader(indexSource)))
    )

    val reader = DirectoryReader.open(dir)
    val profileMatcher = new LuceneIndexProfileMatcher(reader, new LevensteinDistance, 0.5F, 0.7F, 10000, 3)

    val candidates = profileMatcher.findMatches(
      ProfileRecord.fromJson(
        """{"_id":"1","info":{"name":"Володя","secName":"Шеменков"},"friends":[{"name":"Ирина","secName":"Гуда"},{"name":"Анна","secName":"Тильняк"},{"name":"Анастасия","secName":"Звягинцева"},{"name":"Лилия","secName":"Заболотная"}]}"""
      )
    )
    assert(candidates._1.size > 0)
    assert(candidates._1.head._1 === 4)
    assert(candidates._1.head._2._id === "100000000004370")
  }
}
