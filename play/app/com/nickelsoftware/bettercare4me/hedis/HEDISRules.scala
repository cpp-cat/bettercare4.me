/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis

import scala.util.Random

import org.joda.time.DateTime
import org.joda.time.Interval
import org.joda.time.LocalDate

import com.nickelsoftware.bettercare4me.hedis.hedis2014.AAB
import com.nickelsoftware.bettercare4me.hedis.hedis2014.AAB_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.ASM
import com.nickelsoftware.bettercare4me.hedis.hedis2014.ASM_12_18_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.ASM_19_50_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.ASM_51_64_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.ASM_5_11_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.AWC
import com.nickelsoftware.bettercare4me.hedis.hedis2014.AWC_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.BCS
import com.nickelsoftware.bettercare4me.hedis.hedis2014.BCSRule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CCS
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CCS_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDCEE
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDCEERule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDCHbA1cTest
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDCHbA1cTest7Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDCHbA1cTest8Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDCHbA1cTest9Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDCHbA1cTestRule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDCHbA1cTestValue
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_BPC
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_BPC_C1_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_BPC_C2_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_BPC_T_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_LDL_C
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_LDL_C_TestRule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_LDL_C_TestValueRule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_LDL_C_Value
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_MAN
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_MAN_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CHL
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CHL_16_20_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CHL_21_26_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_DTaP
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_DTaP_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_HB
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_HB_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_HiB
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_HiB_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_IPV
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_IPV_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_MMR
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_MMR_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_PC
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_PC_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_VZV
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_VZV_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CMC
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CMC_LDL_C_TestRule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CMC_LDL_C_TestValueRule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.COL
import com.nickelsoftware.bettercare4me.hedis.hedis2014.COL_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CWP
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CWP_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.LBP
import com.nickelsoftware.bettercare4me.hedis.hedis2014.LBP_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_AC
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_ACE_ARB_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_AC_C_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_AC_P1_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_AC_P2_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_AC_V_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_ADD
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_DGX_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_DUT_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.URI
import com.nickelsoftware.bettercare4me.hedis.hedis2014.URI_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.W15
import com.nickelsoftware.bettercare4me.hedis.hedis2014.W15_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.W34
import com.nickelsoftware.bettercare4me.hedis.hedis2014.W34_Rule
import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.nickelsoftware.bettercare4me.utils.NickelException
import com.nickelsoftware.bettercare4me.utils.Utils
import com.nickelsoftware.bettercare4me.utils.Utils.add2Map

/**
 * Singleton to avoid creating empty object over and over again and other useful names
 */
object HEDISRule {

  val meetDemographic: String = "meetDemographic"
  val eligible: String = "eligible"
  val excluded: String = "excluded"
  val meetMeasure: String = "meetMeasure"

  val emptyMeetDemographicCriteriaScore = RuleCriteriaScore(name = meetDemographic)
  val emptyEligibleCriteriaScore = RuleCriteriaScore(name = eligible)
  val emptyExcludedCriteriaScore = RuleCriteriaScore(name = excluded)
  val emptyMeetMeasureCriteriaScore = RuleCriteriaScore(name = meetMeasure)
  val emptyRuleScore = RuleScore()
  val emptyScorecard = Scorecard()
}

/**
 * Keep track of a particular criteria of an HEDIS rule, a rule criteria is one of:
 * 	- is patient meet demographic
 *  - is patient eligible
 *  - is patient excluded
 *  - is patient meet the measure
 *
 * @param name is the criteria name (one of: meetDemographic, eligible, excluded, meetMeasure)
 * @param oride is an override, if set will indicate whether the criteria is met or not
 * @param criteriaScore is a map(Predicate.predicateName, List of matching claims) for the particular criteria
 */
case class RuleCriteriaScore(name: String = "", oride: Option[Boolean] = None, criteriaScore: Map[String, List[Claim]] = Map()) {

  def isCriteriaMet: Boolean = {
    oride match {
      case Some(b) => b
      case None => !criteriaScore.isEmpty
    }
  }

  def addScore(predicateName: String, l: List[Claim]): RuleCriteriaScore = {
    if(criteriaScore.contains(predicateName)) RuleCriteriaScore(name, None, add2Map(predicateName, l, criteriaScore))
    else RuleCriteriaScore(name, None, Map((predicateName -> l)))
  }
  
  def addScore(b: Boolean): RuleCriteriaScore = RuleCriteriaScore(name, Some(b), Map())
}

