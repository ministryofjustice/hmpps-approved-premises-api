package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas1OutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas1OutOfServiceBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Temporality
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas1OutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.withinSeconds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class OutOfServiceBedTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var outOfServiceBedTransformer: Cas1OutOfServiceBedTransformer

  @Nested
  inner class GetAllOutOfServiceBeds {
    @Test
    fun `Get All Out-Of-Service Beds without JWT returns 401`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}/out-of-service-beds")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get All Out-Of-Service Beds on non existent Premises returns 404`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.get()
        .uri("/cas1/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/out-of-service-beds")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Get All Out-Of-Service Beds returns OK with correct body when user has one of roles FUTURE_MANAGER, MANAGER, MATCHER`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withStartDate(LocalDate.now().plusDays(2))
          withEndDate(LocalDate.now().plusDays(4))
          withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          withBed(bed)
        }

        val cancelledOutOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withStartDate(LocalDate.now().plusDays(2))
          withEndDate(LocalDate.now().plusDays(4))
          withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          withBed(bed)
        }

        cas1OutOfServiceBedCancellationEntityFactory.produceAndPersist {
          withOutOfServiceBed(cancelledOutOfServiceBed)
        }

        val expectedJson = objectMapper.writeValueAsString(listOf(outOfServiceBedTransformer.transformJpaToApi(outOfServiceBed)))

        webTestClient.get()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }
  }

  @Nested
  inner class GetOutOfServiceBed {
    @Test
    fun `Get Out-Of-Service Bed without JWT returns 401`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
        withBed(
          bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          },
        )
      }

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get Out-Of-Service Bed for non-existent premises returns 404`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.get()
        .uri("/cas1/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/out-of-service-beds")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Get Out-Of-Service Bed for non-existent out-of-service bed returns 404`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        webTestClient.get()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Get Out-Of-Service Bed returns OK with correct body when user has one of roles FUTURE_MANAGER, MANAGER, MATCHER`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withStartDate(LocalDate.now().plusDays(2))
          withEndDate(LocalDate.now().plusDays(4))
          withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          withBed(
            bedEntityFactory.produceAndPersist {
              withYieldedRoom {
                roomEntityFactory.produceAndPersist {
                  withYieldedPremises { premises }
                }
              }
            },
          )
        }

        val expectedJson = objectMapper.writeValueAsString(outOfServiceBedTransformer.transformJpaToApi(outOfServiceBed))

        webTestClient.get()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }
  }

  @Nested
  inner class CreateOutOfServiceBed {
    @Test
    fun `Create Out-Of-Service Beds without JWT returns 401`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val bed = bedEntityFactory.produceAndPersist {
        withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
      }

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/out-of-service-beds")
        .bodyValue(
          NewCas1OutOfServiceBed(
            startDate = LocalDate.parse("2022-08-15"),
            endDate = LocalDate.parse("2022-08-18"),
            bedId = bed.id,
            reason = UUID.randomUUID(),
            referenceNumber = "REF-123",
            notes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Create Out-Of-Service Beds returns 400 Bad Request if the bed ID does not reference a bed on the premises`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist {
              withYieldedApArea {
                apAreaEntityFactory.produceAndPersist()
              }
            }
          }
        }

        val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewCas1OutOfServiceBed(
              startDate = LocalDate.parse("2022-08-17"),
              endDate = LocalDate.parse("2022-08-18"),
              reason = reason.id,
              referenceNumber = "REF-123",
              notes = "notes",
              bedId = UUID.randomUUID(),
            ),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("invalid-params[0].propertyName").isEqualTo("$.bedId")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Create Out-Of-Service Beds returns OK with correct body when user has one of roles FUTURE_MANAGER, MANAGER, MATCHER`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        bedEntityFactory.produceAndPersistMultiple(3) {
          withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
        }

        val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewCas1OutOfServiceBed(
              startDate = LocalDate.parse("2022-08-17"),
              endDate = LocalDate.parse("2022-08-18"),
              bedId = bed.id,
              reason = reason.id,
              referenceNumber = "REF-123",
              notes = "notes",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath(".outOfServiceFrom").isEqualTo("2022-08-17")
          .jsonPath(".outOfServiceTo").isEqualTo("2022-08-18")
          .jsonPath(".bed.id").isEqualTo(bed.id.toString())
          .jsonPath(".bed.name").isEqualTo(bed.name)
          .jsonPath(".room.id").isEqualTo(bed.room.id.toString())
          .jsonPath(".room.name").isEqualTo(bed.room.name)
          .jsonPath(".premises.id").isEqualTo(premises.id.toString())
          .jsonPath(".premises.name").isEqualTo(premises.name)
          .jsonPath(".apArea.id").isEqualTo(premises.probationRegion.apArea.id.toString())
          .jsonPath(".apArea.name").isEqualTo(premises.probationRegion.apArea.name)
          .jsonPath(".reason.id").isEqualTo(reason.id.toString())
          .jsonPath(".reason.name").isEqualTo(reason.name)
          .jsonPath(".reason.isActive").isEqualTo(true)
          .jsonPath(".daysLostCount").isEqualTo(2)
          .jsonPath(".temporality").isEqualTo(Temporality.past.value)
          .jsonPath(".referenceNumber").isEqualTo("REF-123")
          .jsonPath(".notes").isEqualTo("notes")
          .jsonPath(".status").isEqualTo("active")
          .jsonPath(".cancellation").isEqualTo(null)
      }
    }

    @Test
    fun `Create Out-Of-Service Bed succeeds even if overlapping with Booking`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
        }

        val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        BookingEntityFactory()
          .withBed(bed)
          .withPremises(premises)
          .withArrivalDate(LocalDate.parse("2022-08-15"))
          .withDepartureDate(LocalDate.parse("2022-08-18"))
          .produce()

        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewCas1OutOfServiceBed(
              startDate = LocalDate.parse("2022-08-17"),
              endDate = LocalDate.parse("2022-08-18"),
              bedId = bed.id,
              reason = reason.id,
              referenceNumber = "REF-123",
              notes = "notes",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath(".outOfServiceFrom").isEqualTo("2022-08-17")
          .jsonPath(".outOfServiceTo").isEqualTo("2022-08-18")
          .jsonPath(".bed.id").isEqualTo(bed.id.toString())
          .jsonPath(".bed.name").isEqualTo(bed.name)
          .jsonPath(".room.id").isEqualTo(bed.room.id.toString())
          .jsonPath(".room.name").isEqualTo(bed.room.name)
          .jsonPath(".premises.id").isEqualTo(premises.id.toString())
          .jsonPath(".premises.name").isEqualTo(premises.name)
          .jsonPath(".apArea.id").isEqualTo(premises.probationRegion.apArea.id.toString())
          .jsonPath(".apArea.name").isEqualTo(premises.probationRegion.apArea.name)
          .jsonPath(".reason.id").isEqualTo(reason.id.toString())
          .jsonPath(".reason.name").isEqualTo(reason.name)
          .jsonPath(".reason.isActive").isEqualTo(true)
          .jsonPath(".daysLostCount").isEqualTo(2)
          .jsonPath(".temporality").isEqualTo(Temporality.past.value)
          .jsonPath(".referenceNumber").isEqualTo("REF-123")
          .jsonPath(".notes").isEqualTo("notes")
          .jsonPath(".status").isEqualTo("active")
          .jsonPath(".cancellation").isEqualTo(null)
      }
    }

    @Test
    fun `Create Out-Of-Service Beds for current day does not break GET all Premises endpoint`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
        }

        cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withBed(
            bedEntityFactory.produceAndPersist {
              withYieldedRoom {
                roomEntityFactory.produceAndPersist {
                  withYieldedPremises { premises }
                }
              }
            },
          )
          withStartDate(LocalDate.now().minusDays(2))
          withEndDate(LocalDate.now().plusDays(2))
          withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
        }

        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withOriginalArrivalDate(LocalDate.now().minusDays(4))
          withArrivalDate(LocalDate.now().minusDays(4))
          withOriginalDepartureDate(LocalDate.now().plusDays(6))
          withDepartureDate(LocalDate.now().plusDays(6))
        }

        webTestClient.get()
          .uri("/premises")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
      }
    }

    @Test
    fun `Create Out-Of-Service Bed returns 409 Conflict when An out-of-service bed for the same bed overlaps`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        `Given an Offender` { _, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
            }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

          val existingOutOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withBed(bed)
            withStartDate(LocalDate.parse("2022-07-15"))
            withEndDate(LocalDate.parse("2022-08-15"))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }

          webTestClient.post()
            .uri("/cas1/premises/${premises.id}/out-of-service-beds")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewCas1OutOfServiceBed(
                startDate = LocalDate.parse("2022-08-01"),
                endDate = LocalDate.parse("2022-08-30"),
                reason = reason.id,
                referenceNumber = "REF-123",
                notes = "notes",
                bedId = bed.id,
              ),
            )
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody()
            .jsonPath("title").isEqualTo("Conflict")
            .jsonPath("status").isEqualTo(409)
            .jsonPath("detail").isEqualTo("An out-of-service bed already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingOutOfServiceBed.id}")
        }
      }
    }

    @Test
    fun `Create Out-Of-Service Bed returns OK with correct body when only cancelled out-of-service beds for the same bed overlap`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        `Given an Offender` { _, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
            }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

          val existingOutOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withBed(bed)
            withStartDate(LocalDate.parse("2022-07-15"))
            withEndDate(LocalDate.parse("2022-08-15"))
            withReason(
              cas1OutOfServiceBedReasonEntityFactory.produceAndPersist(),
            )
          }

          existingOutOfServiceBed.cancellation = cas1OutOfServiceBedCancellationEntityFactory.produceAndPersist {
            withOutOfServiceBed(existingOutOfServiceBed)
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }

          webTestClient.post()
            .uri("/cas1/premises/${premises.id}/out-of-service-beds")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewCas1OutOfServiceBed(
                startDate = LocalDate.parse("2022-08-01"),
                endDate = LocalDate.parse("2022-08-30"),
                reason = reason.id,
                referenceNumber = "REF-123",
                notes = "notes",
                bedId = bed.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath(".outOfServiceFrom").isEqualTo("2022-08-01")
            .jsonPath(".outOfServiceTo").isEqualTo("2022-08-30")
            .jsonPath(".bed.id").isEqualTo(bed.id.toString())
            .jsonPath(".bed.name").isEqualTo(bed.name)
            .jsonPath(".room.id").isEqualTo(bed.room.id.toString())
            .jsonPath(".room.name").isEqualTo(bed.room.name)
            .jsonPath(".premises.id").isEqualTo(premises.id.toString())
            .jsonPath(".premises.name").isEqualTo(premises.name)
            .jsonPath(".apArea.id").isEqualTo(premises.probationRegion.apArea.id.toString())
            .jsonPath(".apArea.name").isEqualTo(premises.probationRegion.apArea.name)
            .jsonPath(".reason.id").isEqualTo(reason.id.toString())
            .jsonPath(".reason.name").isEqualTo(reason.name)
            .jsonPath(".reason.isActive").isEqualTo(true)
            .jsonPath(".daysLostCount").isEqualTo(30)
            .jsonPath(".temporality").isEqualTo(Temporality.past.value)
            .jsonPath(".referenceNumber").isEqualTo("REF-123")
            .jsonPath(".notes").isEqualTo("notes")
            .jsonPath(".status").isEqualTo("active")
            .jsonPath(".cancellation").isEqualTo(null)
        }
      }
    }
  }

  @Nested
  inner class UpdateOutOfServiceBed {
    @Test
    fun `Update Out-Of-Service Bed without JWT returns 401`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
        withBed(
          bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          },
        )
      }

      bedEntityFactory.produceAndPersist {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
          }
        }
      }

      webTestClient.put()
        .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}")
        .bodyValue(
          UpdateCas1OutOfServiceBed(
            startDate = LocalDate.parse("2022-08-15"),
            endDate = LocalDate.parse("2022-08-18"),
            reason = UUID.randomUUID(),
            referenceNumber = "REF-123",
            notes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Update Out-Of-Service Bed for non-existent premises returns 404`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.put()
        .uri("/cas1/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/out-of-service-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          UpdateCas1OutOfServiceBed(
            startDate = LocalDate.parse("2022-08-15"),
            endDate = LocalDate.parse("2022-08-18"),
            reason = UUID.randomUUID(),
            referenceNumber = "REF-123",
            notes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `Update Out-Of-Service Bed for non-existent out-of-service bed returns 404`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.put()
        .uri("/cas1/premises/${premises.id}/out-of-service-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          UpdateCas1OutOfServiceBed(
            startDate = LocalDate.parse("2022-08-15"),
            endDate = LocalDate.parse("2022-08-18"),
            reason = UUID.randomUUID(),
            referenceNumber = "REF-123",
            notes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Update Out-Of-Service Beds returns OK with correct body when user has one of roles FUTURE_MANAGER, MANAGER, MATCHER`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withStartDate(LocalDate.now().plusDays(2))
          withEndDate(LocalDate.now().plusDays(4))
          withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          withBed(bed)
        }

        val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

        webTestClient.put()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateCas1OutOfServiceBed(
              startDate = LocalDate.parse("2022-08-17"),
              endDate = LocalDate.parse("2022-08-18"),
              reason = reason.id,
              referenceNumber = "REF-123",
              notes = "notes",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath(".outOfServiceFrom").isEqualTo("2022-08-17")
          .jsonPath(".outOfServiceTo").isEqualTo("2022-08-18")
          .jsonPath(".bed.id").isEqualTo(bed.id.toString())
          .jsonPath(".bed.name").isEqualTo(bed.name)
          .jsonPath(".room.id").isEqualTo(bed.room.id.toString())
          .jsonPath(".room.name").isEqualTo(bed.room.name)
          .jsonPath(".premises.id").isEqualTo(premises.id.toString())
          .jsonPath(".premises.name").isEqualTo(premises.name)
          .jsonPath(".apArea.id").isEqualTo(premises.probationRegion.apArea.id.toString())
          .jsonPath(".apArea.name").isEqualTo(premises.probationRegion.apArea.name)
          .jsonPath(".reason.id").isEqualTo(reason.id.toString())
          .jsonPath(".reason.name").isEqualTo(reason.name)
          .jsonPath(".reason.isActive").isEqualTo(true)
          .jsonPath(".daysLostCount").isEqualTo(2)
          .jsonPath(".temporality").isEqualTo(Temporality.past.value)
          .jsonPath(".referenceNumber").isEqualTo("REF-123")
          .jsonPath(".notes").isEqualTo("notes")
          .jsonPath(".status").isEqualTo("active")
          .jsonPath(".cancellation").isEqualTo(null)
      }
    }

    @Test
    fun `Update Out-Of-Service Beds returns 409 Conflict when a booking for the same bed overlaps`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist {
              withYieldedApArea {
                apAreaEntityFactory.produceAndPersist()
              }
            }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withStartDate(LocalDate.parse("2022-08-16"))
          withEndDate(LocalDate.parse("2022-08-30"))
          withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          withBed(bed)
        }

        val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

        val existingBooking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.approvedPremises)
          withCrn("CRN123")
          withYieldedPremises { premises }
          withBed(bed)
          withArrivalDate(LocalDate.parse("2022-07-15"))
          withDepartureDate(LocalDate.parse("2022-08-15"))
        }

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.put()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateCas1OutOfServiceBed(
              startDate = LocalDate.parse("2022-08-01"),
              endDate = LocalDate.parse("2022-08-30"),
              reason = reason.id,
              referenceNumber = "REF-123",
              notes = "notes",
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail").isEqualTo("A booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingBooking.id}")
      }
    }

    @Test
    fun `Update Out-Of-Service Beds returns OK with correct body when only cancelled bookings for the same bed overlap`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist {
                withYieldedApArea {
                  apAreaEntityFactory.produceAndPersist()
                }
              }
            }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withStartDate(LocalDate.parse("2022-08-16"))
            withEndDate(LocalDate.parse("2022-08-30"))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
            withBed(bed)
          }

          val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

          val existingBooking = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.approvedPremises)
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises { premises }
            withBed(bed)
            withArrivalDate(LocalDate.parse("2022-07-15"))
            withDepartureDate(LocalDate.parse("2022-08-15"))
          }

          existingBooking.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
            withYieldedBooking { existingBooking }
            withDate(LocalDate.parse("2022-07-01"))
            withReason(cancellationReasonEntityFactory.produceAndPersist())
          }.toMutableList()

          webTestClient.put()
            .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdateCas1OutOfServiceBed(
                startDate = LocalDate.parse("2022-08-01"),
                endDate = LocalDate.parse("2022-08-30"),
                reason = reason.id,
                referenceNumber = "REF-123",
                notes = "notes",
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath(".outOfServiceFrom").isEqualTo("2022-08-01")
            .jsonPath(".outOfServiceTo").isEqualTo("2022-08-30")
            .jsonPath(".bed.id").isEqualTo(bed.id.toString())
            .jsonPath(".bed.name").isEqualTo(bed.name)
            .jsonPath(".room.id").isEqualTo(bed.room.id.toString())
            .jsonPath(".room.name").isEqualTo(bed.room.name)
            .jsonPath(".premises.id").isEqualTo(premises.id.toString())
            .jsonPath(".premises.name").isEqualTo(premises.name)
            .jsonPath(".apArea.id").isEqualTo(premises.probationRegion.apArea.id.toString())
            .jsonPath(".apArea.name").isEqualTo(premises.probationRegion.apArea.name)
            .jsonPath(".reason.id").isEqualTo(reason.id.toString())
            .jsonPath(".reason.name").isEqualTo(reason.name)
            .jsonPath(".reason.isActive").isEqualTo(true)
            .jsonPath(".daysLostCount").isEqualTo(30)
            .jsonPath(".temporality").isEqualTo(Temporality.past.value)
            .jsonPath(".referenceNumber").isEqualTo("REF-123")
            .jsonPath(".notes").isEqualTo("notes")
            .jsonPath(".status").isEqualTo("active")
            .jsonPath(".cancellation").isEqualTo(null)
        }
      }
    }

    @Test
    fun `Update Out-Of-Service Beds returns 409 Conflict when An out-of-service bed for the same bed overlaps`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        `Given an Offender` { _, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
            }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

          val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withStartDate(LocalDate.parse("2022-08-16"))
            withEndDate(LocalDate.parse("2022-08-30"))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
            withBed(bed)
          }

          val existingOutOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withBed(bed)
            withStartDate(LocalDate.parse("2022-07-15"))
            withEndDate(LocalDate.parse("2022-08-15"))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }

          webTestClient.put()
            .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdateCas1OutOfServiceBed(
                startDate = LocalDate.parse("2022-08-01"),
                endDate = LocalDate.parse("2022-08-30"),
                reason = reason.id,
                referenceNumber = "REF-123",
                notes = "notes",
              ),
            )
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody()
            .jsonPath("title").isEqualTo("Conflict")
            .jsonPath("status").isEqualTo(409)
            .jsonPath("detail").isEqualTo("An out-of-service bed already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingOutOfServiceBed.id}")
        }
      }
    }

    @Test
    fun `Update Out-Of-Service Beds returns OK with correct body when only cancelled out-of-service beds for the same bed overlap`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        `Given an Offender` { _, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
            }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

          val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withStartDate(LocalDate.parse("2022-08-16"))
            withEndDate(LocalDate.parse("2022-08-30"))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
            withBed(bed)
          }

          val existingOutOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withBed(bed)
            withStartDate(LocalDate.parse("2022-07-15"))
            withEndDate(LocalDate.parse("2022-08-15"))
            withReason(
              cas1OutOfServiceBedReasonEntityFactory.produceAndPersist(),
            )
          }

          existingOutOfServiceBed.cancellation = cas1OutOfServiceBedCancellationEntityFactory.produceAndPersist {
            withOutOfServiceBed(existingOutOfServiceBed)
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }

          webTestClient.put()
            .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdateCas1OutOfServiceBed(
                startDate = LocalDate.parse("2022-08-01"),
                endDate = LocalDate.parse("2022-08-30"),
                reason = reason.id,
                referenceNumber = "REF-123",
                notes = "notes",
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath(".outOfServiceFrom").isEqualTo("2022-08-01")
            .jsonPath(".outOfServiceTo").isEqualTo("2022-08-30")
            .jsonPath(".bed.id").isEqualTo(bed.id.toString())
            .jsonPath(".bed.name").isEqualTo(bed.name)
            .jsonPath(".room.id").isEqualTo(bed.room.id.toString())
            .jsonPath(".room.name").isEqualTo(bed.room.name)
            .jsonPath(".premises.id").isEqualTo(premises.id.toString())
            .jsonPath(".premises.name").isEqualTo(premises.name)
            .jsonPath(".apArea.id").isEqualTo(premises.probationRegion.apArea.id.toString())
            .jsonPath(".apArea.name").isEqualTo(premises.probationRegion.apArea.name)
            .jsonPath(".reason.id").isEqualTo(reason.id.toString())
            .jsonPath(".reason.name").isEqualTo(reason.name)
            .jsonPath(".reason.isActive").isEqualTo(true)
            .jsonPath(".daysLostCount").isEqualTo(30)
            .jsonPath(".temporality").isEqualTo(Temporality.past.value)
            .jsonPath(".referenceNumber").isEqualTo("REF-123")
            .jsonPath(".notes").isEqualTo("notes")
            .jsonPath(".status").isEqualTo("active")
            .jsonPath(".cancellation").isEqualTo(null)
        }
      }
    }
  }

  @Nested
  inner class CancelOutOfServiceBed {
    @Test
    fun `Cancel Out-Of-Service Bed without JWT returns 401`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
        withBed(
          bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          },
        )
      }

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}/cancellations")
        .bodyValue(
          NewCas1OutOfServiceBedCancellation(
            notes = "Unauthorized",
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Cancel Out-Of-Service Bed for non-existent premises returns 404`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.post()
        .uri("/cas1/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/out-of-service-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488/cancellations")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewCas1OutOfServiceBedCancellation(
            notes = "Non-existent premises",
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `Cancel Out-Of-Service Bed for non-existent out-of-service bed returns 404`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/out-of-service-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488/cancellations")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewCas1OutOfServiceBedCancellation(
            notes = "Non-existent out-of-service bed",
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Cancel Out-Of-Service Bed returns OK with correct body when user has one of roles FUTURE_MANAGER, MANAGER, MATCHER`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
        }

        val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withStartDate(LocalDate.now().plusDays(2))
          withEndDate(LocalDate.now().plusDays(4))
          withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          withBed(
            bedEntityFactory.produceAndPersist {
              withYieldedRoom {
                roomEntityFactory.produceAndPersist {
                  withYieldedPremises { premises }
                }
              }
            },
          )
        }

        cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}/cancellations")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewCas1OutOfServiceBedCancellation(
              notes = "Some cancellation notes",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.notes").isEqualTo("Some cancellation notes")
          .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
      }
    }
  }
}
