package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import org.assertj.core.api.Assertions
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas2.Cas2ApplicationSubmittedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.Cas2ExampleMetricsRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.cas2.SubmittedApplicationReportRow
import java.time.Instant
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
        roles = listOf("ROLE_PRISON"),
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
        roles = listOf("ROLE_PRISON", "ROLE_CAS2_MI"),
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
    fun `streams spreadsheet of Cas2SubmittedApplicationEvents`() {
      val event1Id = UUID.randomUUID()
      val event2Id = UUID.randomUUID()

      val event1Details = Cas2ApplicationSubmittedEventDetailsFactory()
        .withSubmittedAt(Instant.parse("2023-12-31T10:00:00+01:00"))
        .produce()
      val event2Details = Cas2ApplicationSubmittedEventDetailsFactory()
        .withSubmittedAt(Instant.parse("2024-01-01T10:00:00+01:00"))
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

      val event1 = domainEventFactory.produceAndPersist {
        withId(event1Id)
        withType(DomainEventType.CAS2_APPLICATION_SUBMITTED)
        withData(objectMapper.writeValueAsString(event1ToSave))
      }

      val event2 = domainEventFactory.produceAndPersist {
        withId(event2Id)
        withType(DomainEventType.CAS2_APPLICATION_SUBMITTED)
        withData(objectMapper.writeValueAsString(event2ToSave))
      }

      val expectedDataFrame = listOf(
        SubmittedApplicationReportRow(
          eventId = event2Id.toString(),
          applicationId = event2.applicationId.toString(),
          personCrn = event2Details.personReference.crn.toString(),
          personNoms = event2Details.personReference.noms,
          referringPrisonCode = event2Details.referringPrisonCode.toString(),
          submittedAt = event2Details.submittedAt.toString().split("T").first(),
          submittedBy = event2Details.submittedBy.staffMember.username.toString(),
        ),
        SubmittedApplicationReportRow(
          eventId = event1Id.toString(),
          applicationId = event1.applicationId.toString(),
          personCrn = event1Details.personReference.crn.toString(),
          personNoms = event1Details.personReference.noms,
          referringPrisonCode = event1Details.referringPrisonCode.toString(),
          submittedAt = event1Details.submittedAt.toString().split("T").first(),
          submittedBy = event1Details.submittedBy.staffMember.username.toString(),
        ),
      )
        .toDataFrame()

      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_PRISON", "ROLE_CAS2_MI"),
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

  @Test
  fun `downloaded report is streamed as a spreadsheet`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
      roles = listOf("ROLE_PRISON", "ROLE_CAS2_MI"),
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
}
