/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models

import org.joda.time.DateTime
import com.nickelsoftware.bettercare4me.hedis.HEDISRule
import com.nickelsoftware.bettercare4me.utils.NickelException
import org.joda.time.LocalDate
import com.github.tototoshi.csv.CSVParser


/**
 * Patient scorecard restored back from Cassandra
 * 
 * This class is the equivalent of Patient + Scorecard, however restored from Cassandra rather than computed from raw claims
 */
case class PatientScorecardResult(patient: Patient, scorecardResult: Map[String, RuleResult]=Map()) {
  
  def addRuleResult(ruleName: String, criteriaName: String, isCriteriaMet: Boolean, criteriaScore: List[String]): PatientScorecardResult = {
    val ruleResult = scorecardResult.getOrElse(ruleName, RuleResult.emptyRuleResult)
    PatientScorecardResult(patient, scorecardResult + (ruleName -> ruleResult))
  }
}

/**
 * Utility object
 */
object RuleResult {
  val emptyRuleResult = RuleResult(CriteriaResult.emptyCriteriaResult, CriteriaResult.emptyCriteriaResult, CriteriaResult.emptyCriteriaResult)
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
case class CriteriaResultDetail(claimId: String, providerLName: String, providerFName: String, dos: DateTime, reason: String)
