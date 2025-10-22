package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.Cas3IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3PremisesComplete
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3PremisesWithBedspaces
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3PremisesWithUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ArchiveBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3UnarchiveBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
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
    fun `When archive the last online bedspace returns OK and archives the premises with the latest bedspace end date when given valid data`() {
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

  @Nested
  inner class CancelScheduledArchiveBedspace {
    @BeforeEach
    fun setup() {
      clock.setNow(Instant.parse("2025-08-27T15:21:34Z"))
    }

    @Test
    fun `Cancel scheduled archive bedspace returns 200 OK when successful`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val scheduledArchiveBedspaceDate = LocalDate.now().plusDays(1)
        givenACas3PremisesWithBedspaces(
          region = user.probationRegion,
          bedspaceCount = 2,
          bedspacesStartDates = listOf(LocalDate.now().minusDays(30), LocalDate.now().minusDays(60)),
          bedspacesEndDates = listOf(scheduledArchiveBedspaceDate),
        ) { premises, bedspaces ->
          val bedspace = bedspaces.first()
          val previousBedspaceEndDate = LocalDate.now().minusDays(10)
          val bedspaceArchiveDomainEvent = createBedspaceArchiveDomainEvent(
            bedspace.id,
            premises.id,
            user.id,
            previousBedspaceEndDate,
            scheduledArchiveBedspaceDate,
          )

          webTestClient.put()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces/${bedspace.id}/cancel-archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("id").isEqualTo(bedspace.id)
            .jsonPath("endDate").isEqualTo(previousBedspaceEndDate)

          assertThatBedspaceArchiveCancelled(bedspace.id, bedspaceArchiveDomainEvent.id, previousBedspaceEndDate)
        }
      }
    }

    @Test
    fun `When cancel scheduled archive bedspace in a premises that is scheduled to be archived returns Success and cancels the scheduled archiving of the premises and all associated bedspaces`() {
      val archivedPremisesDate = LocalDate.now().plusWeeks(1)
      givenACas3PremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = LocalDate.now().minusDays(180),
        premisesEndDate = archivedPremisesDate,
        premisesStatus = Cas3PremisesStatus.online,
        bedspaceCount = 3,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(180),
          LocalDate.now().minusDays(90),
          LocalDate.now().minusDays(30),
        ),
        bedspaceEndDates = listOf(
          archivedPremisesDate,
          LocalDate.now().minusDays(13),
          archivedPremisesDate,
        ),
      ) { user, jwt, premises, bedspaces ->
        val bedspaceOne = bedspaces.first()
        val bedspaceTwo = bedspaces.drop(1).first()
        val bedspaceThree = bedspaces.drop(2).first()

        val previousBedspaceOneEndDate = LocalDate.now().minusDays(10)
        val domainEventTransactionId = UUID.randomUUID()

        val archivePremisesDomainEvent = createCas3PremisesArchiveDomainEvent(premises, user, archivedPremisesDate, transactionId = domainEventTransactionId)
        val archiveBedspaceOneDomainEvent = createBedspaceArchiveDomainEvent(bedspaceOne.id, premises.id, user.id, previousBedspaceOneEndDate, archivedPremisesDate, transactionId = domainEventTransactionId)
        val archiveBedspaceTwoDomainEvent = createBedspaceArchiveDomainEvent(bedspaceTwo.id, premises.id, user.id, null, bedspaceTwo.endDate!!, transactionId = UUID.randomUUID())
        val archiveBedspaceThreeDomainEvent = createBedspaceArchiveDomainEvent(bedspaceThree.id, premises.id, user.id, null, archivedPremisesDate, transactionId = domainEventTransactionId)

        webTestClient.put()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces/${bedspaceOne.id}/cancel-archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(bedspaceOne.id)
          .jsonPath("endDate").isEqualTo(previousBedspaceOneEndDate)

        // check that premises was updated correctly
        val updatedPremises = cas3PremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises).isNotNull()
        assertThat(updatedPremises.status).isEqualTo(Cas3PremisesStatus.online)
        assertThat(updatedPremises.endDate).isNull()

        val updatedArchivePremisesDomainEvent = domainEventRepository.findByIdOrNull(archivePremisesDomainEvent.id)
        assertThat(updatedArchivePremisesDomainEvent).isNotNull()
        assertThat(updatedArchivePremisesDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))

        // check that bedspaces were updated correctly
        assertThatBedspaceArchiveCancelled(bedspaceOne.id, archiveBedspaceOneDomainEvent.id, previousBedspaceOneEndDate)
        assertThatBedspaceArchiveCancelled(bedspaceThree.id, archiveBedspaceThreeDomainEvent.id, null)

        // check that bedspace not part of a scheduled archive not updated
        val notUpdatedBedspace = cas3BedspacesRepository.findById(bedspaceTwo.id).get()
        assertThat(notUpdatedBedspace.endDate).isEqualTo(bedspaceTwo.endDate)

        val notUpdatedBedspaceArchiveDomainEvent =
          domainEventRepository.findByIdOrNull(archiveBedspaceTwoDomainEvent.id)
        assertThat(notUpdatedBedspaceArchiveDomainEvent).isNotNull()
        assertThat(notUpdatedBedspaceArchiveDomainEvent?.cas3CancelledAt).isNull()
      }
    }

    @Test
    fun `Cancel scheduled archive bedspace returns 403 when user does not have permission to manage premises without CAS3_ASSESOR role`() {
      givenACas3PremisesComplete(
        roles = listOf(UserRole.CAS3_REFERRER),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now(clock).minusDays(30),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now(clock).plusDays(1),
        ),
      ) { _, jwt, premises, bedspaces ->
        val scheduledToArchivedBedspace = bedspaces.first()

        webTestClient.put()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces/${scheduledToArchivedBedspace.id}/cancel-archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Cancel scheduled archive bedspace returns 400 when bedspace does not exist`() {
      givenACas3PremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
      ) { _, jwt, premises ->
        val nonExistentBedspaceId = UUID.randomUUID()

        webTestClient.put()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces/$nonExistentBedspaceId/cancel-archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.bedspaceId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @Test
    fun `Cancel scheduled archive bedspace returns 400 when bedspace is not archived`() {
      givenACas3PremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now(clock).minusDays(10),
        ),
        bedspaceEndDates = listOf(
          null,
        ),
      ) { _, jwt, premises, bedspaces ->
        val onlineBedspace = bedspaces.first()

        webTestClient.put()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces/${onlineBedspace.id}/cancel-archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.bedspaceId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("bedspaceNotScheduledToArchive")
      }
    }

    @Test
    fun `Cancel scheduled archive bedspace returns 400 when bedspace is already archived`() {
      givenACas3PremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now(clock).minusDays(10),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now(clock).minusDays(1),
        ),
      ) { _, jwt, premises, bedspaces ->
        val archivedBedspace = bedspaces.first()

        webTestClient.put()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces/${archivedBedspace.id}/cancel-archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.bedspaceId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("bedspaceAlreadyArchived")
      }
    }

    @Test
    fun `Cancel scheduled archive bedspace returns 404 when the premises is not found`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        webTestClient.put()
          .uri("/cas3/v2/premises/${UUID.randomUUID()}/bedspaces/${UUID.randomUUID()}/cancel-archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Cancel scheduled archive bedspace returns 403 when user does not have permission to manage premises in that region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        givenACas3PremisesWithBedspaces(
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(30),
          ),
          bedspacesEndDates = listOf(
            LocalDate.now(clock).minusDays(1),
          ),
        ) { premises, bedspaces ->
          val archivedBedspace = bedspaces.first()

          webTestClient.put()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces/${archivedBedspace.id}/cancel-archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    private fun assertThatBedspaceArchiveCancelled(bedspaceId: UUID, archiveBedspaceDomainEventId: UUID, previousEndDate: LocalDate?) {
      val updatedBedspace = cas3BedspacesRepository.findById(bedspaceId).get()
      assertThat(updatedBedspace.endDate).isEqualTo(previousEndDate)

      val updatedBedspaceArchiveDomainEvent =
        domainEventRepository.findByIdOrNull(archiveBedspaceDomainEventId)
      assertThat(updatedBedspaceArchiveDomainEvent).isNotNull()
      assertThat(updatedBedspaceArchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))
    }
  }

  @Nested
  inner class UnarchiveBedspace {
    @Test
    fun `Unarchive bedspace returns 200 OK when successful`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenACas3PremisesWithBedspaces(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(30),
          ),
          bedspacesEndDates = listOf(
            LocalDate.now(clock).minusDays(1),
          ),
        ) { premises, bedspaces ->
          val archivedBedspace = bedspaces.first()
          val cas3UnarchiveBedspace = Cas3UnarchiveBedspace(LocalDate.now().plusDays(1))

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(cas3UnarchiveBedspace)
            .exchange()
            .expectStatus()
            .isOk

          // Verify the bedspace was updated
          val updatedBedspace = cas3BedspacesRepository.findById(archivedBedspace.id).get()
          assertThat(updatedBedspace.startDate).isEqualTo(cas3UnarchiveBedspace.restartDate)
          assertThat(updatedBedspace.endDate).isNull()

          val allEvents = domainEventRepository.findAll()
          assertThat(allEvents).hasSize(1)
          assertThat(allEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)
          assertThat(allEvents[0].cas3BedspaceId).isEqualTo(archivedBedspace.id)
          assertThat(allEvents[0].cas3PremisesId).isEqualTo(premises.id)
        }
      }
    }

    @Test
    fun `Unarchive bedspace when premises is archived returns 200 OK and unarchive premises and bedspace successfully`() {
      givenACas3PremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesEndDate = LocalDate.now().minusDays(30),
        premisesStatus = Cas3PremisesStatus.archived,
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(180),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now().minusDays(40),
        ),
      ) { _, jwt, premises, bedspaces ->
        val archivedBedspace = bedspaces.first()
        val cas3UnarchiveBedspace = Cas3UnarchiveBedspace(LocalDate.now().plusDays(1))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(cas3UnarchiveBedspace)
          .exchange()
          .expectStatus()
          .isOk

        // Verify the bedspace was updated
        val updatedBedspace = cas3BedspacesRepository.findById(archivedBedspace.id).get()
        assertThat(updatedBedspace.startDate).isEqualTo(cas3UnarchiveBedspace.restartDate)
        assertThat(updatedBedspace.endDate).isNull()

        // Verify the premises was updated
        val updatedPremises = cas3PremisesRepository.findByIdOrNull(premises.id)
        assertThat(updatedPremises).isNotNull()
        assertThat(updatedPremises?.startDate).isEqualTo(cas3UnarchiveBedspace.restartDate)
        assertThat(updatedPremises?.endDate).isNull()

        val allEvents = domainEventRepository.findAll()
        assertThat(allEvents).hasSize(2)
        assertThat(allEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)
        assertThat(allEvents[0].cas3BedspaceId).isEqualTo(archivedBedspace.id)
        assertThat(allEvents[0].cas3PremisesId).isEqualTo(premises.id)
        assertThat(allEvents[1].type).isEqualTo(DomainEventType.CAS3_PREMISES_UNARCHIVED)
        assertThat(allEvents[1].cas3PremisesId).isEqualTo(premises.id)
      }
    }

    @Test
    fun `Unarchive bedspace when premises is online returns 200 OK and unarchive bedspace successfully`() {
      givenACas3PremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(180),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now().minusDays(40),
        ),
      ) { _, jwt, premises, bedspaces ->
        val archivedBedspace = bedspaces.first()
        val cas3UnarchiveBedspace = Cas3UnarchiveBedspace(LocalDate.now().plusDays(5))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(cas3UnarchiveBedspace)
          .exchange()
          .expectStatus()
          .isOk

        // Verify the bedspace was updated
        val updatedBedspace = cas3BedspacesRepository.findById(archivedBedspace.id).get()
        assertThat(updatedBedspace.startDate).isEqualTo(cas3UnarchiveBedspace.restartDate)
        assertThat(updatedBedspace.endDate).isNull()

        // Verify the premises was not updated
        val updatedPremises = cas3PremisesRepository.findByIdOrNull(premises.id)
        assertThat(updatedPremises).isNotNull()
        assertThat(updatedPremises?.startDate).isEqualTo(premises.startDate)
        assertThat(updatedPremises?.endDate).isNull()

        val allEvents = domainEventRepository.findAll()
        assertThat(allEvents).hasSize(1)
        assertThat(allEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)
        assertThat(allEvents[0].cas3BedspaceId).isEqualTo(archivedBedspace.id)
        assertThat(allEvents[0].cas3PremisesId).isEqualTo(premises.id)
      }
    }

    @Test
    fun `Unarchive bedspace returns 400 when restart date is too far in the past`() {
      givenACas3PremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(30),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now().minusDays(10),
        ),
      ) { _, jwt, premises, bedspaces ->
        val archivedBedspace = bedspaces.first()
        val cas3UnarchiveBedspace = Cas3UnarchiveBedspace(LocalDate.now().minusDays(8))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(cas3UnarchiveBedspace)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.restartDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("invalidRestartDateInThePast")
      }
    }

    @Test
    fun `Unarchive bedspace returns 400 when restart date is too far in the future`() {
      givenACas3PremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(30),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now().minusDays(1),
        ),
      ) { _, jwt, premises, bedspaces ->
        val archivedBedspace = bedspaces.first()
        val cas3UnarchiveBedspace = Cas3UnarchiveBedspace(LocalDate.now().plusDays(8))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(cas3UnarchiveBedspace)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.restartDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("invalidRestartDateInTheFuture")
      }
    }

    @Test
    fun `Unarchive bedspace returns 400 when restart date is before last archive end date`() {
      givenACas3PremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(30),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now().minusDays(5),
        ),
      ) { _, jwt, premises, bedspaces ->
        val archivedBedspace = bedspaces.first()
        val cas3UnarchiveBedspace = Cas3UnarchiveBedspace(LocalDate.now().minusDays(7))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(cas3UnarchiveBedspace)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.restartDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("beforeLastBedspaceArchivedDate")
      }
    }

    @Test
    fun `Unarchive bedspace returns 400 when bedspace does not exist`() {
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
        ) { premises, _ ->
          val nonExistentBedspaceId = UUID.randomUUID()
          val cas3UnarchiveBedspace = Cas3UnarchiveBedspace(LocalDate.now().plusDays(3))

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces/$nonExistentBedspaceId/unarchive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(cas3UnarchiveBedspace)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.title").isEqualTo("Bad Request")
            .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.bedspaceId")
            .jsonPath("$.invalid-params[0].errorType").isEqualTo("doesNotExist")
        }
      }
    }

    @Test
    fun `Unarchive bedspace returns 400 when bedspace is not archived`() {
      givenACas3PremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(10),
        ),
        bedspaceEndDates = listOf(
          null,
        ),
      ) { _, jwt, premises, bedspaces ->
        val onlineBedspace = bedspaces.first()
        val cas3UnarchiveBedspace = Cas3UnarchiveBedspace(LocalDate.now().plusDays(2))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces/${onlineBedspace.id}/unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(cas3UnarchiveBedspace)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.bedspaceId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("bedspaceNotArchived")
      }
    }

    @Test
    fun `Unarchive bedspace returns 403 when user does not have permission to manage premises`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val latestBedspaceArchiveDate = LocalDate.now(clock).plusDays(35)
        givenACas3PremisesWithBedspaces(
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(360),
          ),
          bedspacesEndDates = listOf(
            latestBedspaceArchiveDate,
          ),
        ) { premises, bedspace ->
          val restartDate = LocalDate.now().plusDays(1)

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces/${bedspace[0].id}/unarchive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              mapOf("restartDate" to restartDate.toString()),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }
  }

  @Nested
  inner class CancelScheduledUnarchiveBedspace {
    @BeforeEach
    fun setup() {
      clock.setNow(Instant.parse("2025-09-03T11:34:09Z"))
    }

    @Test
    fun `Cancel scheduled unarchive bedspace returns 200 OK when successful`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val bedspaceStartDate = LocalDate.now(clock).minusDays(10)
        givenACas3PremisesWithBedspaces(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(bedspaceStartDate),
          bedspacesEndDates = listOf(
            LocalDate.now(clock).minusDays(3),
          ),
          bedspacesCreatedDate = listOf(bedspaceStartDate),
        ) { premises, bedspaces ->
          val scheduledBedspaceToUnarchived = bedspaces.first()
          createBedspaceUnarchiveDomainEvent(
            scheduledBedspaceToUnarchived,
            premises.id,
            user.id,
            LocalDate.now(clock).minusDays(30),
          )
          val lastBedspaceUnarchiveDomainEvent = createBedspaceUnarchiveDomainEvent(
            scheduledBedspaceToUnarchived,
            premises.id,
            user.id,
            LocalDate.now(clock).plusDays(10),
          )

          webTestClient.put()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces/${scheduledBedspaceToUnarchived.id}/cancel-unarchive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("id").isEqualTo(scheduledBedspaceToUnarchived.id)
            .jsonPath("startDate").isEqualTo(scheduledBedspaceToUnarchived.createdDate)
            .jsonPath("endDate").isEqualTo(scheduledBedspaceToUnarchived.endDate)

          // Verify the bedspace was updated
          val updatedBedspace = cas3BedspacesRepository.findById(scheduledBedspaceToUnarchived.id).get()
          assertThat(updatedBedspace.createdDate).isEqualTo(scheduledBedspaceToUnarchived.createdDate)
          assertThat(updatedBedspace.startDate).isEqualTo(scheduledBedspaceToUnarchived.startDate)
          assertThat(updatedBedspace.endDate).isEqualTo(scheduledBedspaceToUnarchived.endDate)

          val updatedBedspaceUnarchiveDomainEvent =
            domainEventRepository.findByIdOrNull(lastBedspaceUnarchiveDomainEvent.id)
          assertThat(updatedBedspaceUnarchiveDomainEvent).isNotNull()
          assertThat(updatedBedspaceUnarchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))
        }
      }
    }

    @Test
    fun `Cancel scheduled unarchive bedspace returns 400 when it has a field validation error (startDate is today)`() {
      val startDate = LocalDate.now(clock)

      givenACas3PremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          startDate,
        ),
        bedspaceEndDates = listOf(
          null,
        ),
      ) { _, jwt, premises, bedspaces ->
        val scheduledToUnarchivedBedspace = bedspaces.first()
        webTestClient.put()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces/${scheduledToUnarchivedBedspace.id}/cancel-unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.bedspaceId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("bedspaceAlreadyOnline")

        // Verify the bedspace was not updated
        val originalBedspace = cas3BedspacesRepository.findById(scheduledToUnarchivedBedspace.id).get()
        assertThat(originalBedspace.startDate).isEqualTo(startDate)
      }
    }

    @Test
    fun `Cancel scheduled unarchive bedspace returns 403 when user does not have permission to manage premises without CAS3_ASSESOR role`() {
      givenACas3PremisesComplete(
        roles = listOf(UserRole.CAS3_REFERRER),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now(clock).minusDays(10),
        ),
        bedspaceEndDates = listOf(
          null,
        ),
      ) { _, jwt, premises, bedspaces ->
        val scheduledToUnarchivedBedspace = bedspaces.first()
        webTestClient.put()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces/${scheduledToUnarchivedBedspace.id}/cancel-unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Cancel scheduled unarchive bedspace returns 404 when the bedspace is not found`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        webTestClient.put()
          .uri("/cas3/v2/premises/${UUID.randomUUID()}/bedspaces/${UUID.randomUUID()}/cancel-unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Cancel scheduled unarchive bedspace returns 403 when user does not have permission to manage premises in that region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenACas3PremisesWithBedspaces(
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(30),
          ),
          bedspacesEndDates = listOf(
            null,
          ),
        ) { premises, bedspaces ->
          val scheduledToUnarchivedBedspace = bedspaces.first()
          webTestClient.put()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces/${scheduledToUnarchivedBedspace.id}/cancel-unarchive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }
  }
}
