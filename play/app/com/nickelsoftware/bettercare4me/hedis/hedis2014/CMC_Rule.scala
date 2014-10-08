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
import com.nickelsoftware.bettercare4me.models.LabClaim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.nickelsoftware.bettercare4me.utils.Utils

object CMC {

  val nameTest = "CMC-LDL-C-Test-HEDIS-2014"
  val nameTestValue = "CMC-LDL-C-Test-Value-HEDIS-2014"

  val AMIorCABG = "AMI or CABG"
  val CPIprocedure = "CPI procedure"
  val IVDdiagnosis = "IVD Diagnosis"
    
  // ICD D - diagnosis of acute myocardial infarction (AMI)
  val icdDA = List("410.x1")
  val icdDAS = icdDA.toSet
  
  // CPT - coronary artery bypass graft (CABG) procedure
  //@TODO expand codes
  val cptA = List("33510-33514", "33516-33519", "33521-33523", "33533-33536")
  val cptAS = cptA.toSet

  // ICD P - coronary artery bypass graft (CABG) procedure
  //@TODO expand codes
  val icdPA = List("36.1*", "36.2")
  val icdPAS = icdPA.toSet

  // HCPCS - coronary artery bypass graft (CABG) procedure
  //@TODO expand codes
  val hcpcsA = List("S2205-S2209")
  val hcpcsAS = hcpcsA.toSet
  
  // POS - acute inpatient facility
  val posA = List("21", "25", "51", "55")
  val posAS = posA.toSet

  // ICD P - Percutaneous transluminal coronary angioplasty [PTCA] or coronary atherectomy
  //@TODO expand codes
  val icdPB = List("00.66", "36.01", "36.02", "36.05-36.07")
  val icdPBS = icdPB.toSet

  // CPT - Percutaneous transluminal coronary angioplasty [PTCA] or coronary atherectomy
  val cptB = List("92980", "92982", "92995")
  val cptBS = cptB.toSet

  // HCPCS - percutaneous coronary intervention (PCI) procedure
  val hcpcsB = List("G0290")
  val hcpcsBS = hcpcsB.toSet

  // ICD D - diagnosis of ischemic vascular disease (IVD)
  //@TODO expand codes
  val icdDB = List("411*", "413*", "414.0*", "414.2", "414.8", "414.9", "429.2", "433.00-434.91", "440.1", "440.2*", "440.4", "444*", "445*")
  val icdDBS = icdDB.toSet
  
  // CPT - outpatient visit and acute inpatient
  //@TODO expand codes
  val cptC = List("99201-99220", "99221-99233", "99238", "99239", "99241-99255", "99261-99263", "99291", "99341-99350", "99384-99387", "99394-99397", "99401-99404", "99411", "99412", "99420", "99429", "99455", "99456")
  val cptCS = cptC.toSet
  
  // UB Revenue - outpatient visit and acute inpatient
  //@TODO expand codes
  val ubA = List("010*", "0110-0114", "0119", "0120-0124", "0129", "0130-0134", "0139", "0140-0144", "0149", "0150-0154", "0159", "016*", "0200-0219", "051*", "0520-0523", "0526-0529", "0570-0599", "072*", "0982", "0983", "0987")
  val ubAS = ubA.toSet
  
  // POS - outpatient visit and acute inpatient
  val posB = List("4", "5", "6", "7", "8", "9", "11", "12", "15", "16", "20", "22", "23", "24", "26", "49", "50", "52", "53", "55", "57", "62", "65", "71", "72", "95", "99")
  val posBS = posB.toSet
}
/**
 * CAD Event Cholesterol Test Rule
 *
 * CAD Event Cholesterol Test indicates whether a patient, aged 18 to 75 years, who either had a cardiovascular event during the
 * year prior to the measurement year, or had a diagnosis of ischemic vascular disease (IVD) during both the measurement year
 * and the year prior, had at least one LDL cholesterol screening test done.
 *
 * DENOMINATOR:
 * Identifies the unique count of patients, aged 18 to 75 years at the end of the measurement year, who either had a cardiovascular
 * event during the year prior to the measurement year, or had a diagnosis of ischemic vascular disease both during the
 * measurement year and the year prior.
 *
 * EXCLUSIONS:
 * None
 *
 * NUMERATOR:
 * Identifies patients, aged 18 to 75 years, who either had a cardiovascular event during the year prior to the measurement year, or
 * had a diagnosis of IVD during both the measurement year and the year prior, and had at least one LDL cholesterol screening test done.
 *
 */
