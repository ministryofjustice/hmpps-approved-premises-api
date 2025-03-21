package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer

@Service
class Cas1ChangeRequestTransformer(
  private val personTransformer: PersonTransformer,
) {

  fun findOpenResultsToChangeRequestSummary(
    result: Cas1ChangeRequestRepository.FindOpenChangeRequestResult,
    person: PersonSummaryInfoResult,
  ) = Cas1ChangeRequestSummary(
    result.id,
    personTransformer.personSummaryInfoToPersonSummary(person),
    type = Cas1ChangeRequestType.valueOf(result.type),
    result.createdAt,
    result.lengthOfStayDays,
    result.tier,
    result.expectedArrivalDate,
    result.actualArrivalDate,
  )
}
