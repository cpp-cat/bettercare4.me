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

  val cptLipidTest = "Lipid Test Claim (CPT)"
  val loincLipidTest = "Lipid Test Lab Claim (LOINC)"

  /**
   * CPT codes for Lipid Test
   */
  val cptA = List("80061", "83700", "83701", "83704", "83721", "3048F", "3049F", "3050F")
  val cptAS = cptA.toSet

  /**
   * LOINC codes for Lipid Test
   */
  val loincA = List("2089-1", "12773-8", "13457-7", "18261-8", "18262-6", "22748-8", "39469-2", "49132-4", "55440-2")
  val loincAS = loincA.toSet
}

/**
 * Diabetes Lipid Test < 100 mg/dL
 *
 * Diabetes Eye Exam indicates whether a patient with type 1 or type 2 diabetes, aged 18 to 75 years, had an eye exam performed.
 * This excludes patients with a previous diagnosis of polycystic ovaries, gestational diabetes, or steroid-induced diabetes.
 *
 * NUMERATOR:
 * Identifies patients with type 1 or type 2 diabetes, aged 18 to 75 years, who had an eye exam done. NOTE: Through
 * administrative data there is no way to determine whether a dilated eye exam was performed. Therefore, eye exams provided by
 * eye care professionals are used as a proxy for dilated exams.
 *
 */
class CDC_LDL_C_TestValueRule(config: RuleConfig, hedisDate: DateTime) extends CDCRuleBase(config, hedisDate) {

  val name = CDC_LDL_C_Value.name
  val fullName = "Diabetes Lipid Test"
  val description = "Identifies patients with type 1 or type 2 diabetes, aged 18 to 75 years, who had at least one LDL cholesterol lab result record with a value greater than zero and less than 100 mg/dL."

  import CDC_LDL_C_Value._
  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = getIntervalFromYears(1).toDuration().getStandardDays().toInt
    val dos = hedisDate.minusDays(Random.nextInt(days))

    // At least one Lipid Test (during the measurement year)
    pickOne(List(

      // Possible set: CPT
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptA))),

      // Another possible set: Most recent LDL-C test result > 0 and < 100 mg/dL
      () => List(pl.createLabClaim(patient.patientID, provider.providerID, dos, loinc = pickOne(loincA), result=Random.nextDouble*99.0)) ))()
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = getIntervalFromYears(1)

    def rules = List[(Scorecard) => Scorecard](

      // Check for patient has CPT
      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, cptAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, cptLipidTest, claims)
      },

      // Check for LOINC on Lab Claim
      (s: Scorecard) => {
        val claims = filterClaims(ph.loinc, loincAS, { claim: LabClaim => measurementInterval.contains(claim.dos) && claim.result>0.0 && claim.result<100.0 })
        s.addScore(name, HEDISRule.meetMeasure, loincLipidTest, claims)
      })

    applyRules(scorecard, rules)
  }
}
