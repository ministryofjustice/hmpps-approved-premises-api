package uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@MustBeDocumented
@io.swagger.v3.oas.annotations.responses.ApiResponses(
  value = [
    ApiResponse(
      responseCode = "401",
      description = "not authenticated",
      content = [Content(schema = Schema(implementation = Problem::class))],
    ),
    ApiResponse(
      responseCode = "403",
      description = "unauthorised",
      content = [Content(schema = Schema(implementation = Problem::class))],
    ),
    ApiResponse(
      responseCode = "500",
      description = "unexpected error",
      content = [Content(schema = Schema(implementation = Problem::class))],
    ),
  ],
)
annotation class DefaultErrorResponses
