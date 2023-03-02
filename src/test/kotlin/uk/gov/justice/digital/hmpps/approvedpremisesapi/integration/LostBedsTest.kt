package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApprovedPremisesLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewTemporaryAccommodationLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateTemporaryAccommodationLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedsTransformer
import java.time.LocalDate
import java.util.UUID

class LostBedsTest : IntegrationTestBase() {
  @Autowired
  lateinit var lostBedsTransformer: LostBedsTransformer

  @Test
  fun `List Lost Beds without JWT returns 401`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    webTestClient.get()
      .uri("/premises/${premises.id}/lost-beds")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `List Lost Beds on non existent Premises returns 404`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/lost-beds")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
  fun `List Lost Beds on Approved Premises returns OK with correct body when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val lostBeds = approvedPremisesLostBedsEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        withNumberOfBeds(5)
        withPremises(premises)
      }

      val expectedJson = objectMapper.writeValueAsString(listOf(lostBedsTransformer.transformJpaToApi(lostBeds)))

      webTestClient.get()
        .uri("/premises/${premises.id}/lost-beds")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(expectedJson)
    }
  }

  @Test
  fun `List Lost Beds on Temporary Accommodation premises returns OK with correct body`() {
    `Given a User` { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
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

      val lostBeds = temporaryAccommodationLostBedEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        withYieldedBed { bed }
        withPremises(premises)
      }

      val expectedJson = objectMapper.writeValueAsString(listOf(lostBedsTransformer.transformJpaToApi(lostBeds)))

      webTestClient.get()
        .uri("/premises/${premises.id}/lost-beds")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(expectedJson)
    }
  }

  @Test
  fun `Get Lost Bed without JWT returns 401`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    val lostBeds = approvedPremisesLostBedsEntityFactory.produceAndPersist {
      withStartDate(LocalDate.now().plusDays(2))
      withEndDate(LocalDate.now().plusDays(4))
      withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
      withNumberOfBeds(5)
      withPremises(premises)
    }

    webTestClient.get()
      .uri("/premises/${premises.id}/lost-beds/${lostBeds.id}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Lost Bed for non-existent premises returns 404`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/lost-beds")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Get Lost Bed for non-existent lost bed returns 404`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises/${premises.id}/lost-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
  fun `Get Lost Bed on Approved Premises returns OK with correct body when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val lostBeds = approvedPremisesLostBedsEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        withNumberOfBeds(5)
        withPremises(premises)
      }

      val expectedJson = objectMapper.writeValueAsString(lostBedsTransformer.transformJpaToApi(lostBeds))

      webTestClient.get()
        .uri("/premises/${premises.id}/lost-beds/${lostBeds.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(expectedJson)
    }
  }

  @Test
  fun `Get Lost Bed on Temporary Accommodation premises returns OK with correct body`() {
    `Given a User` { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
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

      val lostBeds = temporaryAccommodationLostBedEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        withYieldedBed { bed }
        withPremises(premises)
      }

      val expectedJson = objectMapper.writeValueAsString(lostBedsTransformer.transformJpaToApi(lostBeds))

      webTestClient.get()
        .uri("/premises/${premises.id}/lost-beds/${lostBeds.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(expectedJson)
    }
  }

  @Test
  fun `Create Lost Beds without JWT returns 401`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    webTestClient.post()
      .uri("/premises/${premises.id}/lost-beds")
      .bodyValue(
        NewApprovedPremisesLostBed(
          startDate = LocalDate.parse("2022-08-15"),
          endDate = LocalDate.parse("2022-08-18"),
          numberOfBeds = 1,
          reason = UUID.randomUUID(),
          referenceNumber = "REF-123",
          notes = null,
          serviceName = ServiceName.approvedPremises,
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create Lost Beds on Temporary Accommodation premises returns 400 Bad Request if the bed ID does not reference a bed on the premises`() {
    `Given a User` { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withYieldedApArea {
              apAreaEntityFactory.produceAndPersist()
            }
          }
        }
      }

      val reason = lostBedReasonEntityFactory.produceAndPersist {
        withServiceScope(ServiceName.temporaryAccommodation.value)
      }

      webTestClient.post()
        .uri("/premises/${premises.id}/lost-beds")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewTemporaryAccommodationLostBed(
            startDate = LocalDate.parse("2022-08-17"),
            endDate = LocalDate.parse("2022-08-18"),
            reason = reason.id,
            referenceNumber = "REF-123",
            notes = "notes",
            bedId = UUID.randomUUID(),
            serviceName = ServiceName.temporaryAccommodation,
          )
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
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
  fun `Create Lost Beds on Approved Premises returns OK with correct body when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withTotalBeds(3)
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val reason = lostBedReasonEntityFactory.produceAndPersist {
        withServiceScope(ServiceName.approvedPremises.value)
      }

      webTestClient.post()
        .uri("/premises/${premises.id}/lost-beds")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewApprovedPremisesLostBed(
            startDate = LocalDate.parse("2022-08-17"),
            endDate = LocalDate.parse("2022-08-18"),
            numberOfBeds = 3,
            reason = reason.id,
            referenceNumber = "REF-123",
            notes = "notes",
            serviceName = ServiceName.approvedPremises,
          )
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath(".startDate").isEqualTo("2022-08-17")
        .jsonPath(".endDate").isEqualTo("2022-08-18")
        .jsonPath(".numberOfBeds").isEqualTo(3)
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
  fun `Create Lost Beds on Temporary Accommodation premises returns OK with correct body when correct data is provided`() {
    `Given a User` { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
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
            withYieldedPremises {
              premises
            }
          }
        }
      }

      val reason = lostBedReasonEntityFactory.produceAndPersist {
        withServiceScope(ServiceName.temporaryAccommodation.value)
      }

      webTestClient.post()
        .uri("/premises/${premises.id}/lost-beds")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewTemporaryAccommodationLostBed(
            startDate = LocalDate.parse("2022-08-17"),
            endDate = LocalDate.parse("2022-08-18"),
            reason = reason.id,
            referenceNumber = "REF-123",
            notes = "notes",
            bedId = bed.id,
            serviceName = ServiceName.temporaryAccommodation,
          )
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
  fun `Create Lost Beds on Approved Premises for current day does not break GET all Premises endpoint`() {
    `Given a User`(roles = listOf(UserRole.MANAGER)) { userEntity, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withTotalBeds(3)
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val lostBed = approvedPremisesLostBedsEntityFactory.produceAndPersist {
        withPremises(premises)
        withStartDate(LocalDate.now().minusDays(2))
        withEndDate(LocalDate.now().plusDays(2))
        withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
      }

      val booking = bookingEntityFactory.produceAndPersist {
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
  fun `Update Lost Bed without JWT returns 401`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    val lostBeds = approvedPremisesLostBedsEntityFactory.produceAndPersist {
      withStartDate(LocalDate.now().plusDays(2))
      withEndDate(LocalDate.now().plusDays(4))
      withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
      withNumberOfBeds(5)
      withPremises(premises)
    }

    webTestClient.put()
      .uri("/premises/${premises.id}/lost-beds/${lostBeds.id}")
      .bodyValue(
        UpdateApprovedPremisesLostBed(
          startDate = LocalDate.parse("2022-08-15"),
          endDate = LocalDate.parse("2022-08-18"),
          numberOfBeds = 1,
          reason = UUID.randomUUID(),
          referenceNumber = "REF-123",
          notes = null,
          serviceName = ServiceName.approvedPremises,
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Update Lost Bed for non-existent premises returns 404`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.put()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/lost-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        UpdateApprovedPremisesLostBed(
          startDate = LocalDate.parse("2022-08-15"),
          endDate = LocalDate.parse("2022-08-18"),
          numberOfBeds = 1,
          reason = UUID.randomUUID(),
          referenceNumber = "REF-123",
          notes = null,
          serviceName = ServiceName.approvedPremises,
        )
      )
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Update Lost Bed for non-existent lost bed returns 404`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.put()
      .uri("/premises/${premises.id}/lost-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        UpdateApprovedPremisesLostBed(
          startDate = LocalDate.parse("2022-08-15"),
          endDate = LocalDate.parse("2022-08-18"),
          numberOfBeds = 1,
          reason = UUID.randomUUID(),
          referenceNumber = "REF-123",
          notes = null,
          serviceName = ServiceName.approvedPremises,
        )
      )
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
  fun `Update Lost Beds on Approved Premises returns OK with correct body when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withTotalBeds(3)
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val lostBeds = approvedPremisesLostBedsEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        withNumberOfBeds(5)
        withPremises(premises)
      }

      val reason = lostBedReasonEntityFactory.produceAndPersist {
        withServiceScope(ServiceName.approvedPremises.value)
      }

      webTestClient.put()
        .uri("/premises/${premises.id}/lost-beds/${lostBeds.id}")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          UpdateApprovedPremisesLostBed(
            startDate = LocalDate.parse("2022-08-17"),
            endDate = LocalDate.parse("2022-08-18"),
            numberOfBeds = 3,
            reason = reason.id,
            referenceNumber = "REF-123",
            notes = "notes",
            serviceName = ServiceName.approvedPremises,
          )
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath(".startDate").isEqualTo("2022-08-17")
        .jsonPath(".endDate").isEqualTo("2022-08-18")
        .jsonPath(".numberOfBeds").isEqualTo(3)
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
  fun `Update Lost Beds on Temporary Accommodation premises returns OK with correct body when correct data is provided`() {
    `Given a User` { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
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

      val lostBeds = temporaryAccommodationLostBedEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        withYieldedBed { bed }
        withPremises(premises)
      }

      val reason = lostBedReasonEntityFactory.produceAndPersist {
        withServiceScope(ServiceName.temporaryAccommodation.value)
      }

      webTestClient.put()
        .uri("/premises/${premises.id}/lost-beds/${lostBeds.id}")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          UpdateTemporaryAccommodationLostBed(
            startDate = LocalDate.parse("2022-08-17"),
            endDate = LocalDate.parse("2022-08-18"),
            reason = reason.id,
            referenceNumber = "REF-123",
            notes = "notes",
            serviceName = ServiceName.temporaryAccommodation,
          )
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
}
