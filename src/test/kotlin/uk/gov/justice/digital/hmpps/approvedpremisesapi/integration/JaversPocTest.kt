package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithMockUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MonitoringInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleaseActionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleasePlanEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleasePlanRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JaversTimelineService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TimelineService
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class JaversPocTest : IntegrationTestBase() {

  @Autowired
  lateinit var releasePlanRepository: ReleasePlanRepository

  @Autowired
  lateinit var javersTimelineService: JaversTimelineService

  @Autowired
  lateinit var timelineService: TimelineService

  @Test
  @WithMockUser(username = "audit_user")
  fun `print all audit timeline entries for a space booking`() {
    val spaceBooking = createSpaceBooking()

    /**
     * Create release plan
     */
    val releasePlan = createReleasePlan(spaceBooking, "Initial Plan")
    releasePlanRepository.saveAndFlush(releasePlan)

    /**
     * Create release action and monitoring information
     */
    val action1 = ReleaseActionEntity(
      id = UUID.randomUUID(),
      description = "Action 1",
      actionCadence = "Daily",
    )

    val monitoringInformation = MonitoringInformation(
      description = "some description",
      otherInformation = null,
    )

    releasePlan.releaseActions.add(action1)
    releasePlan.monitoringInformation.add(monitoringInformation)
    releasePlanRepository.saveAndFlush(releasePlan)

    /**
     * Update release plan
     */
    releasePlan.description = "Updated Plan"
    releasePlan.expectedReleaseTime = LocalTime.of(11, 0)

    /**
     * Add release action and remove the release action
     */
    val action2 = ReleaseActionEntity(
      id = UUID.randomUUID(),
      description = "Action 2",
      actionCadence = "Weekly",
    )

    releasePlan.releaseActions.add(action2)
    releasePlan.releaseActions.remove(action1)
    releasePlanRepository.saveAndFlush(releasePlan)

    /**
     * Delete release plan
     */
    releasePlanRepository.delete(releasePlan)

    val mapper = JsonMapper.builder()
      .addModule(JavaTimeModule())
      .enable(SerializationFeature.INDENT_OUTPUT)
      .build()

    println("********TimelineRecordsV1************")

    val timelinesV1 = javersTimelineService.getTimelineRecordsForSpaceBooking(spaceBooking.id)

    println(mapper.writeValueAsString(timelinesV1))

    println("*****************************")

    println("********TimelineRecordsV2************")

    val timelinesV2 = timelineService.getTimelineRecordsForSpaceBooking(spaceBooking.id)

    println(mapper.writeValueAsString(timelinesV2))
    println("*****************************")
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

/*private fun TimelineRecord.renderForPoc(): String = buildString {
  appendLine("$type by $author on $commitDate")
  changes.forEach { change ->
    when (change) {
      is UpdateFieldChange -> appendLine("- ${change.field}: '${change.oldValue}' -> '${change.value}'")
      else -> appendLine("- ${change.field}: '${change.value}'")
    }
  }
}*/
