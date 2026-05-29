package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.history.Revision
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleaseActionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleaseActionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleasePlanEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleasePlanRepository
import java.util.UUID

@Service
class AuditService(
  private val releasePlanRepository: ReleasePlanRepository,
  private val releaseActionRepository: ReleaseActionRepository,
) {

  @Transactional(readOnly = true)
  fun getReleasePlanRevisions(
    releasePlanId: UUID,
  ): List<Revision<Int, ReleasePlanEntity>> = releasePlanRepository.findRevisions(releasePlanId)
    .content
    .toList()

  @Transactional(readOnly = true)
  fun getReleaseActionRevisions(
    releaseActionId: UUID,
  ): List<Revision<Int, ReleaseActionEntity>> = releaseActionRepository.findRevisions(releaseActionId)
    .content
    .toList()

  @Transactional(readOnly = true)
  fun getRevisionsForSpaceBooking(
    spaceBooking: Cas1SpaceBookingEntity,
  ): List<Revision<Int, ReleasePlanEntity>> = releasePlanRepository.getBySpaceBooking(spaceBooking)
    ?.flatMap { releasePlan ->
      releasePlanRepository.findRevisions(releasePlan.id).content
    }
    ?.sortedBy { revision ->
      revision.metadata.revisionNumber.orElse(0)
    }
    ?: emptyList()
}
