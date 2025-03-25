package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent

import java.time.OffsetDateTime
import java.util.UUID

data class SnsEvent(
  val eventType: String,
  val version: Int,
  val description: String?,
  val detailUrl: String,
  val occurredAt: OffsetDateTime,
  val additionalInformation: SnsEventAdditionalInformation,
  val personReference: SnsEventPersonReferenceCollection,
)

data class SnsEventPersonReferenceCollection(
  val identifiers: List<SnsEventPersonReference>,
)

data class SnsEventPersonReference(
  val type: String,
  val value: String,
)

data class SnsEventAdditionalInformation(
  val applicationId: UUID? = null,
  val bookingId: UUID? = null,
)
