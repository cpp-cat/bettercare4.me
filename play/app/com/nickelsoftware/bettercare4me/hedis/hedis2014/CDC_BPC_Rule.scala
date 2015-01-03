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

object CDC_BPC {

  val nameTest = "CDC-BPC-Test-HEDIS-2014"
  val nameC1 = "CDC-BPC-C1-HEDIS-2014"
  val nameC2 = "CDC-BPC-C2-HEDIS-2014"

  val c1Tag = "< 140/80 mmHg"
  val c2Tag = "< 140/90 mmHg"

  val bpTest = "BP Test Performed"
  val bpTestC1 = "BP Test " + c1Tag
  val bpTestC2 = "BP Test " + c2Tag

  /**
   * CPT codes for BP Test for S-BP
   */
  val cptST = List("3074F", "3075F", "3077F")
  val cptSTS = cptST.toSet

  /**
   * CPT codes for BP Test for D-BP
   */
  val cptDT = List("3078F", "3079F", "3080F")
  val cptDTS = cptDT.toSet

  /**
   * CPT codes for BP Test S-BP <140 mmHg
   */
  val cptSC = List("3074F", "3075F")

  /**
   * CPT codes for BP Test D-BP <80 mmHg
   */
  val cptDC1 = List("3078F")

  /**
   * CPT codes for BP Test D-BP <90 mmHg
   */
  val cptDC2 = List("3079F")
}

/**
 * Diabetes BP Test
 *
 * Diabetes BP Test indicates whether a patient with type 1 or type 2 diabetes, aged 18 to 75 years, had an eye exam performed.
 * This excludes patients with a previous diagnosis of polycystic ovaries, gestational diabetes, or steroid-induced diabetes.
 *
 * NUMERATOR:
 * Identifies patients with type 1 or type 2 diabetes, aged 18 to 75 years, who had a BP test done with the latest test within the specified value.
 *
 * NOTE: Specification is not clear, BP test must be done in an outpatient facility or acute inpatient facility and
 * this is specified by CPT codes while the BP test is also specified by CPT II codes. The claim had only one
 * CPT code, therefore not sure how to check it was done in an outpatient or acute inpatient facility. This implementation
 * do not check that the BP test was done in an outpatient or acute inpatient facility.
 *
 */
class CDC_BPC_Rule(val name: String, tag: String, cptS: List[String], cptD: List[String], meetPredicate: String, config: RuleConfig, hedisDate: DateTime) extends CDCRuleBase(config, hedisDate) {

  val fullName = "Diabetes BP Test " + tag
  val description = "Diabetes BP Test indicates whether a patient with type 1 or type 2 diabetes, aged 18 to 75 years, had a BP test performed " + tag +
    ". This excludes patients with a previous diagnosis of polycystic ovaries, gestational diabetes, or steroid-induced diabetes."

  import CDC_BPC._
  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = Utils.daysBetween(hedisDate.minusMonths(5), hedisDate)
    val dos1 = hedisDate.minusDays(Random.nextInt(days)).minusDays(days)
    val dos2 = hedisDate.minusDays(Random.nextInt(days)) // will be most recent and should be compliant

    // Latest BP test is compliant, earlier test non-compliant
    List(
      pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, cpt = "3077F"),
      pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, cpt = "3080F"),
      pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, dos2, cpt = pickOne(cptS)),
      pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, dos2, cpt = pickOne(cptD)))
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = getIntervalFromYears(1)

    def rules = List[(Scorecard) => Scorecard](

      // Check for patient has CPT
      (s: Scorecard) => {

        // Get all DBP tests done during the year, keep the most current
        val claimsS = filterClaims(ph.cpt, cptSTS, { claim: MedClaim => measurementInterval.contains(claim.dos) })

        // Get all DBP tests done during the year, keep the most current
        val claimsD = filterClaims(ph.cpt, cptDTS, { claim: MedClaim => measurementInterval.contains(claim.dos) })

        // check if the most current match the supplied criteria
        if (claimsS.isEmpty || claimsD.isEmpty) s
        else {

          val claimS = (claimsS sortWith { (c1: MedClaim, c2: MedClaim) => c1.dos.isAfter(c2.dos) }).head
          val claimD = (claimsD sortWith { (c1: MedClaim, c2: MedClaim) => c1.dos.isAfter(c2.dos) }).head
          if (cptS.contains(claimS.cpt) && cptD.contains(claimD.cpt)) s.addScore(name, HEDISRule.meetMeasure, meetPredicate, List(claimS, claimD))
          else s
        }
      })

    applyRules(scorecard, rules)
  }
}

class CDC_BPC_T_Rule(config: RuleConfig, hedisDate: DateTime) extends CDC_BPC_Rule(CDC_BPC.nameTest, "", CDC_BPC.cptST, CDC_BPC.cptDT, CDC_BPC.bpTest, config, hedisDate)
class CDC_BPC_C1_Rule(config: RuleConfig, hedisDate: DateTime) extends CDC_BPC_Rule(CDC_BPC.nameC1, "(" + CDC_BPC.c1Tag + ")", CDC_BPC.cptSC, CDC_BPC.cptDC1, CDC_BPC.bpTestC1, config, hedisDate)
class CDC_BPC_C2_Rule(config: RuleConfig, hedisDate: DateTime) extends CDC_BPC_Rule(CDC_BPC.nameC2, "(" + CDC_BPC.c2Tag + ")", CDC_BPC.cptSC, CDC_BPC.cptDC2, CDC_BPC.bpTestC2, config, hedisDate)
