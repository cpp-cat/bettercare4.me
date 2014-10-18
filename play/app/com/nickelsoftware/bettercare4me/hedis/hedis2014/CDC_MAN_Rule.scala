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
import com.nickelsoftware.bettercare4me.models.RxClaim
import com.nickelsoftware.bettercare4me.utils.Utils

object CDC_MAN {

  val name = "CDC-MAN-HEDIS-2014"

  val urineMicroalbuminTest = "Urine Microalbumin Test"
  val neuphrologist = "Vist to a Neuphrologist"
  val nephropathyTreatment1 = "Diagnosis / Treatment for Nephropathy (1)"
  val nephropathyTreatment2 = "Diagnosis / Treatment for Nephropathy (2)"
  val nephropathyTreatment3 = "Diagnosis / Treatment for Nephropathy (3)"
  val nephropathyTreatment4 = "Diagnosis / Treatment for Nephropathy (4)"
  val nephropathyTreatment5 = "Diagnosis / Treatment for Nephropathy (5)"
  val nephropathyTreatment6 = "Diagnosis / Treatment for Nephropathy (6)"
  val nephropathyTreatment7 = "Diagnosis / Treatment for Nephropathy (7)"
  val aceArbTheraphy = "ACE / ARB Theraphy"

  /**
   * Urine Microalbumin Test CPT codes
   */
  val cptA = List("82042", "82043", "82044", "84156", "3060F", "3061F", "3062F")
  val cptAS = cptA.toSet

  /**
   * Urine Microalbumin Test LOINC codes
   */
  val loincA = List("3060F", "3061F", "1753-3", "1754-1", "1755-8", "1757-4", "2887-8", "2888-6", "2889-4", "2890-2", "9318-7", "11218-5", "12842-1", "13801-6", "14585-4", "18373-1", "20621-9", "21059-1", "21482-5", "26801-1", "27298-9", "30000-4", "30001-2", "30003-8", "32209-9", "32294-1", "32551-4", "34366-5", "34535-5", "35663-4", "40486-3", "40662-9", "40663-7", "43605-5", "43606-3", "43607-1", "44292-1", "14956-7", "14957-5", "14958-3", "14959-1", "13705-9", "47558-2", "49023-5", "50561-0", "50949-7", "53121-0", "53525-2", "53530-2", "53531-0", "53532-8", "56553-1", "57369-1", "58448-2", "58992-9", "59159-4", "60678-0", "63474-1", "69419-0")
  val loincAS = loincA.toSet

  /**
   * Visit to a Neuphrologist
   */
  val nuccA = List("207RN0300X", "2080P0210X")
  val nuccAS = nuccA.toSet

  /**
   * Diagnosis / Treatment for Nephropathy (ICD Diagnosis)
   */
  val icdDA = List(
    "250.4", "250.40", "250.41", "250.42", "250.43",
    "403", "403.0", "403.00", "403.01", "403.1", "403.10", "403.11", "403.9", "403.90", "403.91",
    "404", "404.0", "404.00", "404.01", "404.02", "404.03", "404.1", "404.10", "404.11", "404.12", "404.13", "404.9", "404.90", "404.91", "404.92", "404.93",
    "405.01", "405.11", "405.91",
    "5800", "5801", "5802", "5803", "5804", "5805", "5806", "5807", "5808", "5809", "5810", "5811", "5812", "5813", "5814", "5815", "5816", "5817", "5818", "5819", "5820", "5821", "5822", "5823", "5823", "5824", "5825", "5826", "5827", "5828", "5829", "5830", "5831", "5832", "5833", "5834", "5835", "5836", "5837", "5838", "5839", "5840", "5841", "5842", "5843", "5844", "5845", "5846", "5847", "5848", "5849", "5850", "5851", "5852", "5853", "5854", "5855", "5856", "5857", "5858", "5859", "5860", "5861", "5862", "5863", "5864", "5865", "5866", "5867", "5868", "5869", "5870", "5871", "5872", "5873", "5874", "5875", "5876", "5877", "5878", "5879", "5880", "5881", "5882", "5883", "5884", "5885", "5886", "5887", "5888", "5889",
    "753.0", "753.1", "753.10", "753.11", "753.12", "753.13", "753.14", "753.15", "753.16", "753.17", "753.19",
    "791.0", "V42.0", "V45.1")
  val icdDAS = icdDA.toSet

  /**
   * Diagnosis / Treatment for Nephropathy (CPT)
   */
  val cptB = List("36145", "36147", "36800", "36810", "36815", "36818", "36819", "36820", "36821", "36831", "36832", "36833", "50300", "50320", "50340", "50360", "50365", "50370", "50380", "90920", "90921", "90924", "90925", "90935", "90937", "90939", "90940", "90945", "90947", "90957", "90958", "90959", "90960", "90961", "90962", "90965", "90966", "90969", "90970", "90989", "90993", "90997", "90999", "99512", "3066F", "4009F")
  val cptBS = cptB.toSet

  /**
   * Diagnosis / Treatment for Nephropathy (HCPCS)
   */
  val hcpcsA = List("G0257", "G0314", "G0315", "G0316", "G0317", "G0318", "G0319", "G0322", "G0323", "G0326", "G0327", "G0392", "G0393", "S9339")
  val hcpcsAS = hcpcsA.toSet

  /**
   * Diagnosis / Treatment for Nephropathy (icdP)
   */
  val icdPA = List("38.95", "39.27", "39.42", "39.43", "39.53", "39.93", "39.94", "39.95", "54.98", "55.4", "55.51", "55.52", "55.53", "55.54", "55.61", "55.69")
  val icdPAS = icdPA.toSet

