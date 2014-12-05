/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.cassandra

import java.io.FileReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.asScalaSet
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConversions.mapAsScalaMap
import scala.concurrent.Future
import scala.concurrent.future

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Metadata
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.ResultSetFuture
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientParser
import com.nickelsoftware.bettercare4me.utils.NickelException

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Object managing a connection to Cassandra
 */
object cassandra {

  val config = loadConfig
  def node = config.getOrElse("node", "127.0.0.1").asInstanceOf[String]

  val cluster = Cluster.builder().addContactPoint(node).build()
  log(cluster.getMetadata)

  private def log(metadata: Metadata): Unit = {
    Logger.info(s"Connected to cluster: ${metadata.getClusterName}")
    for (host <- metadata.getAllHosts()) {
      Logger.info(s"Datatacenter: ${host.getDatacenter()}; Host: ${host.getAddress()}; Rack: ${host.getRack()}")
    }
  }

  private def loadConfig(): Map[String, Object] = {
    val yaml = new Yaml(new SafeConstructor());
    yaml.load(new FileReader("data/cassandra.yaml")).asInstanceOf[java.util.Map[String, Object]].toMap
  }
}

/**
 * Object managing session to bettercare4me keyspace
 */
object Bettercare4me {

  val session = cassandra.cluster.connect("bettercare4me")

  // for later...
  val patientsStmt = session.prepare("SELECT id, data FROM bettercare4me.patients")

  def queryPatients: Future[Iterable[Patient]] = {

    future {
      val future: ResultSetFuture = session.executeAsync("SELECT id, data FROM patients")
      try {
        val result: ResultSet = future.get(10, TimeUnit.SECONDS)

        val patients = for (row <- result) yield PatientParser.fromList(row.getList("data", classOf[String]).toList)

        // That's it!!
        patients

      } catch {
        case ex: TimeoutException => {
          Logger.error("Oops, TimeoutException while querying patients from cassandra")
          future.cancel(true); // Ensure any resource used by this query driver
          throw NickelException("Oops, TimeoutException while querying patients from cassandra")
        }
      }
    }
  }
  
  def close = {
    session.close()
    cassandra.cluster.close()
  }

}