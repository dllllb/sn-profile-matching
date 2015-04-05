package com.digsolab.papers.util

import org.scalatest.FunSuite


class NameUtilsTestSuite extends FunSuite {

    /* Parsing first name / last name pairs tests */

    test("frequent name and surname direct order") {
      val fullName = ("Иван", "Петров")
      val (name, lastName) = NameUtils.parseNameSecNamePair(fullName._1, fullName._2)
      assert(name == "Иван")
      assert(lastName == "Петров")
    }

    test("frequent name and surname reverse order") {
      val fullName = ("Петров", "Иван")
      val (name, lastName) = NameUtils.parseNameSecNamePair(fullName._1, fullName._2)
      assert(name == "Иван")
      assert(lastName == "Петров")
    }

    test("Reverse order + actual name appears in both dicts") {
      val fullName = ("Казакова", "Ольга")
      val (name, lastName) = NameUtils.parseNameSecNamePair(fullName._1, fullName._2)
      assert(name == "Ольга")
      assert(lastName == "Казакова")
    }

    test("infrequent name and surname") {
      val fullName = ("Иван", "Петров")
      val (name, lastName) = NameUtils.parseNameSecNamePair(fullName._1, fullName._2)
      assert(name == "Иван")
      assert(lastName == "Петров")
    }

    test("Name used as both name and surname") {
      val fullName = ("Иван", "Иван")
      val (name, lastName) = NameUtils.parseNameSecNamePair(fullName._1, fullName._2)
      assert(name == "Иван")
      assert(lastName == "Иван")
    }

    test("Surname used as both name and surname") {
      val fullName = ("Петров", "Петров")
      val (name, lastName) = NameUtils.parseNameSecNamePair(fullName._1, fullName._2)
      assert(name == "Петров")
      assert(lastName == "Петров")
    }

    test("Both name and surname are not found in any of dicts") {
      val fullName = ("Синтетическое_имя", "Синтетическая_фамилия")
      val (name, lastName) = NameUtils.parseNameSecNamePair(fullName._1, fullName._2)
      assert(name == "Синтетическое_имя")
      assert(lastName == "Синтетическая_фамилия")
    }

    test("Name not present in names/surnames dict, surname in the wrong position") {
      val fullName = ("Петров", "Синтетическое_имя")
      val (name, lastName) = NameUtils.parseNameSecNamePair(fullName._1, fullName._2)
      assert(name == "Синтетическое_имя")
      assert(lastName == "Петров")
    }

    test("Name in the wrong position, the word in the first position is not present in any of the dicts") {
      val fullName = ("Мэ", "Bерик")
      val (name, lastName) = NameUtils.parseNameSecNamePair(fullName._1, fullName._2)
      assert(name == "Bерик")
      assert(lastName == "Мэ")
    }

    test("Name in the wrong position, empty lastName") {
      val fullName = ("", "Bерик")
      val (name, lastName) = NameUtils.parseNameSecNamePair(fullName._1, fullName._2)
      assert(name == "Bерик")
      assert(lastName == "")
    }


    /* Matching last names tests */

    test("Matching lastnames method successfully matches two equal cyrillic last names") {
      val firstLastName = "Золотарева"
      val secondLastName = "золотарева"
      val areMatched = NameUtils.matchingLastNames(firstLastName, secondLastName)
      assert(areMatched)
    }

    test("Matching lastnames method successfully matches two cyrillic and latin version of a last name") {
      val firstLastName = "Золотарева"
      val secondLastName = "zolotareva"
      val areMatched = NameUtils.matchingLastNames(firstLastName, secondLastName)
      assert(areMatched)
    }

    test("Matching lastnames method successfully matches two last names with difference within levenshtein distance") {
      val firstLastName = "Schepeleva"
      val secondLastName = "shepeleva"
      val areMatched = NameUtils.matchingLastNames(firstLastName, secondLastName)
      assert(areMatched)
    }

    test("Matching lastnames method doesnt match unequal cyrillic last names") {
      val firstLastName = "Золотарева"
      val secondLastName = "Иванова"
      val areMatched = NameUtils.matchingLastNames(firstLastName, secondLastName)
      assert(!areMatched)
    }

    test("Matching lastnames diference out of levenshtein distance bounds") {
      val firstLastName = "Золотарева"
      val secondLastName = "Золотаревов"
      val areMatched = NameUtils.matchingLastNames(firstLastName, secondLastName)
      assert(!areMatched)
    }

    test("Matching lastnames method doesnt match unequal cyrillic and latin last names") {
      val firstLastName = "Золотарева"
      val secondLastName = "ivanova"
      val areMatched = NameUtils.matchingLastNames(firstLastName, secondLastName)
      assert(!areMatched)
    }
}
