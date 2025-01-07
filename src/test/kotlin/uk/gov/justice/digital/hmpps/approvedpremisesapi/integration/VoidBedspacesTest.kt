package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3VoidBedspacesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.withinSeconds
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("LargeClass")
class VoidBedspacesTest : IntegrationTestBase() {
  @Autowired
  lateinit var cas3VoidBedspacesTransformer: Cas3VoidBedspacesTransformer

  lateinit var probationRegion: ProbationRegionEntity

  @BeforeEach
  fun before() {
    probationRegion = givenAProbationRegion()
  }

  @Test
  fun `List Void Bedspaces without JWT returns 401`() {
    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegion }
    }

    webTestClient.get()
      .uri("/premises/${premises.id}/lost-beds")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `List Void Bedspaces on non existent Premises returns 404`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/lost-beds")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `List Void Bedspaces on Temporary Accommodation premises returns OK with correct body`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { probationRegion }
      }

      val voidBedspaces = cas3VoidBedspacesEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
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

      val expectedJson = objectMapper.writeValueAsString(listOf(cas3VoidBedspacesTransformer.transformJpaToApi(voidBedspaces)))

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
  fun `List Void Bedspaces on Temporary Accommodation premises that's not in the user's region returns 403 Forbidden`() {
    givenAUser { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { probationRegion }
      }

      val bed = bedEntityFactory.produceAndPersist {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
          }
        }
      }

      cas3VoidBedspacesEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        withYieldedBed { bed }
        withPremises(premises)
      }

      webTestClient.get()
        .uri("/premises/${premises.id}/lost-beds")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get Void Bedspace without JWT returns 401`() {
    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegion }
    }

    val voidBedspaces = cas3VoidBedspacesEntityFactory.produceAndPersist {
      withStartDate(LocalDate.now().plusDays(2))
      withEndDate(LocalDate.now().plusDays(4))
      withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
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
      .uri("/premises/${premises.id}/lost-beds/${voidBedspaces.id}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Void Bedspace for non-existent premises returns 404`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/lost-beds")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Get Void Bedspace for non-existent void bedspace returns 404`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { probationRegion }
      }

      webTestClient.get()
        .uri("/premises/${premises.id}/lost-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Get Void Bedspace on Temporary Accommodation premises returns OK with correct body`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { probationRegion }
      }

      val bed = bedEntityFactory.produceAndPersist {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
          }
        }
      }

      val voidBedspaces = cas3VoidBedspacesEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        withYieldedBed { bed }
        withPremises(premises)
      }

      val expectedJson = objectMapper.writeValueAsString(cas3VoidBedspacesTransformer.transformJpaToApi(voidBedspaces))

      webTestClient.get()
        .uri("/premises/${premises.id}/lost-beds/${voidBedspaces.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(expectedJson)
    }
  }

  @Test
  fun `Get Void Bedspace on Temporary Accommodation premises that's not in the user's region returns 403 Forbidden`() {
    givenAUser { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { probationRegion }
      }

      val bed = bedEntityFactory.produceAndPersist {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
          }
        }
      }

      val voidBedspaces = cas3VoidBedspacesEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        withYieldedBed { bed }
        withPremises(premises)
      }

      val expectedJson = objectMapper.writeValueAsString(cas3VoidBedspacesTransformer.transformJpaToApi(voidBedspaces))

      webTestClient.get()
        .uri("/premises/${premises.id}/lost-beds/${voidBedspaces.id}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Create Void Bedspaces without JWT returns 401`() {
    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegion }
    }

    val bed = bedEntityFactory.produceAndPersist {
      withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
    }

    webTestClient.post()
      .uri("/premises/${premises.id}/lost-beds")
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
  fun `Create Void Bedspaces on Temporary Accommodation premises returns 400 Bad Request if the bed ID does not reference a bed on the premises`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { probationRegion }
      }

      val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist {
        withServiceScope(ServiceName.temporaryAccommodation.value)
      }

      webTestClient.post()
        .uri("/premises/${premises.id}/lost-beds")
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

  @Test
  fun `Create Void Bedspaces on Temporary Accommodation premises returns OK with correct body when correct data is provided`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { probationRegion }
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

      val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist {
        withServiceScope(ServiceName.temporaryAccommodation.value)
      }

      webTestClient.post()
        .uri("/premises/${premises.id}/lost-beds")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewLostBed(
            startDate = LocalDate.parse("2022-08-17"),
            endDate = LocalDate.parse("2022-08-18"),
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
  fun `Create Void Bedspaces on Temporary Accommodation premises that's not in the user's region returns 403 Forbidden`() {
    givenAUser { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { probationRegion }
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

      val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist {
        withServiceScope(ServiceName.temporaryAccommodation.value)
      }

      webTestClient.post()
        .uri("/premises/${premises.id}/lost-beds")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewLostBed(
            startDate = LocalDate.parse("2022-08-17"),
            endDate = LocalDate.parse("2022-08-18"),
            reason = reason.id,
            referenceNumber = "REF-123",
            notes = "notes",
            bedId = bed.id,
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Update Void Bedspace without JWT returns 401`() {
    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegion }
    }

    val voidBedspaces = cas3VoidBedspacesEntityFactory.produceAndPersist {
      withStartDate(LocalDate.now().plusDays(2))
      withEndDate(LocalDate.now().plusDays(4))
      withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
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

    val bed = bedEntityFactory.produceAndPersist {
      withYieldedRoom {
        roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
        }
      }
    }

    webTestClient.put()
      .uri("/premises/${premises.id}/lost-beds/${voidBedspaces.id}")
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
  fun `Update Void Bedspace for non-existent premises returns 404`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.put()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/lost-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488")
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
  fun `Update Void Bedspace for non-existent void bedspace returns 404`() {
    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegion }
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.put()
      .uri("/premises/${premises.id}/lost-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488")
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
  fun `Update Void Bedspaces on Temporary Accommodation premises returns OK with correct body when correct data is provided`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { probationRegion }
      }

      val bed = bedEntityFactory.produceAndPersist {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
          }
        }
      }

      val voidBedspaces = cas3VoidBedspacesEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        withYieldedBed { bed }
        withPremises(premises)
      }

      val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist {
        withServiceScope(ServiceName.temporaryAccommodation.value)
      }

      webTestClient.put()
        .uri("/premises/${premises.id}/lost-beds/${voidBedspaces.id}")
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
  fun `Update Void Bedspaces on Temporary Accommodation premises that's not in the user's region returns 403 Forbidden`() {
    givenAUser { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { probationRegion }
      }

      val bed = bedEntityFactory.produceAndPersist {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
          }
        }
      }

      val voidBedspaces = cas3VoidBedspacesEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        withYieldedBed { bed }
        withPremises(premises)
      }

      val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist {
        withServiceScope(ServiceName.temporaryAccommodation.value)
      }

      webTestClient.put()
        .uri("/premises/${premises.id}/lost-beds/${voidBedspaces.id}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
        .isForbidden
    }
  }

  @Test
  fun `Cancel Void Bedspace without JWT returns 401`() {
    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegion }
    }

    val voidBedspaces = cas3VoidBedspacesEntityFactory.produceAndPersist {
      withStartDate(LocalDate.now().plusDays(2))
      withEndDate(LocalDate.now().plusDays(4))
      withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
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
      .uri("/premises/${premises.id}/lost-beds/${voidBedspaces.id}/cancellations")
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
  fun `Cancel Void Bedspace for non-existent premises returns 404`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.post()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/lost-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488/cancellations")
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
  fun `Cancel Void Bedspace for non-existent void bedspace returns 404`() {
    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegion }
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.post()
      .uri("/premises/${premises.id}/lost-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488/cancellations")
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

  @Test
  fun `Cancel Void Bedspace on Temporary Accommodation premises returns OK with correct body when correct data is provided`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { probationRegion }
      }

      val bed = bedEntityFactory.produceAndPersist {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
          }
        }
      }

      val voidBedspaces = cas3VoidBedspacesEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        withYieldedBed { bed }
        withPremises(premises)
      }

      val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist {
        withServiceScope(ServiceName.temporaryAccommodation.value)
      }

      webTestClient.post()
        .uri("/premises/${premises.id}/lost-beds/${voidBedspaces.id}/cancellations")
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

  @Test
  fun `Cancel Void Bedspace on Temporary Accommodation premises that's not in the user's region returns 403 Forbidden`() {
    givenAUser { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { probationRegion }
      }

      val bed = bedEntityFactory.produceAndPersist {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
          }
        }
      }

      val voidBedspaces = cas3VoidBedspacesEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        withYieldedBed { bed }
        withPremises(premises)
      }

      val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist {
        withServiceScope(ServiceName.temporaryAccommodation.value)
      }

      webTestClient.post()
        .uri("/premises/${premises.id}/lost-beds/${voidBedspaces.id}/cancellations")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewLostBedCancellation(
            notes = "Some cancellation notes",
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Create Void Bedspace on a Temporary Accommodation premises returns 409 Conflict when a booking for the same bed overlaps`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { probationRegion }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist {
          withServiceScope(ServiceName.temporaryAccommodation.value)
        }

        val existingBooking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn("CRN123")
          withYieldedPremises { premises }
          withYieldedBed { bed }
          withArrivalDate(LocalDate.parse("2022-07-15"))
          withDepartureDate(LocalDate.parse("2022-08-15"))
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/lost-beds")
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
          .jsonPath("detail").isEqualTo("A Booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingBooking.id}")
      }
    }
  }

  @Test
  fun `Create Void Bedspace on a Temporary Accommodation premises returns OK with correct body when only cancelled bookings for the same bed overlap`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { probationRegion }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist {
          withServiceScope(ServiceName.temporaryAccommodation.value)
        }

        val existingBooking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
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

        webTestClient.post()
          .uri("/premises/${premises.id}/lost-beds")
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

  @Test
  fun `Create Void Bedspace on a Temporary Accommodation premises returns 409 Conflict when a void bedspace for the same bed overlaps`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { probationRegion }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist {
          withServiceScope(ServiceName.temporaryAccommodation.value)
        }

        val existingLostBed = cas3VoidBedspacesEntityFactory.produceAndPersist {
          withBed(bed)
          withPremises(premises)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/lost-beds")
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
  fun `Create Void Bedspace on a Temporary Accommodation premises returns OK with correct body when only cancelled void bedspaces for the same bed overlap`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { probationRegion }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist {
          withServiceScope(ServiceName.temporaryAccommodation.value)
        }

        val existingLostBed = cas3VoidBedspacesEntityFactory.produceAndPersist {
          withBed(bed)
          withPremises(premises)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason {
            cas3VoidBedspaceReasonEntityFactory.produceAndPersist()
          }
        }

        existingLostBed.cancellation = cas3VoidBedspaceCancellationEntityFactory.produceAndPersist {
          withVoidBedspace(existingLostBed)
          withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/lost-beds")
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

  @Test
  fun `Update Void Bedspaces on Temporary Accommodation premises returns 409 Conflict when a booking for the same bed overlaps`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { probationRegion }
      }

      val bed = bedEntityFactory.produceAndPersist {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
          }
        }
      }

      val voidBedspaces = cas3VoidBedspacesEntityFactory.produceAndPersist {
        withStartDate(LocalDate.parse("2022-08-16"))
        withEndDate(LocalDate.parse("2022-08-30"))
        withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        withYieldedBed { bed }
        withPremises(premises)
      }

      val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist {
        withServiceScope(ServiceName.temporaryAccommodation.value)
      }

      val existingBooking = bookingEntityFactory.produceAndPersist {
        withServiceName(ServiceName.temporaryAccommodation)
        withCrn("CRN123")
        withYieldedPremises { premises }
        withYieldedBed { bed }
        withArrivalDate(LocalDate.parse("2022-07-15"))
        withDepartureDate(LocalDate.parse("2022-08-15"))
      }

      govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

      webTestClient.put()
        .uri("/premises/${premises.id}/lost-beds/${voidBedspaces.id}")
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
  fun `Update Void Bedspaces on Temporary Accommodation premises returns OK with correct body when only cancelled bookings for the same bed overlap`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { probationRegion }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val voidBedspaces = cas3VoidBedspacesEntityFactory.produceAndPersist {
          withStartDate(LocalDate.parse("2022-08-16"))
          withEndDate(LocalDate.parse("2022-08-30"))
          withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          withYieldedBed { bed }
          withPremises(premises)
        }

        val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist {
          withServiceScope(ServiceName.temporaryAccommodation.value)
        }

        val existingBooking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
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
          .uri("/premises/${premises.id}/lost-beds/${voidBedspaces.id}")
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
  fun `Update Void Bedspaces on Temporary Accommodation premises returns 409 Conflict when a void bedspace for the same bed overlaps`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { probationRegion }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist {
          withServiceScope(ServiceName.temporaryAccommodation.value)
        }

        val voidBedspaces = cas3VoidBedspacesEntityFactory.produceAndPersist {
          withStartDate(LocalDate.parse("2022-08-16"))
          withEndDate(LocalDate.parse("2022-08-30"))
          withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          withYieldedBed { bed }
          withPremises(premises)
        }

        val existingLostBed = cas3VoidBedspacesEntityFactory.produceAndPersist {
          withBed(bed)
          withPremises(premises)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/lost-beds/${voidBedspaces.id}")
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
  fun `Update Void Bedspaces on Temporary Accommodation premises returns OK with correct body when only cancelled void bedspaces for the same bed overlap`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { probationRegion }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val reason = cas3VoidBedspaceReasonEntityFactory.produceAndPersist {
          withServiceScope(ServiceName.temporaryAccommodation.value)
        }

        val voidBedspaces = cas3VoidBedspacesEntityFactory.produceAndPersist {
          withStartDate(LocalDate.parse("2022-08-16"))
          withEndDate(LocalDate.parse("2022-08-30"))
          withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          withYieldedBed { bed }
          withPremises(premises)
        }

        val existingLostBed = cas3VoidBedspacesEntityFactory.produceAndPersist {
          withBed(bed)
          withPremises(premises)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason {
            cas3VoidBedspaceReasonEntityFactory.produceAndPersist()
          }
        }

        existingLostBed.cancellation = cas3VoidBedspaceCancellationEntityFactory.produceAndPersist {
          withVoidBedspace(existingLostBed)
          withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/lost-beds/${voidBedspaces.id}")
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
