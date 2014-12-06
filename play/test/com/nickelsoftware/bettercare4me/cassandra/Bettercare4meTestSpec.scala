/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.cassandra;

import scala.collection.Set
import org.scalatest._
import org.scalatestplus.play._
import play.api.Logger
import play.api.Play.current
import org.scalatest.SuiteMixin
import org.scalatest.Suite
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import scala.language.postfixOps
import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import scala.io.Source
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.Provider
import play.api.test.FakeApplication


// -----------------------------------------------------------------------------------------------------------
// Cassandra database access object
// -----------------------------------------------------------------------------------------------------------

trait TestCassandra extends SuiteMixin { this: Suite =>

  abstract override def withFixture(test: NoArgTest) = {

    // pre test
    
    try super.withFixture(test) // To be stackable, must call super.withFixture
    finally {

      // post test
    }
  }
}


class Bettercare4meTestSpec extends PlaySpec with OneAppPerSuite with TestCassandra {

  "The Bettercare4me object" must {

    "read patients from cassandra" in {
      Bettercare4me.queryPatients(991) map { l => l.toList mustBe List(Patient("patient-1-0", "MARCIA", "COOPER", "F", new LocalDate(1964, 9, 30).toDateTimeAtStartOfDay()))}
    }

    "read providers from cassandra" in {
      Bettercare4me.queryProviders(991) map { l => l.toList mustBe List(
          Provider("provider-1-0", "MIREYA", "MADDOX"),
          Provider("provider-1-2", "HERMELINDA", "KIRKLAND")
          )}
    }

    "insert providers into cassandra" in {
      Bettercare4me.batchProviders(995, List(Provider("test-5", "Fist", "Last")))
      Bettercare4me.queryProviders(995) map { l => l.toList mustBe List(Provider("test-5", "First", "Last"))}
    }
    
  }
}
