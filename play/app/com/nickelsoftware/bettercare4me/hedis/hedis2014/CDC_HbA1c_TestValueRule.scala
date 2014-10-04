/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014

import scala.math.BigDecimal.double2bigDecimal
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

object CDCHbA1cTestValue {

  val name7 = "CDC-HbA1c-Test-LT7-HEDIS-2014"
  val name8 = "CDC-HbA1c-Test-LT8-HEDIS-2014"
  val name9 = "CDC-HbA1c-Test-GT9-HEDIS-2014"

  /**
   * LOINC codes for HbA1c test
   */
  val loincA = List("4548-4", "4549-2", "17856-6", "69261-8", "62388-4", "71875-9")
  val loincAS = loincA.toSet
}

/**
 * Diabetes HbA1c Test (Less Than 7%, Less Than 8%, More Than 9%)
 *
 * Diabetes HbA1c less than 7%, 8% or more than 9% indicates whether a patient with type 1 or type 2 diabetes, aged 18 to 75 years, had at least one
 * HbA1c test record in the database, and the most recent non-zero test result value was less than 7%, 8% or more than 9%. This excludes patients with
 * a previous diagnosis of polycystic ovaries, gestational diabetes, or steroid-induced diabetes.
 *
 * NUMERATOR:
 * Identifies patients with type 1 or type 2 diabetes, aged 18 to 75 years, who had at least one HbA1c lab result record with a value
 * greater than zero and less than 7%, 8% or more than 9%.
 *
 */
class CDCHbA1cTestValueRule(ruleName: String, tag: String, cptV: String, baseVal: Double, meetCriteria: (LabClaim) => Boolean, config: RuleConfig, hedisDate: DateTime) extends CDCRuleBase(config, hedisDate) {

  val name = ruleName
  val fullName = "Diabetes HbA1c " + tag + "."
  val description = "Diabetes HbA1c " + tag + " indicates whether a patient with type 1 or type 2 diabetes, aged 18 to 75 years, had at least one" +
    "HbA1c test record in the database, and the most recent non-zero test result value was " + tag + ". This excludes patients with" +
    "a previous diagnosis of polycystic ovaries, gestational diabetes, or steroid-induced diabetes."

  import CDCHbA1cTestValue._
  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = getIntervalFromYears(1).toDuration().getStandardDays().toInt
    val dos = hedisDate.minusDays(Random.nextInt(days))
    val dos2 = dos.minusDays(Random.nextInt(80))
    pickOne(List(

      // One possible set: Most recent HbA1c test result (during the measurement year)
      () => List(
        pl.createLabClaim(patient.patientID, provider.providerID, dos, loinc = pickOne(loincA), result = baseVal + Random.nextDouble * 0.01),
        pl.createLabClaim(patient.patientID, provider.providerID, dos2, loinc = pickOne(loincA), result = baseVal + Random.nextDouble * 0.01)),

      // Another possible set: by cpt
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = cptV))))()
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = getIntervalFromYears(1)

    def rules = List[(Scorecard) => Scorecard](

      // Check if patient has claim with cpt
      (s: Scorecard) => {
        val claims = ph.claims4CPT(cptV) filter { claim: MedClaim => measurementInterval.contains(claim.dos) }
        s.addScore(name, HEDISRule.meetMeasure, "HbA1C " + tag + " (CPT)", claims)
      },

      // Check if most recent lab test
      (s: Scorecard) => {
        val claims = filterClaims(ph.loinc, loincAS, { claim: LabClaim => measurementInterval.contains(claim.dos) })
        if (claims.isEmpty) s
        else {
          val claim = (claims sortWith { (c1: LabClaim, c2: LabClaim) => c1.dos.isAfter(c2.dos) }).head
          if (meetCriteria(claim)) s.addScore(name, HEDISRule.meetMeasure, "HbA1C Test Value " + tag, List(claim))
          else s
        }
      })

    applyRules(scorecard, rules)
  }
}

class CDCHbA1cTest7Rule(config: RuleConfig, hedisDate: DateTime) extends CDCHbA1cTestValueRule(CDCHbA1cTestValue.name7, "less than 7%", "3044F", 0.06, { (c: LabClaim) => c.result < 0.07 }, config, hedisDate)
class CDCHbA1cTest8Rule(config: RuleConfig, hedisDate: DateTime) extends CDCHbA1cTestValueRule(CDCHbA1cTestValue.name8, "less than 8%", "3044F", 0.07, { (c: LabClaim) => c.result < 0.08 }, config, hedisDate)
class CDCHbA1cTest9Rule(config: RuleConfig, hedisDate: DateTime) extends CDCHbA1cTestValueRule(CDCHbA1cTestValue.name9, "greater than 9%", "3046F", 0.091, { (c: LabClaim) => c.result > 0.09 }, config, hedisDate)
