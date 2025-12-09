package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.manageadjudicationsapi.AdjudicationsPage
import java.time.LocalDate
import java.time.ZoneOffset

@Component
@SuppressWarnings("MagicNumber")
class AdjudicationTransformer {
  fun transformToApi(adjudicationsPage: AdjudicationsPage, getLast12MonthsOnly: Boolean): List<Adjudication> {
    val adjudications = adjudicationsPage.results.filter {
      if (getLast12MonthsOnly) {
        it.incidentDetails.dateTimeOfIncident.toLocalDate().isAfter(LocalDate.now().minusMonths(12))
      } else {
        true
      }
    }.map {result ->
      Adjudication(
        id = result.offenceDetails.offenceCode.toLong(),
        reportedAt = result.incidentDetails.dateTimeOfIncident.toInstant(ZoneOffset.UTC),
        establishment = result.hearings.firstOrNull()?.agencyId
          ?: throw RuntimeException("Agency not found"),
        offenceDescription = result.offenceDetails.offenceRule.paragraphDescription,
        hearingHeld = true,
        finding = result.status,
      )
    }
    return adjudications
  }
}
