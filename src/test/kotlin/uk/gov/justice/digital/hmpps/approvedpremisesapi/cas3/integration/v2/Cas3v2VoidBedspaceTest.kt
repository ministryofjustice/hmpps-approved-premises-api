package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.Cas3IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspaceCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspaceRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3VoidBedspacesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenCas3PremisesAndBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.withConflictMessage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.withNotFoundMessage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3v2VoidBedspaceTest : Cas3IntegrationTestBase() {

  @Autowired
  private lateinit var cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository

  @Autowired
  lateinit var cas3VoidBedspacesTransformer: Cas3VoidBedspacesTransformer

  @Nested
  inner class GetVoidBedspace {

    fun doGetRequest(jwt: String, premisesId: UUID, bedspaceId: UUID, voidBedspaceId: UUID) = webTestClient.get()
      .uri("/cas3/v2/premises/$premisesId/bedspaces/$bedspaceId/void-bedspaces/$voidBedspaceId")
      .headers(buildTemporaryAccommodationHeaders(jwt))
      .exchange()

    @Test
    fun `Get Void Bedspace for non-existent premises returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        doGetRequest(jwt, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Get Void Bedspace for non-existent void bedspace returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        doGetRequest(jwt, premises.id, UUID.randomUUID(), UUID.randomUUID())
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Get Void Bedspace returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        val voidBedspace = createVoidBedspaces(premises).first()

        val result = doGetRequest(jwt, premises.id, voidBedspace.bedspace!!.id, voidBedspace.id)
          .expectStatus().isOk
          .expectBody(Cas3VoidBedspace::class.java)
          .returnResult().responseBody!!

        assertAll({
          assertThat(result.id).isEqualTo(voidBedspace.id)
          assertThat(result.bedspaceId).isEqualTo(voidBedspace.bedspace!!.id)
          assertThat(result.bedspaceName).isEqualTo(voidBedspace.bedspace!!.reference)
          assertThat(result.startDate).isEqualTo(voidBedspace.startDate)
          assertThat(result.endDate).isEqualTo(voidBedspace.endDate)
          assertThat(result.reason.id).isEqualTo(voidBedspace.reason.id)
          assertThat(result.referenceNumber).isEqualTo(voidBedspace.referenceNumber)
          assertThat(result.notes).isEqualTo(voidBedspace.notes)
          assertThat(result.cancellationDate).isNull()
          assertThat(result.cancellationNotes).isNull()
        })
      }
    }

    @Test
    fun `Get Void Bedspace that's not in the user's region returns 403 Forbidden`() {
      givenAUser { user, jwt ->
        val otherRegion = givenAProbationRegion()
        val otherPremises = givenACas3Premises(otherRegion)
        val voidBedspace = createVoidBedspaces(otherPremises).first()
        doGetRequest(jwt, otherPremises.id, voidBedspace.bedspace!!.id, voidBedspace.id)
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get Void Bedspace returns cancelled void bedspace`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        val voidBedspace = createVoidBedspaces(premises).first()
        voidBedspace.cancellationDate = OffsetDateTime.now().minusDays(1)
        cas3VoidBedspacesRepository.save(voidBedspace)

        val result = doGetRequest(jwt, premises.id, voidBedspace.bedspace!!.id, voidBedspace.id)
          .expectStatus().isOk
          .expectBody(Cas3VoidBedspace::class.java)
          .returnResult().responseBody!!

        assertAll({
          assertThat(result.id).isEqualTo(voidBedspace.id)
          assertThat(result.bedspaceId).isEqualTo(voidBedspace.bedspace!!.id)
          assertThat(result.bedspaceName).isEqualTo(voidBedspace.bedspace!!.reference)
          assertThat(result.startDate).isEqualTo(voidBedspace.startDate)
          assertThat(result.endDate).isEqualTo(voidBedspace.endDate)
          assertThat(result.reason.id).isEqualTo(voidBedspace.reason.id)
          assertThat(result.referenceNumber).isEqualTo(voidBedspace.referenceNumber)
          assertThat(result.notes).isEqualTo(voidBedspace.notes)
          assertThat(result.cancellationDate).isEqualTo(LocalDate.now().minusDays(1))
          assertThat(result.cancellationNotes).isNull()
        })
      }
    }
  }

  @Nested
  inner class GetVoidBedspaces {
    fun doGetRequest(jwt: String, premisesId: UUID) = webTestClient.get()
      .uri("/cas3/v2/premises/$premisesId/bedspaces/void-bedspaces")
      .headers(buildTemporaryAccommodationHeaders(jwt))
      .exchange()

    @Test
    fun `User without CAS_ASSESSOR role is forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        doGetRequest(jwt, premises.id)
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get Void Bedspaces for non existent Premises returns 404`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()
      doGetRequest(jwt, UUID.randomUUID()).expectStatus().isNotFound
    }

    @Test
    fun `Get Void Bedspaces returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        val voidBedspaces = createVoidBedspaces(premises)
        val cancelledVoidBedspaces = createCancelledVoidBedspaces(premises)

        val result = doGetRequest(jwt, premises.id)
          .expectStatus()
          .isOk
          .expectBodyList(Cas3VoidBedspace::class.java)
          .returnResult()
          .responseBody!!

        assertAll({
          assertThat(result).hasSize(3)
          assertThat(result.map { it.id }).containsExactlyInAnyOrderElementsOf(voidBedspaces.map { it.id })
          assertThat(result.map { it.id }).doesNotContainAnyElementsOf(cancelledVoidBedspaces.map { it.id })
        })
      }
    }
  }

  @Nested
  inner class CreateVoidBedspace {
    fun doPostRequest(jwt: String, premisesId: UUID, bedspaceId: UUID, voidBedspace: Cas3VoidBedspaceRequest) = webTestClient.post()
      .uri("/cas3/v2/premises/$premisesId/bedspaces/$bedspaceId/void-bedspaces")
      .headers(buildTemporaryAccommodationHeaders(jwt))
      .bodyValue(voidBedspace)
      .exchange()

    @Test
    fun `Create Void Bedspace returns 201 when successfully created`() {
      val (user, jwt) = givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR))
      val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
      val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist()
      val voidBedspace = buildVoidBedspace(reason.id)
      val result = doPostRequest(jwt, premises.id, bedspace.id, voidBedspace)
        .expectStatus()
        .isCreated
        .expectBody(Cas3VoidBedspace::class.java)
        .returnResult().responseBody!!

      assertAll({
        assertThat(result.id).isNotNull
        assertThat(result.bedspaceId).isEqualTo(bedspace.id)
        assertThat(result.bedspaceName).isEqualTo(bedspace.reference)
        assertThat(result.startDate).isEqualTo(voidBedspace.startDate)
        assertThat(result.endDate).isEqualTo(voidBedspace.endDate)
        assertThat(result.reason.id).isEqualTo(voidBedspace.reasonId)
        assertThat(result.referenceNumber).isEqualTo(voidBedspace.referenceNumber)
        assertThat(result.notes).isEqualTo(voidBedspace.notes)
        assertThat(result.cancellationDate).isNull()
        assertThat(result.cancellationNotes).isNull()
      })
    }

    @Test
    fun `Create Void Bedspace returns 404 Bad Request if the bedspace ID does not reference a bedspace on the premises`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist()
        val invalidBedspaceId = UUID.randomUUID()
        val voidBedspace = buildVoidBedspace(reason.id)
        doPostRequest(jwt, premises.id, invalidBedspaceId, voidBedspace)
          .expectStatus()
          .isNotFound
          .withNotFoundMessage("No Cas3Bedspace with an ID of $invalidBedspaceId could be found")
      }
    }

    @Test
    fun `Create Void Bedspace that's not in the user's region returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val otherRegionPremises = givenACas3Premises()
        val (_, bedspace) = givenCas3PremisesAndBedspace(user, premises = otherRegionPremises)
        val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist()

        val voidBedspace = buildVoidBedspace(reason.id)

        doPostRequest(jwt, otherRegionPremises.id, bedspace.id, voidBedspace)
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Create Void Bedspace returns 409 Conflict when a booking for the same bedspace has overlapping dates`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
        val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist()

        val arrivalDate = LocalDate.now().plusDays(5)
        val startDate = LocalDate.now().plusDays(10)
        val departureDate = LocalDate.now().plusDays(15)
        val endDate = LocalDate.now().plusDays(20)

        val existingBooking = cas3BookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn("CRN123")
          withYieldedPremises { premises }
          withBedspace(bedspace)
          withArrivalDate(arrivalDate)
          withDepartureDate(departureDate)
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val voidBedspace = buildVoidBedspace(reason.id, startDate = startDate, endDate = endDate)

        doPostRequest(jwt, premises.id, bedspace.id, voidBedspace)
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("A Booking already exists for dates from $arrivalDate to $departureDate which overlaps with the desired dates: ${existingBooking.id}")
      }
    }

    @Test
    fun `Create Void Bedspace returns OK with correct body when any bookings with overlapping dates are cancelled`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
        val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist()

        val arrivalDate = LocalDate.now().plusDays(5)
        val departureDate = LocalDate.now().plusDays(15)

        val existingBooking = cas3BookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn("CRN123")
          withYieldedPremises { premises }
          withBedspace(bedspace)
          withArrivalDate(arrivalDate)
          withDepartureDate(departureDate)
        }

        existingBooking.cancellations.add(
          cas3CancellationEntityFactory.produceAndPersist {
            withYieldedBooking { existingBooking }
            withDate(LocalDate.parse("2022-07-01"))
            withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          },
        )

        val voidBedspace = buildVoidBedspace(reason.id)
        val result = doPostRequest(jwt, premises.id, bedspace.id, voidBedspace)
          .expectStatus()
          .isCreated
          .expectBody(Cas3VoidBedspace::class.java)
          .returnResult().responseBody!!

        assertAll({
          assertThat(result.id).isNotNull
          assertThat(result.bedspaceId).isEqualTo(bedspace.id)
          assertThat(result.bedspaceName).isEqualTo(bedspace.reference)
          assertThat(result.startDate).isEqualTo(voidBedspace.startDate)
          assertThat(result.endDate).isEqualTo(voidBedspace.endDate)
          assertThat(result.reason.id).isEqualTo(voidBedspace.reasonId)
          assertThat(result.referenceNumber).isEqualTo(voidBedspace.referenceNumber)
          assertThat(result.notes).isEqualTo(voidBedspace.notes)
          assertThat(result.cancellationDate).isNull()
          assertThat(result.cancellationNotes).isNull()
        })
      }
    }

    @Test
    fun `Create Void Bedspace returns 409 Conflict when a void bedspace for the same bedspace has overlapping dates`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
        val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist()

        val startDate = LocalDate.now().plusDays(5)
        val newStartDate = LocalDate.now().plusDays(10)
        val endDate = LocalDate.now().plusDays(15)
        val newEndDate = LocalDate.now().plusDays(20)

        val existingVoidBedspace = cas3VoidBedspaceEntityFactory.produceAndPersist {
          withBedspace(bedspace)
          withStartDate(startDate)
          withEndDate(endDate)
          withYieldedReason { reason }
        }

        val voidBedspace = buildVoidBedspace(reason.id, newStartDate, newEndDate)
        doPostRequest(jwt, premises.id, bedspace.id, voidBedspace)
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("A Void Bedspace already exists for dates from $startDate to $endDate which overlaps with the desired dates: ${existingVoidBedspace.id}")
      }
    }

    @Test
    fun `Create Void Bedspace returns OK with correct body when any existing Void Bedspaces with overlapping dates are cancelled`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        val bedspaceWithCancelledVoidBedspace = createCancelledVoidBedspaces(premises).first().bedspace
        val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist()

        val voidBedspace = buildVoidBedspace(
          reason.id,
          startDate = bedspaceWithCancelledVoidBedspace!!.startDate!!.plusDays(1),
          endDate = bedspaceWithCancelledVoidBedspace.endDate!!.minusDays(1),
        )
        val result = doPostRequest(jwt, premises.id, bedspaceWithCancelledVoidBedspace.id, voidBedspace)
          .expectStatus()
          .isCreated
          .expectBody(Cas3VoidBedspace::class.java)
          .returnResult().responseBody!!

        assertAll({
          assertThat(result.id).isNotNull
          assertThat(result.bedspaceId).isEqualTo(bedspaceWithCancelledVoidBedspace.id)
          assertThat(result.bedspaceName).isEqualTo(bedspaceWithCancelledVoidBedspace.reference)
          assertThat(result.startDate).isEqualTo(voidBedspace.startDate)
          assertThat(result.endDate).isEqualTo(voidBedspace.endDate)
          assertThat(result.reason.id).isEqualTo(voidBedspace.reasonId)
          assertThat(result.referenceNumber).isEqualTo(voidBedspace.referenceNumber)
          assertThat(result.notes).isEqualTo(voidBedspace.notes)
          assertThat(result.cancellationDate).isNull()
          assertThat(result.cancellationNotes).isNull()
        })
      }
    }
  }

  @Nested
  inner class UpdateVoidBedspace {

    fun buildUpdateVoidBedspace(voidBedspace: Cas3VoidBedspaceEntity) = Cas3VoidBedspaceRequest(
      startDate = voidBedspace.startDate.plusDays(1),
      endDate = voidBedspace.endDate.plusDays(2),
      reasonId = voidBedspace.reason.id,
      referenceNumber = voidBedspace.referenceNumber,
      notes = voidBedspace.notes,
    )

    fun doPutRequest(
      jwt: String,
      premisesId: UUID,
      bedspaceId: UUID,
      voidBedspaceId: UUID,
      voidBedspace: Cas3VoidBedspaceRequest,
    ) = webTestClient.put()
      .uri("/cas3/v2/premises/$premisesId/bedspaces/$bedspaceId/void-bedspaces/$voidBedspaceId")
      .headers(buildTemporaryAccommodationHeaders(jwt))
      .bodyValue(voidBedspace)
      .exchange()

    @Test
    fun `Update Void Bedspace for non-existent premises returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        val voidBedspaceEntity = createVoidBedspaces(premises).first()
        val updateVoidBedspace = buildUpdateVoidBedspace(voidBedspaceEntity)

        doPutRequest(
          jwt,
          UUID.randomUUID(),
          voidBedspaceEntity.bedspace!!.id,
          voidBedspaceEntity.id,
          updateVoidBedspace,
        )
          .expectStatus()
          .isNotFound
          .withNotFoundMessage("No Cas3VoidBedspace with an ID of ${voidBedspaceEntity.id} could be found")
      }
    }

    @Test
    fun `Update Void Bedspace for non-existent bedspace returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        val voidBedspaceEntity = createVoidBedspaces(premises).first()
        val updateVoidBedspace = buildUpdateVoidBedspace(voidBedspaceEntity)

        doPutRequest(jwt, premises.id, UUID.randomUUID(), voidBedspaceEntity.id, updateVoidBedspace)
          .expectStatus()
          .isNotFound
          .withNotFoundMessage("No Cas3VoidBedspace with an ID of ${voidBedspaceEntity.id} could be found")
      }
    }

    @Test
    fun `Update Void Bedspace for non-existent void bedspace returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        val voidBedspaceEntity = createVoidBedspaces(premises).first()
        val updateVoidBedspace = buildUpdateVoidBedspace(voidBedspaceEntity)

        val invalidId = UUID.randomUUID()
        doPutRequest(jwt, premises.id, voidBedspaceEntity.bedspace!!.id, invalidId, updateVoidBedspace)
          .expectStatus()
          .isNotFound
          .withNotFoundMessage("No Cas3VoidBedspace with an ID of $invalidId could be found")
      }
    }

    @Test
    fun `Update Void Bedspace returns OK with correct body when correct data is provided, and does not conflict with canceled bookings or void bedspaces`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
        val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist()
        val existingVoidBedspace = cas3VoidBedspaceEntityFactory.produceAndPersist {
          withBedspace(bedspace)
          withStartDate(LocalDate.now())
          withEndDate(LocalDate.now().plusDays(2))
          withYieldedReason { reason }
        }

        val nonConflictingStartDate = LocalDate.now().plusDays(10)
        val nonConflictingEndDate = LocalDate.now().plusDays(12)

        // add a canceled booking - should pass validation
        val existingBooking = cas3BookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn("CRN")
          withYieldedPremises { premises }
          withBedspace(existingVoidBedspace.bedspace!!)
          withArrivalDate(nonConflictingStartDate)
          withDepartureDate(nonConflictingStartDate)
        }

        existingBooking.cancellations = mutableListOf(
          cas3CancellationEntityFactory.produceAndPersist {
            withYieldedBooking { existingBooking }
            withDate(LocalDate.now())
            withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          },
        )

        // add a canceled void bedspace - should pass validation
        cas3VoidBedspaceEntityFactory.produceAndPersist {
          withBedspace(bedspace)
          withStartDate(nonConflictingStartDate)
          withEndDate(nonConflictingEndDate)
          withYieldedReason { reason }
          withCancellationDate(OffsetDateTime.now())
        }

        val updatedReason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist()
        val updatedReferenceNumer = "updated reference number"
        val updatedNotes = "updatednotes"

        val updateVoidBedspace = Cas3VoidBedspaceRequest(
          startDate = nonConflictingStartDate,
          endDate = nonConflictingEndDate,
          reasonId = updatedReason.id,
          referenceNumber = updatedReferenceNumer,
          notes = updatedNotes,
        )

        val result =
          doPutRequest(jwt, premises.id, bedspace.id, existingVoidBedspace.id, updateVoidBedspace)
            .expectStatus()
            .isOk
            .expectBody(Cas3VoidBedspace::class.java)
            .returnResult().responseBody!!

        assertAll({
          assertThat(result.id).isEqualTo(existingVoidBedspace.id)
          assertThat(result.bedspaceId).isEqualTo(bedspace.id)
          assertThat(result.startDate).isEqualTo(nonConflictingStartDate)
          assertThat(result.endDate).isEqualTo(nonConflictingEndDate)
          assertThat(result.reason.id).isEqualTo(updatedReason.id)
          assertThat(result.referenceNumber).isEqualTo(updatedReferenceNumer)
          assertThat(result.notes).isEqualTo(updatedNotes)
          assertThat(result.cancellationDate).isNull()
          assertThat(result.cancellationNotes).isNull()
        })

        val updatedEntity =
          cas3VoidBedspacesRepository.findVoidBedspace(premises.id, bedspace.id, existingVoidBedspace.id)!!
        assertThat(result).isEqualTo(cas3VoidBedspacesTransformer.toCas3VoidBedspace(updatedEntity))
      }
    }

    @Test
    fun `Update Void Bedspace where premises is not in the user's region returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val otherRegion = givenAProbationRegion()
        val otherPremises = givenACas3Premises(otherRegion)

        val voidBedspaceEntity = createVoidBedspaces(otherPremises).first()
        val voidBedspace = buildUpdateVoidBedspace(voidBedspaceEntity)
        doPutRequest(jwt, otherPremises.id, voidBedspaceEntity.bedspace!!.id, voidBedspaceEntity.id, voidBedspace)
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Update Void Bedspaces returns 409 Conflict when a booking for the same bedspace overlaps`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val premises = givenACas3Premises(user.probationRegion)
        val voidBedspaceEntity = createVoidBedspaces(premises).first()

        val existingBooking = cas3BookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn("CRN123")
          withYieldedPremises { premises }
          withBedspace(voidBedspaceEntity.bedspace!!)
          withArrivalDate(voidBedspaceEntity.startDate.plusDays(10))
          withDepartureDate(voidBedspaceEntity.startDate.plusDays(15))
        }

        val overlappingStartDate = existingBooking.departureDate.minusDays(1)
        val overlappingEndDate = existingBooking.departureDate.plusDays(1)

        val updateVoidBedspace = Cas3VoidBedspaceRequest(
          startDate = overlappingStartDate,
          endDate = overlappingEndDate,
          reasonId = voidBedspaceEntity.reason.id,
          referenceNumber = voidBedspaceEntity.referenceNumber,
          notes = "updatedNotes",
        )

        doPutRequest(jwt, premises.id, voidBedspaceEntity.bedspace!!.id, voidBedspaceEntity.id, updateVoidBedspace)
          .expectStatus()
          .is4xxClientError
          .withConflictMessage("A Booking already exists for dates from ${existingBooking.arrivalDate} to ${existingBooking.departureDate} which overlaps with the desired dates: ${existingBooking.id}")
      }
    }

    @Test
    fun `Update Void Bedspaces returns 409 Conflict when a void bedspace for the same bedspace overlaps`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
        val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist()
        val voidBedspaceEntity = cas3VoidBedspaceEntityFactory.produceAndPersist {
          withBedspace(bedspace)
          withStartDate(LocalDate.now())
          withEndDate(LocalDate.now().plusDays(2))
          withYieldedReason { reason }
        }

        val updateVoidBedspace = buildUpdateVoidBedspace(voidBedspaceEntity)

        doPutRequest(jwt, premises.id, bedspace.id, voidBedspaceEntity.id, updateVoidBedspace)
          .expectStatus()
          .is4xxClientError
          .withConflictMessage("A Void Bedspace already exists for dates from ${voidBedspaceEntity.startDate} to ${voidBedspaceEntity.endDate} which overlaps with the desired dates: ${voidBedspaceEntity.id}")
      }
    }
  }

  @Nested
  inner class CancelVoidBedspace {
    val voidBedspaceCancellation = Cas3VoidBedspaceCancellation("this is now cancelled")

    fun doPutRequest(
      jwt: String,
      premisesId: UUID,
      bedspaceId: UUID,
      voidBedspaceId: UUID,
      voidBedspace: Cas3VoidBedspaceCancellation,
    ) = webTestClient.put()
      .uri("/cas3/v2/premises/$premisesId/bedspaces/$bedspaceId/void-bedspaces/$voidBedspaceId/cancellations")
      .headers(buildTemporaryAccommodationHeaders(jwt))
      .bodyValue(voidBedspace)
      .exchange()

    @Test
    fun `Cancel Void Bedspace for non-existent premises returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        val voidBedspaceEntity = createVoidBedspaces(premises).first()
        doPutRequest(jwt, UUID.randomUUID(), voidBedspaceEntity.bedspace!!.id, voidBedspaceEntity.id, voidBedspaceCancellation)
          .expectStatus()
          .isNotFound
          .withNotFoundMessage("No Cas3VoidBedspace with an ID of ${voidBedspaceEntity.id} could be found")
      }
    }

    @Test
    fun `Cancel Void Bedspace for non-existent bedspace returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        val voidBedspaceEntity = createVoidBedspaces(premises).first()

        doPutRequest(jwt, premises.id, UUID.randomUUID(), voidBedspaceEntity.id, voidBedspaceCancellation)
          .expectStatus()
          .isNotFound
          .withNotFoundMessage("No Cas3VoidBedspace with an ID of ${voidBedspaceEntity.id} could be found")
      }
    }

    @Test
    fun `Cancel Void Bedspace for non-existent void bedspace returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        val voidBedspaceEntity = createVoidBedspaces(premises).first()

        val invalidId = UUID.randomUUID()
        doPutRequest(jwt, premises.id, voidBedspaceEntity.bedspace!!.id, invalidId, voidBedspaceCancellation)
          .expectStatus()
          .isNotFound
          .withNotFoundMessage("No Cas3VoidBedspace with an ID of $invalidId could be found")
      }
    }

    @Test
    fun `Cancel Void Bedspace on Temporary Accommodation premises returns OK with correct body when correct data is provided`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        val voidBedspaceEntity = createVoidBedspaces(premises).first()

        val result = doPutRequest(jwt, premises.id, voidBedspaceEntity.bedspace!!.id, voidBedspaceEntity.id, voidBedspaceCancellation)
          .expectStatus()
          .isOk
          .expectBody(Cas3VoidBedspace::class.java)
          .returnResult()
          .responseBody!!

        assertAll({
          assertThat(result.id).isEqualTo(voidBedspaceEntity.id)
          assertThat(result.bedspaceId).isEqualTo(voidBedspaceEntity.bedspace!!.id)
          assertThat(result.bedspaceName).isEqualTo(voidBedspaceEntity.bedspace!!.reference)
          assertThat(result.startDate).isEqualTo(voidBedspaceEntity.startDate)
          assertThat(result.endDate).isEqualTo(voidBedspaceEntity.endDate)
          assertThat(result.reason.id).isEqualTo(voidBedspaceEntity.reason.id)
          assertThat(result.referenceNumber).isEqualTo(voidBedspaceEntity.referenceNumber)
          assertThat(result.notes).isEqualTo(voidBedspaceEntity.notes)
          assertThat(result.cancellationDate).isNotNull
          assertThat(result.cancellationNotes).isEqualTo("this is now cancelled")
        })
      }
    }

    @Test
    fun `Cancel Void Bedspace on Temporary Accommodation premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser { user, jwt ->
        val otherRegion = givenAProbationRegion()
        val otherPremises = givenACas3Premises(otherRegion)
        val voidBedspaceEntity = createVoidBedspaces(otherPremises).first()
        doPutRequest(jwt, otherPremises.id, voidBedspaceEntity.bedspace!!.id, voidBedspaceEntity.id, voidBedspaceCancellation)
          .expectStatus()
          .isForbidden
      }
    }
  }

  fun buildVoidBedspace(
    reasonId: UUID,
    startDate: LocalDate = LocalDate.now().plusDays(1),
    endDate: LocalDate = LocalDate.now().plusDays(2),
  ) = Cas3VoidBedspaceRequest(
    startDate = startDate,
    endDate = endDate,
    reasonId = reasonId,
    referenceNumber = "Reference",
    notes = "Notes",
  )

  private fun createVoidBedspaces(premises: Cas3PremisesEntity): List<Cas3VoidBedspaceEntity> {
    val bedspaces = cas3BedspaceEntityFactory.produceAndPersistMultiple(5) {
      withPremises(premises)
    }
    val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist()
    val voidBedspace1 = cas3VoidBedspaceEntityFactory.produceAndPersist {
      withStartDate(LocalDate.now().plusDays(1))
      withEndDate(LocalDate.now().plusDays(10))
      withBedspace(bedspaces.get(0))
      withYieldedReason { reason }
    }
    val voidBedspace2 = cas3VoidBedspaceEntityFactory.produceAndPersist {
      withStartDate(LocalDate.now().plusDays(1))
      withEndDate(LocalDate.now().plusDays(10))
      withBedspace(bedspaces.get(3))
      withYieldedReason { reason }
    }
    val voidBedspace3 = cas3VoidBedspaceEntityFactory.produceAndPersist {
      withStartDate(LocalDate.now().plusDays(1))
      withEndDate(LocalDate.now().plusDays(10))
      withBedspace(bedspaces.get(4))
      withYieldedReason { reason }
    }

    return listOf(voidBedspace1, voidBedspace2, voidBedspace3)
  }

  private fun createCancelledVoidBedspaces(premises: Cas3PremisesEntity): List<Cas3VoidBedspaceEntity> {
    val bedspaces = cas3BedspaceEntityFactory.produceAndPersistMultiple(2) {
      withPremises(premises)
      withStartDate(LocalDate.now().minusDays(10))
      withEndDate(LocalDate.now().randomDateAfter(6))
    }

    val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist()
    val voidBedspace1 = cas3VoidBedspaceEntityFactory.produceAndPersist {
      withBedspace(bedspaces.get(0))
      withYieldedReason { reason }
      withCancellationDate(OffsetDateTime.now())
    }

    val voidBedspace2 = cas3VoidBedspaceEntityFactory.produceAndPersist {
      withBedspace(bedspaces.get(1))
      withYieldedReason { reason }
      withCancellationDate(OffsetDateTime.now())
      withCancellationNotes("Cancelled")
    }

    return listOf(voidBedspace1, voidBedspace2)
  }
}
