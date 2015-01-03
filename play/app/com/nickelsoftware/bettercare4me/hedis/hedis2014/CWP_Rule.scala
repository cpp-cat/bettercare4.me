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
import com.github.tototoshi.csv.CSVReader
import java.io.File

object CWP {

  val name = "CWP-HEDIS-2014"

  val pharyngitisPatient = "Pharyngitis Diagnosed Patient"

  // Table CWP­-C: Antibiotic Medication
  val ndcA = CSVReader.open(new File("./data/cwp.ndc.c.csv")).all().flatten
  val ndcAS = ndcA.toSet

  // ICD D pharyngitis diagnosis 
  val icdDA = List("034.0", "462", "463")
  val icdDAS = icdDA.toSet

  // CPT for ED visit type
  //@TODO expand codes
  val cptA = List("99281-99285")
  val cptAS = cptA.toSet

  // UB for ED visit type
  //@TODO expand codes
  val ubA = List("045*", "0981")
  val ubAS = ubA.toSet

  // POS for ED visit type (to exclude claims that has these POS)
  val posA = List("21", "25", "51", "55")
  val posAS = posA.toSet

  // CPT for Outpatient visit type
  //@TODO expand codes
  val cptB = List("99201-99205", "99211-99215", "99217-99220", "99241-99245", "99341-99345", "99347-99350", "99382-99386", "99392-99396", "99401-99404", "99411", "99412", "99420", "99429")
  val cptBS = cptB.toSet

  // UB for Outpatient visit type
  //@TODO expand codes
  val ubB = List("051*", "0520-0523", "0526-0529", "0570-0599", "0982", "0983")
  val ubBS = ubB.toSet

  // CPT group A streptococcus test
  val cptC = List("87070", "87071", "87081", "87430", "87650", "87651", "87652", "87880")
  val cptCS = cptC.toSet

  // LOINC group A streptococcus test
  val loincC = List("626-2", "5036-9", "6556-5", "6557-3", "6558-1", "6559-9", "60489-2", "11268-0", "17656-0", "18481-2", "31971-5", "49610-9", "68954-7")
  val loincCS = loincC.toSet

  // get the claims with pharyngitis diagnosism sorted from oldest to most recent
  def diagnosisClaims(icd: Set[String], ph: PatientHistory, hedisDate: DateTime): List[MedClaim] = {

    val measurementInterval = Utils.getIntervalFromYears(1, hedisDate.minusMonths(6))
    val claims = Utils.filterClaims(ph.icdD, icd, { claim: MedClaim => measurementInterval.contains(claim.dos) && icd.contains(claim.icdDPri) && claim.icdD.isEmpty })
    if (claims.isEmpty) claims
    else claims sortWith { (c1: MedClaim, c2: MedClaim) => c1.dos.isBefore(c2.dos) }
  }
}

/**
 * Pharyngitis Treatment for Children Rule
 *
 * This measure is based on the HEDIS measure Appropriate Testing for Children with Pharyngitis (CWP).
 *
 * Pharyngitis Treatment for Children indicates whether a child, aged 2 to 18 years, who was seen with a diagnosis of pharyngitis
 * and had an antibiotic prescribed, received a group A streptococcus test within 3 days of their pharyngitis visit. This excludes
 * children who had an antibiotic prescription within the previous 30 days.
 *
 * DENOMINATOR:
 * Identifies the unique count of children who had an outpatient or emergency room visit with a single diagnosis of pharyngitis, aged
 * 2 to 18 years during the intake period, and were prescribed an antibiotic within 3 days of their pharyngitis visit.
 * The intake period starts 6 months before the beginning of the measurement year and ends
 * 6 months before the end of the measurement year. If multiple pharyngitis visits meet the denominator criteria, the first one is
 * used. This excludes children who had an antibiotic prescription within the previous 30 days.
 *
 * EXCLUSIONS:
 * Excludes from the eligible population those who had an antibiotic prescription within the previous 30 days.
 * No antibiotic medication prescribed or refilled within 30 days prior to the pharyngitis visit or still active on
 * the date of the visit (claims are checked up to 90 days prior to the visit date)
 * A prescription is still active if the prescription was filled more than 30 days prior to the pharyngitis visit
 * date and the Days Supply is >= the number of date between the prescription fill date and the pharyngitis visit date.
 *
 * NUMERATOR:
 * Identifies patients who had an outpatient visit with a single diagnosis of pharyngitis, aged 2 to 18 years, and were prescribed an
 * antibiotic within 3 days of their pharyngitis visit, and also received a group A streptococcus test within 3 days before or after that visit.
 *
 * NOTE:
 * 1. In 2013, Added LOINC code 68954-7 to Table CWP-D.
 *
 */
