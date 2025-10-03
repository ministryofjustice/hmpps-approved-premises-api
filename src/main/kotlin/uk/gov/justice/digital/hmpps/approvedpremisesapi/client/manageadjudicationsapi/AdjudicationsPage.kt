package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.manageadjudicationsapi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime

data class AdjudicationsPage(
  val results: List<Adjudication>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Adjudication(
  val incidentDetails: IncidentDetailsDto,
  val offenceDetails: OffenceDto,
  val status: String,
  val hearings: List<HearingDto>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IncidentDetailsDto(
  val dateTimeOfIncident: LocalDateTime,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OffenceDto(
  val offenceCode: Int,
  val offenceRule: OffenceRuleDto,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OffenceRuleDto(
  val paragraphDescription: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HearingDto(
  val agencyId: String,
)
