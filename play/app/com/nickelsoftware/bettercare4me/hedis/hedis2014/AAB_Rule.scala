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

object AAB {

  val name = "AAB-HEDIS-2014"

  val aabPatient = "AAB Diagnosed Patient"
  val antibioticMedication = "Antibiotic Medication Prescribed"
  val confictingDiagnosis = "Conficting Diagnosis"

  // ICD D AAB diagnosis 
  val icdDA = List("466.0")
  val icdDAS = icdDA.toSet

  // ICD D exclusion - competing diagnosis
  //@TODO expand codes
  val icdDF = List("001.0-009.3", "033*", "034.0", "041.9", "078.88", "079.88", "079.98", "088*", "090.0-097.9", "098*", "099*", "131*", "382*", "383*", "461*", "462", "463", "464.10-464.31", "473*", "474*", "478.21-478.24", "478.29", "478.71", "478.79", "478.9", "481-486", "590*", "595*", "599.0", "601*", "614.0-616.9", "681*", "682*", "683", "684", "686*", "706.0", "706.1", "730*", "V01.6", "V02.7", "V02.8")
  val icdDFS = icdDF.toSet

  // ICD D exclusion - comorbidity diagnosis
  //@TODO expand codes
  val icdDE = List("010.00-018.96", "042", "140.0-209.79", "277.0*", "279*", "491*", "492*", "493.2*", "494*", "495*", "496", "500-508.9", "510.0-519.9", "V08")
  val icdDES = icdDE.toSet

  // CPT outpatient or emergency department visit
  //@TODO expand codes
  val cptA = List("99201-99205", "99211-99215", "99217-99220", "99241-99245", "99281-99285", "99385", "99386", "99395", "99396", "99401-99404", "99411", "99412", "99420", "99429")
  val cptAS = cptA.toSet

  // UB outpatient or emergency department visit
  //@TODO expand codes
  val ubA = List("0450-0459", "0510-0519", "0520-0523", "0526-0529", "0981-0983")
  val ubAS = ubA.toSet

  def diagnosisClaims(icd: Set[String], ph: PatientHistory, hedisDate: DateTime): List[MedClaim] = {

    val measurementInterval = new Interval(hedisDate.minusYears(1).plusDays(1), hedisDate.minusDays(7))
    val claims = Utils.filterClaims(ph.icdD, icd, { claim: MedClaim => measurementInterval.contains(claim.dos) && icd.contains(claim.icdDPri) })
    if (claims.isEmpty) claims
    else claims sortWith { (c1: MedClaim, c2: MedClaim) => c1.dos.isBefore(c2.dos) }
  }
}

