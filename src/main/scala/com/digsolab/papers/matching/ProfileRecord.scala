package com.digsolab.papers.matching

import com.digsolab.papers.util.{NameUtils, Romanization, Jackson}

case class ProfileName(name: String, secName: String) {
  override def toString = s"$name $secName"
}

object ProfileRecord {
  def fromJson(str: String) = {
    val rawProfile = Jackson.defaultMapper.readValue(str, classOf[ProfileRecord])
    val ProfileRecord(pid, ProfileName(rawName, rawSecName), rawFriends) = rawProfile
    val friends = rawFriends.map { (pn) =>
      ProfileName(Romanization.normalize(pn.name), Romanization.normalize(pn.secName))
    }

    val info = ProfileName(Romanization.normalize(rawName), Romanization.normalize(rawSecName))
    ProfileRecord(pid, info, friends)
  }
}

case class ProfileRecord(_id: String,
                         info: ProfileName,
                         var friends: Seq[ProfileName]
) {
  if (friends == null) friends = Seq()
}

case class Person(id: Long, name: String, secName: String)

case class MailruPerson(id: String, name: String, secName: String)

object ProfileFriends {
  def fromJson(str: String) = {
    val rawProfile = Jackson.defaultMapper.readValue(str, classOf[ProfileFriends])
    val ProfileFriends(info, friends) = rawProfile
    val romFriends = friends.map { (p) =>
      val orderedName = NameUtils.parseNameSecNamePair(p.name, p.secName)
      p.copy(
        name = Romanization.normalize(orderedName._1),
        secName = Romanization.normalize(orderedName._2)
      )
    }
    val orderedName = NameUtils.parseNameSecNamePair(info.name, info.secName)
    val romInfo = info.copy(
      name = Romanization.normalize(orderedName._1),
      secName = Romanization.normalize(orderedName._2)
    )
    ProfileFriends(romInfo, romFriends)
  }
}

case class ProfileFriends(info: Person, friends: Seq[Person])

case class MailruProfileFriend(info: MailruPerson, friend: MailruPerson)