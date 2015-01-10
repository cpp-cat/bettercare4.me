/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models


/**
 * Summary class for HEDIS rules, listing all eligible patients
 */
case class RuleScorecardSummary(ruleName: String, patients: List[PatientRuleSummary]=List()) {
  
  def addPatient(patient: Patient, isExcluded: Boolean, isMeetCriteria: Boolean): RuleScorecardSummary = {
    RuleScorecardSummary(ruleName, PatientRuleSummary(patient, isExcluded, isMeetCriteria) :: patients)
  }
}


/**
 * Class used by RuleScorecardSummary to keep track of patient and whether they are excluded or meet the rule criteria
 * 
 * Patient does meet the demographic and eligibility criteria
 */
case class PatientRuleSummary(patient: Patient, isExcluded: Boolean, isMeetCriteria: Boolean)
