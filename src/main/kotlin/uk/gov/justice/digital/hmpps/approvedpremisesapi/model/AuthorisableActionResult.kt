package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

interface AuthorisableActionResult<EntityType> {
  data class Success<EntityType>(val entity: EntityType) : AuthorisableActionResult<EntityType>
  class Unauthorised<EntityType> : AuthorisableActionResult<EntityType>
  class NotFound<EntityType> : AuthorisableActionResult<EntityType>
}