/**
 * Acute Bronchitis Treatment Without Antibiotics Rule
 *
 * This measure is based on the HEDIS measure Acute Bronchitis Treatment Without Antibiotics (AAB).
 *
 * Acute Bronchitis Treatment Without Antibiotics indicates whether a patient with acute bronchitis, aged 18 to 64 years, was not
 * dispensed an antibiotic prescription on or within 3 days following their bronchitis visit.
 * This excludes patients who had an antibiotic prescription within the previous 30 days, had a competing diagnosis from 30 days
 * prior to the visit to 7 days following the visit, or had a comorbid condition during the year prior to the visit.
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
class AAB_Rule(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  val name = AAB.name
  val fullName = "Acute Bronchitis Treatment Without Antibiotics"
  val description = "Identifies the unique count of patients, who were at least 18 years of age one year prior to the beginning of the measurement " +
    "year and no greater than 64 years of age at the end of the measurement year, and had an outpatient or emergency department " +
    "visit with a diagnosis of acute bronchitis."

  import AAB._

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate)
    age >= 19 && age <= 64
  }

  override def generateEligibleClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // generate an eligible episode in the lst 6 months of the intake period
    val intakeEnd = hedisDate.minusDays(7)
    val days = Utils.daysBetween(intakeEnd.minusMonths(4), intakeEnd)
    val dos1 = intakeEnd.minusDays(10 + Random.nextInt(days)) // provider visit - will be in the last 6 months of the intake period so we can insert excluding claim 
    val dos2 = dos1.plusDays(Random.nextInt(3)) // prescribed antibiotics

    pickOne(List(

      // Visit outpatient or emergency department visit
      () => List(
        pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", cpt = pickOne(cptA)),
        pl.createRxClaim(patient.patientID, provider.providerID, dos2, ndc = pickOne(CWP.ndcA), daysSupply = 20, qty = 40)),
      () => List(
        pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA), roomBoardFlag = "N"),
        pl.createRxClaim(patient.patientID, provider.providerID, dos2, ndc = pickOne(CWP.ndcA), daysSupply = 20, qty = 40))))()
  }

  override def scorePatientEligible(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    if (!isPatientMeetDemographic(patient)) scorecard.addScore(name, HEDISRule.eligible, false)
    else {

      // AAB primary diagnosis
      val claims = diagnosisClaims(icdDAS, ph, hedisDate)

      if (claims.isEmpty) scorecard
      else scorecard.addScore(name, HEDISRule.eligible, aabPatient, claims)
    }
  }

  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // generate an excluding episode within the first 6 months
    val dos1 = hedisDate.minusMonths(12).plusDays(10 + Random.nextInt(60)) // provider visit
    val dos2 = dos1.minusDays(1 + Random.nextInt(28)) // prescribed antibiotics (to make it excluded)
    val dos3 = dos2.minusDays(30) // prescribed antibiotics (to make it excluded, alternate rule)
    val dos4 = dos1.plusDays(7).minusDays(Random.nextInt(20)) // competing diagnosis
    val dos5 = dos1.minusDays(Random.nextInt(364)) // comorbidity

    pickOne(List(

      // Visit at ED
      () => List(
        pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", cpt = pickOne(cptA)),
        pl.createRxClaim(patient.patientID, provider.providerID, dos2, ndc = pickOne(CWP.ndcA), daysSupply = 20, qty = 40)),
      () => List(
        pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA), roomBoardFlag = "N"),
        pl.createRxClaim(patient.patientID, provider.providerID, dos2, ndc = pickOne(CWP.ndcA), daysSupply = 20, qty = 40)),

      // make such that the antibiotics were dispensed more than 30 days prior the episode (diagnosis)
      () => List(
        pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", cpt = pickOne(cptA)),
        pl.createRxClaim(patient.patientID, provider.providerID, dos3, ndc = pickOne(CWP.ndcA), daysSupply = Utils.daysBetween(dos3, dos1), qty = 90)),
      () => List(
        pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA), roomBoardFlag = "N"),
        pl.createRxClaim(patient.patientID, provider.providerID, dos3, ndc = pickOne(CWP.ndcA), daysSupply = Utils.daysBetween(dos3, dos1), qty = 90)),

      // Competing diagnosis
      () => List(
        pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", cpt = pickOne(cptA)),
        pl.createMedClaim(patient.patientID, provider.providerID, dos4, dos4, icdDPri = pickOne(icdDF))),
      () => List(
        pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA), roomBoardFlag = "N"),
        pl.createMedClaim(patient.patientID, provider.providerID, dos4, dos4, icdDPri = pickOne(icdDF))),

      // comorbidity diagnosis
      () => List(
        pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", cpt = pickOne(cptA)),
        pl.createMedClaim(patient.patientID, provider.providerID, dos5, dos5, icdDPri = pickOne(icdDE))),
      () => List(
        pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA), roomBoardFlag = "N"),
        pl.createMedClaim(patient.patientID, provider.providerID, dos5, dos5, icdDPri = pickOne(icdDE)))))()

  }

  override def scorePatientExcluded(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    // get all claims with Antibiotic Medication prior to pharyngitis diagnosis
    def activeRxPriorDianosis(claim: MedClaim, ph: PatientHistory): List[Claim] = {

      // get all claims with Antibiotic Medication prior to AAB diagnosis
      val prior90 = new Interval(claim.dos.minusMonths(3), claim.dos)
      filterClaims(ph.ndc, CWP.ndcAS, { rx: RxClaim => prior90.contains(rx.fillD) && ((Utils.daysBetween(rx.fillD, claim.dos) <= 30) || !rx.fillD.plusDays(rx.daysSupply).isBefore(claim.dos)) })
    }

    // get the episode claims
    val claims = diagnosisClaims(icdDAS, ph, hedisDate)

    def rules = List[(Scorecard) => Scorecard](

      // get all claims with Antibiotic Medication prior to diagnosis to see if should be excluded
      (s: Scorecard) => {
        val rxExclusion = activeRxPriorDianosis(claims.head, ph)
        if (rxExclusion.isEmpty) s
        else s.addScore(name, HEDISRule.excluded, antibioticMedication, List.concat(claims, rxExclusion))
      },

      // check for conficting diagnosis
      (s: Scorecard) => {
        val claim = claims.head
        val interval = new Interval(claim.dos.minusDays(30), claim.dos.plusDays(8))
        val claims2 = filterClaims(ph.icdD, icdDFS, { c: MedClaim => interval.contains(c.dos) })
        if (claims2.isEmpty) s
        else s.addScore(name, HEDISRule.excluded, confictingDiagnosis, List.concat(claims, claims2))
      },

      // check for comorbidity diagnosis
      (s: Scorecard) => {
        val claim = claims.head
        val interval = new Interval(claim.dos.minusYears(1), claim.dos.plusDays(1))
        val claims2 = filterClaims(ph.icdD, icdDES, { c: MedClaim => interval.contains(c.dos) })
        if (claims2.isEmpty) s
        else s.addScore(name, HEDISRule.excluded, confictingDiagnosis, List.concat(claims, claims2))
      })

    if (claims.isEmpty) scorecard
    else applyRules(scorecard, rules)

  }

  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // generate meet measure episode within the first 6 months
    val dos1 = hedisDate.minusMonths(12).plusDays(10 + Random.nextInt(60)) // AAB diagnosis

    pickOne(List(

      // Visit at ED
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", cpt = pickOne(cptA))),
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA), roomBoardFlag = "N"))))()
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    // get the episode claims
    val claims = diagnosisClaims(icdDAS, ph, hedisDate)
    if (claims.isEmpty) scorecard
    else {

      val claim = claims.head

      // check to ensure antibiotic was not prescribed within 3 days of the diagnosis
      val interval = new Interval(claim.dos, claim.dos.plusDays(4))
      val rxs = filterClaims(ph.ndc, CWP.ndcAS, { rx: RxClaim => interval.contains(rx.fillD) })
      if (rxs.isEmpty) scorecard.addScore(name, HEDISRule.meetMeasure, "Patient Meet Measure", claims)
      else scorecard
    }
  }
}
