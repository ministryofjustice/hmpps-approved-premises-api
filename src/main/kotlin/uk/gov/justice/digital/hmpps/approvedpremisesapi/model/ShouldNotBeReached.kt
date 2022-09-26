package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

// Mocking sealed interfaces is currently broken in mockk, so an else branch is need until this is resolved: https://github.com/mockk/mockk/issues/832
fun shouldNotBeReached(): Nothing = throw RuntimeException("This branch should not be reached")
