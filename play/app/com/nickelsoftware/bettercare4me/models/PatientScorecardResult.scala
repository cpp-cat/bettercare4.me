/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models

import org.joda.time.DateTime
import com.nickelsoftware.bettercare4me.hedis.HEDISRule
import com.nickelsoftware.bettercare4me.utils.NickelException
import org.joda.time.LocalDate
import com.github.tototoshi.csv.CSVParser
import com.github.tototoshi.csv.CSVWriter
import java.io.StringWriter
import com.nickelsoftware.bettercare4me.hedis.Scorecard
import com.nickelsoftware.bettercare4me.hedis.RuleScore
import com.nickelsoftware.bettercare4me.hedis.RuleCriteriaScore


/**
 * Object to create PatientScorecardResult from Patient and Scorecard
 */
object PatientScorecardResult {
  
  /**
   * Mapping (Patient, Scorecard) to PatientScorecardResult
   */
  def apply(patient: Patient, scorecard: Scorecard) : PatientScorecardResult = {
    val scorecardResult = scorecard.hedisRuleMap map { case (name, ruleScore) => (name, RuleResult(ruleScore))}
    PatientScorecardResult(patient, scorecardResult)
  }
  
}


/**
 * Patient scorecard restored back from Cassandra
 * 
 * This class is the equivalent of Patient + Scorecard, however restored from Cassandra rather than computed from raw claims
 */
case class PatientScorecardResult(patient: Patient, scorecardResult: Map[String, RuleResult]=Map()) {
  
  def addRuleResult(ruleName: String, criteriaName: String, isCriteriaMet: Boolean, criteriaScore: List[String]): PatientScorecardResult = {
    val ruleResult = scorecardResult.getOrElse(ruleName, RuleResult.emptyRuleResult)
    PatientScorecardResult(patient, scorecardResult + (ruleName -> ruleResult.addCriteriaResult(criteriaName, isCriteriaMet, criteriaScore)))
  }
}

/**
 * Utility object
 */
object RuleResult {
  val emptyRuleResult = RuleResult(CriteriaResult.emptyCriteriaResult, CriteriaResult.emptyCriteriaResult, CriteriaResult.emptyCriteriaResult)
  
  /**
   * Mapping from RuleScore (which came from Scorecard) to RuleResult
   */
  def apply(ruleScore: RuleScore): RuleResult = {
    RuleResult(CriteriaResult(ruleScore.eligible), CriteriaResult(ruleScore.excluded), CriteriaResult(ruleScore.meetMeasure))
  }
}

/**
 * Holds the criteria results for a rule
 */
case class RuleResult(eligibleResult: CriteriaResult, excludedResult: CriteriaResult, meetMeasureResult: CriteriaResult) {
  
  def addCriteriaResult(criteriaName: String, isCriteriaMet: Boolean, criteriaScore: List[String]): RuleResult = {
    criteriaName match {
      case HEDISRule.eligible => RuleResult(CriteriaResult(isCriteriaMet, criteriaScore map { s =>  CriteriaResultDetail(s)}), excludedResult, meetMeasureResult)
      case HEDISRule.excluded => RuleResult(eligibleResult, CriteriaResult(isCriteriaMet, criteriaScore map { s =>  CriteriaResultDetail(s)}), meetMeasureResult)
      case HEDISRule.meetMeasure => RuleResult(eligibleResult, excludedResult, CriteriaResult(isCriteriaMet, criteriaScore map { s =>  CriteriaResultDetail(s)}))
      case _ => throw NickelException("RuleResult.addCriteriaResult: Unknown criteriaName: " + criteriaName)
    }
  }
}


object CriteriaResult {
  val emptyCriteriaResult = CriteriaResult(false, List())
  
  /**
   * Mapping from RuleCriteriaScore (which came from Scorecard) to CriteriaResult
   * 
   * @TODO Need to get the provider first and last name from providerID
   */
  def apply(ruleCriteriaScore: RuleCriteriaScore): CriteriaResult = {
    val criteriaResultReasons = ruleCriteriaScore.criteriaScore.toList flatMap { case (reason, claimList) => 
      claimList map { c => CriteriaResultDetail(c.claimID, c.providerID, c.providerID, c.date, reason) }
    }
    CriteriaResult(ruleCriteriaScore.isCriteriaMet, criteriaResultReasons)
  }
}

/**
 * Rule criteria result with details
 */
case class CriteriaResult(isCriteriaMet: Boolean, criteriaResultReasons: List[CriteriaResultDetail])

object CriteriaResultDetail {
  
  def apply(s: String): CriteriaResultDetail = {
    CSVParser.parse(s, '\\', ',', '"') match {
      case Some(l) => CriteriaResultDetail(l(0), l(1), l(2), LocalDate.parse(l(3)).toDateTimeAtStartOfDay(), l(4))
      case None => throw NickelException("CriteriaResultDetail: Invalid CriteriaResultDetail string representation: " + s)
    } 
  }
}

/**
 * Particular details of a criteria result, ties to the claims meeting the conditions of that criteria
 */
case class CriteriaResultDetail(claimId: String, providerLName: String, providerFName: String, dos: DateTime, reason: String) {
  
  def toCSVString: String = {
    var strWriter = new StringWriter()
    val writer = CSVWriter.open(strWriter)
    writer.writeRow(List(claimId, providerLName, providerFName, dos.toLocalDate().toString, reason))
    writer.flush
    strWriter.toString()
  }
}
