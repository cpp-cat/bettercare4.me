/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014

import scala.util.Random

import org.joda.time.DateTime
import org.joda.time.Interval

import com.nickelsoftware.bettercare4me.hedis.HEDISRuleBase
import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.models.MedClaim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig

/**
 * Breast Cancer Screening Rule
 */
class BCSRule(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  def name = "BCS-HEDIS-2014"
  def fullName = "Breast Cancer Screening"
  def description = "Breast Cancer Screening indicates whether a woman member, aged 42 to 69 years, had a mammogram done during the " +
    "measurement year or the year prior to the measurement year. This excludes women who had a bilateral mastectomy or two " +
    "unilateral mastectomies."

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate)
    patient.gender == "F" && age > 41 && age < 70
  }

  // This rule has 100% eligibility when the demographics are meet
  override def eligibleRate: Int = 100

  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = new Interval(hedisDate.minusYears(2), hedisDate).toDuration().getStandardDays().toInt
    val dos1 = hedisDate.minusDays(Random.nextInt(days))
    pickOne(List(
      // One possible set of claims based on ICD Procedure code
       () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdP = Set(pickOne(List("85.42", "85.44", "85.46", "85.48"))))),
      // Another possible set of claims based on CPT codes and modifier being 50
       () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, cpt = pickOne(List("19180", "19200", "19220", "19240", "19303", "19304", "19305", "19306", "19307")), cptMod1 = "50")),
       () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, cpt = pickOne(List("19180", "19200", "19220", "19240", "19303", "19304", "19305", "19306", "19307")), cptMod2 = "50")),
      // Another possible set of claims based on 2 claims have CPT codes and each have one of the modifier RT and LT
      {() =>
        val dos2 = hedisDate.minusDays(Random.nextInt(days))
        List(
          pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, cpt = pickOne(List("19180", "19200", "19220", "19240", "19303", "19304", "19305", "19306", "19307")), cptMod1 = "RT"),
          pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, cpt = pickOne(List("19180", "19200", "19220", "19240", "19303", "19304", "19305", "19306", "19307")), cptMod2 = "LT"))
      }))()
  }

  override def isPatientExcluded(patient: Patient, ph: PatientHistory): Boolean = {

    def hasCPTwithMod(mod: String): Boolean = {
      firstTrue(List("19180", "19200", "19220", "19240", "19303", "19304", "19305", "19306", "19307"), { cpt: String =>
        firstTrue(ph.claims4CPT(cpt), { claim: MedClaim =>
          claim.dos.isBefore(hedisDate) && (claim.cptMod1 == mod || claim.cptMod2 == mod)
        })
      })
    }

    def rules = List[() => Boolean](
      // Check if patient had Bilateral Mastectomy (anytime prior to or during the measurement year)
      () => firstTrue(List("85.42", "85.44", "85.46", "85.48"), { icdP: String =>
        firstTrue(ph.claims4ICDP(icdP), { claim: MedClaim =>
          claim.dos.isBefore(hedisDate)
        })
      }),

      // Check if patient had a Unilateral Mastectomy with bilateral modifier (anytime prior to or during the measurement year)
      () => hasCPTwithMod("50"),

      // Check if patient had a previous right unilateral mastectomy and a previous
      // left unilateral mastectomy (anytime prior to or during the measurement year)
      () => hasCPTwithMod("RT") && hasCPTwithMod("LT"))

    isAnyRuleMatch(rules)
  }

  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = new Interval(hedisDate.minusYears(2), hedisDate).toDuration().getStandardDays().toInt
    val dos = hedisDate.minusDays(Random.nextInt(days))
    pickOne(List(
      // One possible set of claims based on cpt
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(List("76083", "76090", "76091", "76092")))),
      // Another possible set of claims based on hcpcs
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, hcpcs = pickOne(List("G0202", "G0204", "G0206")))),
      // Another possible set of claims based on ICD Procedure codes
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdP = Set(pickOne(List("87.36", "87.37"))))),
      // Another possible set of claims based on UB Revenue Procedure codes
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, ubRevenue = pickOne(List("0401", "0403"))))))()
  }

  override def isPatientMeetMeasure(patient: Patient, ph: PatientHistory): Boolean = {

    val measurementInterval = new Interval(hedisDate.minusYears(2), hedisDate)
    def rules = List[() => Boolean](
      // Check if patient had a Mamogram performed (during the measurement year or the year before)
      () => firstTrue(List("76083", "76090", "76091", "76092"), { cpt: String =>
        firstTrue(ph.claims4CPT(cpt), { claim: MedClaim => measurementInterval.contains(claim.dos) })
      }),

      () => firstTrue(List("G0202", "G0204", "G0206"), { hcpcs: String =>
        firstTrue(ph.claims4HCPCS(hcpcs), { claim: MedClaim => measurementInterval.contains(claim.dos) })
      }),

      () => firstTrue(List("87.36", "87.37"), { icdP: String =>
        firstTrue(ph.claims4ICDP(icdP), { claim: MedClaim => measurementInterval.contains(claim.dos) })
      }),

      () => firstTrue(List("0401", "0403"), { ubRevevue: String =>
        firstTrue(ph.claims4UBRev(ubRevevue), { claim: MedClaim => measurementInterval.contains(claim.dos) })
      }))

    isAnyRuleMatch(rules)
  }

}
