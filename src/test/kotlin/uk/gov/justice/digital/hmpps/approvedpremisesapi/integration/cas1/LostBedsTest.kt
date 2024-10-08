package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Probation Region`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an AP Area`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.withinSeconds
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class LostBedsTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var lostBedsTransformer: LostBedsTransformer

  @Nested
  inner class GetAllLostBeds {
    @Test
    fun `Get All Lost Beds without JWT returns 401`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { `Given a Probation Region`() }
      }

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}/lost-beds")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get All Lost Beds on non existent Premises returns 404`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.get()
        .uri("/cas1/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/lost-beds")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_LEGACY_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Get All Lost Beds returns OK with correct body when user has one of roles LEGACY_MANAGER, MANAGER, MATCHER`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { `Given a Probation Region`() }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val lostBed = lostBedsEntityFactory.produceAndPersist {
          withStartDate(LocalDate.now().plusDays(2))
          withEndDate(LocalDate.now().plusDays(4))
          withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
          withBed(bed)
          withPremises(premises)
        }

        val cancelledLostBed = lostBedsEntityFactory.produceAndPersist {
          withStartDate(LocalDate.now().plusDays(2))
          withEndDate(LocalDate.now().plusDays(4))
          withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
          withBed(bed)
          withPremises(premises)
        }

        lostBedCancellationEntityFactory.produceAndPersist {
          withLostBed(cancelledLostBed)
        }

        val expectedJson = objectMapper.writeValueAsString(listOf(lostBedsTransformer.transformJpaToApi(lostBed)))

        webTestClient.get()
          .uri("/cas1/premises/${premises.id}/lost-beds")
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
  inner class GetLostBed {
    @Test
    fun `Get Lost Bed without JWT returns 401`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { `Given a Probation Region`() }
      }

      val lostBeds = lostBedsEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        withBed(
          bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          },
        )
        withPremises(premises)
      }

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}/lost-beds/${lostBeds.id}")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get Lost Bed for non-existent premises returns 404`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.get()
        .uri("/cas1/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/lost-beds")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_LEGACY_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Get Lost Bed for non-existent lost bed returns 404`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { `Given a Probation Region`() }
        }

        webTestClient.get()
          .uri("/cas1/premises/${premises.id}/lost-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_LEGACY_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Get Lost Bed returns OK with correct body when user has one of roles LEGACY_MANAGER, MANAGER, MATCHER`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { `Given a Probation Region`() }
        }

        val lostBeds = lostBedsEntityFactory.produceAndPersist {
          withStartDate(LocalDate.now().plusDays(2))
          withEndDate(LocalDate.now().plusDays(4))
          withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
          withBed(
            bedEntityFactory.produceAndPersist {
              withYieldedRoom {
                roomEntityFactory.produceAndPersist {
                  withYieldedPremises { premises }
                }
              }
            },
          )
          withPremises(premises)
        }

        val expectedJson = objectMapper.writeValueAsString(lostBedsTransformer.transformJpaToApi(lostBeds))

        webTestClient.get()
          .uri("/cas1/premises/${premises.id}/lost-beds/${lostBeds.id}")
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
  inner class CreateLostBed {
    @Test
    fun `Create Lost Beds without JWT returns 401`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { `Given a Probation Region`() }
      }

      val bed = bedEntityFactory.produceAndPersist {
        withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
      }

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/lost-beds")
        .bodyValue(
          NewLostBed(
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
    fun `Create Lost Beds returns 400 Bad Request if the bed ID does not reference a bed on the premises`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { `Given a Probation Region`() }
        }

        val reason = lostBedReasonEntityFactory.produceAndPersist {
          withServiceScope(ServiceName.approvedPremises.value)
        }

        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/lost-beds")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewLostBed(
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
    @EnumSource(value = UserRole::class, names = [ "CAS1_LEGACY_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Create Lost Beds returns OK with correct body when user has one of roles LEGACY_MANAGER, MANAGER, MATCHER`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { `Given a Probation Region`() }
        }

        bedEntityFactory.produceAndPersistMultiple(3) {
          withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
        }

        val reason = lostBedReasonEntityFactory.produceAndPersist {
          withServiceScope(ServiceName.approvedPremises.value)
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/lost-beds")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewLostBed(
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
          .jsonPath(".startDate").isEqualTo("2022-08-17")
          .jsonPath(".endDate").isEqualTo("2022-08-18")
          .jsonPath(".bedId").isEqualTo(bed.id.toString())
          .jsonPath(".reason.id").isEqualTo(reason.id.toString())
          .jsonPath(".reason.name").isEqualTo(reason.name)
          .jsonPath(".reason.isActive").isEqualTo(true)
          .jsonPath(".referenceNumber").isEqualTo("REF-123")
          .jsonPath(".notes").isEqualTo("notes")
          .jsonPath(".status").isEqualTo("active")
          .jsonPath(".cancellation").isEqualTo(null)
      }
    }

    @Test
    fun `Create Lost Bed succeeds even if overlapping with Booking`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { `Given a Probation Region`() }
        }

        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
        }

        val reason = lostBedReasonEntityFactory.produceAndPersist {
          withServiceScope(ServiceName.approvedPremises.value)
        }

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
          .uri("/cas1/premises/${premises.id}/lost-beds")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewLostBed(
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
          .jsonPath(".startDate").isEqualTo("2022-08-17")
          .jsonPath(".endDate").isEqualTo("2022-08-18")
          .jsonPath(".bedId").isEqualTo(bed.id.toString())
          .jsonPath(".reason.id").isEqualTo(reason.id.toString())
          .jsonPath(".reason.name").isEqualTo(reason.name)
          .jsonPath(".reason.isActive").isEqualTo(true)
          .jsonPath(".referenceNumber").isEqualTo("REF-123")
          .jsonPath(".notes").isEqualTo("notes")
          .jsonPath(".status").isEqualTo("active")
          .jsonPath(".cancellation").isEqualTo(null)
      }
    }

    @Test
    fun `Create Lost Beds for current day does not break GET all Premises endpoint`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { `Given a Probation Region`() }
        }

        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
        }

        lostBedsEntityFactory.produceAndPersist {
          withPremises(premises)
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
          withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
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
    fun `Create Lost Bed returns 409 Conflict when a lost bed for the same bed overlaps`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        `Given an Offender` { _, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion { `Given a Probation Region`() }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val reason = lostBedReasonEntityFactory.produceAndPersist {
            withServiceScope(ServiceName.approvedPremises.value)
          }

          val existingLostBed = lostBedsEntityFactory.produceAndPersist {
            withBed(bed)
            withPremises(premises)
            withStartDate(LocalDate.parse("2022-07-15"))
            withEndDate(LocalDate.parse("2022-08-15"))
            withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
          }

          webTestClient.post()
            .uri("/cas1/premises/${premises.id}/lost-beds")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewLostBed(
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
            .jsonPath("detail").isEqualTo("A Lost Bed already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingLostBed.id}")
        }
      }
    }

    @Test
    fun `Create Lost Bed returns OK with correct body when only cancelled lost beds for the same bed overlap`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        `Given an Offender` { _, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion { `Given a Probation Region`() }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val reason = lostBedReasonEntityFactory.produceAndPersist {
            withServiceScope(ServiceName.approvedPremises.value)
          }

          val existingLostBed = lostBedsEntityFactory.produceAndPersist {
            withBed(bed)
            withPremises(premises)
            withStartDate(LocalDate.parse("2022-07-15"))
            withEndDate(LocalDate.parse("2022-08-15"))
            withYieldedReason {
              lostBedReasonEntityFactory.produceAndPersist()
            }
          }

          existingLostBed.cancellation = lostBedCancellationEntityFactory.produceAndPersist {
            withLostBed(existingLostBed)
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }

          webTestClient.post()
            .uri("/cas1/premises/${premises.id}/lost-beds")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewLostBed(
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
            .jsonPath(".startDate").isEqualTo("2022-08-01")
            .jsonPath(".endDate").isEqualTo("2022-08-30")
            .jsonPath(".bedId").isEqualTo(bed.id.toString())
            .jsonPath(".reason.id").isEqualTo(reason.id.toString())
            .jsonPath(".reason.name").isEqualTo(reason.name)
            .jsonPath(".reason.isActive").isEqualTo(true)
            .jsonPath(".referenceNumber").isEqualTo("REF-123")
            .jsonPath(".notes").isEqualTo("notes")
            .jsonPath(".status").isEqualTo("active")
            .jsonPath(".cancellation").isEqualTo(null)
        }
      }
    }
  }

  @Nested
  inner class UpdateLostBed {
    @Test
    fun `Update Lost Bed without JWT returns 401`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { `Given a Probation Region`() }
      }

      val lostBeds = lostBedsEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        withBed(
          bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          },
        )
        withPremises(premises)
      }

      bedEntityFactory.produceAndPersist {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
          }
        }
      }

      webTestClient.put()
        .uri("/cas1/premises/${premises.id}/lost-beds/${lostBeds.id}")
        .bodyValue(
          UpdateLostBed(
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
    fun `Update Lost Bed for non-existent premises returns 404`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.put()
        .uri("/cas1/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/lost-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          UpdateLostBed(
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
    fun `Update Lost Bed for non-existent lost bed returns 404`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { `Given a Probation Region`() }
      }

      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.put()
        .uri("/cas1/premises/${premises.id}/lost-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          UpdateLostBed(
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
    @EnumSource(value = UserRole::class, names = [ "CAS1_LEGACY_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Update Lost Beds returns OK with correct body when user has one of roles LEGACY_MANAGER, MANAGER, MATCHER`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { `Given a Probation Region`() }
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

        val lostBeds = lostBedsEntityFactory.produceAndPersist {
          withStartDate(LocalDate.now().plusDays(2))
          withEndDate(LocalDate.now().plusDays(4))
          withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
          withBed(bed)
          withPremises(premises)
        }

        val reason = lostBedReasonEntityFactory.produceAndPersist {
          withServiceScope(ServiceName.approvedPremises.value)
        }

        webTestClient.put()
          .uri("/cas1/premises/${premises.id}/lost-beds/${lostBeds.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateLostBed(
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
          .jsonPath(".startDate").isEqualTo("2022-08-17")
          .jsonPath(".endDate").isEqualTo("2022-08-18")
          .jsonPath(".bedId").isEqualTo(bed.id.toString())
          .jsonPath(".reason.id").isEqualTo(reason.id.toString())
          .jsonPath(".reason.name").isEqualTo(reason.name)
          .jsonPath(".reason.isActive").isEqualTo(true)
          .jsonPath(".referenceNumber").isEqualTo("REF-123")
          .jsonPath(".notes").isEqualTo("notes")
          .jsonPath(".status").isEqualTo("active")
          .jsonPath(".cancellation").isEqualTo(null)
      }
    }

    @Test
    fun `Update Lost Beds returns 409 Conflict when a booking for the same bed overlaps`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { `Given a Probation Region`() }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val lostBeds = lostBedsEntityFactory.produceAndPersist {
          withStartDate(LocalDate.parse("2022-08-16"))
          withEndDate(LocalDate.parse("2022-08-30"))
          withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
          withYieldedBed { bed }
          withPremises(premises)
        }

        val reason = lostBedReasonEntityFactory.produceAndPersist {
          withServiceScope(ServiceName.approvedPremises.value)
        }

        val existingBooking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.approvedPremises)
          withCrn("CRN123")
          withYieldedPremises { premises }
          withYieldedBed { bed }
          withArrivalDate(LocalDate.parse("2022-07-15"))
          withDepartureDate(LocalDate.parse("2022-08-15"))
        }

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.put()
          .uri("/cas1/premises/${premises.id}/lost-beds/${lostBeds.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateLostBed(
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
          .jsonPath("detail").isEqualTo("A Booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingBooking.id}")
      }
    }

    @Test
    fun `Update Lost Beds returns OK with correct body when only cancelled bookings for the same bed overlap`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion { `Given a Probation Region`() }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val lostBeds = lostBedsEntityFactory.produceAndPersist {
            withStartDate(LocalDate.parse("2022-08-16"))
            withEndDate(LocalDate.parse("2022-08-30"))
            withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
            withYieldedBed { bed }
            withPremises(premises)
          }

          val reason = lostBedReasonEntityFactory.produceAndPersist {
            withServiceScope(ServiceName.approvedPremises.value)
          }

          val existingBooking = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.approvedPremises)
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises { premises }
            withYieldedBed { bed }
            withArrivalDate(LocalDate.parse("2022-07-15"))
            withDepartureDate(LocalDate.parse("2022-08-15"))
          }

          existingBooking.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
            withYieldedBooking { existingBooking }
            withDate(LocalDate.parse("2022-07-01"))
            withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          }.toMutableList()

          webTestClient.put()
            .uri("/cas1/premises/${premises.id}/lost-beds/${lostBeds.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdateLostBed(
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
            .jsonPath(".startDate").isEqualTo("2022-08-01")
            .jsonPath(".endDate").isEqualTo("2022-08-30")
            .jsonPath(".bedId").isEqualTo(bed.id.toString())
            .jsonPath(".reason.id").isEqualTo(reason.id.toString())
            .jsonPath(".reason.name").isEqualTo(reason.name)
            .jsonPath(".reason.isActive").isEqualTo(true)
            .jsonPath(".referenceNumber").isEqualTo("REF-123")
            .jsonPath(".notes").isEqualTo("notes")
            .jsonPath(".status").isEqualTo("active")
            .jsonPath(".cancellation").isEqualTo(null)
        }
      }
    }

    @Test
    fun `Update Lost Beds returns 409 Conflict when a lost bed for the same bed overlaps`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        `Given an Offender` { _, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion { `Given a Probation Region`() }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val reason = lostBedReasonEntityFactory.produceAndPersist {
            withServiceScope(ServiceName.approvedPremises.value)
          }

          val lostBeds = lostBedsEntityFactory.produceAndPersist {
            withStartDate(LocalDate.parse("2022-08-16"))
            withEndDate(LocalDate.parse("2022-08-30"))
            withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
            withYieldedBed { bed }
            withPremises(premises)
          }

          val existingLostBed = lostBedsEntityFactory.produceAndPersist {
            withBed(bed)
            withPremises(premises)
            withStartDate(LocalDate.parse("2022-07-15"))
            withEndDate(LocalDate.parse("2022-08-15"))
            withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
          }

          webTestClient.put()
            .uri("/cas1/premises/${premises.id}/lost-beds/${lostBeds.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdateLostBed(
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
            .jsonPath("detail").isEqualTo("A Lost Bed already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingLostBed.id}")
        }
      }
    }

    @Test
    fun `Update Lost Beds returns OK with correct body when only cancelled lost beds for the same bed overlap`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        `Given an Offender` { _, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion { `Given a Probation Region`() }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val reason = lostBedReasonEntityFactory.produceAndPersist {
            withServiceScope(ServiceName.approvedPremises.value)
          }

          val lostBeds = lostBedsEntityFactory.produceAndPersist {
            withStartDate(LocalDate.parse("2022-08-16"))
            withEndDate(LocalDate.parse("2022-08-30"))
            withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
            withYieldedBed { bed }
            withPremises(premises)
          }

          val existingLostBed = lostBedsEntityFactory.produceAndPersist {
            withBed(bed)
            withPremises(premises)
            withStartDate(LocalDate.parse("2022-07-15"))
            withEndDate(LocalDate.parse("2022-08-15"))
            withYieldedReason {
              lostBedReasonEntityFactory.produceAndPersist()
            }
          }

          existingLostBed.cancellation = lostBedCancellationEntityFactory.produceAndPersist {
            withLostBed(existingLostBed)
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }

          webTestClient.put()
            .uri("/cas1/premises/${premises.id}/lost-beds/${lostBeds.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdateLostBed(
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
            .jsonPath(".startDate").isEqualTo("2022-08-01")
            .jsonPath(".endDate").isEqualTo("2022-08-30")
            .jsonPath(".bedId").isEqualTo(bed.id.toString())
            .jsonPath(".reason.id").isEqualTo(reason.id.toString())
            .jsonPath(".reason.name").isEqualTo(reason.name)
            .jsonPath(".reason.isActive").isEqualTo(true)
            .jsonPath(".referenceNumber").isEqualTo("REF-123")
            .jsonPath(".notes").isEqualTo("notes")
            .jsonPath(".status").isEqualTo("active")
            .jsonPath(".cancellation").isEqualTo(null)
        }
      }
    }
  }

  @Nested
  inner class CancelLostBed {
    @Test
    fun `Cancel Lost Bed without JWT returns 401`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { `Given a Probation Region`() }
      }

      val lostBeds = lostBedsEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        withBed(
          bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          },
        )
        withPremises(premises)
      }

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/lost-beds/${lostBeds.id}/cancellations")
        .bodyValue(
          NewLostBedCancellation(
            notes = "Unauthorized",
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Cancel Lost Bed for non-existent premises returns 404`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.post()
        .uri("/cas1/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/lost-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488/cancellations")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewLostBedCancellation(
            notes = "Non-existent premises",
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `Cancel Lost Bed for non-existent lost bed returns 404`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { `Given a Probation Region`() }
      }

      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/lost-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488/cancellations")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewLostBedCancellation(
            notes = "Non-existent lost bed",
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_LEGACY_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Cancel Lost Bed returns OK with correct body when user has one of roles LEGACY_MANAGER, MANAGER, MATCHER`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { `Given an AP Area`() } }
          }
        }

        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
        }

        val lostBeds = lostBedsEntityFactory.produceAndPersist {
          withStartDate(LocalDate.now().plusDays(2))
          withEndDate(LocalDate.now().plusDays(4))
          withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
          withBed(
            bedEntityFactory.produceAndPersist {
              withYieldedRoom {
                roomEntityFactory.produceAndPersist {
                  withYieldedPremises { premises }
                }
              }
            },
          )
          withPremises(premises)
        }

        lostBedReasonEntityFactory.produceAndPersist {
          withServiceScope(ServiceName.approvedPremises.value)
        }

        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/lost-beds/${lostBeds.id}/cancellations")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewLostBedCancellation(
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
