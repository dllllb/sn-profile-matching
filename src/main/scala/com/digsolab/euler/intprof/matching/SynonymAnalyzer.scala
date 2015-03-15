package com.digsolab.euler.intprof.matching

import org.apache.lucene.analysis.Analyzer
import java.io.Reader
import org.apache.lucene.analysis.synonym.{SynonymFilter, SolrSynonymParser}
import org.apache.lucene.analysis.core.{KeywordAnalyzer, KeywordTokenizer}
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents

class SynonymAnalyzer(synonymFileReader: => Reader) extends Analyzer {
  def createComponents(fieldName: String, reader: Reader) = {
    val parser = new SolrSynonymParser(true, true, new KeywordAnalyzer)
    parser.parse(synonymFileReader)
    val synonymMap = parser.build()

    val tokenizer = new KeywordTokenizer(reader)
    val ts = new SynonymFilter(tokenizer, synonymMap, false)

    new TokenStreamComponents(tokenizer, ts)
  }
}
