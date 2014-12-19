/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis

import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.utils.NickelException
import com.nickelsoftware.bettercare4me.utils.Utils.add2Map


object HEDISScoreSummary {
  
  /**
   * Create the initial HEDISScoreSummary based on configuration object `config
   */
  def apply(rules: List[HEDISRule]): HEDISScoreSummary = HEDISScoreSummary(0, rules map { r => (r.name -> RuleScoreSummary(HEDISRuleInfo(r))) } toMap)
  
}
/**
 * Overall summary of a HEDIS analysis
 */
case class HEDISScoreSummary(patientCount: Long, ruleScoreSummaries: Map[String, RuleScoreSummary]) {
  
  def addScoreCard(scorecard: Scorecard): HEDISScoreSummary = {
    
    val rs = ruleScoreSummaries map {case (k, v) => (k -> v.addScore(scorecard))}
    HEDISScoreSummary(patientCount+1, rs)
  }
  
  def addHEDISScoreSummary(scoreSummary: HEDISScoreSummary): HEDISScoreSummary = {
    
    val rs = ruleScoreSummaries map {case (k, v) => (k -> v.addScore(scoreSummary.ruleScoreSummaries(k)))}
    HEDISScoreSummary(patientCount + scoreSummary.patientCount, rs)
  }

  def +(rhs: HEDISScoreSummary) = addHEDISScoreSummary(rhs)
  
  /**
   * Return the list of rule score summary based on a list of measure names, 
   * filtering out those who are not in this analysis.
   * May return an empty list
   */
  def filterMeasures(selectMeasures: List[String]): List[RuleScoreSummary] = {
    val l = for(key <- selectMeasures) yield ruleScoreSummaries.get(key)
    l.flatten
  }
  
  /**
   * Function to persist using collections of Strings - to simply save to Cassandra 
   */
  def persist: (Long, List[String]) = {
    (patientCount, ruleScoreSummaries.toList map { case (_, rs) => rs.toParseString })
  }
  
  override def toString(): String = {
    val l = "Overall score summary over " + patientCount + " patients" :: (ruleScoreSummaries.values map {_.toString} toList) 
    l mkString "\n"
  }

  /**
   * Provide a description of the Score Summary, presenting the rules in order based on the argument list
   * 
   * @param ruleNames ordered list of rules to obtain the score summary
   */
  def toString(ruleNames: List[String]): String = {
    
    val l = "Overall score summary over " + patientCount + " patients" :: (ruleNames map {ruleScoreSummaries.get(_) map { _.toString } getOrElse "" }) 
    l mkString "\n"
  }
}


/**
 * Aggregated rule score, `eligible minus `excluded is same as denominator and `meetMeasure is same as numerator in HEDIS speak
 */
case class RuleScoreSummary(ruleInfo: HEDISRuleInfo, meetDemographics: Long=0, eligible: Long=0, excluded: Long=0, meetMeasure: Long=0) {
  
  private def ratio(num: Long, denom: Long): Double = if(denom > 0) num.toDouble / denom.toDouble * 100 else 0
  
  def eligible2MeetDemographics: Double = ratio(eligible, meetDemographics)
  def excluded2eligible: Double = ratio(excluded, eligible)
  def meetMeasure2eligible: Double = ratio(meetMeasure, eligible)
  
  def numerator: Long = meetMeasure
  def denominator: Long = eligible - excluded
  def scorePct: Double = ratio(numerator, denominator)
  
  def addScore(scorecard: Scorecard) = {
   
    val rs = scorecard.getRuleScore(ruleInfo.name)
    RuleScoreSummary(
        ruleInfo,
        if(rs.meetDemographic.isCriteriaMet) meetDemographics+1 else meetDemographics,
        if(rs.eligible.isCriteriaMet) eligible+1 else eligible,
        if(rs.excluded.isCriteriaMet) excluded+1 else excluded,
        if(rs.meetMeasure.isCriteriaMet) meetMeasure+1 else meetMeasure)
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