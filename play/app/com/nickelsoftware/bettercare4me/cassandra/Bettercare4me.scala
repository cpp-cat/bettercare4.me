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
import com.nickelsoftware.bettercare4me.models.PatientScorecardResult
import com.nickelsoftware.bettercare4me.models.CriteriaResult
import com.nickelsoftware.bettercare4me.hedis.HEDISScoreSummary
import com.nickelsoftware.bettercare4me.models.ClaimGeneratorConfig
import com.nickelsoftware.bettercare4me.hedis.HEDISRule
import com.nickelsoftware.bettercare4me.hedis.HEDISRules
import com.nickelsoftware.bettercare4me.hedis.RuleScoreSummary
import com.nickelsoftware.bettercare4me.hedis.HEDISRuleInfo

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

  // Summary tables
  private val queryHEDISSummaryStmt = cassandra.session.prepare("SELECT name, hedis_date, patient_count, score_summaries, claim_generator_config FROM hedis_summary LIMIT 1000")
  private val queryHEDISReportStmt = cassandra.session.prepare("SELECT patient_count, score_summaries, claim_generator_config FROM hedis_summary WHERE name = ? AND hedis_date = ?")
  private val insertHEDISSummaryStmt = cassandra.session.prepare("INSERT INTO hedis_summary (name, hedis_date, patient_count, score_summaries, claim_generator_config) VALUES (?, ?, ?, ?, ?)")

  private val insertRuleInformationStmt = cassandra.session.prepare("INSERT INTO rules_information (rule_name, hedis_date, full_name, description, patient_count, rule_score_summary) VALUES (?, ?, ?, ?, ?, ?)")
  private val queryRuleInformationStmt = cassandra.session.prepare("SELECT rule_name, hedis_date, full_name, description, patient_count, rule_score_summary FROM rules_information WHERE rule_name = ? AND hedis_date = ?")
    
  private val queryRuleScorecardStmt = cassandra.session.prepare("SELECT batch_id, patient_data, is_excluded, is_meet_criteria FROM rule_scorecards WHERE rule_name = ? AND hedis_date = ?")
  private val insertRuleScorecardStmt = cassandra.session.prepare("INSERT INTO rule_scorecards (rule_name, hedis_date, batch_id, patient_name, patient_id, patient_data, is_excluded, is_meet_criteria) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
    
  private val queryRuleScorecardPaginatedStmt = cassandra.session.prepare("SELECT batch_id, patient_data, is_excluded, is_meet_criteria FROM rule_scorecards_paginated WHERE rule_name = ? AND hedis_date = ? AND page_id = ?")
  private val insertRuleScorecardPaginatedStmt = cassandra.session.prepare("INSERT INTO rule_scorecards_paginated (rule_name, hedis_date, batch_id, page_id, patient_name, patient_id, patient_data, is_excluded, is_meet_criteria) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
  
  private val insertPatientScorecardResultStmt = cassandra.session.prepare("INSERT INTO patient_scorecards (batch_id, hedis_date, patient_id, patient_data, rule_name, is_eligible, eligible_score, is_excluded, excluded_score, is_meet_criteria, meet_criteria_score) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")

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
    future.map(_.all().map(row => PatientParser.fromList(row.getList("data", classOf[String]).toList)))
  }

  /**
   * Get all providers by batch_id
   */
  def queryProviders(batchId: Int): Future[Iterable[Provider]] = {
    val future: ResultSetFuture = cassandra.session.executeAsync(new BoundStatement(queryProvidersStmt).bind(batchId: java.lang.Integer))
    future.map(_.all().map(row => ProviderParser.fromList(row.getList("data", classOf[String]).toList)))
  }

  /**
   * Get all claims by batch_id
   */
  def queryClaims(batchId: Int): Future[Iterable[Claim]] = {
    val future: ResultSetFuture = cassandra.session.executeAsync(new BoundStatement(queryClaimsStmt).bind(batchId: java.lang.Integer))
    future.map(_.all().map(row => ClaimParser.fromList(row.getList("data", classOf[String]).toList)))
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

  /**
   * Query all HEDIS report summary
   */
  def queryHEDISSummary: Future[Iterable[(HEDISScoreSummary, String)]] = {
    val future: ResultSetFuture = cassandra.session.executeAsync(new BoundStatement(queryHEDISSummaryStmt).bind())
    future.map { rs =>
      rs.all() map { row =>
        val configTxt = row.getString("claim_generator_config")
        val config = ClaimGeneratorConfig.loadConfig(configTxt)
        val rules: List[HEDISRule] = config.rulesConfig.map { c => HEDISRules.createRuleByName(c.name, c, config.hedisDate) }.toList
        (HEDISScoreSummary(rules, row.getLong("patient_count"): Long, row.getList("score_summaries", classOf[String]).toList), configTxt)
      }
    }
  }

  /**
   * Query a specific HEDIS report
   */
  def queryHEDISReport(name: String, hedisDate: DateTime): Future[(HEDISScoreSummary, String)] = {
    val future: ResultSetFuture = cassandra.session.executeAsync(new BoundStatement(queryHEDISReportStmt).bind(name, hedisDate.toDate))
    future.map { rs =>
      val row = rs.one()
      val configTxt = row.getString("claim_generator_config")
      val config = ClaimGeneratorConfig.loadConfig(configTxt)
      val rules: List[HEDISRule] = config.rulesConfig.map { c => HEDISRules.createRuleByName(c.name, c, config.hedisDate) }.toList
      (HEDISScoreSummary(rules, row.getLong("patient_count"): Long, row.getList("score_summaries", classOf[String]).toList), configTxt)
    }
  }

  /**
   * HEDIS report summary
   */
  def insertHEDISSummary(name: String, hedisDate: DateTime, patientCount: Long, scoreSummaries: List[String], claimGeneratorConfig: String) = {
    cassandra.session.executeAsync(insertHEDISSummaryStmt.bind(name, hedisDate.toDate(), patientCount: java.lang.Long, scoreSummaries: java.util.List[String], claimGeneratorConfig))
  }
  
  /**
   * Insert the rule information based on RuleScoreSummary into rules_information table
   */
  def insertRuleInformation(hedisDate: DateTime, patientCount: Long, ruleScoreSummary: RuleScoreSummary) = {
    val ri = ruleScoreSummary.ruleInfo
    cassandra.session.executeAsync(insertRuleInformationStmt.bind(ri.name, hedisDate.toDate(), ri.fullName, ri.description, patientCount: java.lang.Long, ruleScoreSummary.toParseString))
  }
  
  /**
   * Return the rule information and stats for a hedis measure (RuleScoreSummary)
   */
  def queryRuleInformation(ruleName: String, hedisDate: DateTime): Future[(Long, RuleScoreSummary)] = {
    val future: ResultSetFuture = cassandra.session.executeAsync(new BoundStatement(queryRuleInformationStmt).bind(ruleName, hedisDate.toDate))
    future.map { rs =>
      val row = rs.one()
      val patientCount = row.getLong("patient_count"): Long
      val ruleScoreSummary = RuleScoreSummary(HEDISRuleInfo(ruleName, row.getString("full_name"), row.getString("description")), row.getString("rule_score_summary"))
      (patientCount, ruleScoreSummary)
    }
  }
  
  /**
   * Return the list of patients for a hedis measure (rule_scorecard table)
   */
  def queryRuleScorecard(ruleName: String, hedisDate: DateTime): Future[Iterable[(Int, Patient, Boolean, Boolean)]] = {
    val future = cassandra.session.executeAsync(new BoundStatement(queryRuleScorecardStmt).bind(ruleName, hedisDate.toDate))
    future.map { rs =>
      rs.all() map { row =>
        val patient = PatientParser.fromList(row.getList("patient_data", classOf[String]).toList)
        (row.getInt("batch_id"): Int, patient, row.getBool("is_excluded"): Boolean, row.getBool("is_meet_criteria"): Boolean)
      }
    }
  }

  /**
   * Insert a rule summary for patient (rule_scorecard table)
   */
  def insertRuleScorecards(ruleName: String, hedisDate: DateTime, batchID: Int, patient: Patient, isExcluded: Boolean, isMeetCriteria: Boolean) = {
    cassandra.session.executeAsync(insertRuleScorecardStmt.bind(ruleName, hedisDate.toDate(), batchID: java.lang.Integer, patient.lastName+", "+patient.firstName, patient.patientID, patient.toList: java.util.List[String], isExcluded: java.lang.Boolean, isMeetCriteria: java.lang.Boolean))
  }
  
  /**
   * Return the paginated list of patients for a hedis measure (rule_scorecard_paginated table)
   */
  def queryRuleScorecardPaginated(ruleName: String, hedisDate: DateTime, pageID: Long): Future[Iterable[(Int, Patient, Boolean, Boolean)]] = {
    val future = cassandra.session.executeAsync(new BoundStatement(queryRuleScorecardPaginatedStmt).bind(ruleName, hedisDate.toDate, pageID: java.lang.Long))
    future.map { rs =>
      rs.all() map { row =>
        val patient = PatientParser.fromList(row.getList("patient_data", classOf[String]).toList)
        (row.getInt("batch_id"): Int, patient, row.getBool("is_excluded"): Boolean, row.getBool("is_meet_criteria"): Boolean)
      }
    }
  }

  /**
   * Insert a rule summary for patient (rule_scorecard_paginated table)
   */
  def insertRuleScorecardsPaginated(ruleName: String, hedisDate: DateTime, batchID: Int, pageID: Long, patient: Patient, isExcluded: Boolean, isMeetCriteria: Boolean) = {
    cassandra.session.executeAsync(insertRuleScorecardPaginatedStmt.bind(ruleName, hedisDate.toDate(), batchID: java.lang.Integer, pageID: java.lang.Long, patient.lastName+", "+patient.firstName, patient.patientID, patient.toList: java.util.List[String], isExcluded: java.lang.Boolean, isMeetCriteria: java.lang.Boolean))
  }

  /**
   * Saving PatientScorecardResult, populating patient_scorecard table
   */
  def insertPatientScorecardResult(batchID: Int, hedisDate: DateTime, patientScorecardResult: PatientScorecardResult) = {
    def toList(cr: CriteriaResult): List[String] = cr.criteriaResultReasons map { _.toCSVString }

    val p = patientScorecardResult.patient
    patientScorecardResult.scorecardResult foreach {
      case (ruleName, rr) =>
        val el = rr.eligibleResult
        val ex = rr.excludedResult
        val mm = rr.meetMeasureResult
        cassandra.session.executeAsync(insertPatientScorecardResultStmt.bind(
          batchID: java.lang.Integer, hedisDate.toDate(), p.patientID, p.toList: java.util.List[String], ruleName,
          el.isCriteriaMet: java.lang.Boolean, toList(el): java.util.List[String],
          ex.isCriteriaMet: java.lang.Boolean, toList(ex): java.util.List[String],
          mm.isCriteriaMet: java.lang.Boolean, toList(mm): java.util.List[String]))
    }
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
   * This is called *only* by Global.onStart at application start.
   * Therefore the fact that it is no thread safe should not be an issue.
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

  /**
   * Query all HEDIS report summary
   */
  def queryHEDISSummary: Future[Iterable[(HEDISScoreSummary, String)]] = {
    bc4me match {
      case Some(c) => c.queryHEDISSummary
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }

  /**
   * Query a specific HEDIS report
   */
  def queryHEDISReport(name: String, hedisDate: DateTime): Future[(HEDISScoreSummary, String)] = {
    bc4me match {
      case Some(c) => c.queryHEDISReport(name, hedisDate)
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }

  /**
   * HEDIS report summary
   */
  def insertHEDISSummary(name: String, hedisDate: DateTime, patientCount: Long, scoreSummaries: List[String], claimGeneratorConfig: String) = {
    bc4me match {
      case Some(c) => c.insertHEDISSummary(name, hedisDate, patientCount, scoreSummaries, claimGeneratorConfig)
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }
  
  /**
   * Insert the rule information based on RuleScoreSummary into rules_information table
   */
  def insertRuleInformation(hedisDate: DateTime, patientCount: Long, ruleScoreSummary: RuleScoreSummary) = {
    bc4me match {
      case Some(c) => c.insertRuleInformation(hedisDate, patientCount, ruleScoreSummary)
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }
  
  /**
   * Return the rule information and stats for a hedis measure (RuleScoreSummary)
   */
  def queryRuleInformation(ruleName: String, hedisDate: DateTime): Future[(Long, RuleScoreSummary)] = {
    bc4me match {
      case Some(c) => c.queryRuleInformation(ruleName, hedisDate)
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }
  
  /**
   * Return the list of patients for a hedis measure
   */
  def queryRuleScorecard(ruleName: String, hedisDate: DateTime): Future[Iterable[(Int, Patient, Boolean, Boolean)]] = {
    bc4me match {
      case Some(c) => c.queryRuleScorecard(ruleName, hedisDate)
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }

  /**
   * Insert a rule summary for a patient
   */
  def insertRuleScorecards(ruleName: String, hedisDate: DateTime, batchID: Int, patient: Patient, isExcluded: Boolean, isMeetCriteria: Boolean) = {
    bc4me match {
      case Some(c) => c.insertRuleScorecards(ruleName, hedisDate, batchID, patient, isExcluded, isMeetCriteria)
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }
  
  /**
   * Return the paginated list of patients for a hedis measure (rule_scorecards_paginated table)
   */
  def queryRuleScorecardPaginated(ruleName: String, hedisDate: DateTime, pageID: Long): Future[Iterable[(Int, Patient, Boolean, Boolean)]] = {
    bc4me match {
      case Some(c) => c.queryRuleScorecardPaginated(ruleName, hedisDate, pageID)
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }

  /**
   * Insert a rule summary for a patient in paginated table (rule_scorecards_paginated table)
   */
  def insertRuleScorecardsPaginated(ruleName: String, hedisDate: DateTime, batchID: Int, pageID: Long, patient: Patient, isExcluded: Boolean, isMeetCriteria: Boolean) = {
    bc4me match {
      case Some(c) => c.insertRuleScorecardsPaginated(ruleName, hedisDate, batchID, pageID, patient, isExcluded, isMeetCriteria)
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }

  /**
   * Saving PatientScorecardResult, populating patient_scorecard table
   */
  def insertPatientScorecardResult(batchID: Int, hedisDate: DateTime, patientScorecardResult: PatientScorecardResult) = {
    bc4me match {
      case Some(c) => c.insertPatientScorecardResult(batchID, hedisDate, patientScorecardResult)
      case _ => throw NickelException("Bettercare4me: Connection to Cassandra not opened, must call Bettercare4me.connect once before use")
    }
  }
  /**
   * Closing the connection with Cassandra cluster
   *
   * This is called *only* by Global.onStop at application shutdown.
   * Therefore the fact that it is no thread safe should not be an issue.
   */
  def close = {
    bc4me match {
      case Some(c) => c.close
      case _ => Logger.warn("Bettercare4me: NOTHING TO CLOSE HERE!!!")
    }
    bc4me = None
  }
}