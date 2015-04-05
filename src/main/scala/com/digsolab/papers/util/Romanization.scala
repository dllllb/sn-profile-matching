package com.digsolab.papers.util

import com.ibm.icu.text.Transliterator

object Romanization {
  val transliterator = Transliterator.getInstance("Russian-Latin/BGN; Latin-ASCII")
  def normalize(str: String) = transliterator.transform(str.toLowerCase)
}
