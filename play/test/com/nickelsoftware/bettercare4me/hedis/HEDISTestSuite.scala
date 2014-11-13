package com.nickelsoftware.bettercare4me.hedis;

import org.scalatest.Suites

class HEDISTestSuite extends Suites(
    new HEDISRulesTestSpec,
    new ScorecardTestSpec
    )