/**
 * Keeping track of how an HEDIS rule is matched by claims
 *
 * @param eligible is a map(Predicate.predicateName, List of matching claims) for `HEDISRule.isPatientEligible
 * @param excluded is a map(Predicate.predicateName, List of matching claims) for `HEDISRule.isPatientExcluded
 * @param meetMeasure is a map(Predicate.predicateName, List of matching claims) for `HEDISRule.isPatientMeetMeasure
 */
case class RuleScore(
  meetDemographic: RuleCriteriaScore = HEDISRule.emptyMeetDemographicCriteriaScore,
  eligible: RuleCriteriaScore = HEDISRule.emptyEligibleCriteriaScore,
  excluded: RuleCriteriaScore = HEDISRule.emptyExcludedCriteriaScore,
  meetMeasure: RuleCriteriaScore = HEDISRule.emptyMeetMeasureCriteriaScore) {

  /**
   * Add a criteria score to the HEDIS Rule based on override
   */
  def addScore(criteriaName: String, oride: Boolean): RuleScore = {
    criteriaName match {
      case HEDISRule.meetDemographic => RuleScore(meetDemographic.addScore(oride), eligible, excluded, meetMeasure)
      case HEDISRule.eligible => RuleScore(meetDemographic, eligible.addScore(oride), excluded, meetMeasure)
      case HEDISRule.excluded => RuleScore(meetDemographic, eligible, excluded.addScore(oride), meetMeasure)
      case HEDISRule.meetMeasure => RuleScore(meetDemographic, eligible, excluded, meetMeasure.addScore(oride))
      case _ => throw NickelException("RuleScore: Unknown criteriaName: " + criteriaName)
    }
  }

  /**
   * Add a criteria score to the HEDIS Rule based on claim match
   * @param criteriaName is the measure predicate name (defined in `HEDISRule object)
   * @param predicateName is the predicate rule name (specific to each measures)
   * @param l the list of claims that satisfy the predicate
   * @throws NickelException for unknown `criteriaName
   */
  def addScore(criteriaName: String, predicateName: String, l: List[Claim]): RuleScore = {
    criteriaName match {
      case HEDISRule.meetDemographic => RuleScore(meetDemographic.addScore(predicateName, l), eligible, excluded, meetMeasure) // don't expect this one to be called!
      case HEDISRule.eligible => RuleScore(meetDemographic, eligible.addScore(predicateName, l), excluded, meetMeasure)
      case HEDISRule.excluded => RuleScore(meetDemographic, eligible, excluded.addScore(predicateName, l), meetMeasure)
      case HEDISRule.meetMeasure => RuleScore(meetDemographic, eligible, excluded, meetMeasure.addScore(predicateName, l))
      case _ => throw NickelException("RuleScore: Unknown criteriaName: " + criteriaName)
    }
  }
}

/**
 * Track the HEDIS metric scores, each HEDIS rule have a `RuleScore
 *
 * When the `Scorecard is used for tracking `Patient compliance, `meetDemographic is used
 * to tag the `Scorecard that the patient meet the demographic of a particular measure
 * (identified by `measureName).
 *
 * @param hedisRuleMap is a map(HEDISRule.name, RuleScore) mapping HEDIS measure name to RuleScore instance
 */