abstract class CMC_RuleBase(val name: String, config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  val fullName = "CAD Event Cholesterol Test"
  val description = "CAD Event Cholesterol Test indicates whether a patient, aged 18 to 75 years, who either had a cardiovascular event during the " +
    "year prior to the measurement year, or had a diagnosis of ischemic vascular disease (IVD) during both the measurement year " +
    "and the year prior, had at least one LDL cholesterol screening test done."

  import CMC._

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate)
    age >= 18 && age <= 75
  }

  // This rule has 0% exclusion when the demographics are meet
  override val exclusionRate: Int = 0
  override def scorePatientExcluded(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard = scorecard.addScore(name, HEDISRule.excluded, false)

  override def generateEligibleClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val d0 = hedisDate.minusYears(2)
    val days1 = new Interval(d0, d0.plusMonths(10)).toDuration().getStandardDays().toInt
    val dos1 = d0.plusDays(Random.nextInt(days1) + 1)
    
    val days2 = getIntervalFromYears(1).toDuration().getStandardDays().toInt
    val dos2 = hedisDate.minusDays(Random.nextInt(days2))
    val dos3 = dos2.minusYears(1)
    
    pickOne(List(

      // acute inpatient facility with a diagnosis of acute myocardial infarction (AMI) or a
      // coronary artery bypass graft (CABG) procedure
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri=pickOne(icdDA), hcfaPOS=pickOne(posA), dischargeStatus="01", roomBoardFlag="Y")),
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, cpt=pickOne(cptA), hcfaPOS=pickOne(posA), dischargeStatus="01", roomBoardFlag="Y")),
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, hcpcs=pickOne(hcpcsA), hcfaPOS=pickOne(posA), dischargeStatus="01", roomBoardFlag="Y")),
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdP=Set(pickOne(icdPA)), hcfaPOS=pickOne(posA), dischargeStatus="01", roomBoardFlag="Y")),

      // A percutaneous coronary intervention (PCI) procedure performed in any setting
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, cpt=pickOne(cptB))),
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdP=Set(pickOne(icdPB)))),
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, hcpcs=pickOne(hcpcsB))),
      
      // At least one outpatient or acute inpatient visit with a diagnosis of ischemic vascular disease (IVD)
      () => List(
          pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, icdDPri=pickOne(icdDB), cpt=pickOne(cptC)),
          pl.createMedClaim(patient.patientID, provider.providerID, dos3, dos3, icdDPri=pickOne(icdDB), ubRevenue=pickOne(ubA), hcfaPOS=pickOne(posB))
          )
        ))()
  }

  override def scorePatientEligible(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val d0 = hedisDate.minusYears(2).plusDays(1)
    val measurementInterval1 = new Interval(d0, d0.plusMonths(10))

    val measurementInterval2 = getIntervalFromYears(1)
    val measurementInterval3 = Utils.getIntervalFromYears(1, hedisDate.minusYears(1))
    
    def rules = List[(Scorecard) => Scorecard](

      // acute inpatient facility with a diagnosis of acute myocardial infarction (AMI) or a
      // coronary artery bypass graft (CABG) procedure
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdD, icdDAS, { claim: MedClaim => measurementInterval1.contains(claim.dos) && posAS.contains(claim.hcfaPOS) && claim.dischargeStatus!="20" && claim.roomBoardFlag=="Y" })
        s.addScore(name, HEDISRule.eligible, AMIorCABG, claims)
      },
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdP, icdPAS, { claim: MedClaim => measurementInterval1.contains(claim.dos) && posAS.contains(claim.hcfaPOS) && claim.dischargeStatus!="20" && claim.roomBoardFlag=="Y" })
        s.addScore(name, HEDISRule.eligible, AMIorCABG, claims)
      },
      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, cptAS, { claim: MedClaim => measurementInterval1.contains(claim.dos) && posAS.contains(claim.hcfaPOS) && claim.dischargeStatus!="20" && claim.roomBoardFlag=="Y" })
        s.addScore(name, HEDISRule.eligible, AMIorCABG, claims)
      },
      (s: Scorecard) => {
        val claims = filterClaims(ph.hcpcs, hcpcsAS, { claim: MedClaim => measurementInterval1.contains(claim.dos) && posAS.contains(claim.hcfaPOS) && claim.dischargeStatus!="20" && claim.roomBoardFlag=="Y" })
        s.addScore(name, HEDISRule.eligible, AMIorCABG, claims)
      },

      // A percutaneous coronary intervention (PCI) procedure performed in any setting
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdP, icdPBS, { claim: MedClaim => measurementInterval1.contains(claim.dos) })
        s.addScore(name, HEDISRule.eligible, CPIprocedure, claims)
      },
      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, cptBS, { claim: MedClaim => measurementInterval1.contains(claim.dos) })
        s.addScore(name, HEDISRule.eligible, CPIprocedure, claims)
      },
      (s: Scorecard) => {
        val claims = filterClaims(ph.hcpcs, hcpcsBS, { claim: MedClaim => measurementInterval1.contains(claim.dos) })
        s.addScore(name, HEDISRule.eligible, CPIprocedure, claims)
      },

      // At least one outpatient or acute inpatient visit with a diagnosis of ischemic vascular disease (IVD)
      (s: Scorecard) => {
        val claims2 = filterClaims(ph.icdD, icdDBS, { claim: MedClaim => measurementInterval2.contains(claim.dos) && (cptCS.contains(claim.cpt) || (ubAS.contains(claim.ubRevenue) && posBS.contains(claim.hcfaPOS)) ) })
        val claims3 = filterClaims(ph.icdD, icdDBS, { claim: MedClaim => measurementInterval3.contains(claim.dos) && (cptCS.contains(claim.cpt) || (ubAS.contains(claim.ubRevenue) && posBS.contains(claim.hcfaPOS)) ) })
        if(claims2.isEmpty || claims3.isEmpty) s
        else s.addScore(name, HEDISRule.eligible, IVDdiagnosis, List.concat(claims2, claims3))
      })
      
    applyRules(scorecard, rules)
  }
}

class CMC_LDL_C_TestRule(config: RuleConfig, hedisDate: DateTime) extends CMC_RuleBase(CMC.nameTest, config, hedisDate) {

  import CDC_LDL_C._
  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = getIntervalFromYears(1).toDuration().getStandardDays().toInt
    val dos = hedisDate.minusDays(Random.nextInt(days))

    // At least one Lipid Test (during the measurement year)
    pickOne(List(

      // Possible set: CPT
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptA))),

      // Another possible set: LOINC on lab claim
      () => List(pl.createLabClaim(patient.patientID, provider.providerID, dos, loinc = pickOne(loincA) )) ))()
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
        val claims = filterClaims(ph.loinc, loincAS, { claim: LabClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, loincLipidTest, claims)
      })
      
    applyRules(scorecard, rules)
  }  
}

class CMC_LDL_C_TestValueRule(config: RuleConfig, hedisDate: DateTime) extends CMC_RuleBase(CMC.nameTestValue, config, hedisDate) {

  import CDC_LDL_C._
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
