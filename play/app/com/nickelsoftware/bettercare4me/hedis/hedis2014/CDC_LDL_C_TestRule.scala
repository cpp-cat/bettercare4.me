/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014

import scala.util.Random

import org.joda.time.DateTime
import org.joda.time.Interval

import com.nickelsoftware.bettercare4me.hedis.HEDISRule
import com.nickelsoftware.bettercare4me.hedis.Scorecard
import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.models.LabClaim
import com.nickelsoftware.bettercare4me.models.MedClaim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig

object CDC_LDL_C {

  val name = "CDC-LDL-C-HEDIS-2014"
}

/**
 * Diabetes Lipid Test
 *
 * Diabetes Lipid Test indicates whether a patient with type 1 or type 2 diabetes, aged 18 to 75 years, had an eye exam performed.
 * This excludes patients with a previous diagnosis of polycystic ovaries, gestational diabetes, or steroid-induced diabetes.
 *
 * NUMERATOR:
 * Identifies patients with type 1 or type 2 diabetes, aged 18 to 75 years, who had a lipid test done.
 *
 */
class CDC_LDL_C_TestRule(config: RuleConfig, hedisDate: DateTime) extends CDCRuleBase(config, hedisDate) {

  val name = CDC_LDL_C.name
  val fullName = "Diabetes Lipid Test"
  val description = "Diabetes Lipid Test indicates whether a patient with type 1 or type 2 diabetes, aged 18 to 75 years, had a lipid test performed. " +
    "This excludes patients with a previous diagnosis of polycystic ovaries, gestational diabetes, or steroid-induced diabetes."

  private val ldl_TestRule = new LDL_C_TestRuleBase(name, config, hedisDate)
  
  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = ldl_TestRule.generateMeetMeasureClaims(pl, patient, provider)
  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = ldl_TestRule.scorePatientMeetMeasure(scorecard, patient, ph)
}
