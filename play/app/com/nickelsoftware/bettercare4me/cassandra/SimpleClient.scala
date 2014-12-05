/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.cassandra

import com.datastax.driver.core.Cluster
import scala.collection.JavaConversions._
import play.api.Logger
import com.datastax.driver.core.Metadata
import com.nickelsoftware.bettercare4me.models.Patient
import scala.concurrent.Future
import com.datastax.driver.core.ResultSetFuture
import com.datastax.driver.core.ResultSet
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import com.nickelsoftware.bettercare4me.utils.NickelException
import com.nickelsoftware.bettercare4me.models.PatientParser

class SimpleClient(node: String) {
  
  private val cluster = Cluster.builder().addContactPoint(node).build()
  log(cluster.getMetadata)
  
  val session = cluster.connect()
  val patientsStmt = session.prepare("SELECT id, data FROM bettercare4me.patients")
  
  private def log(metadata: Metadata): Unit = {
    Logger.info(s"Connected to cluster: ${metadata.getClusterName}")
    for (host <- metadata.getAllHosts()) {
      Logger.info(s"Datatacenter: ${host.getDatacenter()}; Host: ${host.getAddress()}; Rack: ${host.getRack()}")
    }
  }
  
  def queryPatients: Iterable[Patient] = {
    
    val future: ResultSetFuture = session.executeAsync("SELECT id, data FROM bettercare4me.patients")
   try {
       val result: ResultSet = future.get(10, TimeUnit.SECONDS)
       
       val patients = for ( row <- result) yield PatientParser.fromList(row.getList("data", classOf[String]).toList)
       
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