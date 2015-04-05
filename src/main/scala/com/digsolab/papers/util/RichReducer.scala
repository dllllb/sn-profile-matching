package com.digsolab.papers.util

import org.apache.hadoop.mapreduce.Reducer
import org.slf4j.LoggerFactory
import java.lang.Iterable

class RichReducer[KeyIn, ValueIn, KeyOut, ValueOut] extends Reducer[KeyIn, ValueIn, KeyOut, ValueOut] {
  type ReducerContext = Reducer[KeyIn, ValueIn, KeyOut, ValueOut]#Context

  protected val log = LoggerFactory.getLogger(this.getClass)

  override def reduce(key: KeyIn, values: Iterable[ValueIn], context: ReducerContext) {
    try {
      safeReduce(key, values, context)
    } catch {
      case e: Exception =>
        log.warn(s"reduce execution failure: ${e.getMessage}", e)
        context.getCounter("STAT", "reduce execution failure").increment(1)
    }
  }

  def safeReduce(key: KeyIn, values: Iterable[ValueIn], context: ReducerContext) {
    super.reduce(key, values, context)
  }
}
