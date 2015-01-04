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
import com.nickelsoftware.bettercare4me.models.LabClaim
import com.nickelsoftware.bettercare4me.models.MedClaim
import com.nickelsoftware.bettercare4me.models.RxClaim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.nickelsoftware.bettercare4me.utils.Utils

object LBP {

  val name = "LBP-HEDIS-2014"

  val lbpPatient = "LBP Diagnosed Patient"
  val twoLBPEpisodes = "Two LBP Episodes"
  val confictingDiagnosis = "Conficting Diagnosis"
  val cancerDiagnosis = "Cancer Diagnosis"

  // ICD D LBP diagnosis 
  val icdDA = List("721.3", "722.10", "722.32", "722.52", "722.93", "724.02", "724.03", "724.2", "724.3", "724.5", "724.6", "724.70", "724.71", "724.79", "738.5", "739.3", "739.4", "846.0", "846.1", "846.2", "846.3", "846.8", "846.9", "847.2")
  val icdDAS = icdDA.toSet

  // ICD D exclusion - trauma, IV drug abuse, or neurologic impairment diagnosis
  //@TODO expand codes
  val icdDB = List("304.0*", "304.1*", "304.2*", "304.4*", "305.4*", "305.5*", "305.6*", "305.7*", "344.60", "729.2", "800.00-839.9", "850.0-854.19", "860.0-869.1", "905.0-909.9", "926.11", "926.12", "929*", "952*", "958.0-959.9")
  val icdDBS = icdDB.toSet

  // ICD D exclusion - cancer diagnosis
  //@TODO expand codes
  val icdDC = List("140.0-209.79", "230.0-239.9", "V10*")
  val icdDCS = icdDC.toSet

  // CPT outpatient or emergency department visit
  //@TODO expand codes
  val cptA = List("98925-98929", "98940-98942", "99201-99205", "99211-99215", "99217-99220", "99241-99245", "99281-99285", "99341-99345", "99347-99350", "99385", "99386", "99395", "99396", "99401-99404", "99411", "99412", "99420", "99429", "99455", "99456")
  val cptAS = cptA.toSet

  // UB outpatient or emergency department visit
  //@TODO expand codes
  val ubA = List("0450-0459", "0510-0519", "0520-0523", "0526-0529", "0570-0599", "0981-0983")
  val ubAS = ubA.toSet

  // POS outpatient or emergency department visit
  val posA = List("21", "25", "51", "55")
  val posAS = posA.toSet

  // CPT Spinal imaging for low back pain
  //@TODO expand codes
  val cptB = List("72010", "72020", "72052", "72100", "72110", "72114", "72120", "72131-72133", "72141", "72142", "72146-72149", "72156", "72158", "72200", "72202", "72220")
  val cptBS = cptB.toSet

  // UB Spinal imaging for low back pain
  val ubB = List("0320", "0329", "0350", "0352", "0359", "0610", "0612", "0614", "0619", "0972")
  val ubBS = ubB.toSet
}

/**
 * Low Back Pain Imaging Studies Rule
 *
 * This measure is based on the HEDIS measure Use of Imaging Studies for Low Back Pain (LBP).
 *
 * Low Back Pain Imaging Studies indicates whether a patient with low back pain, aged 18 to 50 years, did not have an imaging
 * study (e.g., back x-ray, MRI, CT scan) done within 28 days following the diagnosis. This excludes all patients with a low back
 * pain diagnosis within 6 months prior to the current diagnosis; patients with a diagnosis of trauma, IV drug abuse, or neurologic
 * impairment from one year prior to the low back pain encounter to 28 days following the encounter; patients with a diagnosis of
 * cancer at anytime; or an emergency department visit with a principal diagnosis of low back pain that results in hospital admission.
 *
 * DENOMINATOR:
 * Identifies the unique count of patients, who were at least 18 years of age one year prior to the beginning of the measurement
 * year and no greater than 64 years of age at the end of the measurement year, and had an outpatient or emergency department
 * visit with a diagnosis of acute bronchitis.
 *
 * EXCLUSIONS:
 * Excludes from the eligible population all patients who had an antibiotic prescription within the previous 30 days, had a competing
 * diagnosis from 30 days prior to the visit to 7 days following the visit, or had a comorbid condition during the year prior to the visit.
 *
 * NUMERATOR:
 * Identifies patients with a diagnosis of acute bronchitis, aged 18 to 64 years, who were not prescribed an antibiotic on or within 3
 * days following their acute bronchitis visit.
 *
 */
