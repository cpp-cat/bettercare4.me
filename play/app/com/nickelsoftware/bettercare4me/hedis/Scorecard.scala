/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis

import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.utils.NickelException
import com.nickelsoftware.bettercare4me.utils.Utils.add2Map


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
    val ruleScore = getRuleScore(measureName)
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
      val ruleScore = getRuleScore(measureName)
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

    val ruleScore = getRuleScore(measureName)
    Scorecard(hedisRuleMap + (measureName -> ruleScore.addScore(criteriaName, predicateName, List.empty)))
  }

  /**
   * @return true if patient meets demographic of the measure, provided the scorecard was updated to that effect
   */
  def isPatientMeetDemographic(measureName: String): Boolean = getRuleScore(measureName).meetDemographic.isCriteriaMet

  /**
   * @return true if patient eligible to the measure, provided the scorecard was updated to that effect
   */
  def isPatientEligible(measureName: String): Boolean = getRuleScore(measureName).eligible.isCriteriaMet

  /**
   * @return true if patient excluded from the measure, provided the scorecard was updated to that effect
   */
  def isPatientExcluded(measureName: String): Boolean = getRuleScore(measureName).excluded.isCriteriaMet

  /**
   * @return true if patient meet the measure, provided the scorecard was updated to that effect
   */
  def isPatientMeetMeasure(measureName: String): Boolean = getRuleScore(measureName).meetMeasure.isCriteriaMet
  
  /**
   * @param measureName is the HEDIS rule name (HEDISRule::name)
   * @returns rule score that was recorded for the user
   */
  def getRuleScore(measureName: String): RuleScore = hedisRuleMap.getOrElse(measureName, HEDISRule.emptyRuleScore)
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
