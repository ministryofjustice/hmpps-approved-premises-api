package uk.gov.justice.digital.hmpps.approvedpremisesapi.results

@Deprecated(
  "The ValidatableActionResult and AuthorisableActionResult have been replaced by CasResult, which effectively flattens these two classes into one",
)
sealed interface AuthorisableActionResult<EntityType> {
  @Deprecated(
    "Replaced by CasResult.Success",
    ReplaceWith("uk.gov.justice.digital.hmpps.approvedpremisesapi.CasResult.Success"),
  )
  data class Success<EntityType>(
    val entity: EntityType,
  ) : AuthorisableActionResult<EntityType>

  @Deprecated(
    "Replaced by CasResult.Unauthorised",
    ReplaceWith("uk.gov.justice.digital.hmpps.approvedpremisesapi.CasResult.Unauthorised"),
  )
  class Unauthorised<EntityType> : AuthorisableActionResult<EntityType>

  @Deprecated(
    "Replaced by CasResult.NotFound",
    ReplaceWith("uk.gov.justice.digital.hmpps.approvedpremisesapi.CasResult.NotFound"),
  )
  class NotFound<EntityType>(
    val entityType: String? = null,
    val id: String? = null,
  ) : AuthorisableActionResult<EntityType>
}

fun <T, U> AuthorisableActionResult.NotFound<T>.into(): AuthorisableActionResult.NotFound<U> = AuthorisableActionResult.NotFound(this.entityType, this.id)
