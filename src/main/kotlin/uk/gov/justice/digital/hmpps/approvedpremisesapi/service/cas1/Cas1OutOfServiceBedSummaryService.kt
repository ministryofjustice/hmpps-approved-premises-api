package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Temporality
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import java.time.LocalDate
import java.util.UUID

@Service
class Cas1OutOfServiceBedSummaryService(
  private val cas1PremisesService: Cas1PremisesService,
  private val cas1OutOfServiceBedService: Cas1OutOfServiceBedService,
  private val cas1OutOfServiceBedSummaryTransformer: Cas1OutOfServiceBedSummaryTransformer,
) {

  fun getOutOfServiceBedSummaries(
    premisesId: UUID,
    apAreaId: UUID,
    date: LocalDate,
  ): CasResult<List<Cas1OutOfServiceBedSummary>> {
    if (cas1PremisesService.findPremiseById(premisesId) == null) return CasResult.NotFound("premises", premisesId.toString())

    val (outOfServiceBeds) = cas1OutOfServiceBedService.getOutOfServiceBedsForDate(
      temporality = setOf(Temporality.current),
      premisesId = premisesId,
      date = date,
      apAreaId = apAreaId,
      pageCriteria = PageCriteria(
        sortBy = Cas1OutOfServiceBedSortField.startDate,
        sortDirection = SortDirection.asc,
        page = 1,
        perPage = 1000,
      ),
    )

    val oosBedSummaries = outOfServiceBeds.map(cas1OutOfServiceBedSummaryTransformer::toCas1OutOfServiceBedSummary)

    return CasResult.Success(
      oosBedSummaries,
    )
  }
}
