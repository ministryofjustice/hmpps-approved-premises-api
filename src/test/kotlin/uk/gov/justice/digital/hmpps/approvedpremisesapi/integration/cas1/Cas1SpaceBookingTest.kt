package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Request`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class Cas1SpaceBookingTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var transformer: Cas1SpaceBookingTransformer

  @Test
  fun `Booking a space without JWT returns 401`() {
    `Given a User` { user, _ ->
      `Given a Placement Request`(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      ) { placementRequest, _ ->
        webTestClient.post()
          .uri("/cas1/placement-requests/${placementRequest.id}/space-bookings")
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }
  }

  @Test
  fun `Booking a space for an unknown placement request returns 400 Bad Request`() {
    `Given a User` { _, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withYieldedApArea {
              apAreaEntityFactory.produceAndPersist()
            }
          }
        }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
      }

      val placementRequestId = UUID.randomUUID()

      webTestClient.post()
        .uri("/cas1/placement-requests/$placementRequestId/space-bookings")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewCas1SpaceBooking(
            arrivalDate = LocalDate.now().plusDays(1),
            departureDate = LocalDate.now().plusDays(8),
            premisesId = premises.id,
            requirements = Cas1SpaceBookingRequirements(
              apType = ApType.esap,
              gender = Gender.male,
              essentialCharacteristics = listOf(),
              desirableCharacteristics = listOf(),
            ),
          ),
        )
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.placementRequestId")
        .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
    }
  }

  @Test
  fun `Booking a space for an unknown premises returns 400 Bad Request`() {
    `Given a User` { user, jwt ->
      `Given a Placement Request`(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      ) { placementRequest, _ ->
        webTestClient.post()
          .uri("/cas1/placement-requests/${placementRequest.id}/space-bookings")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewCas1SpaceBooking(
              arrivalDate = LocalDate.now().plusDays(1),
              departureDate = LocalDate.now().plusDays(8),
              premisesId = UUID.randomUUID(),
              requirements = Cas1SpaceBookingRequirements(
                apType = ApType.esap,
                gender = Gender.male,
                essentialCharacteristics = listOf(),
                desirableCharacteristics = listOf(),
              ),
            ),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("invalid-params[0].propertyName").isEqualTo("$.premisesId")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }
  }

  @Test
  fun `Booking a space where the departure date is before the arrival date returns 400 Bad Request`() {
    `Given a User` { user, jwt ->
      `Given a Placement Request`(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      ) { placementRequest, _ ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist {
              withYieldedApArea {
                apAreaEntityFactory.produceAndPersist()
              }
            }
          }
          withYieldedLocalAuthorityArea {
            localAuthorityEntityFactory.produceAndPersist()
          }
        }

        webTestClient.post()
          .uri("/cas1/placement-requests/${placementRequest.id}/space-bookings")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewCas1SpaceBooking(
              arrivalDate = LocalDate.now().plusDays(1),
              departureDate = LocalDate.now(),
              premisesId = premises.id,
              requirements = Cas1SpaceBookingRequirements(
                apType = ApType.esap,
                gender = Gender.male,
                essentialCharacteristics = listOf(),
                desirableCharacteristics = listOf(),
              ),
            ),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("invalid-params[0].propertyName").isEqualTo("$.departureDate")
          .jsonPath("invalid-params[0].errorType").isEqualTo("shouldBeAfterArrivalDate")
      }
    }
  }

  @Test
  fun `Booking a space returns OK with the correct data`() {
    `Given a User` { user, jwt ->
      `Given a Placement Request`(
        placementRequestAllocatedTo = user,
        assessmentAllocatedTo = user,
        createdByUser = user,
      ) { placementRequest, application ->
        val essentialCharacteristics = listOf(
          Cas1SpaceCharacteristic.hasBrailleSignage,
          Cas1SpaceCharacteristic.hasTactileFlooring,
        )

        val desirableCharacteristics = listOf(
          Cas1SpaceCharacteristic.hasEnSuite,
          Cas1SpaceCharacteristic.hasCallForAssistance,
          Cas1SpaceCharacteristic.isGroundFloor,
          Cas1SpaceCharacteristic.isCatered,
        )

        val essentialCriteria = essentialCharacteristics.map {
          it.asCharacteristicEntity()
        }

        val desirableCriteria = desirableCharacteristics.map {
          it.asCharacteristicEntity()
        }

        placementRequest.placementRequirements = placementRequirementsFactory.produceAndPersist {
          withYieldedPostcodeDistrict {
            postCodeDistrictFactory.produceAndPersist()
          }
          withApplication(application as ApprovedPremisesApplicationEntity)
          withAssessment(placementRequest.assessment)
          withEssentialCriteria(essentialCriteria)
          withDesirableCriteria(desirableCriteria)
        }

        placementRequestRepository.saveAndFlush(placementRequest)

        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist {
              withYieldedApArea {
                apAreaEntityFactory.produceAndPersist()
              }
            }
          }
          withYieldedLocalAuthorityArea {
            localAuthorityEntityFactory.produceAndPersist()
          }
        }

        val response = webTestClient.post()
          .uri("/cas1/placement-requests/${placementRequest.id}/space-bookings")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewCas1SpaceBooking(
              arrivalDate = LocalDate.now().plusDays(1),
              departureDate = LocalDate.now().plusDays(8),
              premisesId = premises.id,
              requirements = Cas1SpaceBookingRequirements(
                apType = placementRequest.placementRequirements.apType,
                gender = Gender.male,
                essentialCharacteristics = essentialCharacteristics,
                desirableCharacteristics = desirableCharacteristics,
              ),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .returnResult(Cas1SpaceBooking::class.java)

        val result = response.responseBody.blockFirst()!!

        assertThat(result.person)
        assertThat(result.requirements.apType).isEqualTo(placementRequest.placementRequirements.apType)
        assertThat(result.requirements.gender).isEqualTo(placementRequest.placementRequirements.gender)
        assertThat(result.requirements.essentialCharacteristics).containsExactlyInAnyOrderElementsOf(essentialCharacteristics)
        assertThat(result.requirements.desirableCharacteristics).containsExactlyInAnyOrderElementsOf(desirableCharacteristics)
        assertThat(result.premises.id).isEqualTo(premises.id)
        assertThat(result.premises.name).isEqualTo(premises.name)
        assertThat(result.apArea.id).isEqualTo(premises.probationRegion.apArea!!.id)
        assertThat(result.apArea.name).isEqualTo(premises.probationRegion.apArea!!.name)
        assertThat(result.bookedBy.id).isEqualTo(user.id)
        assertThat(result.bookedBy.name).isEqualTo(user.name)
        assertThat(result.bookedBy.deliusUsername).isEqualTo(user.deliusUsername)
        assertThat(result.arrivalDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(result.departureDate).isEqualTo(LocalDate.now().plusDays(8))
        assertThat(result.createdAt).satisfies(
          { it.isAfter(Instant.now().minusSeconds(10)) },
        )
      }
    }
  }

  private fun Cas1SpaceCharacteristic.asCharacteristicEntity() = characteristicEntityFactory.produceAndPersist {
    withName(this@asCharacteristicEntity.value)
    withPropertyName(this@asCharacteristicEntity.value)
    withServiceScope(ServiceName.approvedPremises.value)
    withModelScope("*")
  }
}
