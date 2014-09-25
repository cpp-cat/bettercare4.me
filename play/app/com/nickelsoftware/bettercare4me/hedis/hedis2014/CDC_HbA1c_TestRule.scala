/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014

import scala.util.Random
import org.joda.time.DateTime
import org.joda.time.Interval
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.MedClaim

object CDCHbA1cTest {
  
  val name = "CDC-HbA1c-Test-HEDIS-2014"

  /**
   * CPT codes for HbA1c Test
   */
  val cptA = List("83036", "83037", "3044F", "3045F", "3046F", "3047F")
  val cptAS = cptA.toSet
}

/**
 * Diabetes HbA1c Test
 *
 * Diabetes HbA1c Test indicates whether a patient with type 1 or type 2 diabetes, aged 18 to 75 years, had a hemoglobin A1c test
 * performed. This excludes patients with a previous diagnosis of polycystic ovaries, gestational diabetes, or steroid-induced
 * diabetes.
 *
 * NUMERATOR:
 * Identifies patients with type 1 or type 2 diabetes, aged 18 to 75 years, who had an HbA1c test done.
 *
 */
class CDCHbA1cTestRule(config: RuleConfig, hedisDate: DateTime) extends CDCRuleBase(config, hedisDate) {

  val name = CDCHbA1cTest.name
  val fullName = "Diabetes HbA1c Test"
  val description = "Diabetes HbA1c Test indicates whether a patient with type 1 or type 2 diabetes, aged 18 to 75 years, had a hemoglobin A1c test" +
    "performed. This excludes patients with a previous diagnosis of polycystic ovaries, gestational diabetes, or steroid-induced" +
    "diabetes."

  import CDCHbA1cTest._
  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = new Interval(hedisDate.minusYears(1), hedisDate).toDuration().getStandardDays().toInt
    val dos = hedisDate.minusDays(Random.nextInt(days))

    // At least one HbA1c test (during the measurement year)
    List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptA)))
  }

  override def isPatientMeetMeasure(patient: Patient, ph: PatientHistory): Boolean = {

    val measurementInterval = new Interval(hedisDate.minusYears(1), hedisDate)

    // Check if patient had at least one HbA1c test (during the measurement year)
    firstMatch(ph.cpt, cptAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
  }

}
