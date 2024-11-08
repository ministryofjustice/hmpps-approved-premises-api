package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AdjudicationsPage
import java.time.LocalDate
import java.time.ZoneOffset

@Component
@SuppressWarnings("MagicNumber")
class AdjudicationTransformer {
  fun transformToApi(adjudicationsPage: AdjudicationsPage, getLast12MonthsOnly: Boolean) =
    adjudicationsPage.results.flatMap { result ->
      result.adjudicationCharges
        .filter {
          if (getLast12MonthsOnly) {
            result.reportTime.toLocalDate().isAfter(LocalDate.now().minusMonths(12))
          } else {
            true
          }
        }
        .map { charge ->
          Adjudication(
            id = result.adjudicationNumber,
            reportedAt = result.reportTime.toInstant(ZoneOffset.UTC),
            establishment = adjudicationsPage.agencies.firstOrNull { it.agencyId == result.agencyId }?.description ?: throw RuntimeException("Agency ${result.agencyId} not found"),
            offenceDescription = charge.offenceDescription,
            hearingHeld = charge.findingCode != null,
            finding = charge.findingCode,
          )
        }
    }
}
