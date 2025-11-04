package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

/**
 *
 * @param crn
 * @param applicationOrigin
 * @param convictionId
 * @param deliusEventNumber
 * @param offenceId
 * @param bailHearingDate
 */
data class NewCas2v2Application(

  val crn: String,

  val applicationOrigin: ApplicationOrigin = ApplicationOrigin.homeDetentionCurfew,

  @Schema(example = "1502724704", description = "")
  val convictionId: Long? = null,

  @Schema(example = "7", description = "")
  val deliusEventNumber: String? = null,

  @Schema(example = "M1502750438", description = "")
  val offenceId: String? = null,

  val bailHearingDate: LocalDate? = null,
)
