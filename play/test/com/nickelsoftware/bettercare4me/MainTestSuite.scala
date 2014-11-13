package com.nickelsoftware.bettercare4me;

import org.scalatest.Suites

import com.nickelsoftware.bettercare4me.actors.ActorsTestSuite
import com.nickelsoftware.bettercare4me.hedis.HEDISTestSuite
import com.nickelsoftware.bettercare4me.hedis.hedis2014.HEDIS2014TestSuite
import com.nickelsoftware.bettercare4me.models.ModelsTestSuite
import com.nickelsoftware.bettercare4me.utils.UtilsTestSuite

class MainTestSuite extends Suites(
  new UtilsTestSuite,
  new ModelsTestSuite,
  new HEDISTestSuite,
  new HEDIS2014TestSuite,
  new ActorsTestSuite)