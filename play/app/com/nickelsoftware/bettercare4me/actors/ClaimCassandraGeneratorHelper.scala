/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.actors

import scala.collection.mutable.HashMap
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Random

import com.nickelsoftware.bettercare4me.cassandra.Bettercare4me
import com.nickelsoftware.bettercare4me.hedis.HEDISRule
import com.nickelsoftware.bettercare4me.hedis.HEDISRules
import com.nickelsoftware.bettercare4me.hedis.HEDISScoreSummary
import com.nickelsoftware.bettercare4me.hedis.Scorecard
import com.nickelsoftware.bettercare4me.models.ClaimGeneratorConfig
import com.nickelsoftware.bettercare4me.models.PatientHistoryFactory
import com.nickelsoftware.bettercare4me.models.PatientScorecardResult
import com.nickelsoftware.bettercare4me.models.PersonGenerator
import com.nickelsoftware.bettercare4me.models.SimplePersistenceLayer
import com.nickelsoftware.bettercare4me.utils.NickelException
import com.nickelsoftware.bettercare4me.utils.cassandra.resultset.toFuture

/**
 * Class for generating patients, providers, and claims for a given \c igen generation
 *
 * @param config Simulation parameters
 */
case object ClaimCassandraGeneratorHelper extends ClaimGeneratorHelper {

  /**
   * Generate claims using simulation parameters from `config
   *
   * Generate the simulated `Patients, `Providers, and `Claims to Cassandra.
   * This simulator uses `SimplePersistenceLayer for created the entities `UUIDs
   *
   * @param igen Generation number (batch ID)
   * @param config the generator's configuration parameters
   */
  def generateClaims(igen: Int, configTxt: String): ClaimGeneratorCounts = {

    val config = ClaimGeneratorConfig.loadConfig(configTxt)
    
    // make sure Cassandra is ready to go
    Bettercare4me.connect

    // The persistence layer provides an abstraction level to the UUID generation
    val persistenceLayer = new SimplePersistenceLayer(igen)

    // Person generator class
    val personGenerator = new PersonGenerator(config.hedisDate, persistenceLayer)

    // create and configure the rules to use for the simulation
    val hedisDate = config.hedisDate
    val rules: List[HEDISRule] = config.rulesConfig.map { c => HEDISRules.createRuleByName(c.name, c, hedisDate) }.toList

    // generate the providers
    val providers = (for (i <- 1 to config.nbrProviders) yield personGenerator.generateProvider).toList

    // insert them into cassandra
    val f1 = Bettercare4me.batchProviders(igen, providers)

    // generate the patients
    val patients = (for (i <- 1 to config.nbrPatients) yield personGenerator.generatePatient).toList

    // insert them into cassandra
    val f2 = Bettercare4me.batchPatients(igen, patients)

    // generate the claims
    var simScores = HashMap[String, (Int, Int, Int)]()
    var nbrClaims = 0L
    val f3 = for {
      patient <- patients
    } yield {
      simScores.clear
      val claims = for {
        rule <- rules
        simScoreTpl = simScores.getOrElseUpdate(rule.config.simParityRuleName, (Random.nextInt(100), Random.nextInt(100), Random.nextInt(100)))
        claim <- rule.generateClaims(persistenceLayer, patient, getOne(providers), simScoreTpl._1, simScoreTpl._2, simScoreTpl._3)
      } yield {
        nbrClaims = nbrClaims + 1
        claim
      }
      Future.sequence(List(Bettercare4me.batchClaimsByPatients(igen, claims),
        Bettercare4me.batchClaimsByProviders(igen, claims)))
    }

    // make sure all the updates completes
    Await.ready(f1, Duration.Inf)
    Await.ready(f2, Duration.Inf)
    Await.ready(Future.sequence(f3), Duration.Inf)

    ClaimGeneratorCounts(patients.size.toLong, providers.size.toLong, nbrClaims)
  }

  def processGeneratedClaims(igen: Int, configTxt: String): HEDISScoreSummary = {

    val patientsFuture = Bettercare4me.queryPatients(igen)
    val providerFuture = Bettercare4me.queryProviders(igen)
    val claimsMapFuture = Bettercare4me.queryClaims(igen) map { c => c groupBy { _.patientID } }
    val config = ClaimGeneratorConfig.loadConfig(configTxt)

    // create and configure the rules to use for the simulation
    val hedisDate = config.hedisDate
    val rules: List[HEDISRule] = config.rulesConfig.map { c => HEDISRules.createRuleByName(c.name, c, hedisDate) }.toList

    val patientsHistoryFuture = for {
      patients <- patientsFuture
      claimsMap <- claimsMapFuture
    } yield patients map { p => (p, PatientHistoryFactory.createPatientHistory(p, claimsMap.getOrElse(p.patientID, List.empty).toList)) }

    // scorecardsFuture is a tuple keeping track of the thread execution (futures) as the first value, and the scorecard as the second
    // full type is: 
    //    val scorecardsFuture: scala.concurrent.Future[Iterable[(
    //      scala.concurrent.Future[List[Iterable[Unit.type]]], 
    //      com.nickelsoftware.bettercare4me.hedis.Scorecard
    //    )]]
    //    
    val scorecardsFuture = patientsHistoryFuture map (_ map {
      case (p, ph) =>
        val scorecard = rules.foldLeft(Scorecard())({ (scorecard, rule) => rule.scoreRule(scorecard, p, ph) })

        // insert into rule_scorecards the rule summary for this patient - Using flatMap to filter out the None and removing the Option type!!
        val f1 = scorecard.hedisRuleMap flatMap {
          case (n, rs) =>
            if (rs.meetDemographic.isCriteriaMet && rs.eligible.isCriteriaMet) Some(Bettercare4me.insertRuleScorecards(n, hedisDate, igen, p, rs.excluded.isCriteriaMet, rs.meetMeasure.isCriteriaMet))
            else None
        }

        // save patient scorecard to cassandra here, keep only the measure that the patient is eligible to
        val patientScorecardResult = PatientScorecardResult(p, scorecard)
        val f3 = Bettercare4me.insertPatientScorecardResult(igen, hedisDate, patientScorecardResult)
        val f4 = Future.sequence(List(Future.sequence(f1), f3))

        (f4, scorecard)
    })

    // fold all the patients scorecards into a HEDISScoreSummary and return it
    val f5 = scorecardsFuture map {
      _.foldLeft(HEDISScoreSummary(rules))({ (scoreSummary, fScorecard) =>
        fScorecard match {
          case (f, scorecard) =>
            Await.ready(f, Duration.Inf)
            scoreSummary.addScoreCard(scorecard)
        }
      })
    }

    Await.result(f5, Duration.Inf)
  }

  def saveHEDISScoreSummary(result: HEDISScoreSummary, configTxt: String): Unit = {

    // insert the HEDISScoreSummary into Cassandra
    val config = ClaimGeneratorConfig.loadConfig(configTxt)
    val hedisDate = config.hedisDate

    val p = result.persist
    val f1 = Bettercare4me.insertHEDISSummary(config.runName, config.hedisDate, p._1, p._2, configTxt)

    // Save in Cassandra HEDIS measures information and stats (rules_information table) based on RuleScoreSummary
    val f2 = Future.sequence(result.ruleScoreSummaries map {
      case (n, rss) =>
        Bettercare4me.insertRuleInformation(hedisDate, result.patientCount, rss)
    })

    Await.ready(f1, Duration.Inf)
    Await.ready(f2, Duration.Inf)
  }

  def paginateRuleScorecards(ruleName: String, configTxt: String): Long = {

    val config = ClaimGeneratorConfig.loadConfig(configTxt)
    val hedisDate = config.hedisDate

    // read the patients for ruleName
    val rowsFuture = Bettercare4me.queryRuleScorecard(ruleName, hedisDate)
    val f1 = rowsFuture map { rows =>
      var pageID: Long = 0
      var count = 0

      // keep track of the list of ( ResultSetFuture => Future[Unit] )
      val f2 = for (row <- rows) yield {
        if (count % 20 == 0) pageID = pageID + 1
        val ff = row match {
          case (batchID, patient, isExcluded, isMeetCriteria) =>
            val localPageID = pageID
            Bettercare4me.insertRuleScorecardsPaginated(ruleName, hedisDate, batchID, localPageID, patient, isExcluded, isMeetCriteria)
          case _ => throw NickelException("ClaimCassandraGeneratorHelper: Unexpected case value in paginateRuleScorecards")
        }
        count = count + 1
        ff
      }

      // f2 is a list of future, change it to a future of list
      val f4 = Future.sequence(f2)

      // upsert rule information table to indicate how many pages of patients this rule has
      val localPageID = pageID
      val f3 = Bettercare4me.insertRuleInformation(ruleName, hedisDate, localPageID)

      // make sure all insert threads are done when f1 is done
      Await.ready(f4, Duration.Inf)
      Await.ready(f3, Duration.Inf)
      localPageID
    }

    // make sure the query thread completes and return the number of pages for this rule
    Await.result(f1, Duration.Inf)
  }
}
