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
import com.nickelsoftware.bettercare4me.utils.Utils

object LDL_C {

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
 * Lipid Test Base Class
 *
 * This class is used to factor out Lipid Test condition (Meet Criteria) of HEDIS rules. 
 * This is used as part of CDC_LDL_C_Test and CMC HEDIS rules.
 * 
 * Lipid Test indicates whether a patient has a Lipid Test performed.
 *
 * NUMERATOR:
 * Identifies patients who had a lipid test done.
 *
 */
class LDL_C_TestRuleBase(name: String, config: RuleConfig, hedisDate: DateTime) {

  import LDL_C._
  
  def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = Utils.daysBetween(hedisDate.minusYears(1), hedisDate)
    val dos = hedisDate.minusDays(Random.nextInt(days))

    // At least one Lipid Test (during the measurement year)
    Utils.pickOne(List(

      // Possible set: CPT
      () => List(pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos, dos, cpt = Utils.pickOne(cptA))),

      // Another possible set: LOINC on lab claim 
      () => List(pl.createLabClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos, loinc = Utils.pickOne(loincA) )) ))()
  }

  def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = Utils.getIntervalFromYears(1, hedisDate)

    def rules = List[(Scorecard) => Scorecard](

      // Check for patient has CPT
      (s: Scorecard) => {
        val claims = Utils.filterClaims(ph.cpt, cptAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, cptLipidTest, claims)
      },

      // Check for LOINC on Lab Claim
      (s: Scorecard) => {
        val claims = Utils.filterClaims(ph.loinc, loincAS, { claim: LabClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, loincLipidTest, claims)
      })
      
    Utils.applyRules(scorecard, rules)
  }
}

/**
 * Lipid Test Value Base Class
 *
 * This class is used to factor out Lipid Test Value condition (Meet Criteria) of HEDIS rules. 
 * This is used as part of CDC_LDL_C_Test and CMC HEDIS rules.
 * 
 * Lipid Test Value indicates whether a patient has a Lipid Test performed and what the value was.
 *
 * NUMERATOR:
 * Identifies patients who had a lipid test with test result done.
 *
 */
class LDL_C_TestValueRuleBase(name: String, config: RuleConfig, hedisDate: DateTime) {

  import LDL_C._
  
  def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = Utils.daysBetween(hedisDate.minusYears(1), hedisDate)
    val dos = hedisDate.minusDays(Random.nextInt(days))

    // At least one Lipid Test (during the measurement year)
    Utils.pickOne(List(

      // Possible set: CPT
      () => List(pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos, dos, cpt = Utils.pickOne(cptA))),

      // Another possible set: Most recent LDL-C test result > 0 and < 100 mg/dL
      () => List(pl.createLabClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos, loinc = Utils.pickOne(loincA), result=Random.nextDouble*99.0)) ))()
  }

  def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = Utils.getIntervalFromYears(1, hedisDate)

    def rules = List[(Scorecard) => Scorecard](

      // Check for patient has CPT
      (s: Scorecard) => {
        val claims = Utils.filterClaims(ph.cpt, cptAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, cptLipidTest, claims)
      },

      // Check for LOINC on Lab Claim
      (s: Scorecard) => {
        val claims = Utils.filterClaims(ph.loinc, loincAS, { claim: LabClaim => measurementInterval.contains(claim.dos) && claim.result>0.0 && claim.result<100.0 })
        s.addScore(name, HEDISRule.meetMeasure, loincLipidTest, claims)
      })

    Utils.applyRules(scorecard, rules)
  }
}

