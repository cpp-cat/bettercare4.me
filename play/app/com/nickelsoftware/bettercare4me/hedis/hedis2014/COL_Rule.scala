/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014

import scala.util.Random

import org.joda.time.DateTime
import org.joda.time.Interval

import com.nickelsoftware.bettercare4me.hedis.HEDISRule
import com.nickelsoftware.bettercare4me.hedis.HEDISRuleBase
import com.nickelsoftware.bettercare4me.hedis.Scorecard
import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.models.MedClaim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.nickelsoftware.bettercare4me.utils.Utils

object COL {

  val name = "COL-HEDIS-2014"

  val totalColectomy = "Total Colectomy"
  val colorectalCancer = "Colorectal cancer"
  val fecalOccultBloodTest = "Fecal Occult Blood Test"
  val flexibleSigmoidoscopy = "Flexible Sigmoidoscopy"
  val colonoscopy = "Colonoscopy"

  // exclusions - Total Colectomy (CPT)
  val cptA = List("44150", "44151", "44152", "44153", "44154", "44155", "44156", "44157", "44158", "44210", "44211", "44212")
  val cptAS = cptA.toSet

  // exclusion - Total Colectomy (ICD P)
  val icdPA = List("45.8")
  val icdPAS = icdPA.toSet

  // exclusion - Colorectal cancer (ICD D)
  val icdDA = List("1530", "153.1", "153.2", "153.3", "153.4", "153.5", "153.6", "153.7", "153.8", "153.9", "154.0", "154.1", "197.5", "V10.05")
  val icdDAS = icdDA.toSet

  // exclusion - Colorectal cancer (HCPCS)
  val hcpcsA = List("G0213", "G0214", "G0215", "G0231")
  val hcpcsAS = hcpcsA.toSet

  // meet criteria - Fecal occult blood test (CPT)
  val cptB = List("82270", "82274")
  val cptBS = cptB.toSet

  // meet criteria - Fecal occult blood test (HCPCS)
  val hcpcsB = List("G0107", "G0328", "G0394")
  val hcpcsBS = hcpcsB.toSet

  // meet criteria - Flexible sigmoidoscopy (CPT)
  val cptC = List("45330", "45331", "45332", "45333", "45334", "45335", "45336", "45337", "45338", "45339", "45340", "45341", "45342", "45343", "45344", "45345")
  val cptCS = cptC.toSet

  // meet criteria - Flexible sigmoidoscopy (HCPCS)
  val hcpcsC = List("G0104")
  val hcpcsCS = hcpcsC.toSet

  // meet criteria - Flexible sigmoidoscopy (ICD P)
  val icdPC = List("45.24")
  val icdPCS = icdPC.toSet

  // meet criteria - Colonoscopy (CPT)
  val cptD = List("44388", "44389", "44390", "44391", "44392", "44393", "44394", "44395", "44396", "44397", "45355", "45378", "45378", "45379", "45380", "45381", "45382", "45383", "45384", "45385", "45386", "45387", "45388", "45389", "45390", "45391", "45392")
  val cptDS = cptD.toSet

  // meet criteria - Colonoscopy (HCPCS)
  val hcpcsD = List("G0105", "G0121")
  val hcpcsDS = hcpcsD.toSet

  // meet criteria - Colonoscopy (ICD P)
  val icdPD = List("45.22", "45.23", "45.25", "45.42", "45.43")
  val icdPDS = icdPD.toSet
}
/**
 * Colorectal Cancer Screen Rule
 *
 * Colorectal Cancer Screen indicates whether a patient, aged 51 to 75 years, had appropriate screening for colorectal cancer.
 * Colorectal cancer screening tests include the following: a fecal occult blood test during the measurement year, a flexible
 * sigmoidoscopy during the measurement year or the previous 4 years, or a colonoscopy during the measurement year or the
 * previous 9 years. This excludes people who had a previous total colectomy or a previous diagnosis of colorectal cancer.
 *
 * DENOMINATOR:
 * Identifies the unique count of members, aged 51 to 75 years. Note: Because this measure looks back at least 24 months for
 * screening, the eligible population includes those who could have been 50 years of age at the time of screening.
 *
 * EXCLUSIONS:
 * Excludes from the eligible population all patients who had a diagnosis of colorectal cancer or a total colectomy done anytime prior
 * to or during the measurement year, based on claims included in the database.
 *
 * NUMERATOR:
 * Identifies members, aged 51 to 75 years, who had appropriate screening for colorectal cancer. Colorectal cancer screening tests
 * include the following: a fecal occult blood test during the measurement year, a flexible sigmoidoscopy during the measurement
 * year or the previous 4 years, or a colonoscopy during the measurement year or the previous 9 years. Because of the timeframes
 * involved, this measure uses as much data as is available to calculate the numerator, up to the 10-year limit. A minimum of 2
 * years of data is necessary for the measure to be calculated at all.
 *
 */
