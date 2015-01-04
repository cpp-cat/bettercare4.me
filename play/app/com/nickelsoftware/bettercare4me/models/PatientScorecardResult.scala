/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models

import java.io.StringWriter

import org.joda.time.DateTime
import org.joda.time.LocalDate

import com.github.tototoshi.csv.CSVParser
import com.github.tototoshi.csv.CSVWriter
import com.nickelsoftware.bettercare4me.hedis.HEDISRule
import com.nickelsoftware.bettercare4me.hedis.RuleCriteriaScore
import com.nickelsoftware.bettercare4me.hedis.RuleScore
import com.nickelsoftware.bettercare4me.hedis.Scorecard
import com.nickelsoftware.bettercare4me.utils.NickelException


/**
 * Object to create PatientScorecardResult from Patient and Scorecard
 */
object PatientScorecardResult {
  
  /**
   * Mapping (Patient, Scorecard) to PatientScorecardResult
   */
  def apply(patient: Patient, scorecard: Scorecard) : PatientScorecardResult = {
    val scorecardResult = scorecard.hedisRuleMap map { case (name, ruleScore) => (name, RuleResult(ruleScore))}
    
    // filter the eligible measure only
    val sr = scorecardResult filter { 
      case (name, ruleResult) => ruleResult.eligibleResult.isCriteriaMet
    }
    PatientScorecardResult(patient, sr)
  }
  
}


/**
 * Patient scorecard restored back from Cassandra
 * 
 * This class is the equivalent of Patient + Scorecard, however restored from Cassandra rather than computed from raw claims
 */
case class PatientScorecardResult(patient: Patient, scorecardResult: Map[String, RuleResult]=Map()) {
  
  def addRuleResult(ruleName: String, ruleFullName: String, criteriaName: String, isCriteriaMet: Boolean, criteriaScore: List[String]): PatientScorecardResult = {
    val ruleResult = scorecardResult.getOrElse(ruleName, RuleResult(ruleName, ruleFullName))
    PatientScorecardResult(patient, scorecardResult + (ruleName -> ruleResult.addCriteriaResult(criteriaName, isCriteriaMet, criteriaScore)))
  }  
  
  /**
   * Method used in Views to list rule results by categories (patientScorecard page)
   */
  def filterScorecardResults(ruleCategories: List[String]): List[RuleResult] = ruleCategories flatMap { n => scorecardResult.get(n) }
}

/**
 * Utility object
 */
object RuleResult {
  
  /**
   * Mapping from RuleScore (which came from Scorecard) to RuleResult
   */
  def apply(ruleScore: RuleScore): RuleResult = {
    RuleResult(ruleScore.ruleName, ruleScore.ruleFullName, CriteriaResult(ruleScore.eligible), CriteriaResult(ruleScore.excluded), CriteriaResult(ruleScore.meetMeasure))
  }
}

/**
 * Holds the criteria results for a rule
 */
case class RuleResult(
    ruleName: String, 
    ruleFullName: String,
    eligibleResult: CriteriaResult=CriteriaResult.emptyCriteriaResult, 
    excludedResult: CriteriaResult=CriteriaResult.emptyCriteriaResult, 
    meetMeasureResult: CriteriaResult=CriteriaResult.emptyCriteriaResult) {
  
  def addCriteriaResult(criteriaName: String, isCriteriaMet: Boolean, criteriaScore: List[String]): RuleResult = {
    criteriaName match {
      case HEDISRule.eligible => RuleResult(ruleName, ruleFullName, CriteriaResult(isCriteriaMet, criteriaScore map { s =>  CriteriaResultDetail(s)}), excludedResult, meetMeasureResult)
      case HEDISRule.excluded => RuleResult(ruleName, ruleFullName, eligibleResult, CriteriaResult(isCriteriaMet, criteriaScore map { s =>  CriteriaResultDetail(s)}), meetMeasureResult)
      case HEDISRule.meetMeasure => RuleResult(ruleName, ruleFullName, eligibleResult, excludedResult, CriteriaResult(isCriteriaMet, criteriaScore map { s =>  CriteriaResultDetail(s)}))
      case _ => throw NickelException("RuleResult.addCriteriaResult: Unknown criteriaName: " + criteriaName)
    }
  }
}


object CriteriaResult {
  val emptyCriteriaResult = CriteriaResult(false, List())
  
  /**
   * Mapping from RuleCriteriaScore (which came from Scorecard) to CriteriaResult
   */
  def apply(ruleCriteriaScore: RuleCriteriaScore): CriteriaResult = {
    val criteriaResultReasons = ruleCriteriaScore.criteriaScore.toList flatMap { case (reason, claimList) => 
      claimList map { c => CriteriaResultDetail(c.claimID, c.providerFirstName, c.providerLastName, c.date, reason) }
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
case class CriteriaResultDetail(claimId: String, providerFirstName: String, providerLastName: String, dos: DateTime, reason: String) {
  
  def toCSVString: String = {
    var strWriter = new StringWriter()
    val writer = CSVWriter.open(strWriter)
    writer.writeRow(List(claimId, providerFirstName, providerLastName, dos.toLocalDate().toString, reason))
    writer.flush
    strWriter.toString()
  }
}
