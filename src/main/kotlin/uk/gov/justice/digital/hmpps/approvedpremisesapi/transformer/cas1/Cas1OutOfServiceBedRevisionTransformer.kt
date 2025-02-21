package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedRevision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionChangeType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.containsAny
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedRevisionType as ApiRevisionType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionType as DomainRevisionType

@Component
class Cas1OutOfServiceBedRevisionTransformer(
  private val cas1OutOfServiceBedReasonTransformer: Cas1OutOfServiceBedReasonTransformer,
  private val userTransformer: UserTransformer,
) {
  fun transformJpaToApi(jpa: Cas1OutOfServiceBedRevisionEntity): Cas1OutOfServiceBedRevision {
    val revisionType = jpa.deriveRevisionType()

    return Cas1OutOfServiceBedRevision(
      id = jpa.id,
      updatedAt = jpa.createdAt.toInstant(),
      updatedBy = jpa.createdBy?.let { userTransformer.transformJpaToApi(it, ServiceName.approvedPremises) },
      revisionType = revisionType,
      startDate = if (shouldDisplayOutOfServiceFrom(revisionType)) jpa.startDate else null,
      endDate = if (shouldDisplayOutOfServiceTo(revisionType)) jpa.endDate else null,
      reason = if (shouldDisplayReason(revisionType)) cas1OutOfServiceBedReasonTransformer.transformJpaToApi(jpa.reason) else null,
      referenceNumber = if (shouldDisplayReferenceNumber(revisionType)) jpa.referenceNumber else null,
      notes = if (shouldDisplayNotes(revisionType)) jpa.notes else null,
    )
  }

  private fun Cas1OutOfServiceBedRevisionEntity.deriveRevisionType(): List<ApiRevisionType> = when (this.revisionType) {
    DomainRevisionType.INITIAL -> listOf(ApiRevisionType.created)
    else -> Cas1OutOfServiceBedRevisionChangeType.unpack(this.changeTypePacked).map { it.apiValue }
  }

  private fun shouldDisplayOutOfServiceFrom(revisionType: List<ApiRevisionType>): Boolean = revisionType.containsAny(ApiRevisionType.created, ApiRevisionType.updatedStartDate)

  private fun shouldDisplayOutOfServiceTo(revisionType: List<ApiRevisionType>): Boolean = revisionType.containsAny(ApiRevisionType.created, ApiRevisionType.updatedEndDate)

  private fun shouldDisplayReason(revisionType: List<ApiRevisionType>): Boolean = revisionType.containsAny(ApiRevisionType.created, ApiRevisionType.updatedReason)

  private fun shouldDisplayReferenceNumber(revisionType: List<ApiRevisionType>): Boolean = revisionType.containsAny(ApiRevisionType.created, ApiRevisionType.updatedReferenceNumber)

  private fun shouldDisplayNotes(revisionType: List<ApiRevisionType>): Boolean = revisionType.containsAny(ApiRevisionType.created, ApiRevisionType.updatedNotes)
}
