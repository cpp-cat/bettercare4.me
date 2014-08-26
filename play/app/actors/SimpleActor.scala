/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package actors

import akka.actor.Actor
import akka.actor.ActorLogging

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

object SimpleActor {

  case class SimpleRequest(key: String)
  case class SimpleSparkRequest(fileName: String)
  case class SimpleResponse(data: String)
}

/**
 * Simple Actor to test project setup
 */
class SimpleActor() extends Actor with ActorLogging {

  import SimpleActor._
  def receive = {

    // Real simple request
    case SimpleRequest(key) =>
      log.info(s"SimpleActor: Received SimpleRequest, with key $key")
      sender ! SimpleResponse(key + " ... done!")

    // Request with simple Spark job to do
    case SimpleSparkRequest(fileName) =>
      log.info(s"SimpleActor: Received SimpleSparkRequest, with fileName $fileName")

      val logFile = "conf/application.conf"
      val conf = new SparkConf()
        .setMaster("local")
        .setAppName("Simple Spark Request")
      val sc = new SparkContext(conf)
      val data = sc.textFile(fileName, 2).cache()
      val numAs = data.filter(line => line.contains("a")).count()
      val numBs = data.filter(line => line.contains("b")).count()

      sender ! SimpleResponse(s"Lines with a: $numAs, Lines with b: $numBs")

  }

}