package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository.FindOpenChangeRequestResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer

@Service
class Cas1ChangeRequestTransformer(
  private val personTransformer: PersonTransformer,
  private val objectMapper: ObjectMapper,
) {

  fun findOpenResultsToChangeRequestSummary(
    result: FindOpenChangeRequestResult,
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

  fun transformEntityToCas1ChangeRequest(
    entity: Cas1ChangeRequestEntity,
  ) = Cas1ChangeRequest(
    id = entity.id,
    type = Cas1ChangeRequestType.valueOf(entity.type.name),
    createdAt = entity.createdAt.toInstant(),
    requestReason = NamedId(
      id = entity.requestReason.id,
      name = entity.requestReason.code,
    ),
    updatedAt = entity.updatedAt.toInstant(),
    decision = entity.decision?.name?.let { Cas1ChangeRequestDecision.valueOf(it) },
    decisionJson = entity.decisionJson?.let { objectMapper.readTree(it) },
    rejectionReason = entity.rejectionReason?.let {
      NamedId(
        id = it.id,
        name = it.code,
      )
    },
  )
}
