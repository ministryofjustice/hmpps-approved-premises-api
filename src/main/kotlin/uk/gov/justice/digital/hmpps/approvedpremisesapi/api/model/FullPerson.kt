package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto

data class FullPerson(
  val name: String,
  val dateOfBirth: java.time.LocalDate,
  val sex: String,
  val status: PersonStatus,
  override val crn: String,
  override val type: PersonType,
  val nomsNumber: String? = null,
  val pncNumber: String? = null,
  val ethnicity: String? = null,
  val nationality: String? = null,
  val religionOrBelief: String? = null,
  val genderIdentity: String? = null,
  val prisonName: String? = null,
  val isRestricted: Boolean? = null,
  @Schema(description = "The person's current tier, if available")
  val tier: TierDto?,
) : Person
