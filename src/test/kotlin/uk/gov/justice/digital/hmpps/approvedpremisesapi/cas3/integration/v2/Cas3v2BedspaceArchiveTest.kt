package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.Cas3IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3PremisesWithBedspaces
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ArchiveBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class Cas3v2BedspaceArchiveTest : Cas3IntegrationTestBase() {

  @Nested
  inner class ArchiveBedspace {
    @BeforeEach
    fun setup() {
      clock.setNow(Instant.parse("2025-07-03T10:15:30Z"))
    }

    @Test
    fun `When archive a bedspace returns OK with correct body when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenACas3PremisesWithBedspaces(
          region = user.probationRegion,
          bedspaceCount = 3,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(360),
            LocalDate.now(clock).minusDays(180),
            // upcoming bedspace
            LocalDate.now(clock).plusDays(4),
          ),
          bedspacesEndDates = listOf(
            null,
            null,
            null,
          ),
        ) { premises, bedspaces ->
          val bedspaceOne = bedspaces.first()
          val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now(clock).plusDays(5))

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces/${bedspaceOne.id}/archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(archiveBedspace)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("id").isEqualTo(bedspaceOne.id)
            .jsonPath("endDate").isEqualTo(archiveBedspace.endDate)

          val allEvents = domainEventRepository.findAll()
          assertThat(allEvents).hasSize(1)
          assertThat(allEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_ARCHIVED)
          assertThat(allEvents[0].cas3BedspaceId).isEqualTo(bedspaceOne.id)
          assertThat(allEvents[0].cas3PremisesId).isEqualTo(premises.id)
        }
      }
    }

    @Test
    fun `When archive the last online bedspace returns OK and archives the premises when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenACas3PremisesWithBedspaces(
          region = user.probationRegion,
          bedspaceCount = 2,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(360),
            LocalDate.now(clock).minusDays(180),
          ),
          bedspacesEndDates = listOf(
            null,
            LocalDate.now(clock).minusDays(2),
          ),
        ) { premises, bedspaces ->
          val bedspaceOne = bedspaces.first()
          val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now(clock).plusDays(5))

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces/${bedspaceOne.id}/archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(archiveBedspace)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("id").isEqualTo(bedspaceOne.id)
            .jsonPath("endDate").isEqualTo(archiveBedspace.endDate)

          val updatedPremises = cas3PremisesRepository.findByIdOrNull(premises.id)
          assertThat(updatedPremises).isNotNull()
          assertThat(updatedPremises?.endDate).isEqualTo(archiveBedspace.endDate)
          assertThat(updatedPremises?.status).isEqualTo(Cas3PremisesStatus.archived)

          val updatedBedspace = cas3BedspacesRepository.findByIdOrNull(bedspaceOne.id)
          assertThat(updatedBedspace).isNotNull()
          assertThat(updatedBedspace?.endDate).isEqualTo(updatedBedspace?.endDate)

          val allEvents = domainEventRepository.findAll()
          assertThat(allEvents).hasSize(2)
          assertThat(allEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_ARCHIVED)
          assertThat(allEvents[0].cas3BedspaceId).isEqualTo(bedspaceOne.id)
          assertThat(allEvents[0].cas3PremisesId).isEqualTo(premises.id)
          assertThat(allEvents[1].type).isEqualTo(DomainEventType.CAS3_PREMISES_ARCHIVED)
          assertThat(allEvents[1].cas3PremisesId).isEqualTo(premises.id)
        }
      }
    }

    @Test
    fun `When archive the last online bedspace returns OK and archives the premises with the latest bedspae end date when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val latestBedspaceArchiveDate = LocalDate.now(clock).plusDays(35)
        givenACas3PremisesWithBedspaces(
          region = user.probationRegion,
          bedspaceCount = 2,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(360),
            LocalDate.now(clock).minusDays(180),
          ),
          bedspacesEndDates = listOf(
            null,
            latestBedspaceArchiveDate,
          ),
        ) { premises, bedspaces ->
          val bedspaceOne = bedspaces.first()
          val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now(clock).plusDays(5))

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces/${bedspaceOne.id}/archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(archiveBedspace)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("id").isEqualTo(bedspaceOne.id)
            .jsonPath("endDate").isEqualTo(archiveBedspace.endDate)

          val updatedPremises = cas3PremisesRepository.findByIdOrNull(premises.id)
          assertThat(updatedPremises).isNotNull()
          assertThat(updatedPremises?.endDate).isEqualTo(latestBedspaceArchiveDate)
          assertThat(updatedPremises?.status).isEqualTo(Cas3PremisesStatus.archived)

          val updatedBedspace = cas3BedspacesRepository.findByIdOrNull(bedspaceOne.id)
          assertThat(updatedBedspace).isNotNull()
          assertThat(updatedBedspace?.endDate).isEqualTo(updatedBedspace?.endDate)

          val allEvents = domainEventRepository.findAll()
          assertThat(allEvents).hasSize(2)
          assertThat(allEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_ARCHIVED)
          assertThat(allEvents[0].cas3BedspaceId).isEqualTo(bedspaceOne.id)
          assertThat(allEvents[0].cas3PremisesId).isEqualTo(premises.id)
          assertThat(allEvents[1].type).isEqualTo(DomainEventType.CAS3_PREMISES_ARCHIVED)
          assertThat(allEvents[1].cas3PremisesId).isEqualTo(premises.id)
        }
      }
    }

    @Test
    fun `When archive a bedspace for a Premises that not exist returns 404 Not Found`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val latestBedspaceArchiveDate = LocalDate.now(clock).plusDays(35)
        givenACas3PremisesWithBedspaces(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(360),
          ),
          bedspacesEndDates = listOf(
            latestBedspaceArchiveDate,
          ),
        ) { premises, bedspaces ->
          val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now(clock).plusDays(5))
          val nonExistPremisesId = UUID.randomUUID()

          webTestClient.post()
            .uri("/cas3/v2/premises/$nonExistPremisesId/bedspaces/${bedspaces[0].id}/archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(archiveBedspace)
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody()
            .jsonPath("$.detail").isEqualTo("No Cas3Premises with an ID of $nonExistPremisesId could be found")
        }
      }
    }

    @Test
    fun `When archive a bedspace with end date before bedspace start date returns 404 Not Found`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenACas3PremisesWithBedspaces(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(2),
          ),
          bedspacesEndDates = listOf(
            null,
          ),
        ) { premises, bedspaces ->
          val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now(clock).minusDays(5))

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces/${bedspaces[0].id}/archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(archiveBedspace)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.title").isEqualTo("Bad Request")
            .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.endDate")
            .jsonPath("$.invalid-params[0].errorType").isEqualTo("endDateBeforeBedspaceStartDate")
        }
      }
    }

    @Test
    fun `When archive a bedspace with a date that clashes with an earlier archive bedspace end date then returns 400 Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenACas3PremisesWithBedspaces(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(300),
          ),
          bedspacesEndDates = listOf(
            null,
          ),
        ) { premises, bedspaces ->
          val previousBedspaceArchiveDate = LocalDate.now(clock).minusDays(3)
          createBedspaceArchiveDomainEvent(bedspaces[0].id, premises.id, user.id, null, previousBedspaceArchiveDate)
          val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now(clock).minusDays(3))

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces/${bedspaces[0].id}/archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(archiveBedspace)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.title").isEqualTo("Bad Request")
            .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.endDate")
            .jsonPath("$.invalid-params[0].errorType").isEqualTo("endDateOverlapPreviousBedspaceArchiveEndDate")
            .jsonPath("$.invalid-params[0].entityId").isEqualTo(bedspaces[0].id.toString())
            .jsonPath("$.invalid-params[0].value").isEqualTo(previousBedspaceArchiveDate.toString())
        }
      }
    }

    @Test
    fun `When archive a bedspace with an active booking after the future archiving date returns 400 Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenACas3PremisesWithBedspaces(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(360),
          ),
          bedspacesEndDates = listOf(
            null,
          ),
        ) { premises, bedspaces ->
          val bedspace = bedspaces.first()
          val bookingDepartureDate = LocalDate.now(clock).plusDays(10)
          cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withPremises(premises)
            withBedspace(bedspace)
            withArrivalDate(LocalDate.now(clock).minusDays(20))
            withDepartureDate(bookingDepartureDate)
            withStatus(Cas3BookingStatus.provisional)
          }
          val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now(clock).plusDays(5))

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces/${bedspace.id}/archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(archiveBedspace)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.title").isEqualTo("Bad Request")
            .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.endDate")
            .jsonPath("$.invalid-params[0].errorType").isEqualTo("existingBookings")
            .jsonPath("$.invalid-params[0].entityId").isEqualTo(bedspace.id.toString())
            .jsonPath("$.invalid-params[0].value").isEqualTo(bookingDepartureDate.plusDays(1).toString())
        }
      }
    }

    @Test
    fun `When archive a bedspace with a void end date after the bedspace archive date returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenACas3PremisesWithBedspaces(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(360),
          ),
          bedspacesEndDates = listOf(
            null,
          ),
        ) { premises, bedspaces ->
          val bedspace = bedspaces.first()
          val bedspaceArchivingDate = LocalDate.now(clock).plusDays(10)
          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBedspace(bedspace)
            withStartDate(bedspaceArchivingDate.minusDays(2))
            withEndDate(bedspaceArchivingDate.plusDays(2))
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }
          val archiveBedspace = Cas3ArchiveBedspace(bedspaceArchivingDate)

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces/${bedspace.id}/archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(archiveBedspace)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.title").isEqualTo("Bad Request")
            .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.endDate")
            .jsonPath("$.invalid-params[0].errorType").isEqualTo("existingVoid")
            .jsonPath("$.invalid-params[0].entityId").isEqualTo(bedspace.id.toString())
            .jsonPath("$.invalid-params[0].value").isEqualTo(bedspaceArchivingDate.plusDays(3).toString())
        }
      }
    }

    @Test
    fun `When archive a bedspace with a booking turnaround after the bedspace archive date returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenACas3PremisesWithBedspaces(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(360),
          ),
          bedspacesEndDates = listOf(
            null,
          ),
        ) { premises, bedspaces ->
          val bedspace = bedspaces.first()

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val bedspaceArchivingDate = LocalDate.now(clock).plusDays(10)

          val booking = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withPremises(premises)
            withBedspace(bedspace)
            withArrivalDate(LocalDate.now(clock).minusDays(20))
            withDepartureDate(bedspaceArchivingDate.minusDays(1))
            withStatus(Cas3BookingStatus.provisional)
          }

          cas3v2TurnaroundFactory.produceAndPersist {
            withBooking(booking)
            withWorkingDayCount(3)
          }

          val archiveBedspace = Cas3ArchiveBedspace(bedspaceArchivingDate)

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces/${bedspace.id}/archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(archiveBedspace)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.title").isEqualTo("Bad Request")
            .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.endDate")
            .jsonPath("$.invalid-params[0].errorType").isEqualTo("existingTurnaround")
            .jsonPath("$.invalid-params[0].entityId").isEqualTo(bedspace.id.toString())
            .jsonPath("$.invalid-params[0].value").isEqualTo(bedspaceArchivingDate.plusDays(4).toString())
        }
      }
    }

    @Test
    fun `When archive a bedspace for a Premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenACas3PremisesWithBedspaces(
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(360),
          ),
          bedspacesEndDates = listOf(
            null,
          ),
        ) { premises, bedspaces ->
          val bedspace = bedspaces.first()

          val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now(clock).plusDays(5))

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces/${bedspace.id}/archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(archiveBedspace)
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }
  }
}
