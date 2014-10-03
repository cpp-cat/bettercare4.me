package com.nickelsoftware.bettercare4me.utils

import org.joda.time.Interval
import org.joda.time.DateTime

object Utils {
  
  /**
   * @returns an interval in months leading to date
   */
  def getIntervalFromMonths(months: Int, date: DateTime): Interval = {
      val temp = date.plusDays(1)
      new Interval(temp.minusMonths(months), temp)
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

  def flattenFile(from: String, to: String): Unit = {

    import com.github.tototoshi.csv.CSVReader
    import com.github.tototoshi.csv.CSVWriter
    import java.io.File
    val l = CSVReader.open(new File(from)).all().flatten
    val w = CSVWriter.open(new File(to))
    w.writeAll(List(l))
    w.close
  }
}