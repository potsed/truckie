Feature: Product display name
  As a driver
  I want the app branded as Truckie
  So that launcher labels and TopAppBar titles match the product name

  # User-facing rename from Truckie1 → Truckie. Java/Kotlin package com.truckie1
  # is intentionally deferred (applicationId / namespace stay truckie1 for now).

  Scenario: Launcher and home titles use Truckie
    Given the product display strings are defined
    Then app_name is "Truckie"
    And home_title is "Truckie"

  Scenario: Window theme is named Theme.Truckie
    Given the app window theme is applied
    Then the theme resource is Theme.Truckie