class LBP_Rule(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  val name = LBP.name
  val fullName = "Low Back Pain Imaging Studies"
  val description = "Low Back Pain Imaging Studies indicates whether a patient with low back pain, aged 18 to 50 years, did not have an imaging " +
    "study (e.g., back x-ray, MRI, CT scan) done within 28 days following the diagnosis. This excludes all patients with a low back " +
    "pain diagnosis within 6 months prior to the current diagnosis; patients with a diagnosis of trauma, IV drug abuse, or neurologic " +
    "impairment from one year prior to the low back pain encounter to 28 days following the encounter; patients with a diagnosis of " +
    "cancer at anytime; or an emergency department visit with a principal diagnosis of low back pain that results in hospital admission."

  import LBP._

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate)
    age >= 19 && age <= 50
  }

  override def generateEligibleClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // generate an eligible episode in the last month of the intake period
    val intakeEnd = hedisDate.minusDays(28)
    val days = Utils.daysBetween(intakeEnd.minusMonths(1), intakeEnd)
    val dos1 = intakeEnd.minusDays(10 + Random.nextInt(days)) // provider visit - will be in the last 6 months of the intake period so we can insert excluding claim 

    pickOne(List(

      // Visit outpatient or emergency department visit
      () => List(pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", cpt = pickOne(cptA))),
      () => List(pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA)))))()
  }

  override def scorePatientEligible(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val interval = new Interval(hedisDate.minusYears(1).plusDays(1), hedisDate.minusDays(28))
    def rules = List[(Scorecard) => Scorecard](

      (s: Scorecard) => {
        val claims = filterClaims(ph.icdD, icdDAS, { claim: MedClaim => interval.contains(claim.dos) && (cptA.contains(claim.cpt) || ubA.contains(claim.ubRevenue) && !posAS.contains(claim.hcfaPOS)) })
        s.addScore(name, fullName, HEDISRule.eligible, lbpPatient, claims)
      })

    if (!isPatientMeetDemographic(patient)) scorecard.addScore(name, fullName, HEDISRule.eligible, false)
    else applyRules(scorecard, rules)
  }

  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // generate an excluding episode within the first 6 months
    val dos1 = hedisDate.minusMonths(12).plusDays(10 + Random.nextInt(60)) // provider visit
    val dos2 = dos1.minusDays(1 + Random.nextInt(175)) // another episode
    val dos3 = dos1.plusDays(28).minusDays(Random.nextInt(365)) // competing diagnosis
    val dos4 = hedisDate.minusDays(Random.nextInt(300)) // cancer

    pickOne(List(

      // Two episodes
      () => List(
        pickOne(List(
          pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", cpt = pickOne(cptA)),
          pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA)))),
        pickOne(List(
          pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, dos2, icdDPri = pickOne(icdDA), hcfaPOS = "01", cpt = pickOne(cptA)),
          pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, dos2, icdDPri = pickOne(icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA))))),

      // Competing diagnosis
      () => List(
        pickOne(List(
          pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", cpt = pickOne(cptA)),
          pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA)))),
        pickOne(List(
          pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, dos3, icdDPri = pickOne(icdDB), hcfaPOS = "01", cpt = pickOne(cptA)),
          pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, dos3, icdDPri = pickOne(icdDB), hcfaPOS = "01", ubRevenue = pickOne(ubA))))),

      // cancer
      () => List(pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos4, dos4, icdDPri = pickOne(icdDC)))))()
  }

  override def scorePatientExcluded(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val interval = new Interval(hedisDate.minusYears(1).plusDays(1), hedisDate.minusDays(28))
    val intervalF = getIntervalFromYears(1)
    def rules = List[(Scorecard) => Scorecard](

      // Two episodes
      (s: Scorecard) => {
        val mclaims: List[List[Claim]] = for {
          c1 <- filterClaims(ph.icdD, icdDAS, { c: MedClaim => interval.contains(c.dos) })
          c2 <- filterClaims(ph.icdD, icdDAS, { c: MedClaim => new Interval(c1.dos.minusMonths(6), c1.dos).contains(c.dos) && c1.claimID != c.claimID })
        } yield List(c1, c2)
        s.addScore(name, fullName, HEDISRule.excluded, twoLBPEpisodes, mclaims.flatten)
      },

      // check for conficting diagnosis
      (s: Scorecard) => {
        val mclaims: List[List[Claim]] = for {
          c1 <- filterClaims(ph.icdD, icdDAS, { c: MedClaim => interval.contains(c.dos) })
          c2 <- filterClaims(ph.icdD, icdDBS, { c: MedClaim => new Interval(c1.dos.minusYears(1), c1.dos.plusDays(29)).contains(c.dos) })
        } yield List(c1, c2)

        s.addScore(name, fullName, HEDISRule.excluded, confictingDiagnosis, mclaims.flatten)
      },

      // check for cancer diagnosis
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdD, icdDCS, { c: MedClaim => intervalF.contains(c.dos) })
        s.addScore(name, fullName, HEDISRule.excluded, cancerDiagnosis, claims)
      })

    applyRules(scorecard, rules)
  }

  override def generateFailMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // generate claims to fail the meet measure criteria
    val dos1 = hedisDate.minusMonths(12).plusDays(10 + Random.nextInt(20)) // provider visit
    val dos2 = dos1.plusDays(Random.nextInt(28)) // another episode

    pickOne(List(

      // Spinal imaging for low back pain
      () => List(pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, dos2, icdDPri = pickOne(icdDA), cpt = pickOne(cptB))),
      () => List(pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, dos2, icdDPri = pickOne(icdDA), ubRevenue = pickOne(ubB))),

      // No imaging performed
      () => List(
        pickOne(List(
          pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", cpt = pickOne(cptA)),
          pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA)))),
        pickOne(List(
          pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, dos2, cpt = pickOne(cptB)),
          pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, dos2, ubRevenue = pickOne(ubB)))))))()
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    // get the LBP diagnosis claims and the imaging claims that are within 28 days after the LBP diagnosis
    val interval = new Interval(hedisDate.minusYears(1).plusDays(1), hedisDate.minusDays(28))

    val claims1: List[List[Claim]] = for {
      c1 <- filterClaims(ph.icdD, icdDAS, { c: MedClaim => interval.contains(c.dos) })
      c2 <- filterClaims(ph.cpt, cptBS, { c: MedClaim => new Interval(c1.dos, c1.dos.plusDays(29)).contains(c.dos) })
    } yield List(c1, c2)

    val claims2: List[List[Claim]] = for {
      c1 <- filterClaims(ph.icdD, icdDAS, { c: MedClaim => interval.contains(c.dos) })
      c2 <- filterClaims(ph.ubRevenue, ubBS, { c: MedClaim => new Interval(c1.dos, c1.dos.plusDays(29)).contains(c.dos) })
    } yield List(c1, c2)

    // this measure checks for failure
    if (claims1.isEmpty && claims2.isEmpty) scorecard.addScore(name, fullName, HEDISRule.meetMeasure, true)
    else scorecard.addScore(name, fullName, HEDISRule.meetMeasure, false)
  }
}
