package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer

@Service
class Cas1ChangeRequestTransformer(
  private val personTransformer: PersonTransformer,
  private val jsonMapper: JsonMapper,
) {

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
    decisionJson = entity.decisionJson?.let { jsonMapper.readTree(it) },
    rejectionReason = entity.rejectionReason?.let {
      NamedId(
        id = it.id,
        name = it.code,
      )
    },
    requestJson = entity.requestJson,
    spaceBookingId = entity.spaceBooking.id,
  )
}

fun ChangeRequestType.toApiType() = Cas1ChangeRequestType.valueOf(this.name)
