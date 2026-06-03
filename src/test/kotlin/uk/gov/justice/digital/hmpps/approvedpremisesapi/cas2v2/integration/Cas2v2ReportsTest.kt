package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.filter
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.events.Cas2ApplicationStatusUpdatedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.events.Cas2ApplicationSubmittedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.events.Cas2StatusFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration.givens.givenASubmittedCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration.givens.givenAnUnsubmittedCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration.givens.givenAnUnsubmittedCas2HdcApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.ApplicationStatusUpdatesReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.SubmittedApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.UnsubmittedApplicationsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.minusDays
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class Cas2v2ReportsTest : IntegrationTestBase() {

  @Nested
  inner class ControlsOnExternalUsers {
    @ParameterizedTest
    @EnumSource(value = Cas2ReportName::class)
    fun `downloading cas2v2 report is forbidden to external users without MI role`(reportName: Cas2ReportName) {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "auth",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.get()
        .uri("/cas2v2/reports/${reportName.value}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  inner class MissingJwt {
    @ParameterizedTest
    @ValueSource(
      strings = [
        "submitted-applications",
        "application-status-updates",
        "unsubmitted-applications",
      ],
    )
    fun `Downloading cas2v2 report without JWT returns 401`(reportName: String) {
      webTestClient.get()
        .uri("/cas2v2/reports/$reportName")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class ControlsOnInternalUsers {
    @ParameterizedTest
    @EnumSource(value = Cas2ReportName::class)
    fun `downloading cas2v2 report is forbidden to NOMIS users without MI role`(reportName: Cas2ReportName) {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_POM"),
      )

      webTestClient.get()
        .uri("/cas2v2/reports/${reportName.value}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  private fun createApplicationSubmittedDomainEvent(
    application: Cas2ApplicationEntity,
    occurredAt: OffsetDateTime,
  ): CreatedEvent {
    val eventData = Cas2ApplicationSubmittedEvent(
      id = UUID.randomUUID(),
      timestamp = Instant.now(),
      eventType = EventType.applicationSubmitted,
      eventDetails = Cas2ApplicationSubmittedEventDetailsFactory()
        .withSubmittedAt(occurredAt.toInstant())
        .produce(),
    )
    val event = domainEventFactory.produceAndPersist {
      withId(eventData.id)
      withType(DomainEventType.CAS2_APPLICATION_SUBMITTED)
      withData(jsonMapper.writeValueAsString(eventData))
      withOccurredAt(occurredAt)
      withApplicationId(application.id)
    }
    return CreatedEvent(event, eventData)
  }

  private data class CreatedEvent(
    val event: DomainEventEntity,
    val data: Cas2ApplicationSubmittedEvent,
  )

  @Nested
  inner class SubmittedApplicationReport {

    @Test
    fun `only includes submissions from last 12 months, excluding HDC`() {
      val oldSubmitted = OffsetDateTime.now().minusDays(365).withOffsetSameInstant(ZoneOffset.UTC)
      val newerSubmitted = OffsetDateTime.now().minusDays(100)
      val tooOldSubmitted = OffsetDateTime.now().minusDays(366)

      val application1 = givenASubmittedCas2Application(crn = "CRN_1", nomsNumber = "NOMS_1")
      val (event1, event1Data) = createApplicationSubmittedDomainEvent(
        application = application1,
        occurredAt = oldSubmitted,
      )

      val application2 = givenASubmittedCas2Application(crn = "CRN_2", nomsNumber = "NOMS_2")
      val (event2, event2Data) = createApplicationSubmittedDomainEvent(
        application = application2,
        occurredAt = newerSubmitted,
      )

      // we don't expect this application to be included because the event occurred at outside of time range
      val application3 = givenASubmittedCas2Application(crn = "CRN_2", nomsNumber = "NOMS_2")
      createApplicationSubmittedDomainEvent(
        application = application3,
        occurredAt = tooOldSubmitted,
      )

      val hdcApplication = givenAnUnsubmittedCas2HdcApplication()
      createApplicationSubmittedDomainEvent(
        application = hdcApplication,
        occurredAt = newerSubmitted,
      )

      val expectedDataFrame = listOf(
        SubmittedApplicationReportRow(
          eventId = event2Data.id.toString(),
          applicationId = event2.applicationId.toString(),
          personCrn = event2Data.eventDetails.personReference.crn.toString(),
          personNoms = event2Data.eventDetails.personReference.noms,
          referringPrisonCode = event2Data.eventDetails.referringPrisonCode.toString(),
          preferredAreas = event2Data.eventDetails.preferredAreas.toString(),
          hdcEligibilityDate = event2Data.eventDetails.hdcEligibilityDate,
          conditionalReleaseDate = event2Data.eventDetails.conditionalReleaseDate,
          submittedAt = event2.occurredAt.toString().split(".").first(),
          submittedBy = event2Data.eventDetails.submittedBy.staffMember.username.toString(),
          startedAt = application2.createdAt.toString().split(".").first(),
          applicationOrigin = ApplicationOrigin.courtBail,
          bailHearingDate = application2.bailHearingDate,
        ),
        SubmittedApplicationReportRow(
          eventId = event1Data.id.toString(),
          applicationId = event1.applicationId.toString(),
          personCrn = event1Data.eventDetails.personReference.crn.toString(),
          personNoms = event1Data.eventDetails.personReference.noms,
          referringPrisonCode = event1Data.eventDetails.referringPrisonCode.toString(),
          preferredAreas = event1Data.eventDetails.preferredAreas.toString(),
          hdcEligibilityDate = event1Data.eventDetails.hdcEligibilityDate,
          conditionalReleaseDate = event1Data.eventDetails.conditionalReleaseDate,
          submittedAt = event1.occurredAt.toString().split(".").first(),
          submittedBy = event1Data.eventDetails.submittedBy.staffMember.username.toString(),
          startedAt = application1.createdAt.toString().split(".").first(),
          applicationOrigin = ApplicationOrigin.courtBail,
          bailHearingDate = application1.bailHearingDate,
        ),
      )
        .toDataFrame()

      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_MI"),
      )

      webTestClient.get()
        .uri("/cas2v2/reports/submitted-applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readExcel(it.responseBody!!.inputStream())
            .convertTo<SubmittedApplicationReportRow>(ExcessiveColumns.Remove)

          assertThat(actual).isEqualTo(expectedDataFrame)
        }
    }

    @Test
    fun `origins are correctly populated`() {
      val submitted = OffsetDateTime.now()

      val allPrisonBailApplications = (1..10).map {
        givenASubmittedCas2Application(
          applicationOrigin = ApplicationOrigin.prisonBail,
        )
      }

      val allCourtBailApplications = (1..10).map {
        givenASubmittedCas2Application(
          applicationOrigin = ApplicationOrigin.courtBail,
        )
      }

      (allPrisonBailApplications + allCourtBailApplications).forEach {
        createApplicationSubmittedDomainEvent(
          application = it,
          occurredAt = submitted,
        )
      }

      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_MI"),
      )

      val responseBody = webTestClient.get()
        .uri("/cas2v2/reports/submitted-applications")
        .header("Content-Type", "text/csv")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        .expectBody<ByteArray>()
        .returnResult()
        .responseBody

      val inputStream = ByteArrayInputStream(responseBody)
      val dataFrame = DataFrame.readExcel(inputStream)

      assertThat(dataFrame.columnsCount()).isEqualTo(13)
      val headers = dataFrame.columns()
      assertThat(headers[0].name()).isEqualTo("eventId")
      assertThat(headers[1].name()).isEqualTo("applicationId")
      assertThat(headers[2].name()).isEqualTo("personCrn")
      assertThat(headers[3].name()).isEqualTo("personNoms")
      assertThat(headers[4].name()).isEqualTo("referringPrisonCode")
      assertThat(headers[5].name()).isEqualTo("preferredAreas")
      assertThat(headers[6].name()).isEqualTo("hdcEligibilityDate")
      assertThat(headers[7].name()).isEqualTo("conditionalReleaseDate")
      assertThat(headers[8].name()).isEqualTo("submittedAt")
      assertThat(headers[9].name()).isEqualTo("submittedBy")
      assertThat(headers[10].name()).isEqualTo("startedAt")
      assertThat(headers[11].name()).isEqualTo("applicationOrigin")
      assertThat(headers[12].name()).isEqualTo("bailHearingDate")

      assertThat(dataFrame.rowsCount()).isEqualTo(20)
      val prisonBailCount = dataFrame.filter { row -> row["applicationOrigin"] == "prisonBail" }.rowsCount()
      val courtBailCount = dataFrame.filter { row -> row["applicationOrigin"] == "courtBail" }.rowsCount()

      assertThat(prisonBailCount).isEqualTo(10)
      assertThat(courtBailCount).isEqualTo(10)
    }
  }

  @Nested
  inner class ApplicationStatusUpdateReport {

    @Test
    fun `streams spreadsheet of cas2v2 Cas2ApplicationStatusUpdatedEvents, last 12 months only`() {
      // create applications and then

      val user = cas2UserEntityFactory.produceAndPersist {
        withServiceOrigin(Cas2ServiceOrigin.BAIL)
      }
      val application1 = cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(user)
        withServiceOrigin(Cas2ServiceOrigin.BAIL)
      }
      val application1ID = application1.id

      val application2 = cas2ApplicationEntityFactory.produceAndPersist {
        withApplicationOrigin(ApplicationOrigin.courtBail)
        withCreatedByUser(user)
        withServiceOrigin(Cas2ServiceOrigin.BAIL)
      }
      val application2ID = application2.id

      val event1Id = UUID.randomUUID()
      val event2Id = UUID.randomUUID()
      val event3Id = UUID.randomUUID()

      val old = Instant.now().minusDays(365)
      val newer = Instant.now().minusDays(100)
      val tooOld = Instant.now().minusDays(366)

      val event1StatusDetails = listOf(
        Cas2StatusDetail("personalInformation", "Personal information"),
        Cas2StatusDetail("riskOfSeriousHarm", "Risk of serious harm"),
        Cas2StatusDetail("hdcAndCpp", "HDC licence and CPP details"),
      )

      val event1Status = Cas2StatusFactory()
        .withStatusDetails(event1StatusDetails)
        .produce()

      val event1Details = Cas2ApplicationStatusUpdatedEventDetailsFactory()
        .withStatus(event1Status)
        .withUpdatedAt(old)
        .produce()

      val event2StatusDetails = emptyList<Cas2StatusDetail>()

      val event2Status = Cas2StatusFactory()
        .withStatusDetails(event2StatusDetails)
        .produce()

      val event2Details = Cas2ApplicationStatusUpdatedEventDetailsFactory()
        .withStatus(event2Status)
        .withUpdatedAt(newer)
        .produce()
      val event3Details = Cas2ApplicationStatusUpdatedEventDetailsFactory()
        .withUpdatedAt(tooOld)
        .produce()

      val event1ToSave = Cas2ApplicationStatusUpdatedEvent(
        id = event1Id,
        timestamp = Instant.now(),
        eventType = EventType.applicationStatusUpdated,
        eventDetails = event1Details,
      )

      val event2ToSave = Cas2ApplicationStatusUpdatedEvent(
        id = event2Id,
        timestamp = Instant.now(),
        eventType = EventType.applicationStatusUpdated,
        eventDetails = event2Details,
      )

      val event3ToSave = Cas2ApplicationStatusUpdatedEvent(
        id = event3Id,
        timestamp = Instant.now(),
        eventType = EventType.applicationStatusUpdated,
        eventDetails = event3Details,
      )

      val event1 = domainEventFactory.produceAndPersist {
        withId(event1Id)
        withApplicationId(application1ID)
        withType(DomainEventType.CAS2_APPLICATION_STATUS_UPDATED)
        withOccurredAt(old.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData(jsonMapper.writeValueAsString(event1ToSave))
      }

      val event2 = domainEventFactory.produceAndPersist {
        withId(event2Id)
        withApplicationId(application2ID)
        withType(DomainEventType.CAS2_APPLICATION_STATUS_UPDATED)
        withOccurredAt(newer.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData(jsonMapper.writeValueAsString(event2ToSave))
      }

      // we don't expect this event to be included as it relates to an update
      // outside the time range
      domainEventFactory.produceAndPersist {
        withId(event3Id)
        withType(DomainEventType.CAS2_APPLICATION_STATUS_UPDATED)
        withOccurredAt(tooOld.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData(jsonMapper.writeValueAsString(event3ToSave))
      }

      val expectedDataFrame = listOf(
        ApplicationStatusUpdatesReportRow(
          eventId = event2Id.toString(),
          applicationId = event2.applicationId.toString(),
          applicationOrigin = ApplicationOrigin.courtBail.toString(),
          personCrn = event2Details.personReference.crn.toString(),
          personNoms = event2Details.personReference.noms,
          newStatus = event2Details.newStatus.name,
          updatedAt = event2Details.updatedAt.toString().split(".").first(),
          updatedBy = event2Details.updatedBy.username,
          statusDetails = "",
        ),
        ApplicationStatusUpdatesReportRow(
          eventId = event1Id.toString(),
          applicationId = event1.applicationId.toString(),
          applicationOrigin = ApplicationOrigin.homeDetentionCurfew.toString(),
          personCrn = event1Details.personReference.crn.toString(),
          personNoms = event1Details.personReference.noms,
          newStatus = event1Details.newStatus.name,
          updatedAt = event1Details.updatedAt.toString().split(".").first(),
          updatedBy = event1Details.updatedBy.username,
          statusDetails = "hdcAndCpp|personalInformation|riskOfSeriousHarm",
        ),
      )
        .toDataFrame()

      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_PRISON", "ROLE_CAS2_MI"),
      )

      webTestClient.get()
        .uri("/cas2v2/reports/application-status-updates")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readExcel(it.responseBody!!.inputStream())
            .convertTo<ApplicationStatusUpdatesReportRow>(ExcessiveColumns.Remove)

          assertThat(actual).isEqualTo(expectedDataFrame)
        }
    }
  }

  @Nested
  inner class UnSubmittedApplicationReport {

    @Test
    fun `streams cas2v2 spreadsheet of data from un-submitted CAS2 applications, newest first`() {
      val old = Instant.now().minusDays(365)
      val newer = Instant.now().minusDays(100)
      val tooOld = Instant.now().minusDays(366)

      val applicableApplication = givenAnUnsubmittedCas2Application(createdAt = newer.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))

      // HDC application, which should not feature in report
      givenAnUnsubmittedCas2HdcApplication(createdAt = old.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))

      // outside time limit -- should not feature in report
      givenAnUnsubmittedCas2Application(createdAt = tooOld.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))

      // submitted application, which should not feature in report
      givenASubmittedCas2Application(createdAt = Instant.now().atOffset(ZoneOffset.ofHoursMinutes(0, 0)).minusDays(51))

      val expectedDataFrame = listOf(
        UnsubmittedApplicationsReportRow(
          applicationId = applicableApplication.id.toString(),
          personCrn = applicableApplication.crn,
          applicationOrigin = applicableApplication.applicationOrigin,
          personNoms = applicableApplication.nomsNumber.toString(),
          startedAt = applicableApplication.createdAt.toString().split(".").first(),
          startedBy = applicableApplication.createdByUser.username,
        ),
      )
        .toDataFrame()

      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_PRISON", "ROLE_CAS2_MI"),
      )

      webTestClient.get()
        .uri("/cas2v2/reports/unsubmitted-applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readExcel(it.responseBody!!.inputStream())
            .convertTo<UnsubmittedApplicationsReportRow>(ExcessiveColumns.Remove)

          assertThat(actual).isEqualTo(expectedDataFrame)
        }
    }
  }
}