  /**
   * Diagnosis / Treatment for Nephropathy (UB)
   */
  val ubA = List("0367",
    "0800", "0801", "0802", "0803", "0804", "0809", "080X",
    "0820", "0821", "0822", "0823", "0824", "0829", "082X",
    "0830", "0831", "0832", "0833", "0834", "0839", "083X",
    "0840", "0841", "0842", "0843", "0844", "0849", "084X",
    "0850", "0851", "0852", "0853", "0854", "0859", "085X",
    "0880", "0881", "0882", "0889", "088X")
  val ubAS = ubA.toSet

  /**
   * Diagnosis / Treatment for Nephropathy (TOB)
   */
  val tobA = List("720", "721", "722", "723", "724", "725", "726", "727", "728", "729")
  val tobAS = tobA.toSet

  /**
   * Diagnosis / Treatment for Nephropathy (HCFA POS)
   */
  val hcfaposA = List("65")
  val hcfaposAS = hcfaposA.toSet
}

class CDC_MAN_Rule(config: RuleConfig, hedisDate: DateTime) extends CDCRuleBase(config, hedisDate) {

  val name = CDC_MAN.name
  val fullName = "Diabetes Microalbumin Test for Nephropathy"
  val description = "Diabetes Microalbumin Test for Nephropathy indicates whether a patient with type 1 or type 2 diabetes, aged 18 to 75 years, had" +
    "a screening test for nephropathy or evidence of nephropathy. This excludes patients with a previous diagnosis of polycystic" +
    "ovaries, gestational diabetes, or steroid-induced diabetes."

  import CDC_MAN._
  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = Utils.daysBetween(hedisDate.minusYears(1), hedisDate)
    val dos = hedisDate.minusDays(Random.nextInt(days))

    pickOne(List(

      // Possible set: Urine Microalbumin Test CPT codes
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptA))),

      // Another possible set: Urine Microalbumin Test LOINC codes
      () => List(pl.createLabClaim(patient.patientID, provider.providerID, dos, loinc = pickOne(loincA))),

      // Visit to a Neuphrologist
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, specialtyCde = pickOne(nuccA))),

      // Diagnosis / Treatment for Nephropathy (ICD Diagnosis)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdDPri = pickOne(icdDA))),

      // Diagnosis / Treatment for Nephropathy (CPT)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptB))),

      // Diagnosis / Treatment for Nephropathy (HCPCS)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, hcpcs = pickOne(hcpcsA))),

      // Diagnosis / Treatment for Nephropathy (ICD P)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdP = Set(pickOne(icdPA)))),

      // Diagnosis / Treatment for Nephropathy (UB)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, ubRevenue = pickOne(ubA))),

      // Diagnosis / Treatment for Nephropathy (TOB)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, tob = pickOne(tobA))),

      // Diagnosis / Treatment for Nephropathy (NDC)
      () => List(pl.createRxClaim(patient.patientID, provider.providerID, dos, ndc = pickOne(CDC.ndcL), daysSupply = 30, qty = 60),
        pl.createRxClaim(patient.patientID, provider.providerID, dos.minusDays(30 + Random.nextInt(7)), ndc = pickOne(CDC.ndcL), daysSupply = 30, qty = 60),
        pl.createRxClaim(patient.patientID, provider.providerID, dos.minusDays(60 + Random.nextInt(7)), ndc = pickOne(CDC.ndcL), daysSupply = 30, qty = 60)),

      // Diagnosis / Treatment for Nephropathy (HCFA POS)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, hcfaPOS = pickOne(hcfaposA)))))()
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = getIntervalFromYears(1)

    def rules = List[(Scorecard) => Scorecard](

      // Check for patient has Urine Microalbumin Test CPT codes
      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, cptAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, urineMicroalbuminTest, claims)
      },

      // Check for patient has Urine Microalbumin Test LOINC codes
      (s: Scorecard) => {
        val claims = filterClaims(ph.loinc, loincAS, { claim: LabClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, urineMicroalbuminTest, claims)
      },

      // Check for patient visit to a Neuphrologist
      (s: Scorecard) => {
        val claims = filterClaims(ph.specialtyCde, nuccAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, neuphrologist, claims)
      },

      // Diagnosis / Treatment for Nephropathy (ICD Diagnosis)
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdD, icdDAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, nephropathyTreatment1, claims)
      },

      // Diagnosis / Treatment for Nephropathy (CPT)
      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, cptBS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, nephropathyTreatment2, claims)
      },

      // Diagnosis / Treatment for Nephropathy (HCPCS)
      (s: Scorecard) => {
        val claims = filterClaims(ph.hcpcs, hcpcsAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, nephropathyTreatment3, claims)
      },

      // Diagnosis / Treatment for Nephropathy (ICD P)
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdP, icdPAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, nephropathyTreatment4, claims)
      },

      // Diagnosis / Treatment for Nephropathy (UB)
      (s: Scorecard) => {
        val claims = filterClaims(ph.ubRevenue, ubAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, nephropathyTreatment5, claims)
      },

      // Diagnosis / Treatment for Nephropathy (TOB)
      (s: Scorecard) => {
        val claims = filterClaims(ph.tob, tobAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, nephropathyTreatment6, claims)
      },

      // Diagnosis / Treatment for Nephropathy (NDC)
      (s: Scorecard) => {
        val claims = filterClaims(ph.ndc, CDC.ndcLS, { claim: RxClaim => measurementInterval.contains(claim.fillD) })
        s.addScore(name, HEDISRule.meetMeasure, aceArbTheraphy, claims)
      },

      // Diagnosis / Treatment for Nephropathy (TOB)
      (s: Scorecard) => {
        val claims = filterClaims(ph.hcfaPOS, hcfaposAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, nephropathyTreatment7, claims)
      })

    applyRules(scorecard, rules)
  }
}
