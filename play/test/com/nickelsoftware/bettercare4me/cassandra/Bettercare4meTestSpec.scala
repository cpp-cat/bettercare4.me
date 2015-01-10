/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.cassandra;

import scala.language.postfixOps

import org.joda.time.DateTime
import org.scalatest.Suite
import org.scalatest.SuiteMixin
import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec

import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.Provider

import play.api.libs.concurrent.Execution.Implicits.defaultContext


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
      Bettercare4me.queryPatients(991) map { l => l.toList mustBe List(Patient("patient-1-0", "MARCIA", "COOPER", "F", new DateTime(1964, 9, 30, 0, 0)))}
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
