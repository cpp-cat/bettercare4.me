/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.utils;

import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec
import org.joda.time.DateTime
import org.joda.time.LocalDate

class UtilsTestSpec extends PlaySpec {

  "The Utils class" must {

    "calculate the number of days between dates" in {

      Utils.daysBetween(new DateTime(2014, 10, 16, 0, 0, 0), new DateTime(2014, 10, 16, 0, 0, 0)) mustBe 0
      Utils.daysBetween(new DateTime(2014, 10, 16, 0, 0, 0), new DateTime(2014, 10, 17, 0, 0, 0)) mustBe 1
      Utils.daysBetween(new DateTime(2014, 10, 16, 0, 0, 0), new DateTime(2014, 10, 26, 0, 0, 0)) mustBe 10
      Utils.daysBetween(DateTime.now(), DateTime.now().plusDays(1)) mustBe 1
      Utils.daysBetween(DateTime.now(), DateTime.now().plusDays(2)) mustBe 2
      Utils.daysBetween(new DateTime(2014, 10, 16, 0, 0, 0), new DateTime(2014, 10, 16, 0, 0, 0).plusMonths(1)) mustBe 31
    }

    "calculate interval between dates" in {

      List(0, 1, 2, 3, 4, 5, 6, 31, 70) foreach { i =>
        val dos1 = new LocalDate(2014, 12, 31).toDateTimeAtStartOfDay()
        val dos2 = dos1.minusDays(i)
        Utils.daysBetween(dos2, dos1) mustBe i

        val daysSupply = Utils.daysBetween(dos2, dos1)
        !dos2.plusDays(daysSupply).isBefore(dos1) mustBe true
        dos2.plusDays(daysSupply).isEqual(dos1) mustBe true
        !dos2.plusDays(daysSupply).isAfter(dos1) mustBe true
      }
    }
    
    "calculate interval across daylight saving time" in {
      
      val dos1 = new LocalDate(2014, 3, 10).toDateTimeAtStartOfDay()
      val dos2 = new LocalDate(2014, 2, 3).toDateTimeAtStartOfDay()
      val daysSupply = Utils.daysBetween(dos2, dos1)
      dos2.plusDays(daysSupply).isEqual(dos1) mustBe true
    }
  }
}