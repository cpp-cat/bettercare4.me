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
import com.nickelsoftware.bettercare4me.models.LabClaim
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.MedClaim

object CDCHbA1cTestValue {

  val name7 = "CDC-HbA1c-Test-LT7-HEDIS-2014"
  val name8 = "CDC-HbA1c-Test-LT8-HEDIS-2014"
  val name9 = "CDC-HbA1c-Test-GT9-HEDIS-2014"

  /**
   * LOINC codes for HbA1c test
   */
  val loincA = List("4548-4", "4549-2", "17856-6", "69261-8", "62388-4", "71875-9")
  val loincAS = loincA.toSet
}

/**
 * Diabetes HbA1c Test (Less Than 7%, Less Than 8%, More Than 9%)
 *
 * Diabetes HbA1c less than 7%, 8% or more than 9% indicates whether a patient with type 1 or type 2 diabetes, aged 18 to 75 years, had at least one
 * HbA1c test record in the database, and the most recent non-zero test result value was less than 7%, 8% or more than 9%. This excludes patients with
 * a previous diagnosis of polycystic ovaries, gestational diabetes, or steroid-induced diabetes.
 *
 * NUMERATOR:
 * Identifies patients with type 1 or type 2 diabetes, aged 18 to 75 years, who had at least one HbA1c lab result record with a value
 * greater than zero and less than 7%, 8% or more than 9%.
 *
 */
class CDCHbA1cTestValueRule(ruleName: String, tag: String, cptV: String, baseVal: Double, meetCriteria: (LabClaim) => Boolean, config: RuleConfig, hedisDate: DateTime) extends CDCRuleBase(config, hedisDate) {

  val name = ruleName
  val fullName = "Diabetes HbA1c " + tag + "."
  val description = "Diabetes HbA1c "+tag+" indicates whether a patient with type 1 or type 2 diabetes, aged 18 to 75 years, had at least one" +
    "HbA1c test record in the database, and the most recent non-zero test result value was "+tag+". This excludes patients with" +
    "a previous diagnosis of polycystic ovaries, gestational diabetes, or steroid-induced diabetes."

  import CDCHbA1cTestValue._
  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = new Interval(hedisDate.minusYears(1), hedisDate).toDuration().getStandardDays().toInt
    val dos = hedisDate.minusDays(Random.nextInt(days))
    val dos2 = dos.minusDays(Random.nextInt(80))
    pickOne(List(

      // One possible set: Most recent HbA1c test result > 0% and < 8% (during the measurement year)
      () => List(
        pl.createLabClaim(patient.patientID, provider.providerID, dos, loinc = pickOne(loincA), result = baseVal + Random.nextDouble*0.01),
        pl.createLabClaim(patient.patientID, provider.providerID, dos2, loinc = pickOne(loincA), result = baseVal + Random.nextDouble*0.01)),

      // Another possible set: cpt = 3044F
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = cptV))))()
  }

  override def isPatientMeetMeasure(patient: Patient, ph: PatientHistory): Boolean = {

    val measurementInterval = new Interval(hedisDate.minusYears(1), hedisDate)

    // fnc to return true if the most recent lab claim has a result < 8%
    def checkMostRecentLabClaim(): Boolean = {

      // get all the claims that match the loinc
      val allClaims = ph.loinc flatMap {
        (_: (String, List[LabClaim])) match {
          case (k, l) =>
            if (loincAS.contains(k)) l filter {(c: LabClaim) => measurementInterval.contains(c.dos)}
            else List.empty

          case _ => List.empty
        }
      } toList

      // sort the claim by dos, in descending order. The head will be the most recent
      if(allClaims.isEmpty) false
      else meetCriteria(allClaims sortWith { (c1: LabClaim, c2: LabClaim) => c1.dos.isAfter(c2.dos) } head) 
    }

    def rules = List[() => Boolean](
        
      // Check if patient has claim with cpt 3044F
      () => firstTrue(ph.claims4CPT(cptV), {claim: MedClaim => measurementInterval.contains(claim.dos)}),

      // Check if most recent lab test
      () => checkMostRecentLabClaim)
      
    isAnyRuleMatch(rules)
  }
}

class CDCHbA1cTest7Rule(config: RuleConfig, hedisDate: DateTime) extends CDCHbA1cTestValueRule(CDCHbA1cTestValue.name7, "less than 7%", "3044F", 0.06, {(c: LabClaim) => c.result < 0.07}, config, hedisDate)
class CDCHbA1cTest8Rule(config: RuleConfig, hedisDate: DateTime) extends CDCHbA1cTestValueRule(CDCHbA1cTestValue.name8, "less than 8%", "3044F", 0.07, {(c: LabClaim) => c.result < 0.08}, config, hedisDate)
class CDCHbA1cTest9Rule(config: RuleConfig, hedisDate: DateTime) extends CDCHbA1cTestValueRule(CDCHbA1cTestValue.name9, "greater than 9%", "3046F", 0.091, {(c: LabClaim) => c.result > 0.09}, config, hedisDate)
