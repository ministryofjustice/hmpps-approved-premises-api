package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3UnarchivePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3v2PremisesArchiveTest : Cas3IntegrationTestBase() {

  @Nested
  inner class CanArchivePremises {
    @BeforeEach
    fun setup() {
      clock.setNow(Instant.parse("2025-08-26T00:00:00Z"))
    }

    @Test
    fun `Can archive premises returns 200 when no blocking bookings or voids exist`() {
      givenACas3PremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt, premises ->
        webTestClient.get()
          .uri("/cas3/v2/premises/${premises.id}/can-archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.items").isArray
          .jsonPath("$.items").isEmpty
      }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.v2.Cas3v2PremisesArchiveTest#getCanArchivePremisesBookingsByStatusCases")
    fun `Can archive premises returns 200 when bookings have departure date after 3 months`(args: Pair<LocalDate, Cas3BookingStatus>) {
      val (departureDate, status) = args
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenACas3PremisesWithBedspaces(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now().minusDays(100),
          ),
        ) { premises, bedspaces ->
          val bedspace = bedspaces.first()

          createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now().minusDays(1),
            departureDate = departureDate,
            status = status,
          )

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/can-archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items.length()").isEqualTo(1)
            .jsonPath("$.items[0].entityId").isEqualTo(bedspace.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bedspace.reference)
            .jsonPath("$.items[0].date").isEqualTo(departureDate)
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 when voids have end dates after 3 months`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenACas3PremisesWithBedspaces(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now().minusDays(100),
          ),
        ) { premises, bedspaces ->
          val bedspace = bedspaces.first()
          val futureEndDate = LocalDate.now(clock).plusMonths(4)

          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBedspace(bedspace)
            withStartDate(LocalDate.now(clock).plusDays(1))
            withEndDate(futureEndDate)
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/can-archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items.length()").isEqualTo(1)
            .jsonPath("$.items[0].entityId").isEqualTo(bedspace.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bedspace.reference)
            .jsonPath("$.items[0].date").isEqualTo(futureEndDate)
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 when there are booking and void after 3 months will return the bedspace with the latest blocking date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenACas3PremisesWithBedspaces(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now().minusDays(100),
          ),
        ) { premises, bedspaces ->
          val bedspace = bedspaces.first()
          val futureVoidEndDate = LocalDate.now(clock).plusMonths(4)
          val latestBlockingDate = futureVoidEndDate.plusWeeks(1)

          createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now(clock).minusDays(1),
            departureDate = latestBlockingDate,
          )

          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBedspace(bedspace)
            withStartDate(LocalDate.now(clock).plusDays(1))
            withEndDate(futureVoidEndDate)
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/can-archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items.length()").isEqualTo(1)
            .jsonPath("$.items[0].entityId").isEqualTo(bedspace.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bedspace.reference)
            .jsonPath("$.items[0].date").isEqualTo(latestBlockingDate)
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 when bookings have departure dates within 3 months`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenACas3PremisesWithBedspaces(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now().minusDays(100),
          ),
        ) { premises, bedspaces ->
          val bedspace = bedspaces.first()
          val nearFutureDepartureDate = LocalDate.now(clock).plusMonths(2) // Within 3 months

          createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now(clock).minusDays(1),
            departureDate = nearFutureDepartureDate,
          )

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/can-archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items").isEmpty
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 when voids have end dates within 3 months`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenACas3PremisesWithBedspaces(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now().minusDays(100),
          ),
        ) { premises, bedspaces ->
          val bedspace = bedspaces.first()
          val nearFutureEndDate = LocalDate.now(clock).plusMonths(2)

          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBedspace(bedspace)
            withStartDate(LocalDate.now(clock).plusDays(1))
            withEndDate(nearFutureEndDate)
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/can-archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items").isEmpty
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 with multiple affected bedspaces when multiple bedspaces have blocking bookings or voids`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenACas3PremisesWithBedspaces(
          region = userEntity.probationRegion,
          bedspaceCount = 2,
          bedspacesStartDates = listOf(
            LocalDate.now().minusDays(100),
            LocalDate.now().minusDays(100),
          ),
        ) { premises, bedspaces ->
          val bedspace1 = bedspaces[0]
          val bedspace2 = bedspaces[1]
          val futureDepartureDate = LocalDate.now(clock).plusMonths(4)
          val futureVoidEndDate = LocalDate.now(clock).plusMonths(5)

          createBooking(
            premises = premises,
            bedspace = bedspace1,
            arrivalDate = LocalDate.now(clock).plusDays(1),
            departureDate = futureDepartureDate,
          )

          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBedspace(bedspace2)
            withStartDate(LocalDate.now(clock).plusDays(1))
            withEndDate(futureVoidEndDate)
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/can-archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items.length()").isEqualTo(2)
            .jsonPath("$.items[0].entityId").isEqualTo(bedspace1.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bedspace1.reference)
            .jsonPath("$.items[0].date").isEqualTo(futureDepartureDate)
            .jsonPath("$.items[1].entityId").isEqualTo(bedspace2.id.toString())
            .jsonPath("$.items[1].entityReference").isEqualTo(bedspace2.reference)
            .jsonPath("$.items[1].date").isEqualTo(futureVoidEndDate)
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 when bookings are cancelled`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenACas3PremisesWithBedspaces(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now().minusDays(100),
          ),
        ) { premises, bedspaces ->
          val bedspace = bedspaces.first()
          val futureDepartureDate = LocalDate.now(clock).plusMonths(4)

          val booking = createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now(clock).plusDays(1),
            departureDate = futureDepartureDate,
          )

          cas3CancellationEntityFactory.produceAndPersist {
            withBooking(booking)
            withReason(cancellationReasonEntityFactory.produceAndPersist())
          }

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/can-archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items").isEmpty
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 when void is cancelled`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenACas3PremisesWithBedspaces(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now().minusDays(100),
          ),
        ) { premises, bedspaces ->
          val bedspace = bedspaces.first()
          val futureDepartureDate = LocalDate.now(clock).plusMonths(4)

          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBedspace(bedspace)
            withStartDate(LocalDate.now(clock).plusDays(1))
            withEndDate(futureDepartureDate)
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
            withCancellationNotes(randomStringMultiCaseWithNumbers(50))
            withCancellationDate(OffsetDateTime.now())
          }

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/can-archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items").isEmpty
        }
      }
    }

    @Test
    fun `Can archive premises returns 404 when premises does not exist`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val nonExistentPremisesId = UUID.randomUUID()

        webTestClient.get()
          .uri("/cas3/v2/premises/$nonExistentPremisesId/can-archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Can archive premises returns 403 when user does not have permission to view premises in that region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val premises = givenACas3Premises()
        webTestClient.get()
          .uri("/cas3/v2/premises/${premises.id}/can-archive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Can archive premises returns 200 with edge case exactly 3 months minus one day`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenACas3PremisesWithBedspaces(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now().minusDays(100),
          ),
        ) { premises, bedspaces ->
          val bedspace = bedspaces.first()
          val exactlyThreeMonthsDate = LocalDate.now(clock).plusMonths(3).minusDays(1)

          createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now(clock).plusDays(1),
            departureDate = exactlyThreeMonthsDate,
          )

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/can-archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items").isEmpty
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 with edge case, turnaround time exceeds 3 months limit`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenACas3PremisesWithBedspaces(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now().minusDays(100),
          ),
        ) { premises, bedspaces ->
          val bedspace = bedspaces.first()
          val justUnder3Months = LocalDate.now(clock).plusMonths(3).minusDays(2)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val booking = createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now(clock).plusDays(1),
            departureDate = justUnder3Months,
          )

          cas3v2TurnaroundFactory.produceAndPersist {
            withWorkingDayCount(3)
            withBooking(booking)
          }

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/can-archive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items.length()").isEqualTo(1)
            .jsonPath("$.items[0].entityId").isEqualTo(bedspace.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bedspace.reference)
            .jsonPath("$.items[0].date").isEqualTo(justUnder3Months.plusDays(3))
        }
      }
    }
  }

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
            .jsonPath("$.invalid-params[0].value").isEqualTo(upcomingBedspace.startDate.plusDays(1).toString())
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

  @Nested
  inner class CancelScheduledUnarchivePremises {
    @BeforeEach
    fun setup() {
      clock.setNow(Instant.parse("2025-08-27T15:21:34Z"))
    }

    @Test
    fun `Cancel scheduled unarchive premises returns 200 OK when successful`() {
      val previousStartDate = LocalDate.now(clock).minusDays(60)
      val previousEndDate = previousStartDate.plusDays(30)
      val newStartDate = LocalDate.now(clock).plusDays(5)
      givenACas3PremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = previousStartDate,
        premisesEndDate = previousEndDate,
        premisesStatus = Cas3PremisesStatus.archived,
      ) { userEntity, jwt, premises ->
        premises.createdAt = OffsetDateTime.now(clock).minusDays(60)
        cas3PremisesRepository.saveAndFlush(premises)

        val bedspace = V2().createBedspaceInPremises(premises, previousStartDate, previousEndDate)

        // previous unarchive domain events
        createCas3PremisesUnarchiveDomainEvent(
          premises,
          userEntity,
          previousEndDate.minusDays(180),
          previousEndDate.plusDays(15),
          previousStartDate.minusDays(30),
        )

        val lastPremisesUnarchiveDomainEvent = createCas3PremisesUnarchiveDomainEvent(
          premises,
          userEntity,
          previousStartDate,
          newStartDate,
          previousEndDate,
        )

        createBedspaceUnarchiveDomainEvent(
          bedspace,
          premises.id,
          userEntity.id,
          previousStartDate.minusDays(40),
        )

        val lastBedspaceUnarchiveDomainEvent = createBedspaceUnarchiveDomainEvent(
          bedspace,
          premises.id,
          userEntity.id,
          newStartDate,
        )

        premises.startDate = newStartDate
        premises.endDate = null
        premises.status = Cas3PremisesStatus.online
        cas3PremisesRepository.save(premises)

        val updatedBedspace = bedspace.copy(
          startDate = newStartDate,
          endDate = null,
        )
        cas3BedspacesRepository.save(updatedBedspace)

        webTestClient.put()
          .uri("/cas3/v2/premises/${premises.id}/cancel-unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(premises.id.toString())
          .jsonPath("startDate").isEqualTo(previousStartDate)
          .jsonPath("endDate").isEqualTo(previousEndDate)

        // Verify that premise was updated
        val updatedPremises = cas3PremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises.status).isEqualTo(Cas3PremisesStatus.archived)
        assertThat(updatedPremises.startDate).isEqualTo(premises.createdAt.toLocalDate())
        assertThat(updatedPremises.endDate).isEqualTo(previousEndDate)

        val updatedPremisesUnarchiveDomainEvent =
          domainEventRepository.findByIdOrNull(lastPremisesUnarchiveDomainEvent.id)
        assertThat(updatedPremisesUnarchiveDomainEvent).isNotNull()
        assertThat(updatedPremisesUnarchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))

        // Verify that bedspace was updated
        val updatedBed = cas3BedspacesRepository.findById(bedspace.id).get()
        assertThat(updatedBed.startDate).isEqualTo(previousStartDate)
        assertThat(updatedBed.endDate).isEqualTo(previousEndDate)

        val updatedBedspaceUnarchiveDomainEvent =
          domainEventRepository.findByIdOrNull(lastBedspaceUnarchiveDomainEvent.id)
        assertThat(updatedBedspaceUnarchiveDomainEvent).isNotNull()
        assertThat(updatedBedspaceUnarchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))
      }
    }

    @Test
    fun `Cancel scheduled unarchive premises returns 404 when premises is not found`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val id = UUID.randomUUID()

        webTestClient.put()
          .uri("/cas3/v2/premises/$id/cancel-unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("detail").isEqualTo("No Cas3Premises with an ID of $id could be found")
      }
    }

    @Test
    fun `Cancel unarchive premises returns 403 when user is not authorized`() {
      givenAUser { _, jwt ->
        val premises = givenACas3Premises()

        webTestClient.put()
          .uri("/cas3/v2/premises/${premises.id}/cancel-unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Nested
  inner class UnarchivePremises {
    @Test
    fun `Unarchive premises returns 200 OK when successful`() {
      givenACas3PremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = LocalDate.now().minusDays(30),
        premisesEndDate = LocalDate.now().minusDays(1),
        premisesStatus = Cas3PremisesStatus.archived,
        bedspaceCount = 3,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(30),
          LocalDate.now().minusDays(20),
          LocalDate.now().minusDays(74),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now().minusDays(1),
          LocalDate.now().minusDays(5),
          LocalDate.now().minusDays(19),
        ),
      ) { user, jwt, premises, bedspaces ->
        val cas3UnarchivePremises = Cas3UnarchivePremises(LocalDate.now().plusDays(1))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(cas3UnarchivePremises)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(premises.id.toString())
          .jsonPath("status").isEqualTo("archived")

        val updatedPremises = cas3PremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises.status).isEqualTo(Cas3PremisesStatus.online)
        assertThat(updatedPremises.startDate).isEqualTo(cas3UnarchivePremises.restartDate)
        assertThat(updatedPremises.endDate).isNull()

        val updatedBedspaces = cas3BedspacesRepository.findAll()
        assertThat(updatedBedspaces).hasSize(3)
        updatedBedspaces.forEach { bedspace ->
          assertThat(bedspace.startDate).isEqualTo(cas3UnarchivePremises.restartDate)
          assertThat(bedspace.endDate).isNull()
        }

        val premisesDomainEvents = domainEventRepository.findByCas3PremisesIdAndType(premises.id, DomainEventType.CAS3_PREMISES_UNARCHIVED)
        assertThat(premisesDomainEvents).hasSize(1)

        val bedspaceOneDomainEvents = domainEventRepository.findByCas3BedspaceId(bedspaces[0].id)
        assertThat(bedspaceOneDomainEvents).hasSize(1)
        assertThat(bedspaceOneDomainEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)

        val bedspaceTwoDomainEvents = domainEventRepository.findByCas3BedspaceId(bedspaces[1].id)
        assertThat(bedspaceTwoDomainEvents).hasSize(1)
        assertThat(bedspaceTwoDomainEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)

        val bedspaceThreeDomainEvents = domainEventRepository.findByCas3BedspaceId(bedspaces[2].id)
        assertThat(bedspaceThreeDomainEvents).hasSize(1)
        assertThat(bedspaceThreeDomainEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)
      }
    }

    @Test
    fun `Given unarchive premises with duplicated bedspace reference returns 200 OK and unarchive the lastest created bedspace with the same reference`() {
      givenACas3PremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = LocalDate.now().minusDays(180),
        premisesEndDate = LocalDate.now().minusDays(1),
        premisesStatus = Cas3PremisesStatus.archived,
        bedspaceCount = 3,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(130),
          LocalDate.now().minusDays(20),
          LocalDate.now().minusDays(80),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now().minusDays(11),
          LocalDate.now().minusDays(21),
          LocalDate.now().minusDays(1),
        ),
      ) { user, jwt, premises, bedspaces ->
        val cas3UnarchivePremises = Cas3UnarchivePremises(LocalDate.now().plusDays(5))
        val bedspaceThree = bedspaces.drop(2).first()

        val duplicatedBedspaceThree = cas3BedspaceEntityFactory.produceAndPersist {
          withStartDate(LocalDate.now().minusDays(120))
          withEndDate(LocalDate.now().minusDays(90))
          withReference(bedspaceThree.reference)
          withPremises(bedspaceThree.premises)
        }

        duplicatedBedspaceThree.createdAt = OffsetDateTime.now().minusDays(120)
        cas3BedspacesRepository.save(duplicatedBedspaceThree)

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(cas3UnarchivePremises)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(premises.id.toString())
          .jsonPath("status").isEqualTo("archived")

        val updatedPremises = cas3PremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises.status).isEqualTo(Cas3PremisesStatus.online)
        assertThat(updatedPremises.startDate).isEqualTo(cas3UnarchivePremises.restartDate)
        assertThat(updatedPremises.endDate).isNull()

        val premisesDomainEvents = domainEventRepository.findByCas3PremisesIdAndType(premises.id, DomainEventType.CAS3_PREMISES_UNARCHIVED)
        assertThat(premisesDomainEvents).hasSize(1)

        val allBedspaces = cas3BedspacesRepository.findAll()
        val updateBedspace = allBedspaces.filter { it.startDate == cas3UnarchivePremises.restartDate }
        assertThat(updateBedspace).hasSize(3)
        assertThat(updateBedspace.map { it.id }).contains(bedspaceThree.id)
        assertThat(updateBedspace.map { it.id }).doesNotContain(duplicatedBedspaceThree.id)

        val bedspaceOneDomainEvents = domainEventRepository.findByCas3BedspaceId(bedspaces[0].id)
        assertThat(bedspaceOneDomainEvents).hasSize(1)
        assertThat(bedspaceOneDomainEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)

        val bedspaceTwoDomainEvents = domainEventRepository.findByCas3BedspaceId(bedspaces[1].id)
        assertThat(bedspaceTwoDomainEvents).hasSize(1)
        assertThat(bedspaceTwoDomainEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)

        val bedspaceThreeDomainEvents = domainEventRepository.findByCas3BedspaceId(bedspaces[2].id)
        assertThat(bedspaceThreeDomainEvents).hasSize(1)
        assertThat(bedspaceThreeDomainEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)

        val duplicatedBedspaceThreeDomainEvents = domainEventRepository.findByCas3BedspaceId(duplicatedBedspaceThree.id)
        assertThat(duplicatedBedspaceThreeDomainEvents).hasSize(0)
      }
    }

    @Test
    fun `Unarchive premises returns 400 when restart date is too far in the past`() {
      givenACas3PremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = LocalDate.now().minusDays(30),
        premisesEndDate = LocalDate.now().minusDays(10),
        premisesStatus = Cas3PremisesStatus.archived,
      ) { _, jwt, premises ->

        val cas3UnarchivePremises = Cas3UnarchivePremises(LocalDate.now().minusDays(8))
        val restartDate = LocalDate.now()

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(cas3UnarchivePremises)
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
    fun `Unarchive premises returns 400 when restart date is too far in the future`() {
      givenACas3PremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = LocalDate.now().minusDays(30),
        premisesEndDate = LocalDate.now().minusDays(1),
        premisesStatus = Cas3PremisesStatus.archived,
      ) { _, jwt, premises ->

        val cas3UnarchivePremises = Cas3UnarchivePremises(LocalDate.now().plusDays(9))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(cas3UnarchivePremises)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.restartDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("invalidRestartDateInTheFuture")
      }
    }

    @Test
    fun `Unarchive premises returns 400 when restart date is before last archive end date`() {
      val lastArchiveEndDate = LocalDate.now().minusDays(3)
      givenACas3PremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = LocalDate.now().minusDays(30),
        premisesEndDate = lastArchiveEndDate,
        premisesStatus = Cas3PremisesStatus.archived,
      ) { _, jwt, premises ->

        val cas3UnarchivePremises = Cas3UnarchivePremises(LocalDate.now().minusDays(6))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(cas3UnarchivePremises)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.restartDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("beforeLastPremisesArchivedDate")
      }
    }

    @Test
    fun `Unarchive premises returns 404 when premises does not exist`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val nonExistentPremisesId = UUID.randomUUID()
        val cas3UnarchivePremises = Cas3UnarchivePremises(LocalDate.now().plusDays(4))

        webTestClient.post()
          .uri("/cas3/v2/premises/$nonExistentPremisesId/unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(cas3UnarchivePremises)
          .exchange()
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("$.title").isEqualTo("Not Found")
          .jsonPath("$.detail").isEqualTo("No Cas3Premises with an ID of $nonExistentPremisesId could be found")
      }
    }

    @Test
    fun `Unarchive premises returns 400 when premises is not archived`() {
      givenACas3PremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStatus = Cas3PremisesStatus.online,
      ) { _, jwt, premises ->

        val cas3UnarchivePremises = Cas3UnarchivePremises(LocalDate.now().plusDays(3))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/unarchive")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(cas3UnarchivePremises)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.premisesId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("premisesNotArchived")
      }
    }

    @Test
    fun `Unarchive premises returns 403 when user does not have permission to manage premises`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        givenACas3Premises(
          startDate = LocalDate.now().minusDays(30),
          endDate = LocalDate.now().minusDays(1),
          status = Cas3PremisesStatus.archived,
        ) { premises ->

          val cas3UnarchivePremises = Cas3UnarchivePremises(LocalDate.now().plusDays(2))

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/unarchive")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(cas3UnarchivePremises)
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }
  }

  private fun createBooking(
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

  private companion object {
    @JvmStatic
    fun getCanArchivePremisesBookingsByStatusCases() = listOf(
      Pair(LocalDate.now().plusMonths(4), Cas3BookingStatus.provisional),
      Pair(LocalDate.now().plusMonths(4), Cas3BookingStatus.arrived),
      Pair(LocalDate.now().plusMonths(5), Cas3BookingStatus.confirmed),
    )
  }
}
