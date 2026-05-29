package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.history.RevisionMetadata
import org.springframework.security.test.context.support.WithMockUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleaseActionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleaseActionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleasePlanEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleasePlanRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RevInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AuditService
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class EnversPocTest : IntegrationTestBase() {

  @Autowired
  lateinit var releasePlanRepository: ReleasePlanRepository

  @Autowired
  lateinit var releaseActionRepository: ReleaseActionRepository

  @Autowired
  lateinit var auditService: AuditService

  @Test
  @WithMockUser(username = "audit_user")
  fun `captures user who made the change`() {
    val spaceBooking = createSpaceBooking()

    val releasePlan = createReleasePlan(spaceBooking, "Release plan 1")

    releasePlanRepository.saveAndFlush(releasePlan)

    val revisions = auditService.getReleasePlanRevisions(releasePlan.id)

    assertThat(revisions).hasSize(1)

    val revision = revisions[0]

    assertThat(revision.metadata.revisionType)
      .isEqualTo(RevisionMetadata.RevisionType.INSERT)

    val revInfo = revision.metadata.getDelegate<RevInfo>()

    assertThat(revInfo.username)
      .isEqualTo("audit_user")
  }

  @Test
  @WithMockUser(username = "audit_user")
  fun `track full lifecycle of release plan and actions`() {
    val spaceBooking = createSpaceBooking()

    // -------------------------
    // 1. CREATE
    // -------------------------
    val releasePlan = createReleasePlan(spaceBooking, "Initial Plan")

    val action1 = ReleaseActionEntity(
      id = UUID.randomUUID(),
      description = "Action 1",
      actionCadence = "Daily",
      releasePlan = releasePlan,
    )

    releasePlan.releaseActions.add(action1)

    releasePlanRepository.saveAndFlush(releasePlan)

    // -------------------------
    // 2. UPDATE + ADD ACTION
    // -------------------------
    releasePlan.description = "Updated Plan"
    releasePlan.expectedReleaseTime = LocalTime.of(11, 0)

    val action2 = ReleaseActionEntity(
      id = UUID.randomUUID(),
      description = "Action 2",
      actionCadence = "Weekly",
      releasePlan = releasePlan,
    )

    releasePlan.releaseActions.add(action2)

    releasePlanRepository.saveAndFlush(releasePlan)

    // -------------------------
    // 3. REMOVE + MODIFY
    // -------------------------
    releasePlan.releaseActions.remove(action1)
    action2.description = "Modified Action 2"

    releasePlanRepository.saveAndFlush(releasePlan)

    // -------------------------
    // ASSERT RELEASE PLAN HISTORY
    // -------------------------

    val planRevisions =
      auditService.getReleasePlanRevisions(releasePlan.id)

    assertThat(planRevisions).hasSize(3)

    assertThat(planRevisions[0].metadata.revisionType)
      .isEqualTo(RevisionMetadata.RevisionType.INSERT)

    assertThat(planRevisions[1].metadata.revisionType)
      .isEqualTo(RevisionMetadata.RevisionType.UPDATE)

    assertThat(planRevisions[2].metadata.revisionType)
      .isEqualTo(RevisionMetadata.RevisionType.UPDATE)

    val planDescriptions =
      planRevisions.map { it.entity.description }

    assertThat(planDescriptions)
      .contains("Initial Plan", "Updated Plan")

    assertThat(planRevisions.last().entity.description)
      .isEqualTo("Updated Plan")

    // -------------------------
    // ASSERT ACTION 1 (CREATE)
    // -------------------------

    val action1Revisions =
      auditService.getReleaseActionRevisions(action1.id)

    assertThat(action1Revisions).isNotEmpty

    assertThat(action1Revisions).hasSize(1)

    assertThat(action1Revisions.map { it.metadata.revisionType })
      .contains(RevisionMetadata.RevisionType.INSERT)

    // -------------------------
    // ASSERT ACTION 2 (CREATE + UPDATE)
    // -------------------------

    val action2Revisions =
      auditService.getReleaseActionRevisions(action2.id)

    assertThat(action2Revisions).isNotEmpty

    assertThat(action2Revisions).hasSize(2)

    assertThat(action2Revisions.map { it.entity.description })
      .contains("Action 2", "Modified Action 2")

    assertThat(action2Revisions.map { it.metadata.revisionType })
      .contains(RevisionMetadata.RevisionType.INSERT, RevisionMetadata.RevisionType.UPDATE)
  }

  @Test
  @WithMockUser(username = "audit_user")
  fun `collate all changes linked to the same space booking`() {
    val spaceBooking = createSpaceBooking()

    val rp1 = createReleasePlan(spaceBooking, "RP1")
    val rp2 = createReleasePlan(spaceBooking, "RP2")

    releasePlanRepository.saveAndFlush(rp1)
    releasePlanRepository.saveAndFlush(rp2)

    // updates
    rp1.description = "RP1 Updated"
    releasePlanRepository.saveAndFlush(rp1)

    rp2.description = "RP2 Updated"
    releasePlanRepository.saveAndFlush(rp2)

    val revisions = auditService.getRevisionsForSpaceBooking(spaceBooking)

    val descriptions = revisions.map { it.entity.description }

    assertThat(descriptions)
      .contains("RP1", "RP2")
      .contains("RP1 Updated", "RP2 Updated")

    // -------------------------
    // ASSERT: user is captured
    // -------------------------
    val users = revisions.map {
      it.metadata.getDelegate<RevInfo>().username
    }

    assertThat(users)
      .allMatch { it == "audit_user" }
  }

  private fun createSpaceBooking(): Cas1SpaceBookingEntity = givenACas1SpaceBooking(
    crn = "CRN123",
    actualArrivalDate = null,
    expectedArrivalDate = LocalDate.of(2025, 3, 25),
    expectedDepartureDate = LocalDate.of(2025, 4, 10),
    cancellationOccurredAt = null,
    nonArrivalConfirmedAt = null,
  )

  private fun createReleasePlan(spaceBooking: Cas1SpaceBookingEntity, description: String): ReleasePlanEntity {
    val rp = ReleasePlanEntity(
      id = UUID.randomUUID(),
      spaceBooking = spaceBooking,
      expectedReleaseTime = LocalTime.of(10, 0),
      expectedArrivalTime = LocalTime.of(14, 0),
      description = description,
      otherInformation = "Info",
      releaseActions = mutableListOf(),
    )
    return releasePlanRepository.saveAndFlush(rp)
  }
}
