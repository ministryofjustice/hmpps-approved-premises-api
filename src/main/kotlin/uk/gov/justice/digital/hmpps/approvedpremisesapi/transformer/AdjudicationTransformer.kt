package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AdjudicationsPage
import java.time.ZoneOffset

@Component
class AdjudicationTransformer {
  fun transformToApi(adjudicationsPage: AdjudicationsPage) = adjudicationsPage.results.flatMap { result ->
    result.adjudicationCharges.map { charge ->
      Adjudication(
        id = result.adjudicationNumber,
        reportedAt = result.reportTime.atOffset(ZoneOffset.UTC),
        establishment = adjudicationsPage.agencies.firstOrNull { it.agencyId == result.agencyId }?.description ?: throw RuntimeException("Agency ${result.agencyId} not found"),
        offenceDescription = charge.offenceDescription,
        hearingHeld = charge.findingCode != null,
        finding = charge.findingCode
      )
    }
  }
}
