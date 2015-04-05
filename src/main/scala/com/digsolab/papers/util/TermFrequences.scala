package com.digsolab.papers.util

import org.apache.lucene.index.Terms

/**
 * @author Dmitri Babaev (dmitri.babaev@gmail.com)
 */
object TermFrequences {
  def termFreqs(terms: Terms) = {
    val it = terms.iterator(null)
    val tfBuilder = Map.newBuilder[String, Long]
    while (it.next() != null) {
      tfBuilder += it.term().utf8ToString() -> it.totalTermFreq()
    }
    val termFreqs = tfBuilder.result()
    termFreqs
  }
}
