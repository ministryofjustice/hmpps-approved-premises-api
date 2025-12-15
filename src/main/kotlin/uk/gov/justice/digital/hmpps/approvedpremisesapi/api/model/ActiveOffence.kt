package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class ActiveOffence(

  val deliusEventNumber: String,

  @Deprecated("Use mainCategoryDescription & subCategoryDescription instead")
  val offenceDescription: String,

  @Schema(example = "M1502750438")
  val offenceId: String,

  @Schema(example = "1502724704")
  val convictionId: Long,

  val offenceDate: java.time.LocalDate? = null,

  val mainOffence: Boolean = false,

  val mainCategoryDescription: String,

  val subCategoryDescription: String? = null,
)
