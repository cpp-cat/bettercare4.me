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

object W15 {

  val name = "W15-HEDIS-2014"

  val wellChildVisit0 = "0 Well Child Visit"
  val wellChildVisit1 = "1 Well Child Visit"
  val wellChildVisit2 = "2 Well Child Visits"
  val wellChildVisit3 = "3 Well Child Visits"
  val wellChildVisit4 = "4 Well Child Visits"
  val wellChildVisit5 = "5 Well Child Visits"
  val wellChildVisit6 = "6 or More Well Child Visits"

  // CPT for well child visit
  val cptA = List("99381", "99382", "99391", "99392", "99432", "99461")
  val cptAS = cptA.toSet

  // ICD D for well child visit
  val icdDA = List("V20.2", "V70.0", "V70.3", "V70.5", "V70.6", "V70.8", "V70.9")
  val icdDAS = icdDA.toSet
}
/**
 * Well-Child Visits in the First 15 Months of Life Rule
 *
 * The percentage of members who turned 15 months old during the measurement year and who had 0 to 6 well-child visits with a PCP during their first 15 months of life.
 *
 * NUMERATOR:
 * Seven separate numerators are calculated, corresponding to the number of
 * members who received 0, 1, 2, 3, 4, 5, 6 or more well-child visits with a PCP during
 * their first 15 months of life.
 *
 */
class W15_Rule(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  val name = W15.name
  val fullName = "Well-Child Visits in the First 15 Months of Life"
  val description = "The percentage of members who turned 15 months old during the measurement year and who had the 0 to 6 well-child visits with a PCP during their first 15 months of life."

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.ageInMonths(hedisDate)
    age >= 15 && age <= 26
  }
  
  import W15._

  // This rule has 100% eligibility when the demographics are meet
  override val eligibleRate: Int = 100

  // This rule has 0% exclusion when the demographics are meet
  override val exclusionRate: Int = 0

  // This rule has 100% meet measure (the predicate will indicate the number of visit performed)
  override val meetMeasureRate: Int = 100

  override def scorePatientExcluded(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = scorecard.addScore(name, HEDISRule.excluded, false)

  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val nVisits = Random.nextInt(7)
    val days = 40
    val dos = patient.dob.plusDays(Random.nextInt(days))
    val rules = List(

      // One possible set of claims based on cpt
      (d: DateTime) => pl.createMedClaim(patient.patientID, provider.providerID, d, d, cpt = pickOne(cptA)),

      // Another possible set of claims based on ICD D
      (d: DateTime) => pl.createMedClaim(patient.patientID, provider.providerID, d, d, icdDPri = pickOne(icdDA)) )
      
    def generateClaims(d: DateTime, n: Int, l: List[Claim]): List[Claim] = {
      if(n == 0) l
      else {
        generateClaims(d.plusDays(days), n-1, pickOne(rules)(d) :: l)
      }
    }
    
    generateClaims(dos, nVisits, List())
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = new Interval(patient.dob, patient.dob.plusMonths(15).plusDays(1))

    def rules = List[(Scorecard) => Scorecard](

      // Check if patient had Bilateral Mastectomy (anytime prior to or during the measurement year)
      (s: Scorecard) => {
        val claims1 = filterClaims(ph.cpt, cptAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claims2 = filterClaims(ph.icdD, icdDAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claims = List.concat(claims1, claims2)
        //* may want to make sure the claims are all on different dos
        claims.size match {
          case 0 => s.addScore(name, HEDISRule.meetMeasure, wellChildVisit0)  
          case 1 => s.addScore(name, HEDISRule.meetMeasure, wellChildVisit1, claims)  
          case 2 => s.addScore(name, HEDISRule.meetMeasure, wellChildVisit2, claims)  
          case 3 => s.addScore(name, HEDISRule.meetMeasure, wellChildVisit3, claims)  
          case 4 => s.addScore(name, HEDISRule.meetMeasure, wellChildVisit4, claims)  
          case 5 => s.addScore(name, HEDISRule.meetMeasure, wellChildVisit5, claims)
          case _ => s.addScore(name, HEDISRule.meetMeasure, wellChildVisit6, claims)  
        }
      })

    applyRules(scorecard, rules)
  }

}
