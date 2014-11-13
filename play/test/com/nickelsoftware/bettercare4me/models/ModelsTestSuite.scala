package com.nickelsoftware.bettercare4me.models;

import org.scalatest.Suites

class ModelsTestSuite extends Suites(
    new GeneratorTestSpec,
    new LabClaimTestSpec,
    new MedClaimTestSpec,
    new PatientTestSpec,
    new ProviderTestSpec,
    new RxClaimTestSpec
    )