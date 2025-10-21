package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.Cas3IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3PremisesComplete
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3PremisesWithBedspaces
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3PremisesWithUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ArchivePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3v2PremisesArchiveTest : Cas3IntegrationTestBase() {

  @Nested
  inner class ArchivePremises {
    @Test
    fun `Given archive a premises when successfully passed all validations then returns 200 OK`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenACas3PremisesWithBedspaces(
          region = user.probationRegion,
          bedspacesStartDates = listOf(
            LocalDate.now().minusDays(100),
            LocalDate.now().minusDays(75),
            LocalDate.now().minusDays(30),
          ),
          bedspacesEndDates = listOf(
            null,
            LocalDate.now().minusDays(2),
            null,
          ),
          bedspaceCount = 3,
        ) { premises, bedspaces ->
          val archivePremises = Cas3ArchivePremises(LocalDate.now())
          val bedspaceOne = bedspaces.first()
          val bedspaceTwo = bedspaces.drop(1).first()
          val bedspaceThree = bedspaces.drop(2).first()

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(archivePremises)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("id").isEqualTo(premises.id.toString())
            .jsonPath("status").isEqualTo("archived")

          val updatedPremises = cas3PremisesRepository.findById(premises.id).get()
          assertThat(updatedPremises.status).isEqualTo(Cas3PremisesStatus.archived)
          assertThat(updatedPremises.endDate).isEqualTo(LocalDate.now())

          val premisesDomainEvents =
            domainEventRepository.findByCas3PremisesIdAndType(premises.id, DomainEventType.CAS3_PREMISES_ARCHIVED)
          assertThat(premisesDomainEvents).hasSize(1)

          val today = LocalDate.now()

          val updatedBedspaces = cas3BedspacesRepository.findByPremisesId(updatedPremises.id)
          assertThat(updatedBedspaces).hasSize(3)
          assertThat(updatedBedspaces.map { it.id to it.endDate })
            .containsExactlyInAnyOrder(
              bedspaceTwo.id to bedspaceTwo.endDate,
              bedspaceOne.id to today,
              bedspaceThree.id to today,
            )

          val bedspaceOneDomainEvents = domainEventRepository.findByCas3BedspaceId(bedspaceOne.id)
          assertThat(bedspaceOneDomainEvents).hasSize(1)
          assertThat(bedspaceOneDomainEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_ARCHIVED)

          val bedspaceThreeDomainEvents = domainEventRepository.findByCas3BedspaceId(bedspaceThree.id)
          assertThat(bedspaceThreeDomainEvents).hasSize(1)
          assertThat(bedspaceThreeDomainEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_ARCHIVED)
        }
      }
    }

    @Test
    fun `Given archive a premises without bedspaces when successfully passed all validations then returns 200 OK`() {
      givenACas3PremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt, premises ->
        val archivePremises = Cas3ArchivePremises(LocalDate.now())

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(archivePremises)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(premises.id.toString())
          .jsonPath("status").isEqualTo("archived")

        val updatedPremises = cas3PremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises.status).isEqualTo(Cas3PremisesStatus.archived)
        assertThat(updatedPremises.endDate).isEqualTo(LocalDate.now())

        val premisesDomainEvents =
          domainEventRepository.findByCas3PremisesIdAndType(premises.id, DomainEventType.CAS3_PREMISES_ARCHIVED)
        assertThat(premisesDomainEvents).hasSize(1)
      }
    }

    @Test
    fun `Given archive a premises when archive date is more than 7 days in the past then returns 400 Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(probationDeliveryUnit = user.probationDeliveryUnit!!)

        val archivePremises = Cas3ArchivePremises(LocalDate.now().minusDays(8))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(archivePremises)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.endDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("invalidEndDateInThePast")
      }
    }

    @Test
    fun `Given archive a premises when archive date is more than 3 months in then future then returns 400 Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(probationDeliveryUnit = user.probationDeliveryUnit!!)

        val archivePremises = Cas3ArchivePremises(LocalDate.now().plusMonths(3).plusDays(1))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(archivePremises)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.endDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("invalidEndDateInTheFuture")
      }
    }

    @Test
    fun `Given archive a premises when archive date is before premises start date then returns 400 Bad Request`() {
      givenACas3PremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = LocalDate.now().minusDays(3),
      ) { user, jwt, premises ->

        val archivePremises = Cas3ArchivePremises(premises.startDate.minusDays(2))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(archivePremises)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.endDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("endDateBeforePremisesStartDate")
          .jsonPath("$.invalid-params[0].entityId").isEqualTo(premises.id.toString())
          .jsonPath("$.invalid-params[0].value").isEqualTo(premises.startDate.toString())
      }
    }

    @Test
    fun `Given archive a premises when archive date clashes with an earlier archive premises end date then returns 400 Bad Request`() {
      givenACas3PremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt, premises ->
        val previousPremisesArchiveDate = LocalDate.now().minusDays(3)

        createCas3PremisesArchiveDomainEvent(premises, user, previousPremisesArchiveDate)

        val archivePremises = Cas3ArchivePremises(previousPremisesArchiveDate.minusDays(3))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(archivePremises)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.endDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("endDateOverlapPreviousPremisesArchiveEndDate")
          .jsonPath("$.invalid-params[0].entityId").isEqualTo(premises.id.toString())
          .jsonPath("$.invalid-params[0].value").isEqualTo(previousPremisesArchiveDate.toString())
      }
    }

    @Test
    fun `Given archive a premises when there is upcoming bedspace then returns 400 Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenACas3PremisesWithBedspaces(
          region = user.probationRegion,
          bedspaceCount = 2,
          bedspacesStartDates = listOf(LocalDate.now().minusDays(100), LocalDate.now().plusDays(5)),
        ) { premises, bedspaces ->

          val upcomingBedspace = bedspaces.drop(1).first()
          val archivePremises = Cas3ArchivePremises(LocalDate.now().plusDays(1))

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(archivePremises)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.title").isEqualTo("Bad Request")
            .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.endDate")
            .jsonPath("$.invalid-params[0].errorType").isEqualTo("existingUpcomingBedspace")
            .jsonPath("$.invalid-params[0].entityId").isEqualTo(upcomingBedspace.id.toString())
            .jsonPath("$.invalid-params[0].value").isEqualTo(upcomingBedspace.startDate?.plusDays(1).toString())
        }
      }
    }

    @Test
    fun `Given archive a premises when bedspaces have active booking and void after the premises archive date then returns 400 Bad Request with correct details`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenACas3PremisesWithBedspaces(
          region = user.probationRegion,
          bedspacesStartDates = listOf(
            LocalDate.now().minusDays(100),
            LocalDate.now().minusDays(75),
            LocalDate.now().minusDays(30),
          ),
          bedspaceCount = 3,
        ) { premises, bedspaces ->
          val premisesArchiveDate = LocalDate.now().plusDays(5)

          val bedspaceOne = bedspaces.first()
          val bookingDepartureDate = premisesArchiveDate.plusDays(10)

          createCas3Booking(
            premises = premises,
            bedspace = bedspaceOne,
            arrivalDate = premisesArchiveDate.minusDays(2),
            departureDate = bookingDepartureDate,
          )

          val bedspaceTwo = bedspaces.drop(1).first()
          val voidEndDate = premisesArchiveDate.plusDays(5)
          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBedspace(bedspaceTwo)
            withStartDate(premisesArchiveDate.minusDays(2))
            withEndDate(voidEndDate)
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          val bedspaceThree = bedspaces.drop(2).first()
          createCas3Booking(
            premises = premises,
            bedspace = bedspaceThree,
            arrivalDate = premisesArchiveDate.minusDays(35),
            departureDate = premisesArchiveDate.plusDays(3),
          )

          val archivePremises = Cas3ArchivePremises(premisesArchiveDate)

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(archivePremises)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.title").isEqualTo("Bad Request")
            .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.endDate")
            .jsonPath("$.invalid-params[0].errorType").isEqualTo("existingBookings")
            .jsonPath("$.invalid-params[0].entityId").isEqualTo(bedspaceOne.id.toString())
            .jsonPath("$.invalid-params[0].value").isEqualTo(bookingDepartureDate.plusDays(1).toString())
        }
      }
    }

    private fun createCas3Booking(
      premises: Cas3PremisesEntity,
      bedspace: Cas3BedspacesEntity,
      arrivalDate: LocalDate,
      departureDate: LocalDate,
      status: Cas3BookingStatus = Cas3BookingStatus.provisional,
    ) = cas3BookingEntityFactory.produceAndPersist {
      withServiceName(ServiceName.temporaryAccommodation)
      withPremises(premises)
      withBedspace(bedspace)
      withCrn(randomStringMultiCaseWithNumbers(6))
      withArrivalDate(arrivalDate)
      withDepartureDate(departureDate)
      withStatus(status)
    }
  }

  @Nested
  inner class CancelScheduledArchivePremises {
    @BeforeEach
    fun setup() {
      clock.setNow(Instant.parse("2025-07-16T21:33:04Z"))
    }

    @Test
    fun `Cancel scheduled archive premises returns 200 OK when successful`() {
      givenACas3PremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesEndDate = LocalDate.now(clock).plusDays(10),
        premisesStatus = Cas3PremisesStatus.archived,
        bedspaceCount = 3,
        bedspaceStartDates = listOf(
          LocalDate.now(clock),
          LocalDate.now(clock),
          LocalDate.now(clock),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now(clock).plusDays(10),
          LocalDate.now(clock).plusDays(10),
          LocalDate.now(clock).plusDays(10),
        ),
      ) { user, jwt, premises, bedspaces ->
        val domainEventTransactionId = UUID.randomUUID()

        val premisesArchiveDate = premises.endDate!!
        val premisesArchiveDomainEvent = createCas3PremisesArchiveDomainEvent(premises, user, premisesArchiveDate, transactionId = domainEventTransactionId)

        val bedspaceOne = bedspaces.first()
        val bedspaceOneArchiveDomainEvent = createBedspaceArchiveDomainEvent(bedspaceOne.id, premises.id, user.id, null, premisesArchiveDate, transactionId = domainEventTransactionId)
        val bedspaceTwo = bedspaces.drop(1).first()
        val bedspaceTwoCurrentEndDate = bedspaceTwo.endDate ?: premisesArchiveDate.plusDays(12)
        val bedspaceTwoArchiveDomainEvent = createBedspaceArchiveDomainEvent(bedspaceTwo.id, premises.id, user.id, bedspaceTwoCurrentEndDate, premisesArchiveDate, transactionId = domainEventTransactionId)
        val bedspaceThree = bedspaces.drop(2).first()
        val bedspaceThreeArchiveDomainEvent = createBedspaceArchiveDomainEvent(bedspaceThree.id, premises.id, user.id, null, premisesArchiveDate, transactionId = domainEventTransactionId)

        webTestClient.put()
          .uri("/cas3/v2/premises/${premises.id}/cancel-archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(premises.id)
          .jsonPath("endDate").doesNotExist()

        // Verify that premise was updated
        val updatedPremises = cas3PremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises.endDate).isNull()
        assertThat(updatedPremises.status).isEqualTo(Cas3PremisesStatus.online)

        val updatedPremisesArchiveDomainEvent = domainEventRepository.findByIdOrNull(premisesArchiveDomainEvent.id)
        assertThat(updatedPremisesArchiveDomainEvent).isNotNull()
        assertThat(updatedPremisesArchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))

        // Verify that bedspace were updated
        val updatedBedspaces = cas3BedspacesRepository.findByPremisesId(premises.id)
        updatedBedspaces.containsAll(listOf(bedspaceOne, bedspaceTwo, bedspaceThree))
        assertThat(updatedBedspaces).hasSize(3)
        assertThat(updatedBedspaces.first { it.id == bedspaceOne.id }.endDate).isNull()
        assertThat(updatedBedspaces.first { it.id == bedspaceTwo.id }.endDate).isEqualTo(bedspaceTwoCurrentEndDate)
        assertThat(updatedBedspaces.first { it.id == bedspaceThree.id }.endDate).isNull()

        val updatedBedspaceOneArchiveDomainEvent = domainEventRepository.findByIdOrNull(bedspaceOneArchiveDomainEvent.id)
        assertThat(updatedBedspaceOneArchiveDomainEvent).isNotNull()
        assertThat(updatedBedspaceOneArchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))

        val updatedBedspaceTwoArchiveDomainEvent = domainEventRepository.findByIdOrNull(bedspaceTwoArchiveDomainEvent.id)
        assertThat(updatedBedspaceTwoArchiveDomainEvent).isNotNull()
        assertThat(updatedBedspaceTwoArchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))

        val updatedBedspaceThreeArchiveDomainEvent = domainEventRepository.findByIdOrNull(bedspaceThreeArchiveDomainEvent.id)
        assertThat(updatedBedspaceThreeArchiveDomainEvent).isNotNull()
        assertThat(updatedBedspaceThreeArchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))
      }
    }

    @Test
    fun `Cancel scheduled archive premises returns 403 when user does not have permission to manage premises without CAS3_ASSESOR role`() {
      givenACas3PremisesWithUser(
        roles = listOf(UserRole.CAS3_REFERRER),
        premisesEndDate = LocalDate.now(clock).plusDays(10),
        premisesStatus = Cas3PremisesStatus.archived,
      ) { _, jwt, premises ->
        webTestClient.put()
          .uri("/cas3/v2/premises/${premises.id}/cancel-archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Cancel scheduled archive premises returns 404 when premises does not exist`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val nonExistentPremises = UUID.randomUUID().toString()

        webTestClient.put()
          .uri("/cas3/v2/premises/$nonExistentPremises/cancel-archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("$.title").isEqualTo("Not Found")
          .jsonPath("$.status").isEqualTo(404)
          .jsonPath("$.detail").isEqualTo("No Cas3Premises with an ID of $nonExistentPremises could be found")
      }
    }

    @Test
    fun `Cancel scheduled archive premises returns 400 (premisesNotScheduledToArchive) when premise is not archived`() {
      givenACas3PremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
      ) { _, jwt, premises ->
        webTestClient.put()
          .uri("/cas3/v2/premises/${premises.id}/cancel-archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.premisesId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("premisesNotScheduledToArchive")
      }
    }

    @Test
    fun `Cancel scheduled archive premise returns 400 when premise already archived today`() {
      givenACas3PremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesEndDate = LocalDate.now(clock),
        premisesStatus = Cas3PremisesStatus.archived,
      ) { _, jwt, premises ->

        webTestClient.put()
          .uri("/cas3/v2/premises/${premises.id}/cancel-archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.premisesId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("premisesAlreadyArchived")
      }
    }

    @Test
    fun `Cancel scheduled archive premise returns 400 when premise has already been archived in the past`() {
      givenACas3PremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesEndDate = LocalDate.now(clock).minusDays(1),
      ) { _, jwt, premises ->

        webTestClient.put()
          .uri("/cas3/v2/premises/${premises.id}/cancel-archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.premisesId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("premisesAlreadyArchived")
      }
    }

    @Test
    fun `Cancel scheduled archive premises returns 403 when user does not have permission to manage premises in that region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val probationRegion = givenAProbationRegion()
        val premises = givenACas3Premises(probationRegion = probationRegion)
        webTestClient.put()
          .uri("/cas3/v2/premises/${premises.id}/cancel-archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }
}