case class Scorecard(hedisRuleMap: Map[String, RuleScore] = Map()) {

  /**
   * Update the scorecard (by returning a new value) by specifying an override value to the measure's criteria
   *
   * @param measureName is the HEDIS measure name (HEDISRule::name)
   * @param criteriaName is the criteria being updated for the HEDIS measure (meetDemographic, eligible, excluded, meetMeasure)
   * @param oride is the override to set for the criteria
   * @throws NickelException for unknown `criteriaName
   */
  def addScore(measureName: String, criteriaName: String, oride: Boolean): Scorecard = {
    val ruleScore = hedisRuleMap.getOrElse(measureName, HEDISRule.emptyRuleScore)
    Scorecard(hedisRuleMap + (measureName -> ruleScore.addScore(criteriaName, oride)))
  }

  /**
   * Update the scorecard (by returning a new value) for particular predicate
   *
   * @param measureName is the HEDIS measure name (HEDISRule::name)
   * @param criteriaName is the criteria being updated for the HEDIS measure (meetDemographic, eligible, excluded, meetMeasure)
   * @param predicateName is the name of the predicate that was evaluated (Predicate::predicateName)
   * @param claims is the list of claim that matched the predicate, if the list is empty then the predicate did not match (no update to scorecard)
   * @throws NickelException for unknown `criteriaName
   */
  def addScore(measureName: String, criteriaName: String, predicateName: String, claims: List[Claim]): Scorecard = {

    if (claims.isEmpty) this
    else {
      val ruleScore = hedisRuleMap.getOrElse(measureName, HEDISRule.emptyRuleScore)
      Scorecard(hedisRuleMap + (measureName -> ruleScore.addScore(criteriaName, predicateName, claims)))
    }
  }

  /**
   * Update the scorecard (by returning a new value) for particular predicate
   *
   * Special case, no matching claims provided, this case is when a predicate matches but there are no claims to report
   *
   * @param measureName is the HEDIS measure name (HEDISRule::name)
   * @param criteriaName is the criteria being updated for the HEDIS measure (meetDemographic, eligible, excluded, meetMeasure)
   * @param predicateName is the name of the predicate that was evaluated (Predicate::predicateName)
   * @throws NickelException for unknown `criteriaName
   */
  def addScore(measureName: String, criteriaName: String, predicateName: String): Scorecard = {

    val ruleScore = hedisRuleMap.getOrElse(measureName, HEDISRule.emptyRuleScore)
    Scorecard(hedisRuleMap + (measureName -> ruleScore.addScore(criteriaName, predicateName, List.empty)))
  }

  /**
   * @return true if patient meets demographic of the measure, provided the scorecard was updated to that effect
   */
  def isPatientMeetDemographic(measureName: String): Boolean = hedisRuleMap.getOrElse(measureName, HEDISRule.emptyRuleScore).meetDemographic.isCriteriaMet

  /**
   * @return true if patient eligible to the measure, provided the scorecard was updated to that effect
   */
  def isPatientEligible(measureName: String): Boolean = hedisRuleMap.getOrElse(measureName, HEDISRule.emptyRuleScore).eligible.isCriteriaMet

  /**
   * @return true if patient excluded from the measure, provided the scorecard was updated to that effect
   */
  def isPatientExcluded(measureName: String): Boolean = hedisRuleMap.getOrElse(measureName, HEDISRule.emptyRuleScore).excluded.isCriteriaMet

  /**
   * @return true if patient meet the measure, provided the scorecard was updated to that effect
   */
  def isPatientMeetMeasure(measureName: String): Boolean = hedisRuleMap.getOrElse(measureName, HEDISRule.emptyRuleScore).meetMeasure.isCriteriaMet
}

/**
 * Trait to define an HEDIS rule.
 *
 * Implementation of this trait are able to generate claims for a patient meeting the rule criteria.
 * The generated claims will be compliant to the rule randomly based on targetCompliance rate (percentage)
 */
trait HEDISRule {

  /**
   * Indicate the name of the rule for configuration and reporting purpose
   */
  val name: String

  /**
   * Indicate the full name of the rule (human readable)
   */
  val fullName: String

  /**
   * Indicate the rule description (human readable)
   */
  val description: String

  /**
   * Indicates the rate at which the patients are eligible to the  measure.
   *
   * To be eligible, the patient must first meet the demographic requirements.
   * Example, an  `eligibleRate` of 25 for CDC H1C, means that 25% of patient of age between 18 - 75
   * will have diabetes. Note that all CDC measure should have the same `eligibleRate`
   */
  val eligibleRate: Int

  /**
   * Indicates the rate at which the patients meet the measure, in %
   *
   * (patient in numerator, i.e., meet measure) / (patient in denominator, i.e., not excluded from measure) * 100
   *
   * This rate does not apply to exclusions (patients excluded from measure).
   *
   */
  val meetMeasureRate: Int

  /**
   * Indicates the rate at which patients are excluded from measure, in %
   *
   * Fraction of eligible patients that meet the exclusion criteria:
   * (excluded patients) / (eligible patients)
   */
  val exclusionRate: Int

  /**
   * Generate the claims for the patient to be in the denominator and possibly in the numerator as well.
   *
   * The patient is randomly in the numerator based on the `targetCompliance` rate.
   */
  def generateClaims(persistenceLayer: PersistenceLayer, patient: Patient, provider: Provider): List[Claim]

  /**
   * Verify if patient meet the demographic of the measure
   */
  def isPatientMeetDemographic(patient: Patient): Boolean

