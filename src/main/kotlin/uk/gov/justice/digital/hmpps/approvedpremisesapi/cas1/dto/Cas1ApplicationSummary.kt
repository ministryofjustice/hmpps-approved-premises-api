package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Cas1ApplicationSummary(

  val id: UUID,

  val person: Person,

  val createdAt: Instant,

  val createdByUserId: UUID,

  val createdByUserName: String?,

  val status: ApprovedPremisesApplicationStatus,

  val isWithdrawn: Boolean,

  val hasRequestsForPlacement: Boolean,

  val submittedAt: Instant? = null,

  val isWomensApplication: Boolean? = null,

  val isPipeApplication: Boolean? = null,

  val isEmergencyApplication: Boolean? = null,

  val isEsapApplication: Boolean? = null,

  val arrivalDate: LocalDate? = null,

  val risks: PersonRisks? = null,

  @Schema(description = "The tier value from when the application was created. For the person's live tier use person.tier")
  val tier: String? = null,

  val releaseType: ReleaseTypeOption? = null,
)