class COL_Rule(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  val name = COL.name
  val fullName = "Colorectal Cancer Screen"
  val description = "Colorectal Cancer Screen indicates whether a patient, aged 51 to 75 years, had appropriate screening for colorectal cancer. " +
    "Colorectal cancer screening tests include the following: a fecal occult blood test during the measurement year, a flexible " +
    "sigmoidoscopy during the measurement year or the previous 4 years, or a colonoscopy during the measurement year or the " +
    "previous 9 years. This excludes people who had a previous total colectomy or a previous diagnosis of colorectal cancer."

  import COL._

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate)
    age >= 51 && age <= 75
  }

  // This rule has 100% eligibility when the demographics are meet
  override val eligibleRate: Int = 100

  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = Utils.daysBetween(hedisDate.minusYears(30), hedisDate)
    val dos = hedisDate.minusDays(Random.nextInt(days))

    pickOne(List(

      // exclusions - Total Colectomy (CPT)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptA))),

      // exclusions - Total Colectomy (ICD P)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdP = Set(pickOne(icdPA)))),

      // exclusion - Colorectal cancer (ICD D)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdDPri = pickOne(icdDA))),

      // exclusion - Colorectal cancer (HCPCS)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, hcpcs = pickOne(hcpcsA)))))()
  }

  override def scorePatientExcluded(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    def rules = List[(Scorecard) => Scorecard](

      // exclusions - Total Colectomy (CPT)
      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, cptAS, { claim: MedClaim => !claim.dos.isAfter(hedisDate) })
        s.addScore(name, HEDISRule.excluded, totalColectomy, claims)
      },

      // exclusions - Total Colectomy (ICD P)
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdP, icdPAS, { claim: MedClaim => !claim.dos.isAfter(hedisDate) })
        s.addScore(name, HEDISRule.excluded, totalColectomy, claims)
      },

      // exclusions - Total Colorectal cancer (ICD D)
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdD, icdDAS, { claim: MedClaim => !claim.dos.isAfter(hedisDate) })
        s.addScore(name, HEDISRule.excluded, colorectalCancer, claims)
      },

      // exclusion - Colorectal cancer (HCPCS)
      (s: Scorecard) => {
        val claims = filterClaims(ph.hcpcs, hcpcsAS, { claim: MedClaim => !claim.dos.isAfter(hedisDate) })
        s.addScore(name, HEDISRule.excluded, colorectalCancer, claims)
      })

    applyRules(scorecard, rules)
  }

  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = Utils.daysBetween(hedisDate.minusYears(1), hedisDate)
    val dos = hedisDate.minusDays(Random.nextInt(days))

    pickOne(List(

      // meet criteria - Fecal occult blood test (CPT)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptB))),

      // meet criteria - Fecal occult blood test (HCPCS)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, hcpcs = pickOne(hcpcsB))),

      // meet criteria - Flexible sigmoidoscopy (CPT)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptC))),

      // meet criteria - Flexible sigmoidoscopy (HCPCS)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, hcpcs = pickOne(hcpcsC))),

      // meet criteria - Flexible sigmoidoscopy (ICD P)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdP = Set(pickOne(icdPC)))),

      // meet criteria - Colonoscopy (CPT)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptD))),

      // meet criteria - Colonoscopy (HCPCS)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, hcpcs = pickOne(hcpcsD))),

      // meet criteria - Colonoscopy (ICD P)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdP = Set(pickOne(icdPD))))))()
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = getIntervalFromYears(1)

    def rules = List[(Scorecard) => Scorecard](

      // meet criteria - Fecal occult blood test (CPT)
      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, cptBS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, fecalOccultBloodTest, claims)
      },

      // meet criteria - Fecal occult blood test (HCPCS)
      (s: Scorecard) => {
        val claims = filterClaims(ph.hcpcs, hcpcsBS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, fecalOccultBloodTest, claims)
      },

      // meet criteria - Flexible sigmoidoscopy (CPT)
      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, cptCS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, flexibleSigmoidoscopy, claims)
      },

      // meet criteria - Flexible sigmoidoscopy (HCPCS)
      (s: Scorecard) => {
        val claims = filterClaims(ph.hcpcs, hcpcsCS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, flexibleSigmoidoscopy, claims)
      },

      // meet criteria - Flexible sigmoidoscopy (ICD P)
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdP, icdPCS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, flexibleSigmoidoscopy, claims)
      },

      // meet criteria - Colonoscopy (CPT)
      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, cptDS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, colonoscopy, claims)
      },

      // meet criteria - Colonoscopy (HCPCS)
      (s: Scorecard) => {
        val claims = filterClaims(ph.hcpcs, hcpcsDS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, colonoscopy, claims)
      },

      // meet criteria - Colonoscopy (ICD P)
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdP, icdPDS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, colonoscopy, claims)
      })

    applyRules(scorecard, rules)
  }
}