  /**
   * Verify if patient meet the demographic of the measure (must be called after `scorePatientMeetDemographic)
   *
   * This is a utility method
   */
  def isPatientMeetDemographic(scorecard: Scorecard): Boolean

  /**
   * Verify if the measure is applicable to the patient based on patient's
   * demographics only.
   *
   * The patient may still not be eligible to the measure if the clinical criteria are not met.
   */
  def scorePatientMeetDemographic(scorecard: Scorecard, patient: Patient): Scorecard

  /**
   * Verify if patient meet the eligibility of the measure (must be called after `scorePatientEligible)
   */
  def isPatientEligible(scorecard: Scorecard): Boolean

  /**
   * Verify if patient is eligible to the measure
   *
   * Patient may be eligible to the measure but excluded if meet the exclusion criteria.
   */
  def scorePatientEligible(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard

  /**
   * Verify if patient meet the exclusion condition of the measure (must be called after `scorePatientExcluded
   */
  def isPatientExcluded(scorecard: Scorecard): Boolean

  /**
   * Verify if patient meet the exclusion condition of the measure
   *
   * Does not verify if patient is eligible, but simply the exclusion criteria
   */
  def scorePatientExcluded(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard

  /**
   * Verify if the patient is in the numerator of the rule, i.e., meets the measure. (must be called after `scorePatientMeetMeasure
   */
  def isPatientMeetMeasure(scorecard: Scorecard): Boolean

  /**
   * Verify if the patient is in the numerator of the rule, i.e., meets the measure.
   */
  def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard

  /**
   * Verify if the patient is in the denominator of the rule, i.e., eligible to the measure and not excluded.
   */
  def isPatientInDenominator(scorecard: Scorecard): Boolean

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
  def scoreRule(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard

}

abstract class HEDISRuleBase(config: RuleConfig, hedisDate: DateTime) extends HEDISRule {

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

  def generateClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // Check if the patient is to be considered for the rule
    if (isPatientMeetDemographic(patient)) {

      // Check if patient is eligible
      if (Random.nextInt(100) < eligibleRate) {

        // Generate the claims to make the patient eligible
        val isExcluded = Random.nextInt(100) < exclusionRate

        val claims = generateEligibleAndExclusionClaims(pl, patient, provider, isExcluded)

        // Check if the patient meet the excluded criteria, if not check if meet criteria
        if (isExcluded) {

          // Already generated the claim to meet the exclusion criteria
          claims

        } else {

          // Check if the patient is in the measure
          if (Random.nextInt(100) < meetMeasureRate) {

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

  def scorePatientMeetDemographic(scorecard: Scorecard, patient: Patient): Scorecard = scorecard.addScore(name, HEDISRule.meetDemographic, isPatientMeetDemographic(patient))

  /**
   * By default, assume there is no claim-based predicates, defaults to isPatientMeetDemographic
   */
  def scorePatientEligible(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard = scorecard.addScore(name, HEDISRule.eligible, isPatientMeetDemographic(patient))

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

object HEDISRules {

  val rules: Map[String, (RuleConfig, DateTime) => HEDISRule] = Map(
    "TEST" -> { (c, d) => new TestRule(c, d) },
    BCS.name -> { (c, d) => new BCSRule(c, d) },
    CDCHbA1cTest.name -> { (c, d) => new CDCHbA1cTestRule(c, d) },
    CDCHbA1cTestValue.name7 -> { (c, d) => new CDCHbA1cTest7Rule(c, d) },
    CDCHbA1cTestValue.name8 -> { (c, d) => new CDCHbA1cTest8Rule(c, d) },
    CDCHbA1cTestValue.name9 -> { (c, d) => new CDCHbA1cTest9Rule(c, d) },
    CDCEE.name -> { (c, d) => new CDCEERule(c, d) },
    CDC_LDL_C.name -> { (c, d) => new CDC_LDL_C_TestRule(c, d) },
    CDC_LDL_C_Value.name -> { (c, d) => new CDC_LDL_C_TestValueRule(c, d) },
    CDC_MAN.name -> { (c, d) => new CDC_MAN_Rule(c, d) },
    CDC_BPC.nameTest -> { (c, d) => new CDC_BPC_T_Rule(c, d) },
    CDC_BPC.nameC1 -> { (c, d) => new CDC_BPC_C1_Rule(c, d) },
    CDC_BPC.nameC2 -> { (c, d) => new CDC_BPC_C2_Rule(c, d) },
    CCS.name -> { (c, d) => new CCS_Rule(c, d) },
    CHL.name16 -> { (c, d) => new CHL_16_20_Rule(c, d) },
    CHL.name21 -> { (c, d) => new CHL_21_26_Rule(c, d) },
    COL.name -> { (c, d) => new COL_Rule(c, d) },
    CIS_VZV.name -> { (c, d) => new CIS_VZV_Rule(c, d) },
    CIS_DTaP.name -> { (c, d) => new CIS_DTaP_Rule(c, d) },
    CIS_HB.name -> { (c, d) => new CIS_HB_Rule(c, d) },
    CIS_HiB.name -> { (c, d) => new CIS_HiB_Rule(c, d) },
    CIS_MMR.name -> { (c, d) => new CIS_MMR_Rule(c, d) },
    CIS_PC.name -> { (c, d) => new CIS_PC_Rule(c, d) },
    CIS_IPV.name -> { (c, d) => new CIS_IPV_Rule(c, d) },
    W15.name -> { (c, d) => new W15_Rule(c, d) },
    W34.name -> { (c, d) => new W34_Rule(c, d) },
    AWC.name -> { (c, d) => new AWC_Rule(c, d) },
    ASM.name5 -> { (c, d) => new ASM_5_11_Rule(c, d) },
    ASM.name12 -> { (c, d) => new ASM_12_18_Rule(c, d) },
    ASM.name19 -> { (c, d) => new ASM_19_50_Rule(c, d) },
    ASM.name51 -> { (c, d) => new ASM_51_64_Rule(c, d) },
    CMC.nameTest -> { (c, d) => new CMC_LDL_C_TestRule(c, d) },
    CMC.nameTestValue -> { (c, d) => new CMC_LDL_C_TestValueRule(c, d) },
    MPM_ADD.nameACE -> { (c, d) => new MPM_ACE_ARB_Rule(c, d) },
    MPM_ADD.nameDGX -> { (c, d) => new MPM_DGX_Rule(c, d) },
    MPM_ADD.nameDUT -> { (c, d) => new MPM_DUT_Rule(c, d) },
    MPM_AC.nameC -> { (c, d) => new MPM_AC_C_Rule(c, d) },
    MPM_AC.nameP1 -> { (c, d) => new MPM_AC_P1_Rule(c, d) },
    MPM_AC.nameP2 -> { (c, d) => new MPM_AC_P2_Rule(c, d) },
    MPM_AC.nameV -> { (c, d) => new MPM_AC_V_Rule(c, d) },
    CWP.name -> { (c, d) => new CWP_Rule(c, d) },
    URI.name -> { (c, d) => new URI_Rule(c, d) },
    AAB.name -> { (c, d) => new AAB_Rule(c, d) },
    LBP.name -> { (c, d) => new LBP_Rule(c, d) })
    

  def createRuleByName(name: String, config: RuleConfig, hedisDate: DateTime): HEDISRule = {
    if (!rules.contains(name)) throw NickelException("HEDISRules: Cannot create HEDISRule; No such rule with name: " + name)
    else rules(name)(config, hedisDate)
  }
}

class TestRule(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  val name = "TEST"
  val fullName = "Test Rule"
  val description = "This rule is for testing."

  def isPatientMeetDemographic(patient: Patient): Boolean = true
  def isPatientExcluded(patient: Patient, patientHistory: PatientHistory): Boolean = false
  def isPatientMeetMeasure(patient: Patient, patientHistory: PatientHistory): Boolean = true

  override def generateClaims(persistenceLayer: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {
    val dos = new LocalDate(2014, 9, 5).toDateTimeAtStartOfDay()
    List(
      persistenceLayer.createMedClaim(patient.patientID, provider.providerID, dos, dos,
        icdDPri = "icd 1", icdD = Set("icd 1", "icd 2"), icdP = Set("icd p1"),
        hcfaPOS = "hcfaPOS", ubRevenue = "ubRevenue"))
  }

  def scorePatientMeetDemographic(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard =
    scorecard.addScore("TEST", HEDISRule.meetDemographic, true)

  def scorePatientExcluded(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard =
    scorecard.addScore("TEST", HEDISRule.excluded, true)

  def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard =
    scorecard.addScore("TEST", HEDISRule.meetMeasure, true)
}