class CWP_Rule(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  val name = CWP.name
  val fullName = "Pharyngitis Treatment for Children"
  val description = "Pharyngitis Treatment for Children indicates whether a child, aged 2 to 18 years, who was seen with a diagnosis of pharyngitis " +
    "and had an antibiotic prescribed, received a group A streptococcus test within 3 days of their pharyngitis visit. This excludes " +
    "children who had an antibiotic prescription within the previous 30 days."

  import CWP._

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate.minusMonths(6))
    age >= 3 && age <= 18
  }

  override def generateEligibleClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // generate an eligible episode in the lst 6 months of the intake period
    val intakeEnd = hedisDate.minusMonths(6)
    val days = Utils.daysBetween(intakeEnd.minusMonths(4), intakeEnd)
    val dos1 = intakeEnd.minusDays(10 + Random.nextInt(days)) // provider visit - will be in the last 6 months of the intake period so we can insert excluding claim 
    val dos2 = dos1.plusDays(Random.nextInt(3)) // prescribed antibiotics

    pickOne(List(

      // Visit at ED
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", cpt = pickOne(cptA)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA), roomBoardFlag = "N"),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),

      // Visit at Outpatient facility
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), cpt = pickOne(cptB)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), ubRevenue = pickOne(ubB)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40))))()
  }

  override def scorePatientEligible(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    if (!isPatientMeetDemographic(patient)) scorecard.addScore(name, HEDISRule.eligible, false)
    else {

      // intake period - find all claim with only pharyngitis (icdDA) is primary diagnosis, no other diagnosis
      val claims = diagnosisClaims(icdDAS, ph, hedisDate)

      if (claims.isEmpty) scorecard
      else {

        // keep the first episode or claim (least recent)
        val claim = claims.head

        // check to ensure antibiotic was prescribed within 3 days of the diagnosis
        val next3 = new Interval(claim.dos, claim.dos.plusDays(4))
        val rxs = filterClaims(ph.ndc, ndcAS, { rx: RxClaim => next3.contains(rx.fillD) })
        if (rxs.isEmpty) scorecard
        else scorecard.addScore(name, HEDISRule.eligible, pharyngitisPatient, List(claim, rxs.head))
      }
    }
  }

  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // generate an excluding episode within the first 6 months
    val dos1 = hedisDate.minusMonths(16).plusDays(10 + Random.nextInt(60)) // provider visit
    val dos2 = dos1.minusDays(1 + Random.nextInt(28)) // prescribed antibiotics (to make it excluded)
    val dos3 = dos2.minusDays(30) // prescribed antibiotics (to make it excluded, alternate rule)
    val dos4 = dos1.plusDays(Random.nextInt(3)) // prescribed antibiotics (to keep it eligible)

    pickOne(List(

      // Visit at ED
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", cpt = pickOne(cptA)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos4, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA), roomBoardFlag = "N"),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos4, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),

      // make such that the antibiotics were dispensed more than 30 days prior the episode (diagnosis)
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", cpt = pickOne(cptA)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, ndc = pickOne(ndcA), daysSupply = Utils.daysBetween(dos3, dos1), qty = 90),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos4, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA), roomBoardFlag = "N"),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, ndc = pickOne(ndcA), daysSupply = Utils.daysBetween(dos3, dos1), qty = 90),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos4, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),

      // Visit at Outpatient facility
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), cpt = pickOne(cptB)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos4, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), ubRevenue = pickOne(ubB)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos4, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),

      // make such that the antibiotics were dispensed more than 30 days prior the episode (diagnosis)
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), cpt = pickOne(cptB)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, ndc = pickOne(ndcA), daysSupply = Utils.daysBetween(dos3, dos1), qty = 90),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos4, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), ubRevenue = pickOne(ubB)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, ndc = pickOne(ndcA), daysSupply = Utils.daysBetween(dos3, dos1), qty = 90),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos4, ndc = pickOne(ndcA), daysSupply = 20, qty = 40))))()
  }

  override def scorePatientExcluded(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    // get all claims with Antibiotic Medication prior to pharyngitis diagnosis
    def activeRxPriorDianosis(claim: MedClaim, ph: PatientHistory): List[Claim] = {

      // get all claims with Antibiotic Medication prior to pharyngitis diagnosis
      val prior90 = new Interval(claim.dos.minusMonths(3), claim.dos)
      filterClaims(ph.ndc, ndcAS, { rx: RxClaim => prior90.contains(rx.fillD) && ((Utils.daysBetween(rx.fillD, claim.dos) <= 30) || !rx.fillD.plusDays(rx.daysSupply).isBefore(claim.dos)) })
    }

    // get the episode claims
    val claims = diagnosisClaims(icdDAS, ph, hedisDate)
    if (claims.isEmpty) scorecard
    else {

      val claim = claims.head

      // get all claims with Antibiotic Medication prior to pharyngitis diagnosis to see if should be excluded
      val rxExclusion = activeRxPriorDianosis(claim, ph)
      if (rxExclusion.isEmpty) scorecard
      else scorecard.addScore(name, HEDISRule.excluded, "Excluded Patient", List.concat(List(claim), rxExclusion))
    }
  }

  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // generate meet measure episode within the first 6 months
    val dos1 = hedisDate.minusMonths(16).plusDays(10 + Random.nextInt(60)) // pharyngitis diagnosis
    val dos2 = dos1.plusDays(Random.nextInt(3)) // prescribed antibiotics
    val dos3 = dos1.minusDays(Random.nextInt(6) - 3) // group A streptococcus test

    pickOne(List(

      // Visit at ED
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", cpt = pickOne(cptA)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40),
        pickOne(List(
          pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, dos3, cpt = pickOne(cptC)),
          pl.createLabClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, loinc = pickOne(loincC))))),
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA), roomBoardFlag = "N"),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40),
        pickOne(List(
          pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, dos3, cpt = pickOne(cptC)),
          pl.createLabClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, loinc = pickOne(loincC))))),

      // Visit at Outpatient facility
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), cpt = pickOne(cptB)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40),
        pickOne(List(
          pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, dos3, cpt = pickOne(cptC)),
          pl.createLabClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, loinc = pickOne(loincC))))),
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA), ubRevenue = pickOne(ubB)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40),
        pickOne(List(
          pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, dos3, cpt = pickOne(cptC)),
          pl.createLabClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, loinc = pickOne(loincC)))))))()
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    // get all claims with Antibiotic Medication prior to pharyngitis diagnosis
    def streptococcusTest(claim: MedClaim, ph: PatientHistory): List[Claim] = {

      // get all claims streptococcus test in a window of +/- 3 days of claim.dos
      val testWindow = new Interval(claim.dos.minusDays(3), claim.dos.plusDays(4))
      val claims1 = filterClaims(ph.cpt, cptCS, { c: MedClaim => testWindow.contains(c.dos) })
      val claims2 = filterClaims(ph.loinc, loincCS, { c: LabClaim => testWindow.contains(c.dos) })

      List.concat(claims1, claims2)
    }

    // get the episode claims
    val claims = diagnosisClaims(icdDAS, ph, hedisDate)
    if (claims.isEmpty) scorecard
    else {

      val claim = claims.head

      // check to ensure antibiotic was prescribed within 3 days of the diagnosis
      val next3 = new Interval(claim.dos, claim.dos.plusDays(4))
      val rxs = filterClaims(ph.ndc, ndcAS, { rx: RxClaim => next3.contains(rx.fillD) })
      if (rxs.isEmpty) scorecard
      else {

        // check for group A streptococcus test
        val testClaims = streptococcusTest(claim, ph)
        if (testClaims.isEmpty) scorecard
        else scorecard.addScore(name, HEDISRule.meetMeasure, "Patient Meet Measure", testClaims)
      }
    }
  }
}
