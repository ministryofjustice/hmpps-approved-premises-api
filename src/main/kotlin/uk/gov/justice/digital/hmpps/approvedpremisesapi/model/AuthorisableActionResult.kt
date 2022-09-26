package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

interface AuthorisableActionResult<EntityType> {
  data class Success<EntityType>(val entity: EntityType) : AuthorisableActionResult<EntityType>
  class Unauthorised<EntityType> : AuthorisableActionResult<EntityType>
  class NotFound<EntityType> : AuthorisableActionResult<EntityType>
}

// Mocking sealed interfaces is currently broken in mockk, so the else branch is need until this is resolved: https://github.com/mockk/mockk/issues/832
fun shouldNotBeReached(): Nothing = throw RuntimeException("This branch should not be reached as only AuthorisableActionResult.Success, AuthorisableActionResult.Unauthorised & AuthorisableActionResult.NotFound are returned")
