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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilInclusive
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosecondsToNearestMillisecond

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
    type = entity.type.toApiType(),
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

  fun transformToChangeRequestSummaries(
    cas1ChangeRequests: List<Cas1ChangeRequestEntity>,
    personInfoResult: PersonInfoResult,
  ): List<Cas1ChangeRequestSummary> {
    val personSummary = personTransformer.transformPersonInfoResultToPersonSummary(personInfoResult)

    return cas1ChangeRequests.map { entity ->
      val booking = entity.spaceBooking

      Cas1ChangeRequestSummary(
        id = entity.id,
        person = personSummary,
        type = Cas1ChangeRequestType.valueOf(entity.type.name),
        createdAt = entity.createdAt.roundNanosecondsToNearestMillisecond().toInstant(),
        lengthOfStayDays = booking.canonicalArrivalDate
          .getDaysUntilInclusive(booking.canonicalDepartureDate).size,
        tier = booking.application?.riskRatings?.tier?.value?.level,
        expectedArrivalDate = booking.expectedArrivalDate,
        actualArrivalDate = booking.actualArrivalDate,
      )
    }
  }
}

fun ChangeRequestType.toApiType() = Cas1ChangeRequestType.valueOf(this.name)
