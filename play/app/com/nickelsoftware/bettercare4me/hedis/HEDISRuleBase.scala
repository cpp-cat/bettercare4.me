/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis

import org.joda.time.DateTime
import org.joda.time.Interval

import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.nickelsoftware.bettercare4me.utils.Utils

abstract class HEDISRuleBase(val config: RuleConfig, hedisDate: DateTime) extends HEDISRule {

  val eligibleRate: Int = config.eligibleRate
  val meetMeasureRate: Int = config.meetMeasureRate
  val exclusionRate: Int = config.exclusionRate

  /**
   * Utility method to get an `Interval` from the `hedisDate` to the nbr of specified years prior to it.
   *
   * @param years number of years for the interval, just prior the `hedisDate`
   * @return The calculated interval, including the hedisDate
   */
  def getIntervalFromYears(years: Int): Interval = Utils.getIntervalFromYears(years, hedisDate)

  /**
   * Utility method to get an `Interval` from the `hedisDate` to the nbr of specified months prior to it.
   *
   * @param months number of months for the interval, just prior the `hedisDate`
   * @return The calculated interval, including the hedisDate
   */
  def getIntervalFromMonths(months: Int): Interval = Utils.getIntervalFromMonths(months, hedisDate)

  /**
   * Utility method to get an `Interval` from the `hedisDate` to the nbr of specified days prior to it.
   *
   * @param days number of days for the interval, just prior the `hedisDate`
   * @return The calculated interval, including the hedisDate
   */
  def getIntervalFromDays(days: Int): Interval = Utils.getIntervalFromDays(days, hedisDate)

  /**
   * Utility method to get an `Interval` from the `date` to `hedisDate`
   *
   * @param date the date to start the interval
   * @return The calculated interval, including the hedisDate
   */
  def getIntervalFromDate(date: DateTime): Interval = new Interval(date, hedisDate.plusDays(1))

  
  def generateEligibleClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = List.empty
  def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = List.empty
  def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = List.empty
  def generateFailMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = List.empty

  def generateEligibleAndExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider, isExcluded: Boolean): List[Claim] = {

    val claims = generateEligibleClaims(pl, patient, provider)

    if (isExcluded) List.concat(claims, generateExclusionClaims(pl, patient, provider))
    else claims
  }

  def generateClaims(pl: PersistenceLayer, patient: Patient, provider: Provider, eligibleSimScore: Int, excludedSimScore: Int, meetCriteriaSimScore: Int): List[Claim] = {

    // Check if the patient is to be considered for the rule
    if (isPatientMeetDemographic(patient)) {

      // Check if patient is eligible
      if (eligibleSimScore < eligibleRate) {

        // Generate the claims to make the patient eligible
        val isExcluded = excludedSimScore < exclusionRate

        val claims = generateEligibleAndExclusionClaims(pl, patient, provider, isExcluded)

        // Check if the patient meet the excluded criteria, if not check if meet criteria
        if (isExcluded) {

          // Already generated the claim to meet the exclusion criteria
          claims

        } else {

          // Check if the patient is in the measure
          if (meetCriteriaSimScore < meetMeasureRate) {

            // Generate the claim to meet the measure
            List.concat(claims, generateMeetMeasureClaims(pl, patient, provider))

          } else {

            // Patient does not meet the measure
            List.concat(claims, generateFailMeasureClaims(pl, patient, provider))
          }
        }

      } else {

        // Patient does not meet eligibility criteria
        List.empty
      }

    } else {

      // Patient does not meet demographics
      List.empty
    }
  }

  /**
   * Utility method to filter all claims in code2Claims with codes (keys of code2Claims) that are in filterCodes
   *
   * @param code2Claims is the mapping of clinical codes to matching claims (from PatientHistory)
   * @param filterCodes is the set of clinical codes that we retain from code2Claims
   * @param f is the filter function applied to claims that have the filtered clinical codes (second level of filtering)
   * @returns All the claims that match both the clinical codes and the filter function f
   */
  def filterClaims[C](code2Claims: Map[String, List[C]], filterCodes: Set[String], f: (C) => Boolean): List[C] = Utils.filterClaims(code2Claims, filterCodes, f)

  /**
   * @returns true if have nbr claims with different dates in claims
   */
  def hasDifferentDates(nbr: Int, claims: List[Claim]): Boolean = Utils.hasDifferentDates(nbr, claims)
  
  /**
   * Utility method to increase readability in the HEDIS Rule classes.
   *
   * Simply fold all the rules and build up the scorecard from an initial value
   *
   * @param scorecard the initial scorecard on which we build up additional scores from the list of rules
   * @param rules is the list of predicates that adds contributions to the scorecard
   * @returns the build up scorecard
   */
  def applyRules(scorecard: Scorecard, rules: List[(Scorecard) => Scorecard]): Scorecard = Utils.applyRules(scorecard, rules)

  def isPatientMeetDemographic(scorecard: Scorecard): Boolean = scorecard.isPatientMeetDemographic(name)
  def isPatientEligible(scorecard: Scorecard): Boolean = scorecard.isPatientEligible(name)
  def isPatientExcluded(scorecard: Scorecard): Boolean = scorecard.isPatientExcluded(name)
  def isPatientMeetMeasure(scorecard: Scorecard): Boolean = scorecard.isPatientMeetMeasure(name)

  /**
   * Verify if the patient is in the denominator of the rule, i.e., eligible to the measure and not excluded.
   */
  def isPatientInDenominator(scorecard: Scorecard): Boolean = isPatientEligible(scorecard) && !isPatientExcluded(scorecard)

  def scorePatientMeetDemographic(scorecard: Scorecard, patient: Patient): Scorecard = scorecard.addScore(name, fullName, HEDISRule.meetDemographic, isPatientMeetDemographic(patient))

  /**
   * By default, assume there is no claim-based predicates, defaults to isPatientMeetDemographic
   */
  def scorePatientEligible(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard = scorecard.addScore(name, fullName, HEDISRule.eligible, isPatientMeetDemographic(patient))

  /**
   * Utility method to pick randomly one item from the list
   */
  def pickOne[A](items: List[A]): A = Utils.pickOne(items)

  /**
   * Apply the rule criteria to patient
   *
   * Apply the following scoring methods:
   * - scorePatientMeetDemographic
   * - scorePatientEligible
   *
   * Then the following methods, conditionally:
   * - scorePatientExcluded (if isPatientEligible is true)
   * - scorePatientMeetMeasure (if isPatientExcluded is false)
   *
   * @param scorecard to be updated with the criteria of this rule
   * @param patient to evaluate on this rule
   * @param ph is the patient claim's history
   * @returns the updated scorecard
   */
  def scoreRule(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {
    val s1 = scorePatientEligible(scorePatientMeetDemographic(scorecard, patient), patient, ph)
    if (isPatientEligible(s1)) {
      val s2 = scorePatientExcluded(s1, patient, ph)
      if (isPatientExcluded(s2)) s2
      else scorePatientMeetMeasure(s2, patient, ph)
    } else s1
  }
}
