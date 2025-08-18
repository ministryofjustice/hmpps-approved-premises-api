package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import com.ninjasquad.springmockk.SpykBean
import org.assertj.core.api.Assertions
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.events.Cas2ApplicationStatusUpdatedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.events.Cas2ApplicationSubmittedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.events.Cas2StatusFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.ApplicationStatusUpdatesReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.SubmittedApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.UnsubmittedApplicationsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class Cas2ReportsTest : IntegrationTestBase() {

  @SpykBean
  private lateinit var applicationAssignmentRepository: Cas2ApplicationAssignmentRepository

  @Nested
  inner class ControlsOnExternalUsers {
    @ParameterizedTest
    @EnumSource(value = Cas2ReportName::class)
    fun `downloading report is forbidden to external users without MI role`(reportName: Cas2ReportName) {
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
    fun `Downloading report without JWT returns 401`(reportName: String) {
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
    @EnumSource(value = Cas2ReportName::class)
    fun `downloading report is forbidden to NOMIS users without MI role`(reportName: Cas2ReportName) {
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
  inner class SubmittedApplications {
    @Test
    fun `streams spreadsheet of Cas2SubmittedApplicationEvents, last 12 months only`() {
      val event1Id = UUID.randomUUID()
      val event2Id = UUID.randomUUID()
      val event3Id = UUID.randomUUID()

      val oldSubmitted = OffsetDateTime.now().minusDays(365)
      val oldCreated = oldSubmitted.minusDays(7)

      val newerSubmitted = OffsetDateTime.now().minusDays(100)
      val newerCreated = newerSubmitted.minusDays(7)

      val tooOldSubmitted = OffsetDateTime.now().minusDays(366)
      val tooOldCreated = tooOldSubmitted.minusSeconds(daysInSeconds(7))

      val user1 = nomisUserEntityFactory.produceAndPersist {
        withNomisUsername("NOMIS_USER_1")
      }

      val user2 = nomisUserEntityFactory.produceAndPersist {
        withNomisUsername("NOMIS_USER_2")
      }

      val user3 = nomisUserEntityFactory.produceAndPersist {
        withNomisUsername("NOMIS_USER_3")
      }

      val applicationId1 = UUID.randomUUID()
      val applicationId2 = UUID.randomUUID()
      val applicationId3 = UUID.randomUUID()

      val application1 = cas2ApplicationEntityFactory.produceAndPersist {
        withId(applicationId1)
        withCreatedByUser(user1)
        withCrn("CRN_1")
        withNomsNumber("NOMS_1")
        withCreatedAt(oldCreated)
        withData("{}")
        withSubmittedAt(oldSubmitted)
        withReferringPrisonCode("NEW")
        withApplicationOrigin(ApplicationOrigin.prisonBail)
      }

      val application2 = cas2ApplicationEntityFactory.produceAndPersist {
        withId(applicationId2)
        withCreatedByUser(user2)
        withCrn("CRN_2")
        withNomsNumber("NOMS_2")
        withCreatedAt(newerCreated)
        withData("{}")
        withSubmittedAt(newerSubmitted)
        withApplicationOrigin(ApplicationOrigin.courtBail)
      }

      val application1Assignment1 = Cas2ApplicationAssignmentEntity(
        id = UUID.randomUUID(),
        application = application1,
        prisonCode = "LON",
        allocatedPomUser = null,
        createdAt = OffsetDateTime.now().minusHours(1),
      )
      val application1Assignment2 = Cas2ApplicationAssignmentEntity(
        id = UUID.randomUUID(),
        application = application1,
        prisonCode = "LON",
        allocatedPomUser = user3,
        createdAt = OffsetDateTime.now(),
      )
      val application1Assignment3 = Cas2ApplicationAssignmentEntity(
        id = UUID.randomUUID(),
        application = application1,
        prisonCode = "NEW",
        allocatedPomUser = null,
        createdAt = OffsetDateTime.now().minusHours(3),
      )
      val application1Assignment4 = Cas2ApplicationAssignmentEntity(
        id = UUID.randomUUID(),
        application = application1,
        prisonCode = "NEW",
        allocatedPomUser = user1,
        createdAt = OffsetDateTime.now().minusHours(4),
      )

      // outside time limit -- should not feature in report
      cas2ApplicationEntityFactory.produceAndPersist {
        withId(applicationId3)
        withCreatedByUser(user2)
        withCreatedAt(tooOldCreated)
        withData("{}")
        withSubmittedAt(tooOldSubmitted)
      }

      applicationAssignmentRepository.save(application1Assignment1)
      applicationAssignmentRepository.save(application1Assignment2)
      applicationAssignmentRepository.save(application1Assignment3)
      applicationAssignmentRepository.save(application1Assignment4)

      val event1Details = Cas2ApplicationSubmittedEventDetailsFactory()
        .withSubmittedAt(oldSubmitted.toInstant())
        .produce()
      val event2Details = Cas2ApplicationSubmittedEventDetailsFactory()
        .withSubmittedAt(newerSubmitted.toInstant())
        .produce()
      val event3Details = Cas2ApplicationSubmittedEventDetailsFactory()
        .withSubmittedAt(tooOldSubmitted.toInstant())
        .produce()

      val event1ToSave = Cas2ApplicationSubmittedEvent(
        id = event1Id,
        timestamp = Instant.now(),
        eventType = EventType.applicationSubmitted,
        eventDetails = event1Details,
      )

      val event2ToSave = Cas2ApplicationSubmittedEvent(
        id = event2Id,
        timestamp = Instant.now(),
        eventType = EventType.applicationSubmitted,
        eventDetails = event2Details,
      )

      val event3ToSave = Cas2ApplicationSubmittedEvent(
        id = event3Id,
        timestamp = Instant.now(),
        eventType = EventType.applicationSubmitted,
        eventDetails = event3Details,
      )

      val event1 = domainEventFactory.produceAndPersist {
        withId(event1Id)
        withType(DomainEventType.CAS2_APPLICATION_SUBMITTED)
        withData(objectMapper.writeValueAsString(event1ToSave))
        withOccurredAt(oldSubmitted)
        withApplicationId(applicationId1)
      }

      val event2 = domainEventFactory.produceAndPersist {
        withId(event2Id)
        withType(DomainEventType.CAS2_APPLICATION_SUBMITTED)
        withData(objectMapper.writeValueAsString(event2ToSave))
        withOccurredAt(newerSubmitted)
        withApplicationId(applicationId2)
      }

      // we don't expect this event to be included as it relates to an application
      // outside the time range
      domainEventFactory.produceAndPersist {
        withId(event3Id)
        withType(DomainEventType.CAS2_APPLICATION_SUBMITTED)
        withData(objectMapper.writeValueAsString(event3ToSave))
        withOccurredAt(tooOldSubmitted)
        withApplicationId(applicationId3)
      }

      val expectedDataFrame = listOf(
        SubmittedApplicationReportRow(
          eventId = event2Id.toString(),
          applicationId = event2.applicationId.toString(),
          personCrn = event2Details.personReference.crn.toString(),
          personNoms = event2Details.personReference.noms,
          referringPrisonCode = event2Details.referringPrisonCode.toString(),
          preferredAreas = event2Details.preferredAreas.toString(),
          hdcEligibilityDate = event2Details.hdcEligibilityDate.toString(),
          conditionalReleaseDate = event2Details.conditionalReleaseDate.toString(),
          submittedAt = event2.occurredAt.toString().split(".").first(),
          submittedBy = event2Details.submittedBy.staffMember.username.toString(),
          startedAt = application2.createdAt.toString().split(".").first(),
          numberOfLocationTransfers = "0",
          numberOfPomTransfers = "0",
          applicationOrigin = application2.applicationOrigin,
          bailHearingDate = null,
        ),
        SubmittedApplicationReportRow(
          eventId = event1Id.toString(),
          applicationId = event1.applicationId.toString(),
          personCrn = event1Details.personReference.crn.toString(),
          personNoms = event1Details.personReference.noms,
          referringPrisonCode = event1Details.referringPrisonCode.toString(),
          preferredAreas = event1Details.preferredAreas.toString(),
          hdcEligibilityDate = event1Details.hdcEligibilityDate.toString(),
          conditionalReleaseDate = event1Details.conditionalReleaseDate.toString(),
          submittedAt = event1.occurredAt.toString().split(".").first(),
          submittedBy = event1Details.submittedBy.staffMember.username.toString(),
          startedAt = application1.createdAt.toString().split(".").first(),
          numberOfLocationTransfers = "2",
          numberOfPomTransfers = "1",
          applicationOrigin = application1.applicationOrigin,
          bailHearingDate = null,
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

          Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
        }
    }
  }

  @Nested
  inner class ApplicationStatusUpdates {
    @Test
    fun `streams spreadsheet of Cas2ApplicationStatusUpdatedEvents, last 12 months only`() {
      val event1Id = UUID.randomUUID()
      val event2Id = UUID.randomUUID()
      val event3Id = UUID.randomUUID()

      val old = Instant.now().minusSeconds(daysInSeconds(365))
      val newer = Instant.now().minusSeconds(daysInSeconds(100))
      val tooOld = Instant.now().minusSeconds(daysInSeconds(366))

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
        withType(DomainEventType.CAS2_APPLICATION_STATUS_UPDATED)
        withOccurredAt(old.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData(objectMapper.writeValueAsString(event1ToSave))
      }

      val event2 = domainEventFactory.produceAndPersist {
        withId(event2Id)
        withType(DomainEventType.CAS2_APPLICATION_STATUS_UPDATED)
        withOccurredAt(newer.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData(objectMapper.writeValueAsString(event2ToSave))
      }

      // we don't expect this event to be included as it relates to an update
      // outside the time range
      domainEventFactory.produceAndPersist {
        withId(event3Id)
        withType(DomainEventType.CAS2_APPLICATION_STATUS_UPDATED)
        withOccurredAt(tooOld.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData(objectMapper.writeValueAsString(event3ToSave))
      }

      val user1 = nomisUserEntityFactory.produceAndPersist {
        withNomisUsername("NOMIS_USER_1")
      }

      val user2 = nomisUserEntityFactory.produceAndPersist {
        withNomisUsername("NOMIS_USER_2")
      }

      // TODO besscerule - added as now the query states that applicationOrigin cannot be null so every test needs to set up app data
      cas2ApplicationEntityFactory.produceAndPersist {
        withId(event2.applicationId!!)
        withCreatedByUser(user2)
        withCrn(event1Details.personReference.crn.toString())
        withNomsNumber(event2Details.personReference.noms)
        withData("{}")
        withReferringPrisonCode("NEW")
      }

      val application1 = cas2ApplicationEntityFactory.produceAndPersist {
        withId(event1.applicationId!!)
        withCreatedByUser(user1)
        withCrn(event1Details.personReference.crn.toString())
        withNomsNumber(event2Details.personReference.noms)
        withData("{}")
        withReferringPrisonCode("NEW")
      }

      val user3 = nomisUserEntityFactory.produceAndPersist {
        withNomisUsername("NOMIS_USER_3")
      }

      val application1Assignment1 = Cas2ApplicationAssignmentEntity(
        id = UUID.randomUUID(),
        application = application1,
        prisonCode = "LON",
        allocatedPomUser = null,
        createdAt = OffsetDateTime.now().minusHours(1),
      )
      val application1Assignment2 = Cas2ApplicationAssignmentEntity(
        id = UUID.randomUUID(),
        application = application1,
        prisonCode = "LON",
        allocatedPomUser = user3,
        createdAt = OffsetDateTime.now(),
      )
      val application1Assignment3 = Cas2ApplicationAssignmentEntity(
        id = UUID.randomUUID(),
        application = application1,
        prisonCode = "NEW",
        allocatedPomUser = null,
        createdAt = OffsetDateTime.now().minusHours(3),
      )
      val application1Assignment4 = Cas2ApplicationAssignmentEntity(
        id = UUID.randomUUID(),
        application = application1,
        prisonCode = "NEW",
        allocatedPomUser = user1,
        createdAt = OffsetDateTime.now().minusHours(4),
      )

      applicationAssignmentRepository.save(application1Assignment1)
      applicationAssignmentRepository.save(application1Assignment2)
      applicationAssignmentRepository.save(application1Assignment3)
      applicationAssignmentRepository.save(application1Assignment4)

      val expectedDataFrame = listOf(
        ApplicationStatusUpdatesReportRow(
          eventId = event2Id.toString(),
          applicationId = event2.applicationId.toString(),
          personCrn = event2Details.personReference.crn.toString(),
          personNoms = event2Details.personReference.noms,
          newStatus = event2Details.newStatus.name,
          updatedAt = event2Details.updatedAt.toString().split(".").first(),
          updatedBy = event2Details.updatedBy.username,
          statusDetails = "",
          numberOfLocationTransfers = "0",
          numberOfPomTransfers = "0",
        ),
        ApplicationStatusUpdatesReportRow(
          eventId = event1Id.toString(),
          applicationId = event1.applicationId.toString(),
          personCrn = event1Details.personReference.crn.toString(),
          personNoms = event1Details.personReference.noms,
          newStatus = event1Details.newStatus.name,
          updatedAt = event1Details.updatedAt.toString().split(".").first(),
          updatedBy = event1Details.updatedBy.username,
          statusDetails = "hdcAndCpp|personalInformation|riskOfSeriousHarm",
          numberOfLocationTransfers = "2",
          numberOfPomTransfers = "1",
        ),
      )
        .toDataFrame()

      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_PRISON", "ROLE_CAS2_MI"),
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

          Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
        }
    }
  }

  @Nested
  inner class UnSubmittedApplications {
    @Test
    fun `streams spreadsheet of data from un-submitted CAS2 applications, newest first`() {
      val old = Instant.now().minusSeconds(daysInSeconds(365))
      val newer = Instant.now().minusSeconds(daysInSeconds(100))
      val tooOld = Instant.now().minusSeconds(daysInSeconds(366))

      val user1 = nomisUserEntityFactory.produceAndPersist {
        withNomisUsername("NOMIS_USER_1")
      }

      val user2 = nomisUserEntityFactory.produceAndPersist {
        withNomisUsername("NOMIS_USER_2")
      }

      val application1 = cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(user1)
        withCrn("CRN_1")
        withNomsNumber("NOMS_1")
        withCreatedAt(old.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData("{}")
        withSubmittedAt(null)
        withApplicationOrigin(ApplicationOrigin.homeDetentionCurfew)
      }

      val application2 = cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(user2)
        withCrn("CRN_2")
        withNomsNumber("NOMS_2")
        withCreatedAt(newer.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData("{}")
        withSubmittedAt(null)
        withApplicationOrigin(ApplicationOrigin.prisonBail)
      }

      // outside time limit -- should not feature in report
      cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(user2)
        withCreatedAt(tooOld.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData("{}")
        withSubmittedAt(null)
      }

      // submitted application, which should not feature in report
      cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(user2)
        withCreatedAt(Instant.now().atOffset(ZoneOffset.ofHoursMinutes(0, 0)).minusDays(51))
        withData("{}")
        withSubmittedAt(Instant.now().atOffset(ZoneOffset.ofHoursMinutes(0, 0)).minusDays(50))
      }

      val expectedDataFrame = listOf(
        UnsubmittedApplicationsReportRow(
          applicationId = application2.id.toString(),
          personCrn = application2.crn,
          personNoms = application2.nomsNumber.toString(),
          startedAt = application2.createdAt.toString().split(".").first(),
          startedBy = application2.createdByUser.nomisUsername,
          applicationOrigin = application2.applicationOrigin,
        ),
        UnsubmittedApplicationsReportRow(
          applicationId = application1.id.toString(),
          personCrn = application1.crn,
          personNoms = application1.nomsNumber.toString(),
          startedAt = application1.createdAt.toString().split(".").first(),
          startedBy = application1.createdByUser.nomisUsername,
          applicationOrigin = application1.applicationOrigin,
        ),
      )
        .toDataFrame()

      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_PRISON", "ROLE_CAS2_MI"),
      )

      webTestClient.get()
        .uri("/cas2/reports/unsubmitted-applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readExcel(it.responseBody!!.inputStream())
            .convertTo<UnsubmittedApplicationsReportRow>(ExcessiveColumns.Remove)

          Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
        }
    }
  }

  private fun daysInSeconds(days: Int): Long = days.toLong() * 60 * 60 * 24
}
