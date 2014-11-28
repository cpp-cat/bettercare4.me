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

object CDC_LDL_C_Value {

  val name = "CDC-LDL-C-Value-HEDIS-2014"
}

/**
 * Diabetes Lipid Test < 100 mg/dL
 *
 * Diabetes Eye Exam indicates whether a patient with type 1 or type 2 diabetes, aged 18 to 75 years, had an eye exam performed.
 * This excludes patients with a previous diagnosis of polycystic ovaries, gestational diabetes, or steroid-induced diabetes.
 *
 * NUMERATOR:
 * Identifies patients with type 1 or type 2 diabetes, aged 18 to 75 years, who had a lipid test value done. NOTE: Through
 * administrative data there is no way to determine whether a dilated eye exam was performed. Therefore, eye exams provided by
 * eye care professionals are used as a proxy for dilated exams.
 *
 */
class CDC_LDL_C_TestValueRule(config: RuleConfig, hedisDate: DateTime) extends CDCRuleBase(config, hedisDate) {

  val name = CDC_LDL_C_Value.name
  val fullName = "Diabetes Lipid Test (< 100 mg/dL)"
  val description = "Identifies patients with type 1 or type 2 diabetes, aged 18 to 75 years, who had at least one LDL cholesterol lab result record with a value greater than zero and less than 100 mg/dL."

  private val ldlTestValueRule = new LDL_C_TestValueRuleBase(name, config, hedisDate)

  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = ldlTestValueRule.generateMeetMeasureClaims(pl, patient, provider)
  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = ldlTestValueRule.scorePatientMeetMeasure(scorecard, patient, ph)
}
