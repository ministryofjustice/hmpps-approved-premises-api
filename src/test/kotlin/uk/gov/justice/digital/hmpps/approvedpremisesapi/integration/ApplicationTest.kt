package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RegistrationClientResponseFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockNotFoundOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockOffenderUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulRegistrationsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockNotFoundInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.RegistrationKeyValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registrations
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationTest : IntegrationTestBase() {
  @Test
  fun `Get all applications without JWT returns 401`() {
    webTestClient.get()
      .uri("/applications")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get all applications returns 200 with correct body - when user does not have roles returns applications they created`() {
    `Given a User` { userEntity, jwt ->
      `Given a User` { otherUser, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          `Given an Offender` { otherOffenderDetails, otherInmateDetails ->
            approvedPremisesApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
              withSchema(
                """
          {
            "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
            "${"\$id"}": "https://example.com/product.schema.json",
            "title": "Thing",
            "description": "A thing",
            "type": "object",
            "properties": {
              "thingId": {
                "description": "The unique identifier for a thing",
                "type": "integer"
              }
            },
            "required": [ "thingId" ]
          }
          """
              )
            }

            val olderJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.parse("2022-09-21T09:45:00+01:00"))
              withSchema(
                """
              {
                "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
                "${"\$id"}": "https://example.com/product.schema.json",
                "title": "Thing",
                "description": "A thing",
                "type": "object",
                "properties": { }
              }
            """
              )
            }

            val upToDateApplicationEntityCreatedByUser = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(newestJsonSchema)
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
              withData(
                """
                {
                   "thingId": 123
                }
              """
              )
            }

            val outdatedApplicationEntityCreatedByUser = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(olderJsonSchema)
              withCreatedByUser(userEntity)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
            }

            val outdatedApplicationEntityNotCreatedByUser = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(olderJsonSchema)
              withCreatedByUser(otherUser)
              withCrn(otherOffenderDetails.otherIds.crn)
              withData("{}")
            }

            CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

            val rawResponseBody = webTestClient.get()
              .uri("/applications")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(rawResponseBody, object : TypeReference<List<ApprovedPremisesApplication>>() {})

            assertThat(responseBody).anyMatch {
              outdatedApplicationEntityCreatedByUser.id == it.id &&
                outdatedApplicationEntityCreatedByUser.crn == it.person?.crn &&
                outdatedApplicationEntityCreatedByUser.createdAt.toInstant() == it.createdAt.toInstant() &&
                outdatedApplicationEntityCreatedByUser.createdByUser.id == it.createdByUserId &&
                outdatedApplicationEntityCreatedByUser.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
                serializableToJsonNode(outdatedApplicationEntityCreatedByUser.data) == serializableToJsonNode(it.data) &&
                olderJsonSchema.id == it.schemaVersion && it.outdatedSchema
            }

            assertThat(responseBody).anyMatch {
              upToDateApplicationEntityCreatedByUser.id == it.id &&
                upToDateApplicationEntityCreatedByUser.crn == it.person?.crn &&
                upToDateApplicationEntityCreatedByUser.createdAt.toInstant() == it.createdAt.toInstant() &&
                upToDateApplicationEntityCreatedByUser.createdByUser.id == it.createdByUserId &&
                upToDateApplicationEntityCreatedByUser.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
                serializableToJsonNode(upToDateApplicationEntityCreatedByUser.data) == serializableToJsonNode(it.data) &&
                newestJsonSchema.id == it.schemaVersion && !it.outdatedSchema
            }

            assertThat(responseBody).noneMatch {
              outdatedApplicationEntityNotCreatedByUser.id == it.id
            }
          }
        }
      }
    }
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["WORKFLOW_MANAGER", "ASSESSOR", "MATCHER", "MANAGER"])
  fun `Get all applications returns 200 with correct body - when user has one of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER returns all applications`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      `Given a User` { otherUser, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          `Given an Offender` { otherOffenderDetails, otherInmateDetails ->
            approvedPremisesApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
              withSchema(
                """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": {
            "thingId": {
              "description": "The unique identifier for a thing",
              "type": "integer"
            }
          },
          "required": [ "thingId" ]
        }
        """
              )
            }

            val olderJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.parse("2022-09-21T09:45:00+01:00"))
              withSchema(
                """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": { }
        }
        """
              )
            }

            val upToDateApplicationEntityCreatedByUser = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(newestJsonSchema)
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
              withData(
                """
          {
             "thingId": 123
          }
          """
              )
            }

            val outdatedApplicationEntityCreatedByUser = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(olderJsonSchema)
              withCreatedByUser(userEntity)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
            }

            val outdatedApplicationEntityNotCreatedByUser = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(olderJsonSchema)
              withCreatedByUser(otherUser)
              withCrn(otherOffenderDetails.otherIds.crn)
              withData("{}")
            }

            val applicationEntityWithAwaitingAnswerState = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(newestJsonSchema)
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
            }
            val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
              withAddedAt(OffsetDateTime.now())
            }
            val assessment = assessmentEntityFactory.produceAndPersist {
              withApplication(applicationEntityWithAwaitingAnswerState)
              withAssessmentSchema(assessmentSchema)
              withAllocatedToUser(otherUser)
            }

            assessmentClarificationNoteEntityFactory.produceAndPersist {
              withAssessment(assessment)
              withCreatedBy(otherUser)
            }

            CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)
            CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, otherOffenderDetails.otherIds.crn, false, false)

            val rawResponseBody = webTestClient.get()
              .uri("/applications")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(rawResponseBody, object : TypeReference<List<ApprovedPremisesApplication>>() {})

            assertThat(responseBody).anyMatch {
              outdatedApplicationEntityCreatedByUser.id == it.id &&
                outdatedApplicationEntityCreatedByUser.crn == it.person?.crn &&
                outdatedApplicationEntityCreatedByUser.createdAt.toInstant() == it.createdAt.toInstant() &&
                outdatedApplicationEntityCreatedByUser.createdByUser.id == it.createdByUserId &&
                outdatedApplicationEntityCreatedByUser.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
                serializableToJsonNode(outdatedApplicationEntityCreatedByUser.data) == serializableToJsonNode(it.data) &&
                olderJsonSchema.id == it.schemaVersion && it.outdatedSchema
            }

            assertThat(responseBody).anyMatch {
              upToDateApplicationEntityCreatedByUser.id == it.id &&
                upToDateApplicationEntityCreatedByUser.crn == it.person?.crn &&
                upToDateApplicationEntityCreatedByUser.createdAt.toInstant() == it.createdAt.toInstant() &&
                upToDateApplicationEntityCreatedByUser.createdByUser.id == it.createdByUserId &&
                upToDateApplicationEntityCreatedByUser.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
                serializableToJsonNode(upToDateApplicationEntityCreatedByUser.data) == serializableToJsonNode(it.data) &&
                newestJsonSchema.id == it.schemaVersion && !it.outdatedSchema
            }

            assertThat(responseBody).anyMatch {
              outdatedApplicationEntityNotCreatedByUser.id == it.id &&
                outdatedApplicationEntityNotCreatedByUser.crn == it.person?.crn &&
                outdatedApplicationEntityNotCreatedByUser.createdAt.toInstant() == it.createdAt.toInstant() &&
                outdatedApplicationEntityNotCreatedByUser.createdByUser.id == it.createdByUserId &&
                outdatedApplicationEntityNotCreatedByUser.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
                serializableToJsonNode(outdatedApplicationEntityNotCreatedByUser.data) == serializableToJsonNode(it.data) &&
                olderJsonSchema.id == it.schemaVersion && it.outdatedSchema
            }

            assertThat(responseBody).anyMatch {
              applicationEntityWithAwaitingAnswerState.id == it.id &&
                it.status == ApplicationStatus.requestedFurtherInformation
            }
          }
        }
      }
    }
  }

  @Test
  fun `Get list of applications returns 500 when a person cannot be found`() {
    `Given a User` { userEntity, jwt ->
      val crn = "X1234"

      produceAndPersistBasicApplication(crn, userEntity)
      CommunityAPI_mockNotFoundOffenderDetailsCall(crn)

      CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, crn, false, false)

      webTestClient.get()
        .uri("/applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .is5xxServerError
        .expectBody()
        .jsonPath("$.detail").isEqualTo("Unable to get Person via crn: $crn")
    }
  }

  @Test
  fun `Get list of applications returns 500 when a person has no NOMS number`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender`(
        offenderDetailsConfigBlock = { withoutNomsNumber() }
      ) { offenderDetails, inmateDetails ->
        produceAndPersistBasicApplication(offenderDetails.otherIds.crn, userEntity)

        CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

        webTestClient.get()
          .uri("/applications")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .is5xxServerError
          .expectBody()
          .jsonPath("$.detail").isEqualTo("No nomsNumber present for CRN")
      }
    }
  }

  @Test
  fun `Get list of applications returns 500 when the person cannot be fetched from the prisons API`() {
    `Given a User` { userEntity, jwt ->
      val crn = "X1234"

      produceAndPersistBasicApplication(crn, userEntity)

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber("ABC123")
        .produce()

      CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

      CommunityAPI_mockSuccessfulOffenderDetailsCall(offenderDetails)
      offenderDetails.otherIds.nomsNumber?.let { PrisonAPI_mockNotFoundInmateDetailsCall(it) }

      webTestClient.get()
        .uri("/applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .is5xxServerError
        .expectBody()
        .jsonPath("$.detail").isEqualTo("Unable to get InmateDetail via crn: $crn")
    }
  }

  @Test
  fun `Get single application without JWT returns 401`() {
    webTestClient.get()
      .uri("/applications/9b785e59-b85c-4be0-b271-d9ac287684b6")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get single application returns 200 with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        approvedPremisesApplicationJsonSchemaRepository.deleteAll()

        val newestJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
          withSchema(
            """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": {
            "thingId": {
              "description": "The unique identifier for a thing",
              "type": "integer"
            }
          },
          "required": [ "thingId" ]
        }
        """
          )
        }

        val applicationEntity = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(newestJsonSchema)
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withData(
            """
          {
             "thingId": 123
          }
          """
          )
        }

        CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

        val rawResponseBody = webTestClient.get()
          .uri("/applications/${applicationEntity.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<String>()
          .responseBody
          .blockFirst()

        val responseBody = objectMapper.readValue(rawResponseBody, ApprovedPremisesApplication::class.java)

        assertThat(responseBody).matches {
          applicationEntity.id == it.id &&
            applicationEntity.crn == it.person.crn &&
            applicationEntity.createdAt.toInstant() == it.createdAt.toInstant() &&
            applicationEntity.createdByUser.id == it.createdByUserId &&
            applicationEntity.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
            serializableToJsonNode(applicationEntity.data) == serializableToJsonNode(it.data) &&
            newestJsonSchema.id == it.schemaVersion && !it.outdatedSchema
        }
      }
    }
  }

  @Test
  fun `Get single application returns 403 when caller did not create application and user is not one of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER`() {
    `Given a User` { userEntity, jwt ->
      `Given a User` { otherUser, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val crn = "X1234"

          val application = produceAndPersistBasicApplication(crn, otherUser)

          CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

          webTestClient.get()
            .uri("/applications/${application.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }
  }

  @Test
  fun `Get single application returns 500 when a person has no NOMS number`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender`(
        offenderDetailsConfigBlock = { withoutNomsNumber() }
      ) { offenderDetails, inmateDetails ->
        val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, userEntity)

        CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

        webTestClient.get()
          .uri("/applications/${application.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .is5xxServerError
          .expectBody()
          .jsonPath("$.detail").isEqualTo("No nomsNumber present for CRN")
      }
    }
  }

  @Test
  fun `Get single application returns 500 when the person cannot be fetched from the prisons API`() {
    `Given a User` { userEntity, jwt ->
      val crn = "X1234"

      val application = produceAndPersistBasicApplication(crn, userEntity)

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber("ABC123")
        .produce()

      CommunityAPI_mockSuccessfulOffenderDetailsCall(offenderDetails)
      offenderDetails.otherIds.nomsNumber?.let { PrisonAPI_mockNotFoundInmateDetailsCall(it) }

      CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

      webTestClient.get()
        .uri("/applications/${application.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .is5xxServerError
        .expectBody()
        .jsonPath("$.detail").isEqualTo("Unable to get InmateDetail via crn: $crn")
    }
  }

  @Test
  fun `Get single online application returns 200 with correct body, non-upgradable outdated application marked as such`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        approvedPremisesApplicationJsonSchemaRepository.deleteAll()

        approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
          withSchema(
            """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": {
            "thingId": {
              "description": "The unique identifier for a thing",
              "type": "integer"
            }
          },
          "required": [ "thingId" ]
        }
        """
          )
        }

        val olderJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.parse("2022-09-21T09:45:00+01:00"))
          withSchema(
            """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": { }
        }
        """
          )
        }

        val nonUpgradableApplicationEntity = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(olderJsonSchema)
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withData("{}")
        }

        CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

        val rawResponseBody = webTestClient.get()
          .uri("/applications/${nonUpgradableApplicationEntity.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<String>()
          .responseBody
          .blockFirst()

        val responseBody = objectMapper.readValue(rawResponseBody, ApprovedPremisesApplication::class.java)

        assertThat(responseBody).matches {
          nonUpgradableApplicationEntity.id == it.id &&
            nonUpgradableApplicationEntity.crn == it.person?.crn &&
            nonUpgradableApplicationEntity.createdAt.toInstant() == it.createdAt.toInstant() &&
            nonUpgradableApplicationEntity.createdByUser.id == it.createdByUserId &&
            nonUpgradableApplicationEntity.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
            serializableToJsonNode(nonUpgradableApplicationEntity.data) == serializableToJsonNode(it.data) &&
            olderJsonSchema.id == it.schemaVersion && it.outdatedSchema
        }
      }
    }
  }

  @Test
  fun `Get single offline application returns 200 with correct body`() {
    `Given a User`(roles = listOf(UserRole.MANAGER)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val offlineApplicationEntity = offlineApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
        }

        CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

        val rawResponseBody = webTestClient.get()
          .uri("/applications/${offlineApplicationEntity.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<String>()
          .responseBody
          .blockFirst()

        val responseBody = objectMapper.readValue(rawResponseBody, OfflineApplication::class.java)

        assertThat(responseBody).matches {
          offlineApplicationEntity.id == it.id &&
            offlineApplicationEntity.crn == it.person.crn &&
            offlineApplicationEntity.createdAt.toInstant() == it.createdAt.toInstant() &&
            offlineApplicationEntity.submittedAt.toInstant() == it.submittedAt?.toInstant()
        }
      }
    }
  }

  @Test
  fun `Create new application without JWT returns 401`() {
    webTestClient.post()
      .uri("/applications")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create new application returns 500 when a person cannot be found`() {
    val crn = "X1234"
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    CommunityAPI_mockNotFoundOffenderDetailsCall(crn)

    approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
    }

    webTestClient.post()
      .uri("/applications")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewApplication(
          crn = crn
        )
      )
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("$.detail").isEqualTo("Unable to get Person via crn: $crn")
  }

  @Test
  fun `Create new application returns 500 when a person has no NOMS number`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender`(
        offenderDetailsConfigBlock = { withoutNomsNumber() }
      ) { offenderDetails, inmateDetails ->
        approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.now())
          withId(UUID.randomUUID())
        }

        webTestClient.post()
          .uri("/applications")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewApplication(
              crn = offenderDetails.otherIds.crn
            )
          )
          .exchange()
          .expectStatus()
          .is5xxServerError
          .expectBody()
          .jsonPath("$.detail").isEqualTo("No nomsNumber present for CRN")
      }
    }
  }

  @Test
  fun `Create new application returns 500 when the person cannot be fetched from the prisons API`() {
    `Given a User` { userEntity, jwt ->
      val offenderDetails = OffenderDetailsSummaryFactory()
        .withNomsNumber("ABC123")
        .produce()

      CommunityAPI_mockSuccessfulOffenderDetailsCall(offenderDetails)
      offenderDetails.otherIds.nomsNumber?.let { PrisonAPI_mockNotFoundInmateDetailsCall(it) }

      webTestClient.post()
        .uri("/applications")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewApplication(
            crn = offenderDetails.otherIds.crn
          )
        )
        .exchange()
        .expectStatus()
        .is5xxServerError
        .expectBody()
        .jsonPath("$.detail").isEqualTo("Unable to get InmateDetail via crn: ${offenderDetails.otherIds.crn}")
    }
  }

  @Test
  fun `Create new application returns 201 with correct body and Location header`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.now())
          withId(UUID.randomUUID())
        }

        val result = webTestClient.post()
          .uri("/applications")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewApplication(
              crn = offenderDetails.otherIds.crn,
              convictionId = 123,
              deliusEventNumber = "1",
              offenceId = "789"
            )
          )
          .exchange()
          .expectStatus()
          .isCreated
          .returnResult(ApprovedPremisesApplication::class.java)

        assertThat(result.responseHeaders["Location"]).anyMatch {
          it.matches(Regex("/applications/.+"))
        }

        assertThat(result.responseBody.blockFirst()).matches {
          it.person.crn == offenderDetails.otherIds.crn &&
            it.schemaVersion == applicationSchema.id
        }
      }
    }
  }

  @Test
  fun `Create new application without risks returns 201 with correct body and Location header`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.now())
          withId(UUID.randomUUID())
        }

        val result = webTestClient.post()
          .uri("/applications?createWithRisks=false")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewApplication(
              crn = offenderDetails.otherIds.crn,
              convictionId = 123,
              deliusEventNumber = "1",
              offenceId = "789"
            )
          )
          .exchange()
          .expectStatus()
          .isCreated
          .returnResult(ApprovedPremisesApplication::class.java)

        assertThat(result.responseHeaders["Location"]).anyMatch {
          it.matches(Regex("/applications/.+"))
        }

        assertThat(result.responseBody.blockFirst()).matches {
          it.person.crn == offenderDetails.otherIds.crn &&
            it.schemaVersion == applicationSchema.id
        }
      }
    }
  }

  @Test
  fun `Update existing application returns 200 with correct body`() {
    `Given a User` { submittingUser, jwt ->
      `Given a User`(roles = listOf(UserRole.ASSESSOR), qualifications = listOf(UserQualification.PIPE)) { assessorUser, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
            withSchema(
              """
              {
                "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
                "${"\$id"}": "https://example.com/product.schema.json",
                "title": "Thing",
                "description": "A thing",
                "type": "object",
                "properties": {
                  "thingId": {
                    "description": "The unique identifier for a thing",
                    "type": "integer"
                  }
                },
                "required": [ "thingId" ]
              }
            """
            )
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withId(applicationId)
            withApplicationSchema(applicationSchema)
            withCreatedByUser(submittingUser)
          }

          val resultBody = webTestClient.put()
            .uri("/applications/$applicationId")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdateApplication(
                data = mapOf("thingId" to 123),
                isWomensApplication = false,
                isPipeApplication = true
              )
            )
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(String::class.java)
            .responseBody
            .blockFirst()

          val result = objectMapper.readValue(resultBody, Application::class.java)

          assertThat(result.person.crn).isEqualTo(offenderDetails.otherIds.crn)
        }
      }
    }
  }

  @Test
  fun `Submit application returns 200, creates and allocates an assessment, saves a domain event`() {
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(
          listOf(
            StaffUserTeamMembershipFactory().produce()
          )
        )
      }
    ) { submittingUser, jwt ->
      `Given a User`(roles = listOf(UserRole.ASSESSOR), qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS)) { assessorUser, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
            withSchema(
              """
              {
                "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
                "${"\$id"}": "https://example.com/product.schema.json",
                "title": "Thing",
                "description": "A thing",
                "type": "object",
                "properties": {
                  "isWomensApplication": {
                    "description": "whether this is a womens application",
                    "type": "boolean"
                  },
                  "isPipeApplication": {
                    "description": "whether this is a PIPE application",
                    "type": "boolean"
                  }
                },
                "required": [ "isWomensApplication", "isPipeApplication" ]
              }
            """
            )
            withIsPipeJsonLogicRule("""{"var": "isPipeApplication"}""")
            withIsWomensJsonLogicRule("""{"var": "isWomensApplication"}""")
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withId(applicationId)
            withApplicationSchema(applicationSchema)
            withCreatedByUser(submittingUser)
            withData(
              """
              {
                 "isWomensApplication": true,
                 "isPipeApplication": true
              }
            """
            )
          }

          CommunityAPI_mockSuccessfulRegistrationsCall(
            offenderDetails.otherIds.crn,
            Registrations(
              registrations = listOf(
                RegistrationClientResponseFactory()
                  .withType(
                    RegistrationKeyValue(
                      code = "MAPP",
                      description = "MAPPA"
                    )
                  )
                  .withRegisterCategory(
                    RegistrationKeyValue(
                      code = "A",
                      description = "A"
                    )
                  )
                  .withRegisterLevel(
                    RegistrationKeyValue(
                      code = "1",
                      description = "1"
                    )
                  )
                  .produce()
              )
            )
          )

          webTestClient.post()
            .uri("/applications/$applicationId/submission")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              SubmitApplication(
                translatedDocument = mapOf("isWomensApplication" to true, "isPipeApplication" to true)
              )
            )
            .exchange()
            .expectStatus()
            .isOk

          val persistedApplication = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)!! as ApprovedPremisesApplicationEntity

          assertThat(persistedApplication.isWomensApplication).isTrue
          assertThat(persistedApplication.isPipeApplication).isTrue

          val createdAssessment = assessmentRepository.findAll().first { it.application.id == applicationId }
          assertThat(createdAssessment.allocatedToUser.id).isEqualTo(assessorUser.id)

          val persistedDomainEvent = domainEventRepository.findAll().firstOrNull { it.applicationId == applicationId }

          assertThat(persistedDomainEvent).isNotNull
          assertThat(persistedDomainEvent!!.crn).isEqualTo(offenderDetails.otherIds.crn)
          assertThat(persistedDomainEvent.type).isEqualTo(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
        }
      }
    }
  }

  @Test
  fun `Reallocate application to different assessor without JWT returns 401`() {
    webTestClient.post()
      .uri("/applications/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
      .bodyValue(
        Reallocation(
          userId = UUID.randomUUID()
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Reallocate application to different assessor without WORKFLOW_MANAGER role returns 403`() {
    `Given a User` { userEntity, jwt ->
      webTestClient.post()
        .uri("/applications/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Reallocation(
            userId = UUID.randomUUID()
          )
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Reallocate application to different assessor returns 200, creates new assessment, deallocates old one`() {
    `Given a User`(roles = listOf(UserRole.WORKFLOW_MANAGER)) { requestUser, jwt ->
      `Given a User`(roles = listOf(UserRole.ASSESSOR)) { otherUser, _ ->
        `Given a User`(roles = listOf(UserRole.ASSESSOR)) { assigneeUser, _ ->
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()
          val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist()

          val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(otherUser)
            withApplicationSchema(applicationSchema)
          }

          val existingAssessment = assessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withAllocatedToUser(otherUser)
            withAssessmentSchema(assessmentSchema)
          }

          webTestClient.post()
            .uri("/applications/${application.id}/allocations")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Reallocation(
                userId = assigneeUser.id
              )
            )
            .exchange()
            .expectStatus()
            .isOk

          val assessments = assessmentRepository.findAll()

          assertThat(assessments.first { it.id == existingAssessment.id }.reallocatedAt).isNotNull
          assertThat(assessments).anyMatch { it.application.id == application.id && it.allocatedToUser.id == assigneeUser.id }
        }
      }
    }
  }

  private fun serializableToJsonNode(serializable: Any?): JsonNode {
    if (serializable == null) return NullNode.instance
    if (serializable is String) return objectMapper.readTree(serializable)

    return objectMapper.readTree(objectMapper.writeValueAsString(serializable))
  }

  private fun produceAndPersistBasicApplication(crn: String, userEntity: UserEntity): ApplicationEntity {
    val jsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
      withSchema(
        """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": {
            "thingId": {
              "description": "The unique identifier for a thing",
              "type": "integer"
            }
          },
          "required": [ "thingId" ]
        }
        """
      )
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(jsonSchema)
      withCrn(crn)
      withCreatedByUser(userEntity)
      withData(
        """
          {
             "thingId": 123
          }
          """
      )
    }

    return application
  }
}
