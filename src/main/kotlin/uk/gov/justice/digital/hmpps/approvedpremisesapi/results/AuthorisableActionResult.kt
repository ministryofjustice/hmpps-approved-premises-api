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

fun <T, U> AuthorisableActionResult.NotFound<T>.into(): AuthorisableActionResult.NotFound<U> =
  AuthorisableActionResult.NotFound(this.entityType, this.id)

@Deprecated(
  message = "A helper method to avoid having to handle multiple result types. Refactor to CasResult where possible rather than using this method.",
)
fun <T> AuthorisableActionResult<ValidatableActionResult<T>>.toCasResult() =
  when (this) {
    is AuthorisableActionResult.Success -> {
      when (this.entity) {
        is ValidatableActionResult.Success -> {
          CasResult.Success(this.entity.entity)
        }

        is ValidatableActionResult.ConflictError ->
          CasResult.ConflictError(
            conflictingEntityId = this.entity.conflictingEntityId,
            message = this.entity.message,
          )

        is ValidatableActionResult.FieldValidationError -> CasResult.FieldValidationError(this.entity.validationMessages)
        is ValidatableActionResult.GeneralValidationError -> CasResult.GeneralValidationError(this.entity.message)
      }
    }

    is AuthorisableActionResult.NotFound -> CasResult.NotFound(entityType = this.entityType, id = this.id)
    is AuthorisableActionResult.Unauthorised -> CasResult.Unauthorised()
  }
