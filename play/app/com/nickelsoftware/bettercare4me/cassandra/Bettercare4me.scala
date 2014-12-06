/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.cassandra

import java.io.FileReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import scala.collection.JavaConversions._
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
//import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.ExecutionContext.Implicits.global 
import com.datastax.driver.core.BoundStatement
import com.nickelsoftware.bettercare4me.utils.cassandra.resultset._
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.ProviderParser
import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.models.ClaimParser
import com.datastax.driver.core.BatchStatement
import org.joda.time.DateTime
import com.datastax.driver.core.PreparedStatement


/**
 * Class managing a connection to Cassandra cluster and 
 * session to keyspace using configuration file
 * 
 * Default config file name: "data/cassandra.yaml"
 */
class Cassandra(fname: String = "data/cassandra.yaml") {

  val config = loadConfig
  def node = config.getOrElse("node", "127.0.0.1").asInstanceOf[String]

  val cluster = Cluster.builder().addContactPoint(node).build()
  log(cluster.getMetadata)
  
  val session = cluster.connect(config.getOrElse("keyspace", "bettercare4me").asInstanceOf[String])
  Logger.info(s"Session connected to keyspace: ${session.getLoggedKeyspace()}")

  private def log(metadata: Metadata): Unit = {
    Logger.info(s"Connected to cluster: ${metadata.getClusterName} using $fname")
    for (host <- metadata.getAllHosts()) {
      Logger.info(s"Datatacenter: ${host.getDatacenter()}; Host: ${host.getAddress()}; Rack: ${host.getRack()}")
    }
  }

  private def loadConfig(): Map[String, Object] = {
    val yaml = new Yaml(new SafeConstructor());
    yaml.load(new FileReader(fname)).asInstanceOf[java.util.Map[String, Object]].toMap
  }
}

/**
 * Class to handle Bettercare4me data access, 
 * wrapper class around Cassandra connection class
 * 
 * Local class that manage the data access.
 */
protected[cassandra] class Bc4me(cassandra: Cassandra) {

  // prepared statements
  private val queryPatientsStmt = cassandra.session.prepare("SELECT data FROM patients WHERE batch_id = ?")
  private val queryProvidersStmt = cassandra.session.prepare("SELECT data FROM providers WHERE batch_id = ?")
  private val queryClaimsStmt = cassandra.session.prepare("SELECT data FROM claims_patients WHERE batch_id = ?")

  private val insertPatientsStmt = cassandra.session.prepare("INSERT INTO patients (batch_id, id, data) VALUES (?, ?, ?)")
  private val insertProvidersStmt = cassandra.session.prepare("INSERT INTO providers (batch_id, id, data) VALUES (?, ?, ?)")
  private val insertClaims1Stmt = cassandra.session.prepare("INSERT INTO claims_patients (batch_id, id, patient_id, dos, data) VALUES (?, ?, ?, ?, ?)")
  private val insertClaims2Stmt = cassandra.session.prepare("INSERT INTO claims_providers (batch_id, id, provider_id, dos, data) VALUES (?, ?, ?, ?, ?)")
  
  /**
   * execute provided query, can be used for testing to initialize database
   */
  def execute(s: String) = {
    cassandra.session.execute(s)
  }
  
  /**
   * Get all patients by batch_id
   */
  def queryPatients(batchId: Int): Future[Iterable[Patient]] = {
    val future: ResultSetFuture = cassandra.session.executeAsync(new BoundStatement(queryPatientsStmt).bind(batchId: java.lang.Integer))
    
    // use the implicit conversion of ResultSetFuture into Future[ResultSet] using the import cassandra.resultser._ above
    // the convert the ResultSet into List[Row] using ResultSet.all()
    future.map(_.all().map( row => PatientParser.fromList(row.getList("data", classOf[String]).toList)))
  }

  /**
   * Get all providers by batch_id
   */
  def queryProviders(batchId: Int): Future[Iterable[Provider]] = {
    val future: ResultSetFuture = cassandra.session.executeAsync(new BoundStatement(queryProvidersStmt).bind(batchId: java.lang.Integer))
    future.map(_.all().map( row => ProviderParser.fromList(row.getList("data", classOf[String]).toList)))
  }

  /**
   * Get all claims by batch_id
   */
  def queryClaims(batchId: Int): Future[Iterable[Claim]] = {
    val future: ResultSetFuture = cassandra.session.executeAsync(new BoundStatement(queryClaimsStmt).bind(batchId: java.lang.Integer))
    future.map(_.all().map( row => ClaimParser.fromList(row.getList("data", classOf[String]).toList)))
  }
  
  /**
   * Batch insert into patients table
   * Turns out it's better to loop on each items than to batch them
   * 
   * INSERT INTO patients (batch_id, id, data) VALUES (?, ?, ?)
   */
  def batchPatients(batchId: Int, patients: List[Patient]) = {
    patients foreach { p => cassandra.session.executeAsync(insertPatientsStmt.bind(batchId: java.lang.Integer, p.patientID, p.toList: java.util.List[String])) }
  }
  
  /**
   * Batch insert into providers table
   * 
   * INSERT INTO providers (batch_id, id, data) VALUES (?, ?, ?)
   */
  def batchProviders(batchId: Int, providers: List[Provider]) = {
    providers foreach { p => cassandra.session.executeAsync(insertProvidersStmt.bind(batchId: java.lang.Integer, p.providerID, p.toList: java.util.List[String])) }
  }
  
  /**
   * Batch insert into claims by patient table
   * 
   * INSERT INTO claims_patients (batch_id, id, patient_id, dos, data) VALUES (?, ?, ?, ?, ?)
   */
  def batchClaimsByPatients(batchId: Int, claims: List[Claim]) = {
    claims foreach { c => cassandra.session.executeAsync(insertClaims1Stmt.bind(batchId: java.lang.Integer, c.claimID, c.patientID, c.date.toDate(), c.toList: java.util.List[String])) }
  }
  
  /**
   * Batch insert into claims by provider table
   * 
   * INSERT INTO claims_providers (batch_id, id, provider_id, dos, data) VALUES (?, ?, ?, ?, ?)
   */
  def batchClaimsByProviders(batchId: Int, claims: List[Claim]) = {
    claims foreach { c => cassandra.session.executeAsync(insertClaims2Stmt.bind(batchId: java.lang.Integer, c.claimID, c.providerID, c.date.toDate(), c.toList: java.util.List[String])) }
  }
  
  def close = {
    cassandra.session.close()
    cassandra.cluster.close()
  }
}

