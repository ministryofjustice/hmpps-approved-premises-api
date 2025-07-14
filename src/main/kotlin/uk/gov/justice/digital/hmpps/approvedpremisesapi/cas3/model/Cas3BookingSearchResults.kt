package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonInclude
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Cas3BookingSearchResults(
  val resultsCount: Int,
  val results: List<Cas3BookingSearchResult>,
)

data class Cas3BookingSearchResult(
  val person: Cas3BookingSearchResultPersonSummary,
  val booking: Cas3BookingSearchResultBookingSummary,
  val premises: Cas3BookingSearchResultPremisesSummary,
  val bedspace: Cas3BookingSearchResultBedspaceSummary,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Cas3BookingSearchResultPersonSummary(
  val crn: String,
  val name: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Cas3BookingSearchResultBookingSummary(
  val id: UUID,
  val status: Cas3BookingStatus,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val createdAt: Instant,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Cas3BookingSearchResultPremisesSummary(
  val id: UUID,
  val name: String,
  val addressLine1: String,
  val postcode: String,
  val addressLine2: String? = null,
  val town: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Cas3BookingSearchResultBedspaceSummary(
  val id: UUID,
  val reference: String,
)

data class Cas3BookingSearchResultDto(
  var personName: String?,
  val personCrn: String,
  val bookingId: UUID,
  val bookingStatus: String,
  val bookingStartDate: LocalDate,
  val bookingEndDate: LocalDate,
  val bookingCreatedAt: OffsetDateTime,
  val premisesId: UUID,
  val premisesName: String,
  val premisesAddressLine1: String,
  val premisesAddressLine2: String?,
  val premisesTown: String?,
  val premisesPostcode: String,
  val bedspaceId: UUID,
  val bedspaceReference: String,
)
