package com.digsolab.euler.intprof.util

import org.apache.hadoop.mapreduce.Mapper
import org.slf4j.LoggerFactory

class RichMapper[KeyIn, ValueIn, KeyOut, ValueOut] extends Mapper[KeyIn, ValueIn, KeyOut, ValueOut] {
  type MapperContext = Mapper[KeyIn, ValueIn, KeyOut, ValueOut]#Context

  protected val log = LoggerFactory.getLogger(this.getClass)

  override def map(key: KeyIn, value: ValueIn, context: MapperContext) {
    try {
      safeMap(key, value, context)
    } catch {
      case e: Exception =>
        log.warn(s"map execution failure: ${e.getMessage}", e)
        context.getCounter("STAT", "map execution failure").increment(1)
    }
  }

  def safeMap(key: KeyIn, value: ValueIn, context: MapperContext) {
    super.map(key, value, context)
  }
}
