package uk.gov.justice.digital.hmpps.approvedpremisesapi.results

sealed interface AuthorisableActionResult<EntityType> {
  data class Success<EntityType>(val entity: EntityType) : AuthorisableActionResult<EntityType>
  class Unauthorised<EntityType> : AuthorisableActionResult<EntityType>
  class NotFound<EntityType>(val entityType: String? = null, val id: String? = null) : AuthorisableActionResult<EntityType>
}

fun <T, U> AuthorisableActionResult.NotFound<T>.into(): AuthorisableActionResult.NotFound<U> = AuthorisableActionResult.NotFound(this.entityType, this.id)
