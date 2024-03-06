package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import org.assertj.core.api.Assertions
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas2.Cas2ApplicationStatusUpdatedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas2.Cas2ApplicationSubmittedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas2.Cas2StatusFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.Cas2ExampleMetricsRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2.ApplicationStatusUpdatesReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2.SubmittedApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2.UnsubmittedApplicationsReportRow
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class Cas2ReportsTest : IntegrationTestBase() {

  @Nested
  inner class ControlsOnExternalUsers {
    @Test
    fun `downloading report is forbidden to external users without MI role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "auth",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.get()
        .uri("/cas2/reports/example-report")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `downloading report is permitted to external users with MI role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "auth",
        roles = listOf("ROLE_CAS2_ASSESSOR", "ROLE_CAS2_MI"),
      )

      webTestClient.get()
        .uri("/cas2/reports/example-report")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
    }
  }

  @Nested
  inner class MissingJwt {
    @Test
    fun `Downloading report without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2/reports/example-report")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class ControlsOnInternalUsers {
    @Test
    fun `downloading report is forbidden to NOMIS users without MI role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_POM"),
      )

      webTestClient.get()
        .uri("/cas2/reports/example-report")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `downloading report is permitted to NOMIS users with MI role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_POM", "ROLE_CAS2_MI"),
      )

      webTestClient.get()
        .uri("/cas2/reports/example-report")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
    }
  }

  @Nested
  inner class SubmittedApplications {
    @Test
    fun `streams spreadsheet of Cas2SubmittedApplicationEvents, last 12 months only`() {
      val event1Id = UUID.randomUUID()
      val event2Id = UUID.randomUUID()
      val event3Id = UUID.randomUUID()

      val oldSubmitted = Instant.now().minusSeconds(daysInSeconds(365))
      val oldCreated = oldSubmitted.minusSeconds(daysInSeconds(7))

      val newerSubmitted = Instant.now().minusSeconds(daysInSeconds(100))
      val newerCreated = newerSubmitted.minusSeconds(daysInSeconds(7))

      val tooOldSubmitted = Instant.now().minusSeconds(daysInSeconds(366))
      val tooOldCreated = tooOldSubmitted.minusSeconds(daysInSeconds(7))

      val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
        withAddedAt(OffsetDateTime.now())
        withId(UUID.randomUUID())
      }

      val user1 = nomisUserEntityFactory.produceAndPersist {
        withNomisUsername("NOMIS_USER_1")
      }

      val user2 = nomisUserEntityFactory.produceAndPersist {
        withNomisUsername("NOMIS_USER_2")
      }

      val applicationId1 = UUID.randomUUID()
      val applicationId2 = UUID.randomUUID()
      val applicationId3 = UUID.randomUUID()

      val application1 = cas2ApplicationEntityFactory.produceAndPersist {
        withId(applicationId1)
        withApplicationSchema(applicationSchema)
        withCreatedByUser(user1)
        withCrn("CRN_1")
        withNomsNumber("NOMS_1")
        withCreatedAt(oldCreated.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData("{}")
        withSubmittedAt(oldSubmitted.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
      }

      val application2 = cas2ApplicationEntityFactory.produceAndPersist {
        withId(applicationId2)
        withApplicationSchema(applicationSchema)
        withCreatedByUser(user2)
        withCrn("CRN_2")
        withNomsNumber("NOMS_2")
        withCreatedAt(newerCreated.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData("{}")
        withSubmittedAt(newerSubmitted.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
      }

      // outside time limit -- should not feature in report
      cas2ApplicationEntityFactory.produceAndPersist {
        withId(applicationId3)
        withApplicationSchema(applicationSchema)
        withCreatedByUser(user2)
        withCreatedAt(tooOldCreated.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData("{}")
        withSubmittedAt(tooOldSubmitted.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
      }

      val event1Details = Cas2ApplicationSubmittedEventDetailsFactory()
        .withSubmittedAt(oldSubmitted)
        .produce()
      val event2Details = Cas2ApplicationSubmittedEventDetailsFactory()
        .withSubmittedAt(newerSubmitted)
        .produce()
      val event3Details = Cas2ApplicationSubmittedEventDetailsFactory()
        .withSubmittedAt(tooOldSubmitted)
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
        withOccurredAt(oldSubmitted.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withApplicationId(applicationId1)
      }

      val event2 = domainEventFactory.produceAndPersist {
        withId(event2Id)
        withType(DomainEventType.CAS2_APPLICATION_SUBMITTED)
        withData(objectMapper.writeValueAsString(event2ToSave))
        withOccurredAt(newerSubmitted.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withApplicationId(applicationId2)
      }

      // we don't expect this event to be included as it relates to an application
      // outside the time range
      domainEventFactory.produceAndPersist {
        withId(event3Id)
        withType(DomainEventType.CAS2_APPLICATION_SUBMITTED)
        withData(objectMapper.writeValueAsString(event3ToSave))
        withOccurredAt(tooOldSubmitted.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
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
          submittedAt = event2Details.submittedAt.toString().split(".").first(),
          submittedBy = event2Details.submittedBy.staffMember.username.toString(),
          startedAt = application2.createdAt.toString().split(".").first(),
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
          submittedAt = event1Details.submittedAt.toString().split(".").first(),
          submittedBy = event1Details.submittedBy.staffMember.username.toString(),
          startedAt = application1.createdAt.toString().split(".").first(),
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
        ),
        ApplicationStatusUpdatesReportRow(
          eventId = event1Id.toString(),
          applicationId = event1.applicationId.toString(),
          personCrn = event1Details.personReference.crn.toString(),
          personNoms = event1Details.personReference.noms,
          newStatus = event1Details.newStatus.name,
          updatedAt = event1Details.updatedAt.toString().split(".").first(),
          updatedBy = event1Details.updatedBy.username,
          statusDetails = "personalInformation|riskOfSeriousHarm|hdcAndCpp",
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

      val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
        withAddedAt(OffsetDateTime.now())
        withId(UUID.randomUUID())
      }

      val user1 = nomisUserEntityFactory.produceAndPersist {
        withNomisUsername("NOMIS_USER_1")
      }

      val user2 = nomisUserEntityFactory.produceAndPersist {
        withNomisUsername("NOMIS_USER_2")
      }

      val application1 = cas2ApplicationEntityFactory.produceAndPersist {
        withApplicationSchema(applicationSchema)
        withCreatedByUser(user1)
        withCrn("CRN_1")
        withNomsNumber("NOMS_1")
        withCreatedAt(old.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData("{}")
        withSubmittedAt(null)
      }

      val application2 = cas2ApplicationEntityFactory.produceAndPersist {
        withApplicationSchema(applicationSchema)
        withCreatedByUser(user2)
        withCrn("CRN_2")
        withNomsNumber("NOMS_2")
        withCreatedAt(newer.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData("{}")
        withSubmittedAt(null)
      }

      // outside time limit -- should not feature in report
      cas2ApplicationEntityFactory.produceAndPersist {
        withApplicationSchema(applicationSchema)
        withCreatedByUser(user2)
        withCreatedAt(tooOld.atOffset(ZoneOffset.ofHoursMinutes(0, 0)))
        withData("{}")
        withSubmittedAt(null)
      }

      // submitted application, which should not feature in report
      cas2ApplicationEntityFactory.produceAndPersist {
        withApplicationSchema(applicationSchema)
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
        ),
        UnsubmittedApplicationsReportRow(
          applicationId = application1.id.toString(),
          personCrn = application1.crn,
          personNoms = application1.nomsNumber.toString(),
          startedAt = application1.createdAt.toString().split(".").first(),
          startedBy = application1.createdByUser.nomisUsername,
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

  @Test
  fun `downloaded report is streamed as a spreadsheet`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
      roles = listOf("ROLE_CAS2_MI"),
    )

    val expectedDataFrame = listOf(Cas2ExampleMetricsRow(id = "123", data = "example"))
      .toDataFrame()

    webTestClient.get()
      .uri("/cas2/reports/example-report")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .consumeWith {
        val actual = DataFrame
          .readExcel(it.responseBody!!.inputStream())
          .convertTo<Cas2ExampleMetricsRow>(ExcessiveColumns.Remove)

        Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
      }
  }

  private fun daysInSeconds(days: Int): Long {
    return days.toLong() * 60 * 60 * 24
  }
}
