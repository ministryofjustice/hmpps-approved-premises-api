package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.givens.givenASubmittedCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.givens.givenASubmittedCas2HdcApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.givens.givenAnUnsubmittedCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.givens.givenAnUnsubmittedCas2HdcApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ReportsService.ApplicationStatusUpdatesReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ReportsService.SubmittedApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ReportsService.UnsubmittedApplicationsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.events.Cas2ApplicationStatusUpdatedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.events.Cas2ApplicationSubmittedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.events.Cas2StatusFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.minusDays
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class Cas2v2ReportsTest : IntegrationTestBase() {

  @Nested
  inner class ControlsOnExternalUsers {
    @ParameterizedTest
    @EnumSource(value = Cas2HdcReportName::class)
    fun `downloading cas2v2 report is forbidden to external users without MI role`(reportName: Cas2HdcReportName) {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "auth",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.get()
        .uri("/cas2/reports/${reportName.value}")
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
        .uri("/cas2/reports/$reportName")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class ControlsOnInternalUsers {
    @ParameterizedTest
    @EnumSource(value = Cas2HdcReportName::class)
    fun `downloading cas2v2 report is forbidden to NOMIS users without MI role`(reportName: Cas2HdcReportName) {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_POM"),
      )

      webTestClient.get()
        .uri("/cas2/reports/${reportName.value}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

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

      val hdcApplication = givenASubmittedCas2HdcApplication()
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
          cohort = "Court Bail",
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
          cohort = "Court Bail",
        ),
      )
        .toDataFrame()

      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_MI"),
      )

      webTestClient.get()
        .uri("/cas2/reports/submitted-applications")
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
    fun `origin and cohort are correctly populated`() {
      var submitted = OffsetDateTime.now()

      fun createSubmittedApplication(
        origin: ApplicationOrigin,
        cohort: Cas2Cohort,
      ) {
        createApplicationSubmittedDomainEvent(
          application = givenASubmittedCas2Application(
            applicationOrigin = origin,
            cohort = cohort,
          ),
          occurredAt = submitted,
        )
        submitted -= Duration.ofSeconds(10)
      }

      createSubmittedApplication(ApplicationOrigin.prisonBail, Cas2Cohort.PRISON_BAIL)
      createSubmittedApplication(ApplicationOrigin.courtBail, Cas2Cohort.COURT_BAIL)
      createSubmittedApplication(ApplicationOrigin.other, Cas2Cohort.ATCR)
      createSubmittedApplication(ApplicationOrigin.other, Cas2Cohort.HCRD)
      createSubmittedApplication(ApplicationOrigin.other, Cas2Cohort.HEFR)
      createSubmittedApplication(ApplicationOrigin.other, Cas2Cohort.ISC)
      createSubmittedApplication(ApplicationOrigin.other, Cas2Cohort.RARR)
      createSubmittedApplication(ApplicationOrigin.other, Cas2Cohort.FROM_AP)

      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_MI"),
      )

      webTestClient.get()
        .uri("/cas2/reports/submitted-applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        .expectBody()
        .consumeWith {
          val dataFrame = DataFrame
            .readExcel(it.responseBody!!.inputStream())

          assertThat(dataFrame[0]["applicationOrigin"]).isEqualTo("prisonBail")
          assertThat(dataFrame[0]["cohort"]).isEqualTo("Prison Bail")

          assertThat(dataFrame[1]["applicationOrigin"]).isEqualTo("courtBail")
          assertThat(dataFrame[1]["cohort"]).isEqualTo("Court Bail")

          assertThat(dataFrame[2]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[2]["cohort"]).isEqualTo("Alternative to custodial recall")

          assertThat(dataFrame[3]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[3]["cohort"]).isEqualTo("Homeless at conditional release date")

          assertThat(dataFrame[4]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[4]["cohort"]).isEqualTo("Homeless at end of fixed-term recall")

          assertThat(dataFrame[5]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[5]["cohort"]).isEqualTo("Intensive supervision courts")

          assertThat(dataFrame[6]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[6]["cohort"]).isEqualTo("Risk Assessed Recall Review")

          assertThat(dataFrame[7]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[7]["cohort"]).isEqualTo("Referral from Approved Premises")
        }
    }
  }

  @Nested
  inner class ApplicationStatusUpdateReport {

    @Test
    fun `provide last 12 months only, excluding hdc`() {
      val old = Instant.now().minusDays(365)
      val newer = Instant.now().minusDays(100)
      val tooOld = Instant.now().minusDays(366)

      val application1 = givenASubmittedCas2Application(
        applicationOrigin = ApplicationOrigin.prisonBail,
      )
      val event1Status = Cas2StatusFactory()
        .withStatusDetails(
          listOf(
            Cas2StatusDetail("personalInformation", "Personal information"),
            Cas2StatusDetail("riskOfSeriousHarm", "Risk of serious harm"),
            Cas2StatusDetail("hdcAndCpp", "HDC licence and CPP details"),
          ),
        )
        .produce()
      val event1Data = Cas2ApplicationStatusUpdatedEvent(
        id = UUID.randomUUID(),
        timestamp = Instant.now(),
        eventType = EventType.applicationStatusUpdated,
        eventDetails = Cas2ApplicationStatusUpdatedEventDetailsFactory()
          .withStatus(event1Status)
          .withUpdatedAt(old)
          .produce(),
      )
      val event1 = domainEventFactory.produceAndPersist {
        withId(event1Data.id)
        withApplicationId(application1.id)
        withType(DomainEventType.CAS2_APPLICATION_STATUS_UPDATED)
        withOccurredAt(old.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData(jsonMapper.writeValueAsString(event1Data))
      }

      val application2 = givenASubmittedCas2Application(
        applicationOrigin = ApplicationOrigin.courtBail,
      )
      val event2Data = Cas2ApplicationStatusUpdatedEvent(
        id = UUID.randomUUID(),
        timestamp = Instant.now(),
        eventType = EventType.applicationStatusUpdated,
        eventDetails = Cas2ApplicationStatusUpdatedEventDetailsFactory()
          .withStatus(
            Cas2StatusFactory()
              .withStatusDetails(emptyList())
              .produce(),
          )
          .withUpdatedAt(newer)
          .produce(),
      )
      val event2 = domainEventFactory.produceAndPersist {
        withId(event2Data.id)
        withApplicationId(application2.id)
        withType(DomainEventType.CAS2_APPLICATION_STATUS_UPDATED)
        withOccurredAt(newer.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData(jsonMapper.writeValueAsString(event2Data))
      }

      // we don't expect this event to be included as it relates to an update
      // outside the time range
      val application3UpdateOutsideOfTimeRange = givenASubmittedCas2Application(
        applicationOrigin = ApplicationOrigin.courtBail,
      )
      val event3Data = Cas2ApplicationStatusUpdatedEvent(
        id = UUID.randomUUID(),
        timestamp = Instant.now(),
        eventType = EventType.applicationStatusUpdated,
        eventDetails = Cas2ApplicationStatusUpdatedEventDetailsFactory()
          .withUpdatedAt(tooOld)
          .produce(),
      )
      domainEventFactory.produceAndPersist {
        withId(event3Data.id)
        withApplicationId(application3UpdateOutsideOfTimeRange.id)
        withType(DomainEventType.CAS2_APPLICATION_STATUS_UPDATED)
        withOccurredAt(tooOld.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData(jsonMapper.writeValueAsString(event3Data))
      }

      val hdcApplication = givenASubmittedCas2HdcApplication(applicationOrigin = ApplicationOrigin.courtBail)
      val event4Data = Cas2ApplicationStatusUpdatedEvent(
        id = UUID.randomUUID(),
        timestamp = Instant.now(),
        eventType = EventType.applicationStatusUpdated,
        eventDetails = Cas2ApplicationStatusUpdatedEventDetailsFactory()
          .withStatus(
            Cas2StatusFactory()
              .withStatusDetails(emptyList())
              .produce(),
          )
          .withUpdatedAt(newer)
          .produce(),
      )
      domainEventFactory.produceAndPersist {
        withId(event4Data.id)
        withApplicationId(hdcApplication.id)
        withType(DomainEventType.CAS2_APPLICATION_STATUS_UPDATED)
        withOccurredAt(newer.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData(jsonMapper.writeValueAsString(event2Data))
      }

      val expectedDataFrame = listOf(
        ApplicationStatusUpdatesReportRow(
          eventId = event2.id.toString(),
          applicationId = event2.applicationId.toString(),
          applicationOrigin = ApplicationOrigin.courtBail.toString(),
          cohort = "Court Bail",
          personCrn = event2Data.eventDetails.personReference.crn.toString(),
          personNoms = event2Data.eventDetails.personReference.noms,
          newStatus = event2Data.eventDetails.newStatus.name,
          updatedAt = event2Data.eventDetails.updatedAt.toString().split(".").first(),
          updatedBy = event2Data.eventDetails.updatedBy.username,
          statusDetails = "",
        ),
        ApplicationStatusUpdatesReportRow(
          eventId = event1.id.toString(),
          applicationId = event1.applicationId.toString(),
          applicationOrigin = ApplicationOrigin.prisonBail.toString(),
          cohort = "Court Bail",
          personCrn = event1Data.eventDetails.personReference.crn.toString(),
          personNoms = event1Data.eventDetails.personReference.noms,
          newStatus = event1Data.eventDetails.newStatus.name,
          updatedAt = event1Data.eventDetails.updatedAt.toString().split(".").first(),
          updatedBy = event1Data.eventDetails.updatedBy.username,
          statusDetails = "hdcAndCpp|personalInformation|riskOfSeriousHarm",
        ),
      )
        .toDataFrame()

      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_MI"),
      )

      webTestClient.get()
        .uri("/cas2/reports/application-status-updates")
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

    @Test
    fun `maps cohort correctly`() {
      var occurredAt = OffsetDateTime.now()

      fun createApplicationWithStatusUpdateEvent(
        origin: ApplicationOrigin,
        cohort: Cas2Cohort,
      ) {
        val application1 = givenASubmittedCas2Application(
          applicationOrigin = origin,
          cohort = cohort,
        )

        val eventData = Cas2ApplicationStatusUpdatedEvent(
          id = UUID.randomUUID(),
          timestamp = Instant.now(),
          eventType = EventType.applicationStatusUpdated,
          eventDetails = Cas2ApplicationStatusUpdatedEventDetailsFactory()
            .withStatus(
              Cas2StatusFactory()
                .withStatusDetails(emptyList())
                .produce(),
            )
            .withUpdatedAt(occurredAt.toInstant())
            .produce(),
        )

        domainEventFactory.produceAndPersist {
          withId(eventData.id)
          withApplicationId(application1.id)
          withType(DomainEventType.CAS2_APPLICATION_STATUS_UPDATED)
          withOccurredAt(occurredAt)
          withData(jsonMapper.writeValueAsString(eventData))
        }

        occurredAt -= Duration.ofSeconds(10)
      }

      createApplicationWithStatusUpdateEvent(ApplicationOrigin.prisonBail, Cas2Cohort.PRISON_BAIL)
      createApplicationWithStatusUpdateEvent(ApplicationOrigin.courtBail, Cas2Cohort.COURT_BAIL)
      createApplicationWithStatusUpdateEvent(ApplicationOrigin.other, Cas2Cohort.ATCR)
      createApplicationWithStatusUpdateEvent(ApplicationOrigin.other, Cas2Cohort.HCRD)
      createApplicationWithStatusUpdateEvent(ApplicationOrigin.other, Cas2Cohort.HEFR)
      createApplicationWithStatusUpdateEvent(ApplicationOrigin.other, Cas2Cohort.ISC)
      createApplicationWithStatusUpdateEvent(ApplicationOrigin.other, Cas2Cohort.RARR)
      createApplicationWithStatusUpdateEvent(ApplicationOrigin.other, Cas2Cohort.FROM_AP)

      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_MI"),
      )

      webTestClient.get()
        .uri("/cas2/reports/application-status-updates")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .consumeWith {
          val dataFrame = DataFrame.readExcel(it.responseBody!!.inputStream())

          assertThat(dataFrame[0]["applicationOrigin"]).isEqualTo("prisonBail")
          assertThat(dataFrame[0]["cohort"]).isEqualTo("Prison Bail")

          assertThat(dataFrame[1]["applicationOrigin"]).isEqualTo("courtBail")
          assertThat(dataFrame[1]["cohort"]).isEqualTo("Court Bail")

          assertThat(dataFrame[2]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[2]["cohort"]).isEqualTo("Alternative to custodial recall")

          assertThat(dataFrame[3]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[3]["cohort"]).isEqualTo("Homeless at conditional release date")

          assertThat(dataFrame[4]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[4]["cohort"]).isEqualTo("Homeless at end of fixed-term recall")

          assertThat(dataFrame[5]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[5]["cohort"]).isEqualTo("Intensive supervision courts")

          assertThat(dataFrame[6]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[6]["cohort"]).isEqualTo("Risk Assessed Recall Review")

          assertThat(dataFrame[7]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[7]["cohort"]).isEqualTo("Referral from Approved Premises")
        }
    }
  }

  @Nested
  inner class UnSubmittedApplicationReport {

    @Test
    fun `only includes submissions from last 12 months, excluding HDC`() {
      val old = Instant.now().minusDays(365)
      val newer = Instant.now().minusDays(100)
      val tooOld = Instant.now().minusDays(366)

      val applicableApplicationNomsNotNull = givenAnUnsubmittedCas2Application(createdAt = newer.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
      val applicableApplicationNomsNull = givenAnUnsubmittedCas2Application(createdAt = newer.plusSeconds(1).atOffset(ZoneOffset.ofHoursMinutes(0, 0)), noms = null)

      // HDC application, which should not feature in report
      givenAnUnsubmittedCas2HdcApplication(createdAt = old.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))

      // outside time limit -- should not feature in report
      givenAnUnsubmittedCas2Application(createdAt = tooOld.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))

      // submitted application, which should not feature in report
      givenASubmittedCas2Application(createdAt = Instant.now().atOffset(ZoneOffset.ofHoursMinutes(0, 0)).minusDays(51))

      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_MI"),
      )

      val responseBody = webTestClient.get()
        .uri("/cas2/reports/unsubmitted-applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      val actual = DataFrame
        .readExcel(responseBody.inputStream())
        .convertTo<UnsubmittedApplicationsReportRow>(ExcessiveColumns.Remove)

      assertThat(actual).isEqualTo(
        listOf(
          UnsubmittedApplicationsReportRow(
            applicationId = applicableApplicationNomsNull.id.toString(),
            personCrn = applicableApplicationNomsNull.crn,
            applicationOrigin = applicableApplicationNomsNull.applicationOrigin,
            personNoms = null,
            startedAt = applicableApplicationNomsNull.createdAt.toString().split(".").first(),
            startedBy = applicableApplicationNomsNull.createdByUser.username,
            cohort = "Prison Bail",
          ),
          UnsubmittedApplicationsReportRow(
            applicationId = applicableApplicationNomsNotNull.id.toString(),
            personCrn = applicableApplicationNomsNotNull.crn,
            applicationOrigin = applicableApplicationNomsNotNull.applicationOrigin,
            personNoms = applicableApplicationNomsNotNull.nomsNumber,
            startedAt = applicableApplicationNomsNotNull.createdAt.toString().split(".").first(),
            startedBy = applicableApplicationNomsNotNull.createdByUser.username,
            cohort = "Prison Bail",
          ),
        )
          .toDataFrame(),
      )
    }

    @Test
    fun `maps cohort correctly`() {
      var createdAt = OffsetDateTime.now()

      fun createUnsubmittedApplication(
        origin: ApplicationOrigin,
        cohort: Cas2Cohort,
      ) {
        givenAnUnsubmittedCas2Application(
          applicationOrigin = origin,
          cohort = cohort,
          createdAt = createdAt,
        )

        createdAt -= Duration.ofSeconds(10)
      }

      createUnsubmittedApplication(ApplicationOrigin.prisonBail, Cas2Cohort.PRISON_BAIL)
      createUnsubmittedApplication(ApplicationOrigin.courtBail, Cas2Cohort.COURT_BAIL)
      createUnsubmittedApplication(ApplicationOrigin.other, Cas2Cohort.ATCR)
      createUnsubmittedApplication(ApplicationOrigin.other, Cas2Cohort.HCRD)
      createUnsubmittedApplication(ApplicationOrigin.other, Cas2Cohort.HEFR)
      createUnsubmittedApplication(ApplicationOrigin.other, Cas2Cohort.ISC)
      createUnsubmittedApplication(ApplicationOrigin.other, Cas2Cohort.RARR)
      createUnsubmittedApplication(ApplicationOrigin.other, Cas2Cohort.FROM_AP)

      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_MI"),
      )

      webTestClient.get()
        .uri("/cas2/reports/unsubmitted-applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .consumeWith {
          val dataFrame = DataFrame
            .readExcel(it.responseBody!!.inputStream())

          assertThat(dataFrame[0]["applicationOrigin"]).isEqualTo("prisonBail")
          assertThat(dataFrame[0]["cohort"]).isEqualTo("Prison Bail")

          assertThat(dataFrame[1]["applicationOrigin"]).isEqualTo("courtBail")
          assertThat(dataFrame[1]["cohort"]).isEqualTo("Court Bail")

          assertThat(dataFrame[2]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[2]["cohort"]).isEqualTo("Alternative to custodial recall")

          assertThat(dataFrame[3]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[3]["cohort"]).isEqualTo("Homeless at conditional release date")

          assertThat(dataFrame[4]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[4]["cohort"]).isEqualTo("Homeless at end of fixed-term recall")

          assertThat(dataFrame[5]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[5]["cohort"]).isEqualTo("Intensive supervision courts")

          assertThat(dataFrame[6]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[6]["cohort"]).isEqualTo("Risk Assessed Recall Review")

          assertThat(dataFrame[7]["applicationOrigin"]).isEqualTo("other")
          assertThat(dataFrame[7]["cohort"]).isEqualTo("Referral from Approved Premises")
        }
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
}
