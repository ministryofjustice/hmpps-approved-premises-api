package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RegistrationClientResponseFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulTeamsManagingCaseCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockNotFoundOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockOffenderUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulRegistrationsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockNotFoundInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.RegistrationKeyValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registrations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationTest : IntegrationTestBase() {
  @Autowired
  lateinit var inboundMessageListener: InboundMessageListener

  @Autowired
  lateinit var assessmentTransformer: AssessmentTransformer

  @Autowired
  lateinit var userTransformer: UserTransformer

  @SpykBean
  lateinit var realApplicationTeamCodeRepository: ApplicationTeamCodeRepository

  @SpykBean
  lateinit var realApplicationRepository: ApplicationRepository

  @Test
  fun `Get all applications without JWT returns 401`() {
    webTestClient.get()
      .uri("/applications")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get all applications returns 200 with correct body - when user does not have roles returns applications managed by their teams`() {
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(listOf(StaffUserTeamMembershipFactory().withCode("TEAM1").produce()))
      }
    ) { userEntity, jwt ->
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

            val upToDateApplicationEntityManagedByTeam = approvedPremisesApplicationEntityFactory.produceAndPersist {
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

            upToDateApplicationEntityManagedByTeam.teamCodes += applicationTeamCodeRepository.save(
              ApplicationTeamCodeEntity(
                id = UUID.randomUUID(),
                application = upToDateApplicationEntityManagedByTeam,
                teamCode = "TEAM1"
              )
            )

            val outdatedApplicationEntityManagedByTeam = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(olderJsonSchema)
              withCreatedByUser(userEntity)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
            }

            outdatedApplicationEntityManagedByTeam.teamCodes += applicationTeamCodeRepository.save(
              ApplicationTeamCodeEntity(
                id = UUID.randomUUID(),
                application = outdatedApplicationEntityManagedByTeam,
                teamCode = "TEAM1"
              )
            )

            val outdatedApplicationEntityNotManagedByTeam = approvedPremisesApplicationEntityFactory.produceAndPersist {
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

            val responseBody = objectMapper.readValue(rawResponseBody, object : TypeReference<List<ApprovedPremisesApplicationSummary>>() {})

            assertThat(responseBody).anyMatch {
              outdatedApplicationEntityManagedByTeam.id == it.id &&
                outdatedApplicationEntityManagedByTeam.crn == it.person?.crn &&
                outdatedApplicationEntityManagedByTeam.createdAt.toInstant() == it.createdAt &&
                outdatedApplicationEntityManagedByTeam.createdByUser.id == it.createdByUserId &&
                outdatedApplicationEntityManagedByTeam.submittedAt?.toInstant() == it.submittedAt
            }

            assertThat(responseBody).anyMatch {
              upToDateApplicationEntityManagedByTeam.id == it.id &&
                upToDateApplicationEntityManagedByTeam.crn == it.person?.crn &&
                upToDateApplicationEntityManagedByTeam.createdAt.toInstant() == it.createdAt &&
                upToDateApplicationEntityManagedByTeam.createdByUser.id == it.createdByUserId &&
                upToDateApplicationEntityManagedByTeam.submittedAt?.toInstant() == it.submittedAt
            }

            assertThat(responseBody).noneMatch {
              outdatedApplicationEntityNotManagedByTeam.id == it.id
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

            val responseBody = objectMapper.readValue(rawResponseBody, object : TypeReference<List<ApprovedPremisesApplicationSummary>>() {})

            assertThat(responseBody).anyMatch {
              outdatedApplicationEntityCreatedByUser.id == it.id &&
                outdatedApplicationEntityCreatedByUser.crn == it.person?.crn &&
                outdatedApplicationEntityCreatedByUser.createdAt.toInstant() == it.createdAt &&
                outdatedApplicationEntityCreatedByUser.createdByUser.id == it.createdByUserId &&
                outdatedApplicationEntityCreatedByUser.submittedAt?.toInstant() == it.submittedAt
            }

            assertThat(responseBody).anyMatch {
              upToDateApplicationEntityCreatedByUser.id == it.id &&
                upToDateApplicationEntityCreatedByUser.crn == it.person?.crn &&
                upToDateApplicationEntityCreatedByUser.createdAt.toInstant() == it.createdAt &&
                upToDateApplicationEntityCreatedByUser.createdByUser.id == it.createdByUserId &&
                upToDateApplicationEntityCreatedByUser.submittedAt?.toInstant() == it.submittedAt
            }

            assertThat(responseBody).anyMatch {
              outdatedApplicationEntityNotCreatedByUser.id == it.id &&
                outdatedApplicationEntityNotCreatedByUser.crn == it.person?.crn &&
                outdatedApplicationEntityNotCreatedByUser.createdAt.toInstant() == it.createdAt &&
                outdatedApplicationEntityNotCreatedByUser.createdByUser.id == it.createdByUserId &&
                outdatedApplicationEntityNotCreatedByUser.submittedAt?.toInstant() == it.submittedAt
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
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(listOf(StaffUserTeamMembershipFactory().withCode("TEAM1").produce()))
      }
    ) { userEntity, jwt ->
      val crn = "X1234"

      produceAndPersistBasicApplication(crn, userEntity, "TEAM1")
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
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(listOf(StaffUserTeamMembershipFactory().withCode("TEAM1").produce()))
      }
    ) { userEntity, jwt ->
      `Given an Offender`(
        offenderDetailsConfigBlock = { withoutNomsNumber() }
      ) { offenderDetails, inmateDetails ->
        produceAndPersistBasicApplication(offenderDetails.otherIds.crn, userEntity, "TEAM1")

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
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(listOf(StaffUserTeamMembershipFactory().withCode("TEAM1").produce()))
      }
    ) { userEntity, jwt ->
      val crn = "X1234"

      produceAndPersistBasicApplication(crn, userEntity, "TEAM1")

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
            applicationEntity.createdAt.toInstant() == it.createdAt &&
            applicationEntity.createdByUser.id == it.createdByUserId &&
            applicationEntity.submittedAt?.toInstant() == it.submittedAt &&
            serializableToJsonNode(applicationEntity.data) == serializableToJsonNode(it.data) &&
            newestJsonSchema.id == it.schemaVersion && !it.outdatedSchema
        }
      }
    }
  }

  @Test
  fun `Get single application returns 403 when caller not in a managing team and user is not one of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER`() {
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(listOf(StaffUserTeamMembershipFactory().withCode("TEAM2").produce()))
      }
    ) { userEntity, jwt ->
      `Given a User` { otherUser, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val crn = "X1234"

          val application = produceAndPersistBasicApplication(crn, otherUser, "TEAM1")

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
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(listOf(StaffUserTeamMembershipFactory().withCode("TEAM1").produce()))
      }
    ) { userEntity, jwt ->
      `Given an Offender`(
        offenderDetailsConfigBlock = { withoutNomsNumber() }
      ) { offenderDetails, inmateDetails ->
        val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, userEntity, "TEAM1")

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
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(listOf(StaffUserTeamMembershipFactory().withCode("TEAM1").produce()))
      }
    ) { userEntity, jwt ->
      val crn = "X1234"

      val application = produceAndPersistBasicApplication(crn, userEntity, "TEAM1")

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
            nonUpgradableApplicationEntity.createdAt.toInstant() == it.createdAt &&
            nonUpgradableApplicationEntity.createdByUser.id == it.createdByUserId &&
            nonUpgradableApplicationEntity.submittedAt?.toInstant() == it.submittedAt &&
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
            offlineApplicationEntity.createdAt.toInstant() == it.createdAt &&
            offlineApplicationEntity.submittedAt.toInstant() == it.submittedAt
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
    `Given a User` { userEntity, jwt ->
      val crn = "X1234"

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
  fun `Create new application returns 400 when CRN not managed by any of User's teams`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.now())
          withId(UUID.randomUUID())
        }

        APDeliusContext_mockSuccessfulTeamsManagingCaseCall(
          offenderDetails.otherIds.crn,
          userEntity.deliusStaffCode!!,
          ManagingTeamsResponse(
            teamCodes = emptyList()
          )
        )

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
          .isBadRequest
          .returnResult<ValidationError>()
          .responseBody
          .blockFirst()

        assertThat(result.invalidParams).anyMatch {
          it.propertyName == "$.crn" &&
            it.errorType == "notInCaseload"
        }
      }
    }
  }

  @Test
  fun `Create new application returns 500 and does not create Application without team codes when write to team code table fails`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.now())
          withId(UUID.randomUUID())
        }

        APDeliusContext_mockSuccessfulTeamsManagingCaseCall(
          offenderDetails.otherIds.crn,
          userEntity.deliusStaffCode!!,
          ManagingTeamsResponse(
            teamCodes = listOf(offenderDetails.otherIds.crn)
          )
        )

        every { realApplicationTeamCodeRepository.save(any()) } throws RuntimeException("Database Error")

        webTestClient.post()
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
          .is5xxServerError

        assertThat(approvedPremisesApplicationRepository.findAll().none { it.crn == offenderDetails.otherIds.crn })
      }
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

        APDeliusContext_mockSuccessfulTeamsManagingCaseCall(
          offenderDetails.otherIds.crn,
          userEntity.deliusStaffCode!!,
          ManagingTeamsResponse(
            teamCodes = listOf("TEAM1")
          )
        )

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

        APDeliusContext_mockSuccessfulTeamsManagingCaseCall(
          offenderDetails.otherIds.crn,
          userEntity.deliusStaffCode!!,
          ManagingTeamsResponse(
            teamCodes = listOf("TEAM1")
          )
        )

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
  fun `Create new application for Temporary Accommodation returns 201 with correct body and Location header`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.now())
          withId(UUID.randomUUID())
        }

        val result = webTestClient.post()
          .uri("/applications")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .returnResult(TemporaryAccommodationApplication::class.java)

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
  fun `Update existing AP application returns 200 with correct body`() {
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
              UpdateApprovedPremisesApplication(
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
  fun `Submit application returns 200, creates and allocates an assessment, saves a domain event, emits an SNS event`() {
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
              SubmitApprovedPremisesApplication(
                translatedDocument = {},
                isPipeApplication = true,
                isWomensApplication = true,
                targetLocation = "SW1A 1AA",
                releaseType = ReleaseTypeOption.licence
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

          val emittedMessage = inboundMessageListener.blockForMessage()

          assertThat(emittedMessage.eventType).isEqualTo("approved-premises.application.submitted")
          assertThat(emittedMessage.description).isEqualTo("An application has been submitted for an Approved Premises placement")
          assertThat(emittedMessage.detailUrl).matches("http://frontend/events/application-submitted/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}")
          assertThat(emittedMessage.additionalInformation.applicationId).isEqualTo(applicationId)
          assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
            SnsEventPersonReference("CRN", offenderDetails.otherIds.crn),
            SnsEventPersonReference("NOMS", offenderDetails.otherIds.nomsNumber!!)
          )
        }
      }
    }
  }

  @Test
  fun `When several concurrent submit application requests occur, only one is successful, all others return 400 without persisting domain events`() {
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

          every { realApplicationRepository.save(any()) } answers {
            Thread.sleep(1000)
            it.invocation.args[0] as ApplicationEntity
          }

          val responseStatuses = mutableListOf<HttpStatus>()

          (1..10).map {
            val thread = Thread {
              webTestClient.post()
                .uri("/applications/$applicationId/submission")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  SubmitApprovedPremisesApplication(
                    translatedDocument = {},
                    isPipeApplication = true,
                    isWomensApplication = true,
                    targetLocation = "SW1A 1AA",
                    releaseType = ReleaseTypeOption.licence
                  )
                )
                .exchange()
                .returnResult<String>()
                .consumeWith {
                  synchronized(responseStatuses) {
                    responseStatuses += it.status
                  }
                }
            }

            thread.start()

            thread
          }.forEach(Thread::join)

          val persistedDomainEvents = domainEventRepository.findAll().filter { it.applicationId == applicationId }

          assertThat(persistedDomainEvents).singleElement()
          assertThat(responseStatuses.count { it.value() == 200 }).isEqualTo(1)
          assertThat(responseStatuses.count { it.value() == 400 }).isEqualTo(9)
        }
      }
    }
  }

  @Test
  fun `Submit Temporary Accommodation application returns 200`() {
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(
          listOf(
            StaffUserTeamMembershipFactory().produce()
          )
        )
      }
    ) { submittingUser, jwt ->
      `Given a User` { userEntity, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
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
                "properties": {},
                "required": []
              }
            """
            )
          }

          temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withId(applicationId)
            withApplicationSchema(applicationSchema)
            withCreatedByUser(submittingUser)
            withProbationRegion(submittingUser.probationRegion)
            withData(
              """
              {}
            """
            )
          }

          webTestClient.post()
            .uri("/applications/$applicationId/submission")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .bodyValue(
              SubmitTemporaryAccommodationApplication(
                translatedDocument = {},
              )
            )
            .exchange()
            .expectStatus()
            .isOk
        }
      }
    }
  }

  @Nested
  inner class GetAssessmentForApplication {
    @Test
    fun `Get assessment for application returns an application's assessment when the requesting user is the allocated user`() {
      `Given a User` { applicant, _ ->
        `Given a User` { user, jwt ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            val (application, assessment) = produceAndPersistApplicationAndAssessment(applicant, user, offenderDetails)

            webTestClient.get()
              .uri("/applications/${application.id}/assessment")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  assessmentTransformer.transformJpaToApi(assessment, offenderDetails, inmateDetails)
                )
              )
          }
        }
      }
    }

    @Test
    fun `Get assessment for application returns an application's assessment when the requesting user is a workflow manager`() {
      `Given a User`(roles = listOf(UserRole.WORKFLOW_MANAGER)) { requestUser, jwt ->
        `Given a User`(roles = listOf(UserRole.ASSESSOR)) { applicant, _ ->
          `Given a User`(roles = listOf(UserRole.ASSESSOR)) { assignee, _ ->
            `Given an Offender` { offenderDetails, inmateDetails ->
              val (application, assessment) = produceAndPersistApplicationAndAssessment(
                applicant,
                assignee,
                offenderDetails
              )

              webTestClient.get()
                .uri("/applications/${application.id}/assessment")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    assessmentTransformer.transformJpaToApi(assessment, offenderDetails, inmateDetails)
                  )
                )
            }
          }
        }
      }
    }

    @Test
    fun `Get assessment for an application returns 403 if the user does not have permission`() {
      `Given a User` { applicant, _ ->
        `Given a User` { assignee, _ ->
          `Given a User` { requestUser, jwt ->
            `Given an Offender` { offenderDetails, _ ->

              val (application, _) = produceAndPersistApplicationAndAssessment(applicant, assignee, offenderDetails)

              webTestClient.get()
                .uri("/applications/${application.id}/assessment")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }
    }

    private fun produceAndPersistApplicationAndAssessment(applicant: UserEntity, assignee: UserEntity, offenderDetails: OffenderDetailSummary): Pair<ApprovedPremisesApplicationEntity, AssessmentEntity> {
      val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
        withAddedAt(OffsetDateTime.now())
      }

      val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offenderDetails.otherIds.crn)
        withCreatedByUser(applicant)
        withApplicationSchema(applicationSchema)
      }

      val assessment = assessmentEntityFactory.produceAndPersist {
        withAllocatedToUser(assignee)
        withApplication(application)
        withAssessmentSchema(assessmentSchema)
      }

      assessment.schemaUpToDate = true

      return Pair(application, assessment)
    }
  }

  private fun serializableToJsonNode(serializable: Any?): JsonNode {
    if (serializable == null) return NullNode.instance
    if (serializable is String) return objectMapper.readTree(serializable)

    return objectMapper.readTree(objectMapper.writeValueAsString(serializable))
  }

  private fun produceAndPersistBasicApplication(crn: String, userEntity: UserEntity, managingTeamCode: String): ApplicationEntity {
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

    application.teamCodes += applicationTeamCodeRepository.save(
      ApplicationTeamCodeEntity(
        id = UUID.randomUUID(),
        application = application,
        teamCode = managingTeamCode
      )
    )

    return application
  }
}

@Service
class InboundMessageListener(private val objectMapper: ObjectMapper) {
  private val log = LoggerFactory.getLogger(this::class.java)
  private val messages = mutableListOf<SnsEvent>()

  @JmsListener(destination = "domaineventsqueue", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String?) {
    val (Message) = objectMapper.readValue(rawMessage, Message::class.java)
    val event = objectMapper.readValue(Message, SnsEvent::class.java)

    log.info("Received Domain Event: ", event)
    synchronized(messages) {
      messages.add(event)
    }
  }

  fun clearMessages() = messages.clear()
  fun blockForMessage(): SnsEvent {
    var waitedCount = 0
    while (isEmpty()) {
      if (waitedCount == 300) throw RuntimeException("Never received SQS message from SNS topic after 30s")

      Thread.sleep(100)
      waitedCount += 1
    }

    synchronized(messages) {
      return messages.first()
    }
  }

  fun isEmpty(): Boolean {
    synchronized(messages) {
      return messages.isEmpty()
    }
  }
}

data class EventType(val Value: String, val Type: String)
data class MessageAttributes(val eventType: EventType)
data class Message(
  val Message: String,
  val MessageId: String,
  val MessageAttributes: MessageAttributes
)
