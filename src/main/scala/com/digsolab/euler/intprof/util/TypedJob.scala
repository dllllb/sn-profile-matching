package com.digsolab.euler.intprof.util

import org.apache.hadoop.util.{ToolRunner, Tool}
import org.apache.hadoop.conf.{Configuration, Configured}
import org.apache.hadoop.mapreduce._
import scala.reflect.ClassTag
import org.apache.hadoop.io.RawComparator

class TypedJob[MapKeyIn, MapValIn, MapKeyOut, MapValOut, ReduceKeyOut, ReduceValOut](
  implicit val mapKeyOut: ClassTag[MapKeyOut],
  implicit val mapValOut: ClassTag[MapValOut],
  implicit val reduceKeyOut: ClassTag[ReduceKeyOut],
  implicit val reduceValOut: ClassTag[ReduceValOut]
) extends Configured with Tool {

  val inputFormat: Class[_ <: InputFormat[MapKeyIn, MapValIn]] = null

  val outputFormat: Class[_ <: OutputFormat[ReduceKeyOut, ReduceValOut]] = null

  val mapper: Class[_ <: Mapper[MapKeyIn, MapValIn, MapKeyOut, MapValOut]] = null

  val reducer: Class[_ <: Reducer[MapKeyOut, MapValOut, ReduceKeyOut, ReduceValOut]] = null

  val combiner: Class[_ <: Reducer[MapKeyOut, MapValOut, MapKeyOut, MapValOut]] = null

  val partitioner: Class[_ <: Partitioner[MapKeyOut, MapValOut]] = null

  val groupingComparator: Class[_ <: RawComparator[_]] = null

  val sortComparator: Class[_ <: RawComparator[_]] = null

  def postConfigure(job: Job) {}

  def postExecute(job: Job) {}

  def run(args: Array[String]) = {
    val job = Job.getInstance(getConf)
    job.setJarByClass(this.getClass)

    Option(mapper).foreach(job.setMapperClass)
    Option(reducer).foreach(job.setReducerClass)
    Option(combiner).foreach(job.setCombinerClass)

    job.setMapOutputKeyClass(mapKeyOut.runtimeClass)
    job.setMapOutputValueClass(mapValOut.runtimeClass)
    job.setOutputKeyClass(reduceKeyOut.runtimeClass)
    job.setOutputValueClass(reduceValOut.runtimeClass)

    Option(inputFormat).foreach(job.setInputFormatClass)
    Option(outputFormat).foreach(job.setOutputFormatClass)

    Option(partitioner).foreach(job.setPartitionerClass)
    Option(groupingComparator).foreach(job.setGroupingComparatorClass)
    Option(sortComparator).foreach(job.setSortComparatorClass)

    postConfigure(job)

    if (!job.waitForCompletion(true)) {
      throw new Exception("Job failed!")
    }

    postExecute(job)

    0
  }

  def main(args: Array[String]) {
    System.exit(ToolRunner.run(new Configuration(), this, args))
  }
}
