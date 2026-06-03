package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.history.RevisionMetadata
import org.springframework.security.test.context.support.WithMockUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleaseActionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleasePlanEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleasePlanRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RevInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.render
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AuditService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AuditTimelineService
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class EnversPocTest : IntegrationTestBase() {

  @Autowired
  lateinit var releasePlanRepository: ReleasePlanRepository

  @Autowired
  lateinit var auditService: AuditService

  @Autowired
  lateinit var auditTimelineService: AuditTimelineService

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
    val planRevisions = auditService.getReleasePlanRevisions(releasePlan.id)

    assertThat(planRevisions).hasSize(3)

    assertThat(planRevisions.map { it.metadata.revisionType })
      .contains(
        RevisionMetadata.RevisionType.INSERT,
        RevisionMetadata.RevisionType.UPDATE,
      )

    val planDescriptions = planRevisions.map { it.entity.description }

    assertThat(planDescriptions)
      .contains("Initial Plan", "Updated Plan")

    // -------------------------
    // ASSERT ACTION 1
    // -------------------------
    val action1Revisions = auditService.getReleaseActionRevisions(action1.id)

    assertThat(action1Revisions).hasSize(2)

    assertThat(action1Revisions.map { it.metadata.revisionType })
      .containsExactly(
        RevisionMetadata.RevisionType.INSERT,
        RevisionMetadata.RevisionType.DELETE,
      )

    // -------------------------
    // ASSERT ACTION 2
    // -------------------------
    val action2Revisions = auditService.getReleaseActionRevisions(action2.id)

    assertThat(action2Revisions).hasSize(2)

    assertThat(action2Revisions.map { it.entity.description })
      .contains("Action 2", "Modified Action 2")

    assertThat(action2Revisions.map { it.metadata.revisionType })
      .contains(
        RevisionMetadata.RevisionType.INSERT,
        RevisionMetadata.RevisionType.UPDATE,
      )

    /**
     * --- Audit Timeline ---
     * Release plan updated By audit_user
     * on 2026-06-02T13:49:02.449Z
     *
     * Release action
     * - Description changed from 'Action 2' to 'Modified Action 2'
     *
     * Release plan updated By audit_user
     * on 2026-06-02T13:49:02.426Z
     *
     * Release plan
     * - Description changed from 'Initial Plan' to 'Updated Plan'
     * - Expected release time changed from '10:00' to '11:00'
     *
     * Release action
     * - Created release action with description 'Action 2'
     *
     * Release plan updated By audit_user
     * on 2026-06-02T13:49:02.388Z
     *
     * Release plan
     * - Created release plan with description 'Initial Plan'
     *
     * --------------------------
     */

    val timelines = auditService.getAuditTimeLineForSpaceBooking(spaceBooking)

    println("--- Audit Timeline ---")
    timelines.forEach { println(it.render()) }
    println("--------------------------")
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

  @Test
  @WithMockUser(username = "audit_user")
  fun `print all audit timeline entries for a space booking`() {
    val spaceBooking = createSpaceBooking()

    // -------------------------
    // 1. CREATE RELEASE PLAN/ACTION
    // -------------------------
    val releasePlan = createReleasePlan(spaceBooking, "Initial Plan")

    val action1 = ReleaseActionEntity(
      id = UUID.randomUUID(),
      description = "Action 1",
      actionCadence = "Daily",
    )

    releasePlan.releaseActions.add(action1)

    releasePlanRepository.saveAndFlush(releasePlan)

    // -------------------------
    // 2. UPDATE PLAN + ADD ACTION
    // -------------------------
    releasePlan.description = "Updated Plan"
    releasePlan.expectedReleaseTime = LocalTime.of(11, 0)

    val action2 = ReleaseActionEntity(
      id = UUID.randomUUID(),
      description = "Action 2",
      actionCadence = "Weekly",
    )

    releasePlan.releaseActions.add(action2)

    releasePlanRepository.saveAndFlush(releasePlan)

    // -------------------------
    // 3. REMOVE ACTION + MODIFY ACTION
    // -------------------------
    releasePlan.releaseActions.remove(action1)

    action2.description = "Modified Action 2"

    releasePlanRepository.saveAndFlush(releasePlan)

    // ---------------------------
    // 4. DELETE RELEASE PLAN
    releasePlanRepository.delete(releasePlan)

    val timelines = auditTimelineService.getTimelineForSpaceBooking(spaceBooking)

    /**
     * --- Audit Timeline ---
     * Release plan updated By audit_user
     * on 2026-06-03T06:35:03.796Z
     *
     * Release Plan
     * - Deleted release plan with description 'Updated Plan'
     *
     * Release Actions
     * - Deleted release action with description 'Modified Action 2'
     *
     * Release plan updated By audit_user
     * on 2026-06-03T06:35:03.770Z
     *
     * Release Actions
     * - Description changed from 'Action 2' to 'Modified Action 2'
     *
     * Release Actions
     * - Deleted release action with description 'Action 1'
     *
     * Release plan updated By audit_user
     * on 2026-06-03T06:35:03.747Z
     *
     * Release Plan
     * - Description changed from 'Initial Plan' to 'Updated Plan'
     * - Expected release time changed from '10:00' to '11:00'
     *
     * Release Actions
     * - Created release action with description 'Action 2'
     *
     * Release plan updated By audit_user
     * on 2026-06-03T06:35:03.715Z
     *
     * Release Plan
     * - Created release plan with description 'Initial Plan'
     *
     * Release Actions
     * - Created release action with description 'Action 1'
     */

    println("--- Audit Timeline ---")
    timelines.forEach { println(it.render()) }
    println("--------------------------")
  }

  private fun createSpaceBooking(): Cas1SpaceBookingEntity = givenACas1SpaceBooking(
    crn = "CRN123",
    actualArrivalDate = null,
    expectedArrivalDate = LocalDate.of(2025, 3, 25),
    expectedDepartureDate = LocalDate.of(2025, 4, 10),
    cancellationOccurredAt = null,
    nonArrivalConfirmedAt = null,
  )

  private fun createReleasePlan(spaceBooking: Cas1SpaceBookingEntity, description: String): ReleasePlanEntity = ReleasePlanEntity(
    id = UUID.randomUUID(),
    spaceBooking = spaceBooking,
    expectedReleaseTime = LocalTime.of(10, 0),
    expectedArrivalTime = LocalTime.of(14, 0),
    description = description,
    otherInformation = "Info",
    releaseActions = mutableListOf(),
  )
}
