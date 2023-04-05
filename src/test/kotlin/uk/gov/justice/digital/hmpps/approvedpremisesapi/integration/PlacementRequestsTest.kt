package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Request`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingNotMadeTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class PlacementRequestsTest : IntegrationTestBase() {

  @Autowired
  lateinit var placementRequestTransformer: PlacementRequestTransformer

  @Autowired
  lateinit var bookingNotMadeTransformer: BookingNotMadeTransformer

  @Test
  fun `Get all placement requests without JWT returns 401`() {
    webTestClient.get()
      .uri("/placement-requests")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `It returns all the placement requests for a user`() {
    `Given a User` { user, jwt ->
      `Given an Offender` { offenderDetails1, inmateDetails1 ->
        `Given an Offender` { offenderDetails2, _ ->
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val application1 = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails1.otherIds.crn)
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
          }

          val assessment1 = assessmentEntityFactory.produceAndPersist {
            withAssessmentSchema(assessmentSchema)
            withApplication(application1)
            withSubmittedAt(OffsetDateTime.now())
            withAllocatedToUser(user)
            withDecision(AssessmentDecision.ACCEPTED)
          }

          val application2 = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails2.otherIds.crn)
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
          }

          val assessment2 = assessmentEntityFactory.produceAndPersist {
            withAssessmentSchema(assessmentSchema)
            withApplication(application2)
            withSubmittedAt(OffsetDateTime.now())
            withAllocatedToUser(user)
            withDecision(AssessmentDecision.ACCEPTED)
          }

          val placementRequest = placementRequestFactory.produceAndPersist {
            withAllocatedToUser(user)
            withApplication(application1)
            withAssessment(assessment1)
            withPostcodeDistrict(
              postCodeDistrictRepository.findAll()[0]
            )
            withDesirableCriteria(
              characteristicEntityFactory.produceAndPersistMultiple(5)
            )
            withEssentialCriteria(
              characteristicEntityFactory.produceAndPersistMultiple(3)
            )
          }

          placementRequestFactory.produceAndPersistMultiple(2) {
            withAllocatedToUser(user)
            withReallocatedAt(OffsetDateTime.now())
            withApplication(application2)
            withAssessment(assessment2)
            withPostcodeDistrict(
              postCodeDistrictRepository.findAll()[0]
            )
            withDesirableCriteria(
              characteristicEntityFactory.produceAndPersistMultiple(5)
            )
            withEssentialCriteria(
              characteristicEntityFactory.produceAndPersistMultiple(3)
            )
          }

          webTestClient.get()
            .uri("/placement-requests")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                listOf(
                  placementRequestTransformer.transformJpaToApi(placementRequest, offenderDetails1, inmateDetails1)
                )
              )
            )
        }
      }
    }
  }

  @Test
  fun `Get single Placement Request without a JWT returns 401`() {
    webTestClient.get()
      .uri("/placement-requests/62faf6f4-1dac-4139-9a18-09c1b2852a0f")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get single Placement Request that is not allocated to calling User and without WORKFLOW_MANAGER role returns 403`() {
    `Given a User` { user, jwt ->
      `Given a User` { otherUser, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          `Given an Application`(createdByUser = otherUser) {
            `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn
            ) { placementRequest, _ ->
              webTestClient.get()
                .uri("/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }
    }
  }

  @Test
  fun `Get single Placement Request that is allocated to calling User returns 200`() {
    `Given a User` { user, jwt ->
      `Given a User` { otherUser, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          `Given an Application`(createdByUser = otherUser) {
            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn
            ) { placementRequest, _ ->
              webTestClient.get()
                .uri("/placement-requests/${placementRequest.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    placementRequestTransformer.transformJpaToApi(
                      placementRequest, offenderDetails, inmateDetails
                    )
                  )
                )
            }
          }
        }
      }
    }
  }

  @Test
  fun `Create a Booking from a Placement Request without a JWT returns 401`() {
    webTestClient.post()
      .uri("/placement-requests/62faf6f4-1dac-4139-9a18-09c1b2852a0f/booking")
      .bodyValue(
        NewPlacementRequestBooking(
          arrivalDate = LocalDate.parse("2023-03-29"),
          departureDate = LocalDate.parse("2023-04-01"),
          bedId = UUID.fromString("d5dfd808-b8f4-4cc0-a0ac-fdce7144126e")
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Creating a Booking from a Placement Request that is not allocated to the User returns a 403`() {
    `Given a User` { user, jwt ->
      `Given a User` { otherUser, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          `Given an Application`(createdByUser = otherUser) {
            `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn
            ) { placementRequest, _ ->
              webTestClient.post()
                .uri("/placement-requests/${placementRequest.id}/booking")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  NewPlacementRequestBooking(
                    arrivalDate = LocalDate.parse("2023-03-29"),
                    departureDate = LocalDate.parse("2023-04-01"),
                    bedId = UUID.fromString("d5dfd808-b8f4-4cc0-a0ac-fdce7144126e")
                  )
                )
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }
    }
  }

  @Test
  fun `Creating a Booking from a Placement Request that is allocated to the User returns a 200`() {
    `Given a User` { user, jwt ->
      `Given a User` { otherUser, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          `Given an Application`(createdByUser = otherUser) {
            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn
            ) { placementRequest, _ ->
              val premises = approvedPremisesEntityFactory.produceAndPersist {
                withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                withYieldedProbationRegion {
                  probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
                }
              }

              val room = roomEntityFactory.produceAndPersist {
                withPremises(premises)
              }

              val bed = bedEntityFactory.produceAndPersist {
                withRoom(room)
              }

              webTestClient.post()
                .uri("/placement-requests/${placementRequest.id}/booking")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  NewPlacementRequestBooking(
                    arrivalDate = LocalDate.parse("2023-03-29"),
                    departureDate = LocalDate.parse("2023-04-01"),
                    bedId = bed.id
                  )
                )
                .exchange()
                .expectStatus()
                .isOk
            }
          }
        }
      }
    }
  }

  @Test
  fun `Create a Booking Not Made from a Placement Request without a JWT returns 401`() {
    webTestClient.post()
      .uri("/placement-requests/62faf6f4-1dac-4139-9a18-09c1b2852a0f/booking-not-made")
      .bodyValue(
        NewBookingNotMade(
          notes = "some notes"
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create a Booking Not Made from a Placement Request that is not allocated to the user returns 403`() {
    `Given a User` { user, jwt ->
      `Given a User` { otherUser, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          `Given an Application`(createdByUser = otherUser) {
            `Given a Placement Request`(
              placementRequestAllocatedTo = otherUser,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn
            ) { placementRequest, _ ->
              webTestClient.post()
                .uri("/placement-requests/${placementRequest.id}/booking-not-made")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  NewBookingNotMade(
                    notes = "some notes"
                  )
                )
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }
    }
  }

  @Test
  fun `Create a Booking Not Made from a Placement Request returns 200`() {
    `Given a User` { user, jwt ->
      `Given a User` { otherUser, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          `Given an Application`(createdByUser = otherUser) {
            `Given a Placement Request`(
              placementRequestAllocatedTo = user,
              assessmentAllocatedTo = otherUser,
              createdByUser = otherUser,
              crn = offenderDetails.otherIds.crn
            ) { placementRequest, _ ->
              webTestClient.post()
                .uri("/placement-requests/${placementRequest.id}/booking-not-made")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  NewBookingNotMade(
                    notes = "some notes"
                  )
                )
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.placementRequestId").isEqualTo(placementRequest.id.toString())
                .jsonPath("$.notes").isEqualTo("some notes")
            }
          }
        }
      }
    }
  }
}
