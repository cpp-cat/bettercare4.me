/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis

import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.utils.NickelException
import com.nickelsoftware.bettercare4me.utils.Utils.add2Map
import com.nickelsoftware.bettercare4me.utils.NickelException
import com.nickelsoftware.bettercare4me.utils.NickelException

object HEDISScoreSummary {

  /**
   * Create the initial HEDISScoreSummary based on configuration object `config
   */
  def apply(rules: List[HEDISRule]): HEDISScoreSummary = HEDISScoreSummary(0, rules map { r => (r.name -> RuleScoreSummary(HEDISRuleInfo(r))) } toMap)

  /**
   * Constructor to load from Cassandra
   */
  def apply(rules: List[HEDISRule], patientCount: Long, ruleScoreSummaries: List[String]): HEDISScoreSummary = {
    val ruleInfoMap = rules map { r => (r.name, HEDISRuleInfo(r)) } toMap
    val rssList = ruleScoreSummaries map { s => RuleScoreSummary(ruleInfoMap, s) }
    HEDISScoreSummary(patientCount, rssList map { rss => (rss.ruleInfo.name, rss) } toMap)
  }
}
/**
 * Overall summary of a HEDIS analysis
 */
case class HEDISScoreSummary(patientCount: Long, ruleScoreSummaries: Map[String, RuleScoreSummary]) {

  def addScoreCard(scorecard: Scorecard): HEDISScoreSummary = {

    val rs = ruleScoreSummaries map { case (k, v) => (k -> v.addScore(scorecard)) }
    HEDISScoreSummary(patientCount + 1, rs)
  }

  def addHEDISScoreSummary(scoreSummary: HEDISScoreSummary): HEDISScoreSummary = {

    val rs = ruleScoreSummaries map { case (k, v) => (k -> v.addScore(scoreSummary.ruleScoreSummaries(k))) }
    HEDISScoreSummary(patientCount + scoreSummary.patientCount, rs)
  }

  def +(rhs: HEDISScoreSummary) = addHEDISScoreSummary(rhs)

  /**
   * Return the list of rule score summary based on a list of measure names,
   * filtering out those who are not in this analysis.
   * May return an empty list
   */
  def filterMeasures(selectMeasures: List[String]): List[RuleScoreSummary] = {
    val l = for (key <- selectMeasures) yield ruleScoreSummaries.get(key)
    l.flatten
  }

  /**
   * Function to persist using collections of Strings - to simply save to Cassandra
   */
  def persist: (Long, List[String]) = {
    (patientCount, ruleScoreSummaries.toList map { case (_, rs) => rs.toParseString })
  }

  override def toString(): String = {
    val l = "Overall score summary over " + patientCount + " patients" :: (ruleScoreSummaries.values map { _.toString } toList)
    l mkString "\n"
  }

  /**
   * Provide a description of the Score Summary, presenting the rules in order based on the argument list
   *
   * @param ruleNames ordered list of rules to obtain the score summary
   */
  def toString(ruleNames: List[String]): String = {

    val l = "Overall score summary over " + patientCount + " patients" :: (ruleNames map { ruleScoreSummaries.get(_) map { _.toString } getOrElse "" })
    l mkString "\n"
  }
}

/**
 * Utility object to define constructors for when we're loading from Cassandra
 */
object RuleScoreSummary {

  private def parse(data: String): (String, Long, Long, Long, Long) = {
    val v = data.split(",")
    if (v.size < 5) throw NickelException("RuleScoreSummary: Invalid string representation of RuleScoreSummary: " + data)

    (v(0), v(1).toLong, v(2).toLong, v(3).toLong, v(4).toLong)
  }

  /**
   * Constructor used when loaded from Cassandra
   */
  def apply(ruleInfo: HEDISRuleInfo, data: String): RuleScoreSummary = {
    val v = parse(data)
    if(ruleInfo.name != v._1) throw NickelException("RuleScoreSummary: rule info object not matching serialized data! "+data)
    RuleScoreSummary(ruleInfo, v._2, v._3, v._4, v._5)
  }

  /**
   * Constructor used when loaded from Cassandra
   */
  def apply(ruleInfoMap: Map[String, HEDISRuleInfo], data: String): RuleScoreSummary = {
    val v = parse(data)
    val ruleInfo = ruleInfoMap.getOrElse(v._1, throw NickelException("HEDISScoreSummary: Invalid HEDIS measure name: " + v._1))
    RuleScoreSummary(ruleInfo, v._2, v._3, v._4, v._5)
  }
}

/**
 * Aggregated rule score, `eligible minus `excluded is same as denominator and `meetMeasure is same as numerator in HEDIS speak
 */
case class RuleScoreSummary(ruleInfo: HEDISRuleInfo, meetDemographics: Long = 0, eligible: Long = 0, excluded: Long = 0, meetMeasure: Long = 0) {

  private def ratio(num: Long, denom: Long): Double = if (denom > 0) num.toDouble / denom.toDouble * 100 else 0

  def eligible2MeetDemographics: Double = ratio(eligible, meetDemographics)
  def excluded2eligible: Double = ratio(excluded, eligible)
  def meetMeasure2eligible: Double = ratio(meetMeasure, eligible)

  def numerator: Long = meetMeasure
  def denominator: Long = eligible - excluded
  def scorePct: Double = ratio(numerator, denominator)

  def addScore(scorecard: Scorecard) = {

    val rs = scorecard.getRuleScore(ruleInfo.name, ruleInfo.fullName)
    RuleScoreSummary(
      ruleInfo,
      if (rs.meetDemographic.isCriteriaMet) meetDemographics + 1 else meetDemographics,
      if (rs.eligible.isCriteriaMet) eligible + 1 else eligible,
      if (rs.excluded.isCriteriaMet) excluded + 1 else excluded,
      if (rs.meetMeasure.isCriteriaMet) meetMeasure + 1 else meetMeasure)
  }

  def addScore(rss: RuleScoreSummary): RuleScoreSummary = {

    RuleScoreSummary(
      ruleInfo,
      rss.meetDemographics + meetDemographics,
      rss.eligible + eligible,
      rss.excluded + excluded,
      rss.meetMeasure + meetMeasure)
  }

  def toParseString: String = {
    new StringBuilder(ruleInfo.name).
      append(",").append(meetDemographics).
      append(",").append(eligible).
      append(",").append(excluded).
      append(",").append(meetMeasure).toString
  }

  override def toString(): String = {
    ruleInfo.name + s": ($eligible2MeetDemographics/$excluded2eligible/$meetMeasure2eligible) -- $numerator/$denominator = $scorePct%"
  }
}

object HEDISRuleInfo {

  def apply(rule: HEDISRule): HEDISRuleInfo = HEDISRuleInfo(rule.name, rule.fullName, rule.description)
}

/**
 * Holds information about an HEDIS rule for reporting purpose
 */
case class HEDISRuleInfo(name: String, fullName: String, description: String)