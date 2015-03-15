package com.digsolab.euler.intprof.util

import com.fasterxml.jackson.databind.{SerializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object Jackson {
  val defaultMapper = new ObjectMapper
  defaultMapper.registerModule(DefaultScalaModule)
  defaultMapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false)
  defaultMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
}
