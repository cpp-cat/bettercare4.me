package com.nickelsoftware.bettercare4me.utils

import java.io.File
import java.io.FileReader

import scala.collection.JavaConversions.mapAsScalaMap
import scala.util.Random

import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Interval
import org.yaml.snakeyaml.constructor.SafeConstructor

import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv.CSVWriter
import com.nickelsoftware.bettercare4me.hedis.Scorecard
import com.nickelsoftware.bettercare4me.models.Claim

import play.api.Logger
import scalax.file.Path

/**
 * Object to read properties from environment
 */
object Properties {

  val dataDir: Path = Path.fromString(scala.util.Properties.envOrElse("BC4ME_DATA_DIR", "./data"))
  val cassandraConfig: Path = dataDir / scala.util.Properties.envOrElse("BC4ME_CASSANDRA_CONFIG", "cassandra.yaml")
  val sparkConfig: Path = dataDir / scala.util.Properties.envOrElse("BC4ME_SPARK_CONFIG", "spark.yaml")
}

object Utils {

  def loadYamlConfig(fname: String): Map[String, Object] = {
    try {
      //*
      println("**> Reading config file at "+fname)
      val yaml = new org.yaml.snakeyaml.Yaml(new SafeConstructor());
      yaml.load(new FileReader(fname)).asInstanceOf[java.util.Map[String, Object]].toMap
    } catch {
      case ex: Exception =>
        Logger.error("Utils.loadYamlConfig: Exception caught while loading cassandra config: " + ex.getMessage())
        Map()
    }
  }

  /**
   * @param from date of the start of the interval
   * @param to date of the end date of the interval
   * @returns the number of days between from and to dates
   */
  def daysBetween(from: DateTime, to: DateTime): Int = Days.daysBetween(from, to).getDays()

  /**
   * @returns an interval in months leading to date
   */
  def getIntervalFromMonths(months: Int, date: DateTime): Interval = {
    val temp = date.plusDays(1)
    new Interval(temp.minusMonths(months), temp)
  }

  /**
   * @returns an interval in years leading to date
   */
  def getIntervalFromYears(years: Int, date: DateTime): Interval = {
    val temp = date.plusDays(1)
    new Interval(temp.minusYears(years), temp)
  }

  /**
   * @returns an interval in days leading to date
   */
  def getIntervalFromDays(days: Int, date: DateTime): Interval = {
    val temp = date.plusDays(1)
    new Interval(temp.minusDays(days), temp)
  }

  def add2Map[C](s: String, c: C, map: Map[String, List[C]]): Map[String, List[C]] = {

    val l = map.getOrElse(s, List())
    map + (s -> (c :: l))
  }

  def add2Map[C](s: String, l: List[C], map: Map[String, List[C]]): Map[String, List[C]] = {

    if (l.isEmpty) map
    else {

      val l2 = map.getOrElse(s, List.empty)
      if (l2.isEmpty) map + (s -> l)
      else map + (s -> List.concat(l, l2))
    }
  }

  /**
   * Utility method to pick randomly one item from the list
   */
  def pickOne[A](items: List[A]): A = items(Random.nextInt(items.size))

  /**
   * Utility method to filter all claims in code2Claims with codes (keys of code2Claims) that are in filterCodes
   *
   * @param code2Claims is the mapping of clinical codes to matching claims (from PatientHistory)
   * @param filterCodes is the set of clinical codes that we retain from code2Claims
   * @param f is the filter function applied to claims that have the filtered clinical codes (second level of filtering)
   * @returns All the claims that match both the clinical codes and the filter function f
   */
  def filterClaims[C](code2Claims: Map[String, List[C]], filterCodes: Set[String], f: (C) => Boolean): List[C] = {
    def loop(l: List[C], m: Map[String, List[C]]): List[C] = {
      if (m.isEmpty) l
      else {
        val (k, v) = m.head
        if (filterCodes.contains(k)) loop(List.concat(v.filter(f), l), m.tail)
        else loop(l, m.tail)
      }
    }
    loop(List.empty, code2Claims)
  }

  /**
   * @returns true if have nbr claims with different dates in claims
   */
  def hasDifferentDates(nbr: Int, claims: List[Claim]): Boolean = {
    def loop(dates: Set[DateTime], c: List[Claim]): Boolean = {
      if (dates.size == nbr) true
      else {
        if (c.isEmpty) false
        else {
          if (!dates.contains(c.head.date)) loop(dates + c.head.date, c.tail)
          else loop(dates, c.tail)
        }
      }
    }
    loop(Set(), claims)
  }

  /**
   * Utility method to increase readability in the HEDIS Rule classes.
   *
   * Simply fold all the rules and build up the scorecard from an initial value
   *
   * @param scorecard the initial scorecard on which we build up additional scores from the list of rules
   * @param rules is the list of predicates that adds contributions to the scorecard
   * @returns the build up scorecard
   */
  def applyRules(scorecard: Scorecard, rules: List[(Scorecard) => Scorecard]): Scorecard = rules.foldLeft(scorecard)({ (s, f) => f(s) })

  //
  // import com.nickelsoftware.bettercare4me.utils.Utils._
  // flattenFile("./data/asm.ndc.c.csv", "./data/out.csv")
  //
  def flattenFile(from: String, to: String): Unit = {

    import com.github.tototoshi.csv.CSVReader
    import com.github.tototoshi.csv.CSVWriter
    import java.io.File
    val l = CSVReader.open(new File(from)).all().flatten
    val w = CSVWriter.open(new File(to))
    w.writeAll(List(l))
    w.close
  }

  //
  // import com.nickelsoftware.bettercare4me.utils.Utils._
  // extractNDC("carbamazepine", "./data/MPM_D_2014_(final).csv", "./data/out.csv")
  //
  def extractNDC(name: String, from: String, to: String): Unit = {

    import com.github.tototoshi.csv.CSVReader
    import com.github.tototoshi.csv.CSVWriter
    import java.io.File
    val l = CSVReader.open(new File(from)).all()
    val f = for (r <- l if (r(2).toLowerCase().startsWith(name))) yield r(0)
    val w = CSVWriter.open(new File(to))
    w.writeAll(List(f))
    w.close
  }
}