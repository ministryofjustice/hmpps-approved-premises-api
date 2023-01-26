package uk.gov.justice.digital.hmpps.approvedpremisesapi.results

sealed interface AuthorisableActionResult<EntityType> {
  data class Success<EntityType>(val entity: EntityType) : AuthorisableActionResult<EntityType>
  data class Unauthorised<EntityType>(val id: Any? = null, val entityType: String? = null) : AuthorisableActionResult<EntityType>
  data class NotFound<EntityType>(val id: Any? = null, val entityType: String? = null) : AuthorisableActionResult<EntityType>
}
