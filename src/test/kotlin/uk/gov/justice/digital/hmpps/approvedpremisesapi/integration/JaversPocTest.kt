package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithMockUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineRecord
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateFieldChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleaseActionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleasePlanEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleasePlanRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JaversTimelineService
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class JaversPocTest : IntegrationTestBase() {

  @Autowired
  lateinit var releasePlanRepository: ReleasePlanRepository

  @Autowired
  lateinit var javersTimelineService: JaversTimelineService

  @Test
  @WithMockUser(username = "audit_user")
  fun `print all audit timeline entries for a space booking`() {
    val spaceBooking = createSpaceBooking()

    val releasePlan = createReleasePlan(spaceBooking, "Initial Plan")
    val action1 = ReleaseActionEntity(
      id = UUID.randomUUID(),
      description = "Action 1",
      actionCadence = "Daily",
    )

    releasePlan.releaseActions.add(action1)
    releasePlanRepository.save(releasePlan)
    releasePlanRepository.flush()

    releasePlan.description = "Updated Plan"
    releasePlan.expectedReleaseTime = LocalTime.of(11, 0)

    val action2 = ReleaseActionEntity(
      id = UUID.randomUUID(),
      description = "Action 2",
      actionCadence = "Weekly",
    )

    releasePlan.releaseActions.add(action2)
    releasePlanRepository.save(releasePlan)
    releasePlanRepository.flush()

    releasePlan.releaseActions.remove(action1)
    action2.description = "Modified Action 2"

    releasePlanRepository.save(releasePlan)
    releasePlanRepository.flush()

    releasePlanRepository.delete(releasePlan)

    val timelines = javersTimelineService.getFullAuditHistory(releasePlan.id, ReleasePlanEntity::class.java)

    /**
     *UPDATE by audit_user on 2026-06-03T12:20:31.724134Z
     * - Change: 'ObjectRemoved{ object removed: ReleasePlan/ff527f4a-40b5-4ed3-a0b8-9e3c07a2a3f1 }'
     * - Change: 'TerminalValueChange{ property: 'id', left:'ff527f4a-40b5-4ed3-a0b8-9e3c07a2a3f1',  right:'' }'
     * - Change: 'TerminalValueChange{ property: 'expectedReleaseTime', left:'11:00:00',  right:'' }'
     * - Change: 'TerminalValueChange{ property: 'expectedArrivalTime', left:'14:00:00',  right:'' }'
     * - Change: 'TerminalValueChange{ property: 'description', left:'Updated Plan',  right:'' }'
     * - Change: 'TerminalValueChange{ property: 'otherInformation', left:'Info',  right:'' }'
     * - Change: 'ListChange{ property: 'releaseActions', elementChanges:1, left.size: 1, right.size: 0}'
     *
     * UPDATE by audit_user on 2026-06-03T12:20:31.652459Z
     * - Change: 'ListChange{ property: 'releaseActions', elementChanges:2, left.size: 2, right.size: 1}'
     *
     * UPDATE by audit_user on 2026-06-03T12:20:31.593295Z
     * - Change: 'ValueChange{ property: 'expectedReleaseTime', left:'10:00:00',  right:'11:00:00' }'
     * - Change: 'ValueChange{ property: 'description', left:'Initial Plan',  right:'Updated Plan' }'
     * - Change: 'ListChange{ property: 'releaseActions', elementChanges:1, left.size: 1, right.size: 2}'
     *
     * UPDATE by audit_user on 2026-06-03T12:20:31.524545Z
     * - Change: 'NewObject{ new object: ReleasePlan/ff527f4a-40b5-4ed3-a0b8-9e3c07a2a3f1 }'
     * - Change: 'InitialValueChange{ property: 'id', left:'',  right:'ff527f4a-40b5-4ed3-a0b8-9e3c07a2a3f1' }'
     * - Change: 'InitialValueChange{ property: 'expectedReleaseTime', left:'',  right:'10:00:00' }'
     * - Change: 'InitialValueChange{ property: 'expectedArrivalTime', left:'',  right:'14:00:00' }'
     * - Change: 'InitialValueChange{ property: 'description', left:'',  right:'Initial Plan' }'
     * - Change: 'InitialValueChange{ property: 'otherInformation', left:'',  right:'Info' }'
     * - Change: 'ListChange{ property: 'releaseActions', elementChanges:1, left.size: 0, right.size: 1}'
     */

    println("--- JaVers Timeline Records ---")
    timelines.forEach { println(it.renderForPoc()) }
    println("-------------------------------")
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

private fun TimelineRecord.renderForPoc(): String = buildString {
  appendLine("$type by $author on $commitDate")
  changes.forEach { change ->
    when (change) {
      is UpdateFieldChange -> appendLine("- ${change.field}: '${change.oldValue}' -> '${change.value}'")
      else -> appendLine("- ${change.field}: '${change.value}'")
    }
  }
}
