package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RegistrationClientResponseFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Probation Region`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulTeamsManagingCaseCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APOASysContext_mockSuccessfulNeedsDetailsCall
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.RegistrationKeyValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registrations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationTest : IntegrationTestBase() {
  @Autowired
  lateinit var inboundMessageListener: InboundMessageListener

  @Autowired
  lateinit var assessmentTransformer: AssessmentTransformer

  @Autowired
  lateinit var userTransformer: UserTransformer

  @Autowired
  lateinit var applicationsTransformer: ApplicationsTransformer

  @SpykBean
  lateinit var realApplicationTeamCodeRepository: ApplicationTeamCodeRepository

  @SpykBean
  lateinit var realApplicationRepository: ApplicationRepository

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realApplicationTeamCodeRepository)
    clearMocks(realApplicationRepository)
  }

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
      },
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
          """,
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
            """,
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
              """,
              )
            }

            upToDateApplicationEntityManagedByTeam.teamCodes += applicationTeamCodeRepository.save(
              ApplicationTeamCodeEntity(
                id = UUID.randomUUID(),
                application = upToDateApplicationEntityManagedByTeam,
                teamCode = "TEAM1",
              ),
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
                teamCode = "TEAM1",
              ),
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

  @Test
  fun `Get all applications returns 200 with correct body for Temporary Accommodation - when user has CAS3_ASSESSOR role then returns all submitted applications in region`() {
    `Given a Probation Region` { probationRegion ->
      `Given a User`(roles = listOf(UserRole.CAS3_REFERRER), probationRegion = probationRegion) { otherUser, _ ->
        `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR), probationRegion = probationRegion) { assessorUser, jwt ->
          `Given an Offender` { offenderDetails, _ ->
            temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

            val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
            }

            val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(otherUser)
              withSubmittedAt(OffsetDateTime.parse("2023-06-01T12:34:56.789+01:00"))
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
              withProbationRegion(probationRegion)
            }

            val notSubmittedApplication = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(otherUser)
              withSubmittedAt(null)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
              withProbationRegion(probationRegion)
            }

            val notInRegionApplication = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(otherUser)
              withSubmittedAt(OffsetDateTime.parse("2023-06-01T12:34:56.789+01:00"))
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
              withYieldedProbationRegion {
                probationRegionEntityFactory.produceAndPersist {
                  withYieldedApArea {
                    apAreaEntityFactory.produceAndPersist()
                  }
                }
              }
            }

            CommunityAPI_mockOffenderUserAccessCall(assessorUser.deliusUsername, offenderDetails.otherIds.crn, false, false)

            val rawResponseBody = webTestClient.get()
              .uri("/applications")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody =
              objectMapper.readValue(rawResponseBody, object : TypeReference<List<TemporaryAccommodationApplicationSummary>>() {})

            assertThat(responseBody).anyMatch {
              application.id == it.id &&
                application.crn == it.person.crn &&
                application.createdAt.toInstant() == it.createdAt &&
                application.createdByUser.id == it.createdByUserId &&
                application.submittedAt?.toInstant() == it.submittedAt
            }

            assertThat(responseBody).noneMatch {
              notInRegionApplication.id == it.id
            }

            assertThat(responseBody).noneMatch {
              notSubmittedApplication.id == it.id
            }
          }
        }
      }
    }
  }

  @Test
  fun `Get all applications returns 200 with correct body for Temporary Accommodation - when user has CAS3_REFERRER role then returns all applications created by user`() {
    `Given a Probation Region` { probationRegion ->
      `Given a User`(roles = listOf(UserRole.CAS3_REFERRER), probationRegion = probationRegion) { otherUser, _ ->
        `Given a User`(roles = listOf(UserRole.CAS3_REFERRER), probationRegion = probationRegion) { referrerUser, jwt ->
          `Given an Offender` { offenderDetails, _ ->
            temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

            val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
            }

            val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(referrerUser)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
              withProbationRegion(probationRegion)
            }

            val anotherUsersApplication = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(otherUser)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
              withProbationRegion(probationRegion)
            }

            CommunityAPI_mockOffenderUserAccessCall(referrerUser.deliusUsername, offenderDetails.otherIds.crn, false, false)

            val rawResponseBody = webTestClient.get()
              .uri("/applications")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody =
              objectMapper.readValue(rawResponseBody, object : TypeReference<List<TemporaryAccommodationApplicationSummary>>() {})

            assertThat(responseBody).anyMatch {
              application.id == it.id &&
                application.crn == it.person.crn &&
                application.createdAt.toInstant() == it.createdAt &&
                application.createdByUser.id == it.createdByUserId &&
                application.submittedAt?.toInstant() == it.submittedAt
            }

            assertThat(responseBody).noneMatch {
              anotherUsersApplication.id == it.id
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
      },
    ) { userEntity, jwt ->
      val crn = "X1234"

      produceAndPersistBasicApplication(crn, userEntity, "TEAM1")
      CommunityAPI_mockNotFoundOffenderDetailsCall(crn)
      loadPreemptiveCacheForOffenderDetails(crn)

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
  fun `Get list of applications returns successfully when a person has no NOMS number`() {
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(listOf(StaffUserTeamMembershipFactory().withCode("TEAM1").produce()))
      },
    ) { userEntity, jwt ->
      `Given an Offender`(
        offenderDetailsConfigBlock = { withoutNomsNumber() },
      ) { offenderDetails, inmateDetails ->
        val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, userEntity, "TEAM1")

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

        val responseBody =
          objectMapper.readValue(rawResponseBody, object : TypeReference<List<ApprovedPremisesApplicationSummary>>() {})

        assertThat(responseBody).matches {
          val person = it[0].person as FullPerson

          application.id == it[0].id &&
            application.crn == person.crn &&
            person.nomsNumber == null &&
            person.status == FullPerson.Status.unknown &&
            person.prisonName == null
        }
      }
    }
  }

  @Test
  fun `Get list of applications returns successfully when the person cannot be fetched from the prisons API`() {
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(listOf(StaffUserTeamMembershipFactory().withCode("TEAM1").produce()))
      },
    ) { userEntity, jwt ->
      val crn = "X1234"

      val application = produceAndPersistBasicApplication(crn, userEntity, "TEAM1")

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber("ABC123")
        .produce()

      CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

      CommunityAPI_mockSuccessfulOffenderDetailsCall(offenderDetails)
      loadPreemptiveCacheForOffenderDetails(offenderDetails.otherIds.crn)
      PrisonAPI_mockNotFoundInmateDetailsCall(offenderDetails.otherIds.nomsNumber!!)
      loadPreemptiveCacheForInmateDetails(offenderDetails.otherIds.nomsNumber!!)

      val rawResponseBody = webTestClient.get()
        .uri("/applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult<String>()
        .responseBody
        .blockFirst()

      val responseBody =
        objectMapper.readValue(rawResponseBody, object : TypeReference<List<ApprovedPremisesApplicationSummary>>() {})

      assertThat(responseBody).matches {
        val person = it[0].person as FullPerson

        application.id == it[0].id &&
          application.crn == person.crn &&
          person.nomsNumber == null &&
          person.status == FullPerson.Status.unknown &&
          person.prisonName == null
      }
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
        """,
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
          """,
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
      },
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
  fun `Get single application returns successfully when a person has no NOMS number`() {
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(listOf(StaffUserTeamMembershipFactory().withCode("TEAM1").produce()))
      },
    ) { userEntity, jwt ->
      `Given an Offender`(
        offenderDetailsConfigBlock = { withoutNomsNumber() },
      ) { offenderDetails, inmateDetails ->
        val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, userEntity, "TEAM1")

        CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

        val rawResponseBody = webTestClient.get()
          .uri("/applications/${application.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<String>()
          .responseBody
          .blockFirst()

        val responseBody = objectMapper.readValue(rawResponseBody, ApprovedPremisesApplication::class.java)

        assertThat(responseBody.person is FullPerson).isTrue
        assertThat(responseBody).matches {
          val person = it.person as FullPerson

          application.id == it.id &&
            application.crn == person.crn &&
            person.nomsNumber == null &&
            person.status == FullPerson.Status.unknown &&
            person.prisonName == null
        }
      }
    }
  }

  @Test
  fun `Get single application returns successfully when the person cannot be fetched from the prisons API`() {
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(listOf(StaffUserTeamMembershipFactory().withCode("TEAM1").produce()))
      },
    ) { userEntity, jwt ->
      val crn = "X1234"

      val application = produceAndPersistBasicApplication(crn, userEntity, "TEAM1")

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber("ABC123")
        .produce()

      CommunityAPI_mockSuccessfulOffenderDetailsCall(offenderDetails)
      loadPreemptiveCacheForOffenderDetails(offenderDetails.otherIds.crn)
      PrisonAPI_mockNotFoundInmateDetailsCall(offenderDetails.otherIds.nomsNumber!!)
      loadPreemptiveCacheForInmateDetails(offenderDetails.otherIds.nomsNumber!!)

      CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

      val rawResponseBody = webTestClient.get()
        .uri("/applications/${application.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult<String>()
        .responseBody
        .blockFirst()

      val responseBody = objectMapper.readValue(rawResponseBody, ApprovedPremisesApplication::class.java)

      assertThat(responseBody.person is FullPerson).isTrue

      assertThat(responseBody).matches {
        val person = it.person as FullPerson

        application.id == it.id &&
          application.crn == person.crn &&
          person.nomsNumber == null &&
          person.status == FullPerson.Status.unknown &&
          person.prisonName == null
      }
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
        """,
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
        """,
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
  fun `Get single application returns 200 with correct body for Temporary Accommodation when requesting user created application`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

        val newestJsonSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
          withSchema("{}")
        }

        val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(newestJsonSchema)
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withProbationRegion(userEntity.probationRegion)
          withData(
            """
            {
               "thingId": 123
            }
            """,
          )
        }

        CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

        val rawResponseBody = webTestClient.get()
          .uri("/applications/${applicationEntity.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<String>()
          .responseBody
          .blockFirst()

        val responseBody = objectMapper.readValue(rawResponseBody, TemporaryAccommodationApplication::class.java)

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
  fun `Get single application returns 200 with correct body for Temporary Accommodation when a user with the CAS3_ASSESSOR role requests a submitted application in their region`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given a User`(probationRegion = userEntity.probationRegion) { createdByUser, _ ->
        `Given an Offender` { offenderDetails, _ ->
          temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

          val newestJsonSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
            withSchema("{}")
          }

          val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(newestJsonSchema)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(createdByUser)
            withProbationRegion(createdByUser.probationRegion)
            withSubmittedAt(OffsetDateTime.parse("2023-06-01T12:34:56.789+01:00"))
            withData(
              """
              {
                 "thingId": 123
              }
              """,
            )
          }

          CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

          val rawResponseBody = webTestClient.get()
            .uri("/applications/${applicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody = objectMapper.readValue(rawResponseBody, TemporaryAccommodationApplication::class.java)

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
  }

  @Test
  fun `Get single application returns 403 Forbidden for Temporary Accommodation when a user with the CAS3_ASSESSOR role requests an application not in their region`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given a User` { createdByUser, _ ->
        `Given an Offender` { offenderDetails, _ ->
          temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

          val newestJsonSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
            withSchema("{}")
          }

          val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(newestJsonSchema)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(createdByUser)
            withProbationRegion(createdByUser.probationRegion)
            withSubmittedAt(OffsetDateTime.now())
            withData(
              """
              {
                 "thingId": 123
              }
              """,
            )
          }

          CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

          webTestClient.get()
            .uri("/applications/${applicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }
  }

  @Test
  fun `Get single application returns 403 Forbidden for Temporary Accommodation when a user without the CAS3_ASSESSOR role requests an application not created by them`() {
    `Given a User` { userEntity, jwt ->
      `Given a User`(probationRegion = userEntity.probationRegion) { createdByUser, _ ->
        `Given an Offender` { offenderDetails, _ ->
          temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

          val newestJsonSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
            withSchema("{}")
          }

          val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(newestJsonSchema)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(createdByUser)
            withProbationRegion(createdByUser.probationRegion)
            withSubmittedAt(OffsetDateTime.now())
            withData(
              """
              {
                 "thingId": 123
              }
              """,
            )
          }

          CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, offenderDetails.otherIds.crn, false, false)

          webTestClient.get()
            .uri("/applications/${applicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }
  }

  @Test
  fun `Get placement applications returns 403 Forbidden if incorrect XServiceName`() {
    `Given a User` { user, jwt ->

      `Given a Placement Application`(
        createdByUser = user,
        schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
      ) { placementApplicationEntity ->

        val applicationId = placementApplicationEntity.application.id

        webTestClient.get()
          .uri("/applications/$applicationId/placement-applications")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  fun `Get placement applications without JWT returns 401`() {
    `Given a User` { user, jwt ->

      `Given a Placement Application`(
        createdByUser = user,
        schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
      ) { placementApplicationEntity ->

        val applicationId = placementApplicationEntity.application.id
        webTestClient.get()
          .uri("/applications/$applicationId/placement-applications")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }
  }

  @Test
  fun `Get placement applications returns the transformed objects`() {
    `Given a User` { user, jwt ->
      `Given a Placement Application`(
        createdByUser = user,
        schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
      ) { placementApplicationEntity ->

        val applicationId = placementApplicationEntity.application.id
        val rawResult = webTestClient.get()
          .uri("/applications/$applicationId/placement-applications")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<String>()
          .responseBody
          .blockFirst()

        val body = objectMapper.readValue(rawResult, object : TypeReference<List<PlacementApplication>>() {})
        assertThat(body[0].id).isEqualTo(placementApplicationEntity.id)
        assertThat(body[0].applicationId).isEqualTo(placementApplicationEntity.application.id)
        assertThat(body[0].createdByUserId).isEqualTo(placementApplicationEntity.createdByUser.id)
        assertThat(body[0].schemaVersion).isEqualTo(placementApplicationEntity.schemaVersion.id)
        assertThat(body[0].createdAt).isEqualTo(placementApplicationEntity.createdAt.toInstant())
        assertThat(body[0].submittedAt).isNull()
      }
    }
  }

  @Test
  fun `Get single offline application returns 200 with correct body`() {
    `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { userEntity, jwt ->
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
            offlineApplicationEntity.createdAt.toInstant() == it.createdAt
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
  fun `Create new application returns 404 when a person cannot be found`() {
    `Given a User` { userEntity, jwt ->
      val crn = "X1234"

      CommunityAPI_mockNotFoundOffenderDetailsCall(crn)
      loadPreemptiveCacheForOffenderDetails(crn)

      approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
        withAddedAt(OffsetDateTime.now())
        withId(UUID.randomUUID())
      }

      webTestClient.post()
        .uri("/applications")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewApplication(
            crn = crn,
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("$.detail").isEqualTo("No Offender with an ID of $crn could be found")
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
          ManagingTeamsResponse(
            teamCodes = listOf(offenderDetails.otherIds.crn),
          ),
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
              offenceId = "789",
            ),
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
          ManagingTeamsResponse(
            teamCodes = listOf("TEAM1"),
          ),
        )

        APOASysContext_mockSuccessfulNeedsDetailsCall(
          offenderDetails.otherIds.crn,
          NeedsDetailsFactory().produce(),
        )

        val result = webTestClient.post()
          .uri("/applications")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewApplication(
              crn = offenderDetails.otherIds.crn,
              convictionId = 123,
              deliusEventNumber = "1",
              offenceId = "789",
            ),
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
          ManagingTeamsResponse(
            teamCodes = listOf("TEAM1"),
          ),
        )

        APOASysContext_mockSuccessfulNeedsDetailsCall(
          offenderDetails.otherIds.crn,
          NeedsDetailsFactory().produce(),
        )

        val result = webTestClient.post()
          .uri("/applications?createWithRisks=false")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewApplication(
              crn = offenderDetails.otherIds.crn,
              convictionId = 123,
              deliusEventNumber = "1",
              offenceId = "789",
            ),
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
  fun `Create new application returns successfully when a person has no NOMS number`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender`(
        offenderDetailsConfigBlock = { withoutNomsNumber() },
      ) { offenderDetails, inmateDetails ->
        APDeliusContext_mockSuccessfulTeamsManagingCaseCall(
          offenderDetails.otherIds.crn,
          ManagingTeamsResponse(
            teamCodes = listOf(offenderDetails.otherIds.crn),
          ),
        )

        approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
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
              offenceId = "789",
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .returnResult(ApprovedPremisesApplication::class.java)

        assertThat(result.responseHeaders["Location"]).anyMatch {
          it.matches(Regex("/applications/.+"))
        }

        assertThat(result.responseBody.blockFirst()).matches {
          it.person.crn == offenderDetails.otherIds.crn
        }
      }
    }
  }

  @Test
  fun `Create new application returns successfully when the person cannot be fetched from the prisons API`() {
    `Given a User` { userEntity, jwt ->
      val offenderDetails = OffenderDetailsSummaryFactory()
        .withNomsNumber("ABC123")
        .produce()

      CommunityAPI_mockSuccessfulOffenderDetailsCall(offenderDetails)
      loadPreemptiveCacheForOffenderDetails(offenderDetails.otherIds.crn)
      offenderDetails.otherIds.nomsNumber?.let { PrisonAPI_mockNotFoundInmateDetailsCall(it) }
      loadPreemptiveCacheForInmateDetails(offenderDetails.otherIds.nomsNumber!!)
      APDeliusContext_mockSuccessfulTeamsManagingCaseCall(
        offenderDetails.otherIds.crn,
        ManagingTeamsResponse(
          teamCodes = listOf(offenderDetails.otherIds.crn),
        ),
      )

      val result = webTestClient.post()
        .uri("/applications")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewApplication(
            crn = offenderDetails.otherIds.crn,
            convictionId = 123,
            deliusEventNumber = "1",
            offenceId = "789",
          ),
        )
        .exchange()
        .expectStatus()
        .isCreated
        .returnResult(ApprovedPremisesApplication::class.java)

      assertThat(result.responseHeaders["Location"]).anyMatch {
        it.matches(Regex("/applications/.+"))
      }

      assertThat(result.responseBody.blockFirst()).matches {
        it.person.crn == offenderDetails.otherIds.crn
      }
    }
  }

  @Test
  fun `Create new application for Temporary Accommodation returns 403 Forbidden when user doesn't have CAS3_REFERRER role`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        webTestClient.post()
          .uri("/applications")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewApplication(
              crn = offenderDetails.otherIds.crn,
              convictionId = 123,
              deliusEventNumber = "1",
              offenceId = "789",
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Test
  fun `Create new application for Temporary Accommodation returns 201 with correct body and Location header`() {
    `Given a User`(roles = listOf(UserRole.CAS3_REFERRER)) { _, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.now())
          withId(UUID.randomUUID())
        }

        val offenceId = "789"

        val result = webTestClient.post()
          .uri("/applications")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewApplication(
              crn = offenderDetails.otherIds.crn,
              convictionId = 123,
              deliusEventNumber = "1",
              offenceId = offenceId,
            ),
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
            it.schemaVersion == applicationSchema.id &&
            it.offenceId == offenceId
        }
      }
    }
  }

  @Test
  fun `Create new application for Temporary Accommodation returns successfully when a person has no NOMS number`() {
    `Given a User`(roles = listOf(UserRole.CAS3_REFERRER)) { _, jwt ->
      `Given an Offender`(
        offenderDetailsConfigBlock = { withoutNomsNumber() },
      ) { offenderDetails, _ ->
        val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.now())
          withId(UUID.randomUUID())
        }

        val offenceId = "789"

        val result = webTestClient.post()
          .uri("/applications")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewApplication(
              crn = offenderDetails.otherIds.crn,
              convictionId = 123,
              deliusEventNumber = "1",
              offenceId = offenceId,
            ),
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
            it.schemaVersion == applicationSchema.id &&
            it.offenceId == offenceId
        }
      }
    }
  }

  @Test
  fun `Update existing AP application returns 200 with correct body`() {
    `Given a User` { submittingUser, jwt ->
      `Given a User`(roles = listOf(UserRole.CAS1_ASSESSOR), qualifications = listOf(UserQualification.PIPE)) { assessorUser, _ ->
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
            """,
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
                isPipeApplication = true,
                type = UpdateApplicationType.CAS1,
              ),
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
            StaffUserTeamMembershipFactory().produce(),
          ),
        )
      },
    ) { submittingUser, jwt ->
      `Given a User`(roles = listOf(UserRole.CAS1_ASSESSOR), qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS)) { assessorUser, _ ->
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
            """,
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
            """,
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
                      description = "MAPPA",
                    ),
                  )
                  .withRegisterCategory(
                    RegistrationKeyValue(
                      code = "A",
                      description = "A",
                    ),
                  )
                  .withRegisterLevel(
                    RegistrationKeyValue(
                      code = "1",
                      description = "1",
                    ),
                  )
                  .produce(),
              ),
            ),
          )

          APDeliusContext_mockSuccessfulCaseDetailCall(
            offenderDetails.otherIds.crn,
            CaseDetailFactory().produce(),
          )

          webTestClient.post()
            .uri("/applications/$applicationId/submission")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              SubmitApprovedPremisesApplication(
                translatedDocument = {},
                isPipeApplication = true,
                isWomensApplication = true,
                isEmergencyApplication = true,
                isEsapApplication = true,
                targetLocation = "SW1A 1AA",
                releaseType = ReleaseTypeOption.licence,
                type = "CAS1",
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val persistedApplication = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)!! as ApprovedPremisesApplicationEntity

          assertThat(persistedApplication.isWomensApplication).isTrue
          assertThat(persistedApplication.isPipeApplication).isTrue
          assertThat(persistedApplication.targetLocation).isEqualTo("SW1A 1AA")

          val createdAssessment = approvedPremisesAssessmentRepository.findAll().first { it.application.id == applicationId }
          assertThat(createdAssessment.allocatedToUser!!.id).isEqualTo(assessorUser.id)

          val persistedDomainEvent = domainEventRepository.findAll().firstOrNull { it.applicationId == applicationId }

          assertThat(persistedDomainEvent).isNotNull
          assertThat(persistedDomainEvent!!.crn).isEqualTo(offenderDetails.otherIds.crn)
          assertThat(persistedDomainEvent.type).isEqualTo(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)

          val emittedMessage = inboundMessageListener.blockForMessage()

          assertThat(emittedMessage.eventType).isEqualTo("approved-premises.application.submitted")
          assertThat(emittedMessage.description).isEqualTo("An application has been submitted for an Approved Premises placement")
          assertThat(emittedMessage.detailUrl).matches("http://api/events/application-submitted/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}")
          assertThat(emittedMessage.additionalInformation.applicationId).isEqualTo(applicationId)
          assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
            SnsEventPersonReference("CRN", offenderDetails.otherIds.crn),
            SnsEventPersonReference("NOMS", offenderDetails.otherIds.nomsNumber!!),
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
            StaffUserTeamMembershipFactory().produce(),
          ),
        )
      },
    ) { submittingUser, jwt ->
      `Given a User`(roles = listOf(UserRole.CAS1_ASSESSOR), qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS)) { assessorUser, _ ->
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
            """,
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
            """,
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
                      description = "MAPPA",
                    ),
                  )
                  .withRegisterCategory(
                    RegistrationKeyValue(
                      code = "A",
                      description = "A",
                    ),
                  )
                  .withRegisterLevel(
                    RegistrationKeyValue(
                      code = "1",
                      description = "1",
                    ),
                  )
                  .produce(),
              ),
            ),
          )

          every { realApplicationRepository.save(any()) } answers {
            Thread.sleep(1000)
            it.invocation.args[0] as ApplicationEntity
          }

          APDeliusContext_mockSuccessfulCaseDetailCall(
            offenderDetails.otherIds.crn,
            CaseDetailFactory().produce(),
          )

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
                    isEmergencyApplication = true,
                    isEsapApplication = true,
                    targetLocation = "SW1A 1AA",
                    releaseType = ReleaseTypeOption.licence,
                    type = "CAS1",
                  ),
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
            StaffUserTeamMembershipFactory().produce(),
          ),
        )
      },
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
            """,
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
            """,
            )
          }

          webTestClient.post()
            .uri("/applications/$applicationId/submission")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .bodyValue(
              SubmitTemporaryAccommodationApplication(
                translatedDocument = {},
                type = "CAS3",
                arrivalDate = LocalDate.now(),
                summaryData = object {
                  val num = 50
                  val text = "Hello world!"
                },
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val persistedApplication = temporaryAccommodationApplicationRepository.findByIdOrNull(applicationId)!!
          val persistedAssessment = persistedApplication.getLatestAssessment() as TemporaryAccommodationAssessmentEntity

          assertThat(persistedAssessment.summaryData).isEqualTo("{\"num\":50,\"text\":\"Hello world!\"}")
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
                  assessmentTransformer.transformJpaToApi(
                    assessment,
                    PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Get assessment for application returns an application's assessment when the requesting user is a workflow manager`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { requestUser, jwt ->
        `Given a User`(roles = listOf(UserRole.CAS1_ASSESSOR)) { applicant, _ ->
          `Given a User`(roles = listOf(UserRole.CAS1_ASSESSOR)) { assignee, _ ->
            `Given an Offender` { offenderDetails, inmateDetails ->
              val (application, assessment) = produceAndPersistApplicationAndAssessment(
                applicant,
                assignee,
                offenderDetails,
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
                    assessmentTransformer.transformJpaToApi(
                      assessment,
                      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                    ),
                  ),
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

      val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
        withAllocatedToUser(assignee)
        withApplication(application)
        withAssessmentSchema(assessmentSchema)
      }

      assessment.schemaUpToDate = true

      return Pair(application, assessment)
    }
  }

  @Nested
  inner class ApplicationTimeline {
    val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

    @Test
    fun `Get application timeline without JWT returns 401`() {
      webTestClient.get()
        .uri("/applications/$applicationId/timeline")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get application timeline returns 403 forbidden when not approved premises service`() {
      `Given a User`(roles = listOf(UserRole.CAS1_ADMIN)) { _, jwt ->
        webTestClient.get()
          .uri("/applications/$applicationId/timeline")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get application timeline returns 200 when user has permission and approved premises service`() {
      `Given a User`(roles = listOf(UserRole.CAS1_ADMIN)) { _, jwt ->
        webTestClient.get()
          .uri("/applications/$applicationId/timeline")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
      }
    }

    @Test
    fun `Get application timeline returns correct ten DomainEvents when no information requests`() {
      `Given a User`(roles = listOf(UserRole.CAS1_ADMIN)) { _, jwt ->

        val month = 2
        val year = 2023

        val domainEvents = createTenDomainEvents(year, month)
        val summaries = domainEvents.map {
          TimelineEvent(
            applicationsTransformer.transformDomainEventTypeToTimelineEventType(it.type),
            it.id.toString(),
            it.occurredAt.toInstant(),
          )
        }

        val expectedJson = objectMapper.writeValueAsString(
          summaries,
        )

        webTestClient.get()
          .uri("/applications/$applicationId/timeline")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @Test
    fun `Get application timeline returns correct twenty DomainEvents when we have ten information requests`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->

        val month = 2
        val year = 2023

        val assessment = createAssessment(user, year, month)

        val domainEvents = createTenDomainEvents(year, month)
        val summaries = domainEvents.map {
          TimelineEvent(
            applicationsTransformer.transformDomainEventTypeToTimelineEventType(it.type),
            it.id.toString(),
            it.occurredAt.toInstant(),
          )
        }

        val informationRequests = createTenInformationRequests(assessment, user, year, month)
        val informationRequestSummaries = informationRequests.map {
          TimelineEvent(
            TimelineEventType.approvedPremisesInformationRequest,
            it.id.toString(),
            it.createdAt.toInstant(),
          )
        }

        val allSummaries = summaries + informationRequestSummaries
        val expectedJson = objectMapper.writeValueAsString(
          allSummaries,
        )

        webTestClient.get()
          .uri("/applications/$applicationId/timeline")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    private fun createAssessment(user: UserEntity, year: Int, month: Int): ApprovedPremisesAssessmentEntity {
      val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(user)
        withId(applicationId)
        withApplicationSchema(applicationSchema)

        withCreatedAt(
          LocalDate.of(year, month, 1).toLocalDateTime(),
        )
      }

      val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      return approvedPremisesAssessmentEntityFactory.produceAndPersist {
        withApplication(application)
        withAllocatedToUser(user)
        withAssessmentSchema(assessmentSchema)
      }
    }

    private fun createAssessmentClarificationNote(assessment: ApprovedPremisesAssessmentEntity, user: UserEntity, year: Int, month: Int, dayOfMonth: Int): AssessmentClarificationNoteEntity {
      return assessmentClarificationNoteEntityFactory.produceAndPersist {
        withAssessment(assessment)
        withCreatedAt(
          LocalDate.of(year, month, dayOfMonth).toLocalDateTime(),
        )
        withCreatedBy(user)
      }
    }

    private fun createDomainEvent(year: Int, month: Int, dayOfMonth: Int, type: DomainEventType): DomainEventEntity {
      return domainEventFactory.produceAndPersist {
        withOccurredAt(
          LocalDate.of(year, month, dayOfMonth).toLocalDateTime(),
        )
        withApplicationId(applicationId)
        withType(type)
      }
    }

    private fun createTenDomainEvents(year: Int, month: Int): List<DomainEventEntity> {
      val domainEvent1 = createDomainEvent(year, month, 2, DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
      val domainEvent2 = createDomainEvent(year, month, 3, DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)
      val domainEvent3 = createDomainEvent(year, month, 4, DomainEventType.APPROVED_PREMISES_BOOKING_MADE)
      val domainEvent4 = createDomainEvent(year, month, 5, DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED)
      val domainEvent5 = createDomainEvent(year, month, 6, DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED)
      val domainEvent6 = createDomainEvent(year, month, 7, DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED)
      val domainEvent7 = createDomainEvent(year, month, 8, DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE)
      val domainEvent8 = createDomainEvent(year, month, 9, DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED)
      val domainEvent9 = createDomainEvent(year, month, 10, DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED)
      val domainEvent10 = createDomainEvent(year, month, 11, DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN)

      return listOf(
        domainEvent1, domainEvent2, domainEvent3,
        domainEvent4, domainEvent5, domainEvent6,
        domainEvent7, domainEvent8, domainEvent9,
        domainEvent10,
      )
    }

    private fun createTenInformationRequests(assessment: ApprovedPremisesAssessmentEntity, user: UserEntity, year: Int, month: Int): List<AssessmentClarificationNoteEntity> {
      val informationRequest1 = createAssessmentClarificationNote(assessment, user, year, month, 11)
      val informationRequest2 = createAssessmentClarificationNote(assessment, user, year, month, 12)
      val informationRequest3 = createAssessmentClarificationNote(assessment, user, year, month, 13)
      val informationRequest4 = createAssessmentClarificationNote(assessment, user, year, month, 14)
      val informationRequest5 = createAssessmentClarificationNote(assessment, user, year, month, 15)
      val informationRequest6 = createAssessmentClarificationNote(assessment, user, year, month, 16)
      val informationRequest7 = createAssessmentClarificationNote(assessment, user, year, month, 17)
      val informationRequest8 = createAssessmentClarificationNote(assessment, user, year, month, 18)
      val informationRequest9 = createAssessmentClarificationNote(assessment, user, year, month, 19)
      val informationRequest10 = createAssessmentClarificationNote(assessment, user, year, month, 20)

      return listOf(
        informationRequest1, informationRequest2, informationRequest3,
        informationRequest4, informationRequest5, informationRequest6,
        informationRequest7, informationRequest8, informationRequest9,
        informationRequest10,
      )
    }
  }

  @Nested
  inner class WithdrawApplication {
    @Test
    fun `Withdraw Application without JWT returns 401`() {
      webTestClient.post()
        .uri("/applications/${UUID.randomUUID()}/withdrawal")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Withdraw Application 200`() {
      `Given a User` { user, jwt ->
        val application = produceAndPersistBasicApplication("ABC123", user, "TEAM")

        val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAllocatedToUser(user)
          withAssessmentSchema(assessmentSchema)
        }

        webTestClient.post()
          .uri("/applications/${application.id}/withdrawal")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewWithdrawal(
              reason = WithdrawalReason.alternativeIdentifiedPlacementNoLongerRequired,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk

        val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
        assertThat(updatedApplication.isWithdrawn).isTrue

        val updatedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
        assertThat(updatedAssessment.isWithdrawn).isTrue
      }
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
        """,
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
          """,
      )
    }

    application.teamCodes += applicationTeamCodeRepository.save(
      ApplicationTeamCodeEntity(
        id = UUID.randomUUID(),
        application = application,
        teamCode = managingTeamCode,
      ),
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
  val MessageAttributes: MessageAttributes,
)
