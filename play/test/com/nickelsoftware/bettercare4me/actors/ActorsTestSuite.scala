package com.nickelsoftware.bettercare4me.actors;

import org.scalatest.Suites

class ActorsTestSuite extends Suites(
    new ClaimFileGeneratorTestSpec,
    new ClaimGeneratorTestSpec
    )