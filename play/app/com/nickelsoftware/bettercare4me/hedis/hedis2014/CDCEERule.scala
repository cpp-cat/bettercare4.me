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

object CDCEE {

  val name = "CDC-EE-HEDIS-2014"

  /**
   * NUCC Provider Taxonomy Code for Eye Care Professional
   * @see http://www.nucc.org/
   */
  val specialtyCdeA = List("152W00000X", "152WC0802X", "152WL0500X", "152WP0200X", "152WS0006X", "152WV0400X", "152WX0102X")
  def specialtyCdeAS = specialtyCdeA.toSet

  /**
   * CPT codes for Eye Exam
   */
  val cptA = List(
    "67028", "67030", "67031", "67036", "67039", "67040", "67041", "67042", "67043",
    "67101", "67105", "67107", "67108", "67110", "67112", "67113", "67121", "67141",
    "67145", "67208", "67210", "67218", "67220", "67221", "67227", "67228", "92002",
    "92004", "92012", "92014", "92018", "92019", "92134", "92225", "92226", "92227",
    "92228", "92230", "92235", "92240", "92250", "92260", "99203", "99204", "99205",
    "99213", "99214", "99215", "99242", "99243", "99244", "99245")
  val cptAS = cptA.toSet

  /**
   * CPT Type II for Eye Exam
   */
  val cptB = List("2022F", "2024F", "2026F", "3072F")
  val cptBS = cptB.toSet

  val hcpcsA = List("S0620", "S0621", "S0625", "S3000")
  val hcpcsAS = hcpcsA.toSet
}

/**
 * Diabetes Eye Exam
 *
 * Diabetes Eye Exam indicates whether a patient with type 1 or type 2 diabetes, aged 18 to 75 years, had an eye exam performed.
 * This excludes patients with a previous diagnosis of polycystic ovaries, gestational diabetes, or steroid-induced diabetes.
 *
 * NUMERATOR:
 * Identifies patients with type 1 or type 2 diabetes, aged 18 to 75 years, who had an eye exam done. NOTE: Through
 * administrative data there is no way to determine whether a dilated eye exam was performed. Therefore, eye exams provided by
 * eye care professionals are used as a proxy for dilated exams.
 *
 */
class CDCEERule(config: RuleConfig, hedisDate: DateTime) extends CDCRuleBase(config, hedisDate) {

  val name = CDCEE.name
  val fullName = "Diabetes Eye Exam"
  val description = "Diabetes Eye Exam indicates whether a patient with type 1 or type 2 diabetes, aged 18 to 75 years, had an eye exam performed. " +
    "This excludes patients with a previous diagnosis of polycystic ovaries, gestational diabetes, or steroid-induced diabetes."

  import CDCEE._
  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = new Interval(hedisDate.minusYears(1), hedisDate).toDuration().getStandardDays().toInt
    val dos = hedisDate.minusDays(Random.nextInt(days))
    val splty = pickOne(specialtyCdeA)

    // At least one Eye Exam test (during the measurement year)
    pickOne(List(

      // One possible set: CPT by Eye Professional
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, specialtyCde = splty, cpt = pickOne(cptA))),

      // One possible set: CPT Type II
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptB))),

      // Another possible set: HCPCS by Eye Professional
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, specialtyCde = splty, hcpcs = pickOne(hcpcsA)))))()
  }

  override def isPatientMeetMeasure(patient: Patient, ph: PatientHistory): Boolean = {

    val measurementInterval = new Interval(hedisDate.minusYears(1), hedisDate)

    def rules = List[() => Boolean](

      // Check for patient has CPT by Eye Professional
      () => firstMatch(ph.cpt, cptAS, { claim: MedClaim => measurementInterval.contains(claim.dos) && specialtyCdeAS.contains(claim.specialtyCde) }),

      // Check for CPT Type II
      () => firstMatch(ph.cpt, cptBS, { claim: MedClaim => measurementInterval.contains(claim.dos) }),

      // Check for HCPCS by Eye Professional
      () => firstMatch(ph.hcpcs, hcpcsAS, { claim: MedClaim => measurementInterval.contains(claim.dos) && specialtyCdeAS.contains(claim.specialtyCde) }))

    isAnyRuleMatch(rules)

  }

}