/**
 * Object to maintain single connection to Cassandra for the current application
 */
object Bettercare4me {
  
  private var bc4me: Option[Bc4me] = None
  
  /**
   * Connect to Cassandra cluster and open session to keyspace
   * based on config file
   * 
   * Default config file name: "data/cassandra.yaml"
   */
  def connect(fname: String = "data/cassandra.yaml") = {
    bc4me = Some(new Bc4me(new Cassandra(fname)))
  }
  
  /**
   * Get all patients by batch_id
   */
  def queryPatients(batchId: Int): Future[Iterable[Patient]] = {
    bc4me match {
      case Some(c) => c.queryPatients(batchId)
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }

  /**
   * Get all providers by batch_id
   */
  def queryProviders(batchId: Int): Future[Iterable[Provider]] = {
    bc4me match {
      case Some(c) => c.queryProviders(batchId)
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }

  /**
   * Get all claims by batch_id
   */
  def queryClaims(batchId: Int): Future[Iterable[Claim]] = {
    bc4me match {
      case Some(c) => c.queryClaims(batchId)
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }
  
  /**
   * Batch insert into patients table
   */
  def batchPatients(batchId: Int, patients: List[Patient]) = {
    bc4me match {
      case Some(c) => c.batchPatients(batchId, patients)
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }
  
  /**
   * Batch insert into providers table
   */
  def batchProviders(batchId: Int, providers: List[Provider]) = {
    bc4me match {
      case Some(c) => c.batchProviders(batchId, providers)
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }
  
  /**
   * Batch insert into claims_patients table
   */
  def batchClaimsByPatients(batchId: Int, claims: List[Claim]) = {
    bc4me match {
      case Some(c) => c.batchClaimsByPatients(batchId, claims)
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }
  
  /**
   * Batch insert into claims_providers table
   */
  def batchClaimsByProviders(batchId: Int, claims: List[Claim]) = {
    bc4me match {
      case Some(c) => c.batchClaimsByProviders(batchId, claims)
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }
  
  def close = {
    bc4me match {
      case Some(c) => c.close
      case _ => Logger.warn("Bettercare4me: NOTHING TO CLOSE HERE!!!")
    }
    bc4me = None
  }
}