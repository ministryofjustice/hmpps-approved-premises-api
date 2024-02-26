package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FlagsEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MappaEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskEnvelopeStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisksEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventAssociatedUrl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventUrlType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UnknownPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RegistrationClientResponseFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationSubmittedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingCancelledFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingChangedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingNotMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonDepartedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonNotArrivedFactory
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.RegistrationKeyValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registrations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InOutStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.jsonForObject
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus as ApiApprovedPremisesApplicationStatus

class ApplicationTest : IntegrationTestBase() {
  @Autowired
  lateinit var assessmentTransformer: AssessmentTransformer

  @Autowired
  lateinit var userTransformer: UserTransformer

  @Autowired
  lateinit var applicationTimelineTransformer: ApplicationTimelineTransformer

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
  fun `Get all applications returns 200 - when user has no roles returns applications managed by their teams`() {
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(listOf(StaffUserTeamMembershipFactory().withCode("TEAM1").produce()))
      },
    ) { userEntity, jwt ->
      `Given a User` { otherUser, _ ->
        `Given an Offender` { offenderDetails, _ ->
          `Given an Offender` { otherOffenderDetails, _ ->
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

            CommunityAPI_mockOffenderUserAccessCall(
              userEntity.deliusUsername,
              offenderDetails.otherIds.crn,
              inclusion = false,
              exclusion = false,
            )

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
              objectMapper.readValue(
                rawResponseBody,
                object : TypeReference<List<ApprovedPremisesApplicationSummary>>() {},
              )

            assertThat(responseBody).anyMatch {
              outdatedApplicationEntityManagedByTeam.id == it.id &&
                outdatedApplicationEntityManagedByTeam.crn == it.person.crn &&
                outdatedApplicationEntityManagedByTeam.createdAt.toInstant() == it.createdAt &&
                outdatedApplicationEntityManagedByTeam.createdByUser.id == it.createdByUserId &&
                outdatedApplicationEntityManagedByTeam.submittedAt?.toInstant() == it.submittedAt
            }

            assertThat(responseBody).anyMatch {
              upToDateApplicationEntityManagedByTeam.id == it.id &&
                upToDateApplicationEntityManagedByTeam.crn == it.person.crn &&
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
  fun `Get all applications returns 200 - when user is CAS3_ASSESSOR then returns submitted applications in region`() {
    `Given a Probation Region` { probationRegion ->
      `Given a User`(roles = listOf(UserRole.CAS3_REFERRER), probationRegion = probationRegion) { otherUser, _ ->
        `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR), probationRegion = probationRegion) { assessorUser, jwt ->
          `Given an Offender` { offenderDetails, _ ->
            temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

            val applicationSchema = createApplicationSchema()

            val dateTime = OffsetDateTime.parse("2023-06-01T12:34:56.789+01:00")
            val application =
              createTempApplicationEntity(applicationSchema, otherUser, offenderDetails, probationRegion, dateTime)

            val notSubmittedApplication =
              createTempApplicationEntity(applicationSchema, otherUser, offenderDetails, probationRegion, null)

            val otherProbationRegion = probationRegionEntityFactory.produceAndPersist {
              withYieldedApArea {
                apAreaEntityFactory.produceAndPersist()
              }
            }

            val notInRegionApplication =
              createTempApplicationEntity(applicationSchema, otherUser, offenderDetails, otherProbationRegion, dateTime)

            CommunityAPI_mockOffenderUserAccessCall(
              assessorUser.deliusUsername,
              offenderDetails.otherIds.crn,
              inclusion = false,
              exclusion = false,
            )

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
              objectMapper.readValue(
                rawResponseBody,
                object : TypeReference<List<TemporaryAccommodationApplicationSummary>>() {},
              )

            assertThat(responseBody).anyMatch {
              application.id == it.id && application.crn == it.person.crn &&
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

  private fun createTempApplicationEntity(
    applicationSchema: TemporaryAccommodationApplicationJsonSchemaEntity,
    user: UserEntity,
    offenderDetails: OffenderDetailSummary,
    probationRegion: ProbationRegionEntity,
    submittedAt: OffsetDateTime?,
  ): TemporaryAccommodationApplicationEntity {
    return temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(applicationSchema)
      withCreatedByUser(user)
      withSubmittedAt(submittedAt)
      withCrn(offenderDetails.otherIds.crn)
      withData("{}")
      withProbationRegion(probationRegion)
    }
  }

  private fun createApplicationSchema(): TemporaryAccommodationApplicationJsonSchemaEntity {
    return temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
    }
  }

  @Test
  fun `Get all applications returns 200 for TA - when user is CAS3_REFERRER then returns all applications for user`() {
    `Given a Probation Region` { probationRegion ->
      `Given a User`(roles = listOf(UserRole.CAS3_REFERRER), probationRegion = probationRegion) { otherUser, _ ->
        `Given a User`(roles = listOf(UserRole.CAS3_REFERRER), probationRegion = probationRegion) { referrerUser, jwt ->
          `Given an Offender` { offenderDetails, _ ->
            temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

            val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
            }

            val application =
              createTempApplicationEntity(applicationSchema, referrerUser, offenderDetails, probationRegion, null)

            val anotherUsersApplication =
              createTempApplicationEntity(applicationSchema, otherUser, offenderDetails, probationRegion, null)

            CommunityAPI_mockOffenderUserAccessCall(
              referrerUser.deliusUsername,
              offenderDetails.otherIds.crn,
              inclusion = false,
              exclusion = false,
            )

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
              objectMapper.readValue(
                rawResponseBody,
                object : TypeReference<List<TemporaryAccommodationApplicationSummary>>() {},
              )

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
  fun `Get list of applications returns limited information when a person cannot be found`() {
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(listOf(StaffUserTeamMembershipFactory().withCode("TEAM1").produce()))
      },
    ) { userEntity, jwt ->
      val crn = "X1234"

      val application = produceAndPersistBasicApplication(crn, userEntity, "TEAM1")
      CommunityAPI_mockNotFoundOffenderDetailsCall(crn)
      loadPreemptiveCacheForOffenderDetails(crn)

      CommunityAPI_mockOffenderUserAccessCall(userEntity.deliusUsername, crn, inclusion = false, exclusion = false)

      webTestClient.get()
        .uri("/applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            listOf(
              ApprovedPremisesApplicationSummary(
                createdByUserId = userEntity.id,
                status = ApiApprovedPremisesApplicationStatus.started,
                type = "CAS1",
                id = application.id,
                person = UnknownPerson(
                  crn = crn,
                  type = PersonType.unknownPerson,
                ),
                createdAt = application.createdAt.toInstant(),
                isWomensApplication = null,
                isPipeApplication = null,
                isEmergencyApplication = null,
                isEsapApplication = null,
                arrivalDate = null,
                risks = PersonRisks(
                  crn = crn,
                  roshRisks = RoshRisksEnvelope(RiskEnvelopeStatus.notFound),
                  tier = RiskTierEnvelope(RiskEnvelopeStatus.notFound),
                  flags = FlagsEnvelope(RiskEnvelopeStatus.notFound),
                  mappa = MappaEnvelope(RiskEnvelopeStatus.notFound),
                ),
                submittedAt = null,
                isWithdrawn = false,
              ),
            ),
          ),
        )
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
      ) { offenderDetails, _ ->
        val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, userEntity, "TEAM1")

        CommunityAPI_mockOffenderUserAccessCall(
          userEntity.deliusUsername,
          offenderDetails.otherIds.crn,
          inclusion = false,
          exclusion = false,
        )

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
            person.status == PersonStatus.unknown &&
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

      CommunityAPI_mockOffenderUserAccessCall(
        userEntity.deliusUsername,
        offenderDetails.otherIds.crn,
        inclusion = false,
        exclusion = false,
      )

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
          person.status == PersonStatus.unknown &&
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
      `Given an Offender` { offenderDetails, _ ->
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

        CommunityAPI_mockOffenderUserAccessCall(
          userEntity.deliusUsername,
          offenderDetails.otherIds.crn,
          inclusion = false,
          exclusion = false,
        )

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
          applicationEntity.id == it.id && applicationEntity.crn == it.person.crn &&
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
        `Given an Offender` { offenderDetails, _ ->
          val crn = "X1234"

          val application = produceAndPersistBasicApplication(crn, otherUser, "TEAM1")

          CommunityAPI_mockOffenderUserAccessCall(
            userEntity.deliusUsername,
            offenderDetails.otherIds.crn,
            inclusion = false,
            exclusion = false,
          )

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
      ) { offenderDetails, _ ->
        val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, userEntity, "TEAM1")

        CommunityAPI_mockOffenderUserAccessCall(
          userEntity.deliusUsername,
          offenderDetails.otherIds.crn,
          inclusion = false,
          exclusion = false,
        )

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
            person.status == PersonStatus.unknown &&
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

      CommunityAPI_mockOffenderUserAccessCall(
        userEntity.deliusUsername,
        offenderDetails.otherIds.crn,
        inclusion = false,
        exclusion = false,
      )

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
          person.status == PersonStatus.unknown &&
          person.prisonName == null
      }
    }
  }

  @Test
  fun `Get single online application returns 200 non-upgradable outdated application marked as such`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
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

        CommunityAPI_mockOffenderUserAccessCall(
          userEntity.deliusUsername,
          offenderDetails.otherIds.crn,
          inclusion = false,
          exclusion = false,
        )

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
            nonUpgradableApplicationEntity.crn == it.person.crn &&
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

        CommunityAPI_mockOffenderUserAccessCall(
          userEntity.deliusUsername,
          offenderDetails.otherIds.crn,
          inclusion = false,
          exclusion = false,
        )

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

          CommunityAPI_mockOffenderUserAccessCall(
            userEntity.deliusUsername,
            offenderDetails.otherIds.crn,
            inclusion = false,
            exclusion = false,
          )

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

          CommunityAPI_mockOffenderUserAccessCall(
            userEntity.deliusUsername,
            offenderDetails.otherIds.crn,
            inclusion = false,
            exclusion = false,
          )

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

          CommunityAPI_mockOffenderUserAccessCall(
            userEntity.deliusUsername,
            offenderDetails.otherIds.crn,
            inclusion = false,
            exclusion = false,
          )

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

  @Nested
  inner class GetPlacementApplications {

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

    @Test
    fun `Get placement applications without JWT returns 401`() {
      `Given a User` { user, _ ->

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

    @ParameterizedTest
    @NullSource
    @EnumSource(value = PlacementApplicationDecision::class, names = ["WITHDRAWN_BY_PP"], mode = EnumSource.Mode.EXCLUDE)
    fun `Get placement applications returns the transformed objects`(decision: PlacementApplicationDecision?) {
      `Given a User` { user, jwt ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          decision = decision,
          submittedAt = OffsetDateTime.now(),
        ) { _ ->
          `Given a Placement Application`(
            createdByUser = user,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
            reallocated = true,
            decision = decision,
            submittedAt = OffsetDateTime.now(),
          ) { _ ->
            `Given a Placement Application`(
              createdByUser = user,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              decision = decision,
              submittedAt = OffsetDateTime.now(),
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

              assertThat(body.size).isEqualTo(1)
              assertThat(body[0].id).isEqualTo(placementApplicationEntity.id)
              assertThat(body[0].applicationId).isEqualTo(placementApplicationEntity.application.id)
              assertThat(body[0].createdByUserId).isEqualTo(placementApplicationEntity.createdByUser.id)
              assertThat(body[0].schemaVersion).isEqualTo(placementApplicationEntity.schemaVersion.id)
              assertThat(body[0].createdAt).isEqualTo(placementApplicationEntity.createdAt.toInstant())
              assertThat(body[0].submittedAt).isCloseTo(
                placementApplicationEntity.submittedAt!!.toInstant(),
                within(1, ChronoUnit.SECONDS),
              )
            }
          }
        }
      }
    }

    @Test
    fun `Get placement applications does not return withdrawn placement applications`() {
      `Given a User` { user, jwt ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          decision = PlacementApplicationDecision.WITHDRAWN_BY_PP,
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
          assertThat(body.size).isEqualTo(0)
        }
      }
    }

    @Test
    fun `Get placement applications does not return unsubmitted placement applications`() {
      `Given a User` { user, jwt ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          decision = PlacementApplicationDecision.WITHDRAWN_BY_PP,
          submittedAt = null,
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
          assertThat(body.size).isEqualTo(0)
        }
      }
    }

  }

  @Test
  fun `Get single offline application returns 200 with correct body`() {
    `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val offlineApplicationEntity = offlineApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
        }

        CommunityAPI_mockOffenderUserAccessCall(
          userEntity.deliusUsername,
          offenderDetails.otherIds.crn,
          inclusion = false,
          exclusion = false,
        )

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
    `Given a User` { _, jwt ->
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
    `Given a User` { _, jwt ->
      `Given an Offender` { offenderDetails, _ ->

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
    `Given a User` { _, jwt ->
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
    `Given a User` { _, jwt ->
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
    `Given a User` { _, jwt ->
      `Given an Offender`(
        offenderDetailsConfigBlock = { withoutNomsNumber() },
      ) { offenderDetails, _ ->
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
    `Given a User` { _, jwt ->
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
  fun `Create new application for Temporary Accommodation returns 403 when user isn't  CAS3_REFERRER role`() {
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
  fun `Should get 403 forbidden error when create new application for Temporary Accommodation with user CAS3_REPORTER`() {
    `Given a User`(roles = listOf(UserRole.CAS3_REPORTER)) { _, jwt ->
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
  fun `Create new application for Temporary Accommodation returns 201 with correct body and store prison-name in DB`() {
    `Given a User`(roles = listOf(UserRole.CAS3_REFERRER)) { _, jwt ->
      val agencyName = "HMP Bristol"
      `Given an Offender`(
        offenderDetailsConfigBlock = {
          withCrn("CRN")
          withDateOfBirth(LocalDate.parse("1985-05-05"))
          withNomsNumber("NOMS321")
          withFirstName("James")
          withLastName("Someone")
          withGender("Male")
          withEthnicity("White British")
          withNationality("English")
          withReligionOrBelief("Judaism")
          withGenderIdentity("Prefer to self-describe")
          withSelfDescribedGenderIdentity("This is a self described identity")
        },
        inmateDetailsConfigBlock = {
          withOffenderNo("NOMS321")
          withInOutStatus(InOutStatus.IN)
          withAssignedLivingUnit(
            AssignedLivingUnit(
              agencyId = "BRI",
              locationId = 5,
              description = "B-2F-004",
              agencyName = agencyName,
            ),
          )
        },
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

        val blockFirst = result.responseBody.blockFirst()
        assertThat(blockFirst).matches {
          it.person.crn == offenderDetails.otherIds.crn &&
            it.schemaVersion == applicationSchema.id &&
            it.offenceId == offenceId
        }

        val accommodationApplicationEntity =
          temporaryAccommodationApplicationRepository.findByIdOrNull(blockFirst.id)
        assertThat(accommodationApplicationEntity!!.prisonNameOnCreation).isNotNull()
        assertThat(accommodationApplicationEntity!!.prisonNameOnCreation).isEqualTo(agencyName)
      }
    }
  }

  @Test
  fun `Update existing AP application returns 200 with correct body`() {
    `Given a User` { submittingUser, jwt ->
      `Given a User`(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        qualifications = listOf(UserQualification.PIPE),
      ) { _, _ ->
        `Given an Offender` { offenderDetails, _ ->
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
      `Given a User`(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      ) { assessorUser, _ ->
        `Given an Offender` { offenderDetails, _ ->
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
                sentenceType = SentenceTypeOption.nonStatutory,
                type = "CAS1",
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val persistedApplication =
            approvedPremisesApplicationRepository.findByIdOrNull(applicationId)

          assertThat(persistedApplication?.isWomensApplication).isTrue
          assertThat(persistedApplication?.isPipeApplication).isTrue
          assertThat(persistedApplication?.targetLocation).isEqualTo("SW1A 1AA")
          assertThat(persistedApplication?.sentenceType).isEqualTo(SentenceTypeOption.nonStatutory.toString())
          assertThat(persistedApplication?.apArea?.id).isEqualTo(submittingUser.probationRegion.apArea.id)

          val createdAssessment =
            approvedPremisesAssessmentRepository.findAll().first { it.application.id == applicationId }
          assertThat(createdAssessment.allocatedToUser!!.id).isEqualTo(assessorUser.id)
          assertThat(createdAssessment.createdFromAppeal).isFalse()

          val persistedDomainEvent = domainEventRepository.findAll().firstOrNull { it.applicationId == applicationId }

          assertThat(persistedDomainEvent).isNotNull
          assertThat(persistedDomainEvent!!.crn).isEqualTo(offenderDetails.otherIds.crn)
          assertThat(persistedDomainEvent.type).isEqualTo(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)

          val emittedMessage = snsDomainEventListener.blockForMessage()

          assertThat(emittedMessage.eventType).isEqualTo("approved-premises.application.submitted")
          val emittedMessageDescription = "An application has been submitted for an Approved Premises placement"
          assertThat(emittedMessage.description).isEqualTo(emittedMessageDescription)
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
  fun `Submit application returns 200, creates and allocates an assessment, has given probation region id`() {
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(
          listOf(
            StaffUserTeamMembershipFactory().produce(),
          ),
        )
      },
    ) { submittingUser, jwt ->
      `Given a User`(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      ) { assessorUser, _ ->
        `Given an Offender` { offenderDetails, _ ->
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

          val apArea = apAreaEntityFactory.produceAndPersist {
          }

          val probationRegion = probationRegionEntityFactory.produceAndPersist {
            withApArea(apArea)
          }

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
                sentenceType = SentenceTypeOption.nonStatutory,
                type = "CAS1",
                apAreaId = apArea.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val persistedApplication =
            approvedPremisesApplicationRepository.findByIdOrNull(applicationId)

          assertThat(persistedApplication?.isWomensApplication).isTrue
          assertThat(persistedApplication?.isPipeApplication).isTrue
          assertThat(persistedApplication?.targetLocation).isEqualTo("SW1A 1AA")
          assertThat(persistedApplication?.sentenceType).isEqualTo(SentenceTypeOption.nonStatutory.toString())
          assertThat(persistedApplication?.apArea?.id).isEqualTo(apArea.id)
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
      `Given a User`(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        qualifications = listOf(UserQualification.PIPE, UserQualification.WOMENS),
      ) { _, _ ->
        `Given an Offender` { offenderDetails, _ ->
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
                    sentenceType = SentenceTypeOption.nonStatutory,
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
      `Given a User` { _, _ ->
        `Given an Offender` { offenderDetails, _ ->
          val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
            withSchema(schemaText())
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

  @Test
  fun `Submit Temporary Accommodation application returns 200 with optionl elements in the request`() {
    `Given a User`(
      staffUserDetailsConfigBlock = {
        withTeams(
          listOf(
            StaffUserTeamMembershipFactory().produce(),
          ),
        )
      },
    ) { submittingUser, jwt ->
      `Given a User` { _, _ ->
        `Given an Offender` { offenderDetails, _ ->
          val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
            withSchema(schemaText())
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
                personReleaseDate = LocalDate.now(),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val persistedApplication = temporaryAccommodationApplicationRepository.findByIdOrNull(applicationId)!!
          val persistedAssessment = persistedApplication.getLatestAssessment() as TemporaryAccommodationAssessmentEntity

          assertThat(persistedAssessment.summaryData).isEqualTo("{\"num\":50,\"text\":\"Hello world!\"}")
          assertThat(persistedApplication.personReleaseDate).isEqualTo(LocalDate.now())
        }
      }
    }
  }

  private fun schemaText(): String {
    return """
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
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
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
          `Given a User` { _, jwt ->
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
  }

  @Nested
  inner class ApplicationTimeline {
    private val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

    private val note1Id = UUID.randomUUID()
    private val note2Id = UUID.randomUUID()
    private val note3Id = UUID.randomUUID()

    private val note1CreatedAt = LocalDate.of(2023, 10, 1).toLocalDateTime()
    private val note2CreatedAt = LocalDate.of(2023, 10, 1).toLocalDateTime()
    private val note3CreatedAt = LocalDate.of(2023, 10, 3).toLocalDateTime()

    var year = 2022
    var month = 3

    @Test
    fun `Get application timeline without JWT returns 401`() {
      webTestClient.get()
        .uri("/applications/$applicationId/timeline")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get application timeline returns 501 not implemented when not approved premises service`() {
      `Given a User`(roles = listOf(UserRole.CAS1_ADMIN)) { _, jwt ->
        webTestClient.get()
          .uri("/applications/$applicationId/timeline")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isEqualTo(HttpStatus.NOT_IMPLEMENTED)
      }
    }

    @Test
    fun `Get application timeline returns 200 when user has permission and approved premises service`() {
      `Given a User` { _, jwt ->
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
      `Given a User` { _, jwt ->

        val domainEvents = createTenDomainEvents()

        val domainEventDescriptions = listOf(
          "The application was submitted",
          "The application was assessed and accepted",
          "A booking was made for between 2024-01-01 and 2024-04-01",
          "The person moved into the premises on 2024-01-01",
          "The person was due to move into the premises on 2024-01-01 but did not arrive",
          "The person moved out of the premises on 2024-04-01",
          "A booking was not made for the placement request. The reason was: no suitable premises",
          "The booking was cancelled. The reason was: additional sentencing",
          "The booking had its arrival or departure date changed",
          "The application was withdrawn",
        )

        val summaries = domainEvents.zip(domainEventDescriptions).map {
          TimelineEvent(
            applicationTimelineTransformer.transformDomainEventTypeToTimelineEventType(it.first.type),
            it.first.id.toString(),
            it.first.occurredAt.toInstant(),
            associatedUrls = listOf(TimelineEventAssociatedUrl(type = TimelineEventUrlType.application, url = "http://frontend/applications/${it.first.applicationId}")),
            content = it.second,
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
      `Given a User` { user, jwt ->

        val assessment = createAssessment(user)

        val domainEventDescriptions = listOf(
          "The application was submitted",
          "The application was assessed and accepted",
          "A booking was made for between 2024-01-01 and 2024-04-01",
          "The person moved into the premises on 2024-01-01",
          "The person was due to move into the premises on 2024-01-01 but did not arrive",
          "The person moved out of the premises on 2024-04-01",
          "A booking was not made for the placement request. The reason was: no suitable premises",
          "The booking was cancelled. The reason was: additional sentencing",
          "The booking had its arrival or departure date changed",
          "The application was withdrawn",
        )

        val domainEvents = createTenDomainEvents()
        val summaries = domainEvents.zip(domainEventDescriptions).map {
          TimelineEvent(
            applicationTimelineTransformer.transformDomainEventTypeToTimelineEventType(it.first.type),
            it.first.id.toString(),
            it.first.occurredAt.toInstant(),
            associatedUrls = listOf(TimelineEventAssociatedUrl(type = TimelineEventUrlType.application, url = "http://frontend/applications/${it.first.applicationId}")),
            content = it.second,
          )
        }

        val informationRequests = createTenInformationRequests(assessment, user)
        val informationRequestSummaries = informationRequests.map {
          TimelineEvent(
            TimelineEventType.approvedPremisesInformationRequest,
            it.id.toString(),
            it.createdAt.toInstant(),
            associatedUrls = emptyList(),
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

    @Test
    fun `Get application timeline returns correct manually added application timeline notes`() {
      `Given a User` { user, jwt ->
        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        approvedPremisesApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(applicationSchema)
          withId(applicationId)
          withCreatedByUser(user)
        }

        applicationTimelineNoteEntityFactory.produceAndPersist {
          withApplicationId(applicationId)
          withBody("note1")
          withId(note1Id)
          withCreatedAt(note1CreatedAt)
          withCreatedBy(user)
        }

        applicationTimelineNoteEntityFactory.produceAndPersist {
          withApplicationId(applicationId)
          withBody("note2")
          withId(note2Id)
          withCreatedAt(note2CreatedAt)
          withCreatedBy(user)
        }

        applicationTimelineNoteEntityFactory.produceAndPersist {
          withApplicationId(applicationId)
          withBody("note3")
          withId(note3Id)
          withCreatedAt(note3CreatedAt)
          withCreatedBy(user)
        }

        val notes = createThreeTimelineEventsOfType(userTransformer.transformJpaToApi(user, ServiceName.approvedPremises))

        webTestClient.get()
          .uri("/applications/$applicationId/timeline")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(notes))
      }
    }

    @Test
    fun `Get application timeline includes URLs where applicable`() {
      `Given a User` { _, jwt ->

        val applicationId = UUID.randomUUID()
        val assessmentId = UUID.randomUUID()

        val applicationAndAssessmentIdDomainEvent = domainEventFactory.produceAndPersist {
          withOccurredAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withApplicationId(applicationId)
          withType(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)
          withApplicationId(applicationId)
          withAssessmentId(assessmentId)
          withBookingId(null)
          withData(
            ApplicationAssessedEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = "approved-premises.application.assessed",
              eventDetails = ApplicationAssessedFactory()
                .withApplicationId(applicationId)
                .withDecision("accepted")
                .produce(),
            ),
          )
        }

        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val booking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(LocalDate.parse("2023-03-08"))
          withDepartureDate(LocalDate.parse("2023-03-10"))
        }

        val bookingIdDomainEvent = domainEventFactory.produceAndPersist {
          withOccurredAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withApplicationId(applicationId)
          withType(DomainEventType.APPROVED_PREMISES_BOOKING_MADE)
          withBookingId(booking.id)
          withAssessmentId(null)
          withData(
            BookingMadeEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = "approved-premises.booking.made",
              eventDetails = BookingMadeFactory()
                .withApplicationId(applicationId)
                .withBookingId(booking.id)
                .withArrivalOn(LocalDate.parse("2024-01-01"))
                .withDepartureOn(LocalDate.parse("2024-04-01"))
                .produce(),
            ),
          )
        }

        val applicationAndAssessmentTimelineEvent = TimelineEvent(
          TimelineEventType.approvedPremisesApplicationAssessed,
          applicationAndAssessmentIdDomainEvent.id.toString(),
          applicationAndAssessmentIdDomainEvent.occurredAt.toInstant(),
          associatedUrls = listOf(
            TimelineEventAssociatedUrl(type = TimelineEventUrlType.application, url = "http://frontend/applications/$applicationId"),
            TimelineEventAssociatedUrl(type = TimelineEventUrlType.assessment, url = "http://frontend/assessments/$assessmentId"),
          ),
          content = "The application was assessed and accepted",
        )

        val bookingTimelineEvent = TimelineEvent(
          TimelineEventType.approvedPremisesBookingMade,
          bookingIdDomainEvent.id.toString(),
          bookingIdDomainEvent.occurredAt.toInstant(),
          associatedUrls = listOf(
            TimelineEventAssociatedUrl(type = TimelineEventUrlType.application, url = "http://frontend/applications/$applicationId"),
            TimelineEventAssociatedUrl(type = TimelineEventUrlType.booking, url = "http://frontend/premises/${premises.id}/bookings/${booking.id}"),
          ),
          content = "A booking was made for between 2024-01-01 and 2024-04-01",
        )

        val expected = listOf(
          applicationAndAssessmentTimelineEvent,
          bookingTimelineEvent,
        )

        webTestClient.get()
          .uri("/applications/$applicationId/timeline")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonForObject(expected)
      }
    }

    private fun createThreeTimelineEventsOfType(user: User): List<TimelineEvent> {
      return listOf(
        TimelineEvent(
          TimelineEventType.applicationTimelineNote,
          note1Id.toString(),
          note1CreatedAt.toInstant(),
          "note1",
          user,
          associatedUrls = emptyList(),
        ),
        TimelineEvent(
          TimelineEventType.applicationTimelineNote,
          note2Id.toString(),
          note2CreatedAt.toInstant(),
          "note2",
          user,
          associatedUrls = emptyList(),
        ),
        TimelineEvent(
          TimelineEventType.applicationTimelineNote,
          note3Id.toString(),
          note3CreatedAt.toInstant(),
          "note3",
          user,
          associatedUrls = emptyList(),
        ),
      )
    }

    private fun createAssessment(user: UserEntity): ApprovedPremisesAssessmentEntity {
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

    private fun createAssessmentClarificationNote(
      assessment: ApprovedPremisesAssessmentEntity,
      user: UserEntity,
      dayOfMonth: Int,
    ): AssessmentClarificationNoteEntity {
      return assessmentClarificationNoteEntityFactory.produceAndPersist {
        withAssessment(assessment)
        withCreatedAt(
          LocalDate.of(year, month, dayOfMonth).toLocalDateTime(),
        )
        withCreatedBy(user)
      }
    }

    private fun createDomainEvent(dayOfMonth: Int, type: DomainEventType): DomainEventEntity {
      return domainEventFactory.produceAndPersist {
        withOccurredAt(
          LocalDate.of(year, month, dayOfMonth).toLocalDateTime().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
        )
        withApplicationId(applicationId)
        withType(type)

        when (type) {
          DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED -> withData(
            ApplicationSubmittedEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = "approved-premises.application.submitted",
              eventDetails = ApplicationSubmittedFactory()
                .withApplicationId(applicationId)
                .produce(),
            ),
          )

          DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED -> withData(
            ApplicationAssessedEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = "approved-premises.application.assessed",
              eventDetails = ApplicationAssessedFactory()
                .withDecision("accepted")
                .withApplicationId(applicationId)
                .produce(),
            ),
          )

          DomainEventType.APPROVED_PREMISES_BOOKING_MADE -> withData(
            BookingMadeEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = "approved-premises.booking.made",
              eventDetails = BookingMadeFactory()
                .withApplicationId(applicationId)
                .withArrivalOn(LocalDate.parse("2024-01-01"))
                .withDepartureOn(LocalDate.parse("2024-04-01"))
                .produce(),
            ),
          )

          DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED -> withData(
            PersonArrivedEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = "approved-premises.person.arrived",
              eventDetails = PersonArrivedFactory()
                .withApplicationId(applicationId)
                .withArrivedAt(Instant.parse("2024-01-01T12:34:56.789Z"))
                .produce(),
            ),
          )

          DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED -> withData(
            PersonNotArrivedEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = "approved-premises.person.not-arrived",
              eventDetails = PersonNotArrivedFactory()
                .withApplicationId(applicationId)
                .withExpectedArrivalOn(LocalDate.parse("2024-01-01"))
                .produce(),
            ),
          )

          DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED -> withData(
            PersonDepartedEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = "approved-premises.person.departed",
              eventDetails = PersonDepartedFactory()
                .withApplicationId(applicationId)
                .withDepartedAt(Instant.parse("2024-04-01T12:34:56.789Z"))
                .produce(),
            ),
          )

          DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE -> withData(
            BookingNotMadeEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = "approved-premises.booking.not-made",
              eventDetails = BookingNotMadeFactory()
                .withApplicationId(applicationId)
                .withFailureDescription("no suitable premises")
                .produce(),
            ),
          )

          DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED -> withData(
            BookingCancelledEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = "approved-premises.booking.cancelled",
              eventDetails = BookingCancelledFactory()
                .withApplicationId(applicationId)
                .withCancellationReason("additional sentencing")
                .produce(),
            ),
          )

          DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED -> withData(
            BookingChangedEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = "approved-premises.booking.changed",
              eventDetails = BookingChangedFactory()
                .withApplicationId(applicationId)
                .produce(),
            ),
          )

          DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN -> withData(
            ApplicationWithdrawnEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = "approved-premises.application.withdrawn",
              eventDetails = ApplicationWithdrawnFactory()
                .withApplicationId(applicationId)
                .produce(),
            ),
          )

          else -> fail("Only CAS1 domain events should be used in this test.")
        }
      }
    }

    private fun createTenDomainEvents(): List<DomainEventEntity> {
      val domainEvent1 = createDomainEvent(2, DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
      val domainEvent2 = createDomainEvent(3, DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)
      val domainEvent3 = createDomainEvent(4, DomainEventType.APPROVED_PREMISES_BOOKING_MADE)
      val domainEvent4 = createDomainEvent(5, DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED)
      val domainEvent5 = createDomainEvent(6, DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED)
      val domainEvent6 = createDomainEvent(7, DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED)
      val domainEvent7 = createDomainEvent(8, DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE)
      val domainEvent8 = createDomainEvent(9, DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED)
      val domainEvent9 = createDomainEvent(10, DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED)
      val domainEvent10 = createDomainEvent(11, DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN)

      return listOf(
        domainEvent1, domainEvent2, domainEvent3,
        domainEvent4, domainEvent5, domainEvent6,
        domainEvent7, domainEvent8, domainEvent9,
        domainEvent10,
      )
    }

    private fun createTenInformationRequests(
      assessment: ApprovedPremisesAssessmentEntity,
      user: UserEntity,
    ): List<AssessmentClarificationNoteEntity> {
      val informationRequest1 = createAssessmentClarificationNote(assessment, user, 11)
      val informationRequest2 = createAssessmentClarificationNote(assessment, user, 12)
      val informationRequest3 = createAssessmentClarificationNote(assessment, user, 13)
      val informationRequest4 = createAssessmentClarificationNote(assessment, user, 14)
      val informationRequest5 = createAssessmentClarificationNote(assessment, user, 15)
      val informationRequest6 = createAssessmentClarificationNote(assessment, user, 16)
      val informationRequest7 = createAssessmentClarificationNote(assessment, user, 17)
      val informationRequest8 = createAssessmentClarificationNote(assessment, user, 18)
      val informationRequest9 = createAssessmentClarificationNote(assessment, user, 19)
      val informationRequest10 = createAssessmentClarificationNote(assessment, user, 20)

      return listOf(
        informationRequest1, informationRequest2, informationRequest3,
        informationRequest4, informationRequest5, informationRequest6,
        informationRequest7, informationRequest8, informationRequest9,
        informationRequest10,
      )
    }
  }

  @Nested
  inner class TimelineNotesForApplication {
    @Test
    fun `post ApplicationTimelineNote without JWT returns 401`() {
      webTestClient.post()
        .uri("/applications/${UUID.randomUUID()}/notes")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `post ApplicationTimelineNote with JWT returns 200`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        val applicationId = UUID.randomUUID()

        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        approvedPremisesApplicationEntityFactory.produceAndPersist {
          withId(applicationId)
          withCreatedByUser(user)
          withApplicationSchema(applicationSchema)
        }
        val body = ApplicationTimelineNote(note = "some note")
        webTestClient.post()
          .uri("/applications/$applicationId/notes")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            body,
          )
          .exchange()
          .expectStatus()
          .isOk

        val savedNote =
          applicationTimelineNoteRepository.findApplicationTimelineNoteEntitiesByApplicationId(applicationId)
        savedNote.map {
          assertThat(it.body).isEqualTo("some note")
          assertThat(it.applicationId).isEqualTo(applicationId)
          assertThat(it.createdBy.id).isEqualTo(user.id)
        }
      }
    }
  }

  /**
   * Note - Withdrawal cascading is tested in [WithdrawalTest]
   */
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
    fun `Withdraw Application 200 withdraws application and sends an email to application creator`() {
      `Given a User` { user, jwt ->
        val application = produceAndPersistBasicApplication("ABC123", user, "TEAM")

        val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAllocatedToUser(user)
          withAssessmentSchema(assessmentSchema)
          withSubmittedAt(OffsetDateTime.now())
        }

        webTestClient.post()
          .uri("/applications/${application.id}/withdrawal")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewWithdrawal(
              reason = WithdrawalReason.duplicateApplication,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk

        val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
        assertThat(updatedApplication.isWithdrawn).isTrue

        val updatedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
        assertThat(updatedAssessment.isWithdrawn).isTrue

        emailAsserter.assertEmailsRequestedCount(1)
        emailAsserter.assertEmailRequested(
          user.email!!,
          notifyConfig.templates.applicationWithdrawn,
        )
      }
    }

    @Test
    fun `Withdraw Application 200 withdraws application and sends an email to assessor if assessment is pending`() {
      `Given a User` { applicant, jwt ->
        `Given a User` { assessor, _ ->
          val application = produceAndPersistBasicApplication("ABC123", applicant, "TEAM")

          val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withAllocatedToUser(assessor)
            withAssessmentSchema(assessmentSchema)
            withSubmittedAt(null)
          }

          webTestClient.post()
            .uri("/applications/${application.id}/withdrawal")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewWithdrawal(
                reason = WithdrawalReason.duplicateApplication,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          emailAsserter.assertEmailsRequestedCount(2)
          emailAsserter.assertEmailRequested(
            applicant.email!!,
            notifyConfig.templates.applicationWithdrawn,
          )
          emailAsserter.assertEmailRequested(
            assessor.email!!,
            notifyConfig.templates.assessmentWithdrawn,
          )
        }
      }
    }
  }

  @Nested
  inner class ApplicationsAll {
    @Test
    fun `Get applications all without JWT returns 401`() {
      webTestClient.get()
        .uri("/applications/all")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get applications all not approved premises service returns 403 forbidden`() {
      `Given a User` { _, jwt ->
        webTestClient.get()
          .uri("/applications/all")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get applications all returns 200 and correct body`() {
      `Given a User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          approvedPremisesApplicationJsonSchemaRepository.deleteAll()
          val applicationId1 = UUID.randomUUID()
          val applicationId2 = UUID.randomUUID()

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
            withId(applicationId1)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withCreatedAt(OffsetDateTime.parse("2022-09-24T15:00:00+01:00"))
            withSubmittedAt(OffsetDateTime.parse("2022-09-25T16:00:00+01:00"))
            withData(
              """
          {
             "thingId": 123
          }
          """,
            )
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(newestJsonSchema)
            withId(applicationId2)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withCreatedAt(OffsetDateTime.parse("2022-10-24T16:00:00+01:00"))
            withSubmittedAt(OffsetDateTime.parse("2022-10-25T17:00:00+01:00"))
            withData(
              """
          {
             "thingId": 123
          }
          """,
            )
          }

          val rawResponseBody = webTestClient.get()
            .uri("/applications/all")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(
              rawResponseBody,
              object : TypeReference<List<ApplicationSummary>>() {},
            )

          assertThat(responseBody.count()).isEqualTo(2)
        }
      }
    }

    @Test
    fun `Get applications all returns twelve items if no page parameter passed`() {
      `Given a User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          createTwelveApplications(offenderDetails.otherIds.crn, userEntity)

          val rawResponseBody = webTestClient.get()
            .uri("/applications/all")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(
              rawResponseBody,
              object : TypeReference<List<ApplicationSummary>>() {},
            )

          assertThat(responseBody.count()).isEqualTo(12)
        }
      }
    }

    @Test
    fun `Get applications all returns ten items if page parameter passed is one`() {
      `Given a User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          createTwelveApplications(offenderDetails.otherIds.crn, userEntity)

          val rawResponseBody = webTestClient.get()
            .uri("/applications/all?page=1&sortDirection=asc")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(
              rawResponseBody,
              object : TypeReference<List<ApplicationSummary>>() {},
            )

          assertThat(responseBody.count()).isEqualTo(10)
        }
      }
    }

    @Test
    fun `Get applications all returns twelve items if crn parameter passed and no page`() {
      `Given a User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          createTwelveApplications(crn, userEntity)

          val rawResponseBody = webTestClient.get()
            .uri("/applications/all?crnOrName=$crn")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(
              rawResponseBody,
              object : TypeReference<List<ApplicationSummary>>() {},
            )

          assertThat(responseBody.count()).isEqualTo(12)
        }
      }
    }

    @Test
    fun `Get applications all returns twelve items if crn parameter passed and two crns in db no page`() {
      `Given a User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          `Given an Offender` { offenderDetails2, _ ->

            val crn1 = offenderDetails.otherIds.crn
            createTwelveApplications(crn1, userEntity)

            val crn2 = offenderDetails2.otherIds.crn
            createTwelveApplications(crn2, userEntity)

            val rawResponseBody = webTestClient.get()
              .uri("/applications/all?crnOrName=$crn2")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody =
              objectMapper.readValue(
                rawResponseBody,
                object : TypeReference<List<ApplicationSummary>>() {},
              )

            assertThat(responseBody.count()).isEqualTo(12)
          }
        }
      }
    }

    @Test
    fun `Get applications all returns two items if crn parameter passed and two crns in db and page one`() {
      `Given a User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          `Given an Offender` { offenderDetails2, _ ->

            val crn1 = offenderDetails.otherIds.crn
            createTwelveApplications(crn1, userEntity)

            val crn2 = offenderDetails2.otherIds.crn
            createTwelveApplications(crn2, userEntity)

            val rawResponseBody = webTestClient.get()
              .uri("/applications/all?page=2&sortDirection=desc&crnOrName=$crn2")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
              .expectHeader().valueEquals("X-Pagination-PageSize", 10)
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody =
              objectMapper.readValue(
                rawResponseBody,
                object : TypeReference<List<ApplicationSummary>>() {},
              )

            assertThat(responseBody.count()).isEqualTo(2)
          }
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by createdAt desc`() {
      `Given a User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          val date1 = OffsetDateTime.parse("2022-08-24T15:00:00+01:00")
          val date2 = OffsetDateTime.parse("2022-09-24T15:00:00+01:00")
          val date3 = OffsetDateTime.parse("2022-12-24T15:00:00+01:00")
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }
          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withCreatedAt(date1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withCreatedAt(date2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withCreatedAt(date3)
          }

          val rawResponseBody = webTestClient.get()
            .uri("/applications/all?page=1&sortDirection=desc&sortBy=createdAt")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(
              rawResponseBody,
              object : TypeReference<List<ApplicationSummary>>() {},
            )

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].createdAt).isEqualTo(date3.toInstant())
          assertThat(responseBody[1].createdAt).isEqualTo(date2.toInstant())
          assertThat(responseBody[2].createdAt).isEqualTo(date1.toInstant())
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by createdAt asc`() {
      `Given a User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          val date1 = OffsetDateTime.parse("2022-08-24T15:00:00+01:00")
          val date2 = OffsetDateTime.parse("2022-09-24T15:00:00+01:00")
          val date3 = OffsetDateTime.parse("2022-12-24T15:00:00+01:00")
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }
          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withCreatedAt(date1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withCreatedAt(date2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withCreatedAt(date3)
          }

          val rawResponseBody = webTestClient.get()
            .uri("/applications/all?page=1&sortDirection=asc&sortBy=createdAt")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(
              rawResponseBody,
              object : TypeReference<List<ApprovedPremisesApplicationSummary>>() {},
            )

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].createdAt).isEqualTo(date1.toInstant())
          assertThat(responseBody[1].createdAt).isEqualTo(date2.toInstant())
          assertThat(responseBody[2].createdAt).isEqualTo(date3.toInstant())
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by arrivalDate desc`() {
      `Given a User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          val date1 = OffsetDateTime.parse("2022-08-24T15:00:00+01:00")
          val date2 = OffsetDateTime.parse("2022-09-24T15:00:00+01:00")
          val date3 = OffsetDateTime.parse("2022-12-24T15:00:00+01:00")
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }
          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date3)
          }

          val rawResponseBody = webTestClient.get()
            .uri("/applications/all?page=1&sortDirection=desc&sortBy=arrivalDate")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(
              rawResponseBody,
              object : TypeReference<List<ApprovedPremisesApplicationSummary>>() {},
            )

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].arrivalDate).isEqualTo(date3.toInstant())
          assertThat(responseBody[1].arrivalDate).isEqualTo(date2.toInstant())
          assertThat(responseBody[2].arrivalDate).isEqualTo(date1.toInstant())
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by arrivalDate asc`() {
      `Given a User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          val date1 = OffsetDateTime.parse("2022-08-24T15:00:00+01:00")
          val date2 = OffsetDateTime.parse("2022-09-24T15:00:00+01:00")
          val date3 = OffsetDateTime.parse("2022-12-24T15:00:00+01:00")
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }
          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date3)
          }

          val rawResponseBody = webTestClient.get()
            .uri("/applications/all?page=1&sortDirection=asc&sortBy=arrivalDate")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(
              rawResponseBody,
              object : TypeReference<List<ApprovedPremisesApplicationSummary>>() {},
            )

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].arrivalDate).isEqualTo(date1.toInstant())
          assertThat(responseBody[1].arrivalDate).isEqualTo(date2.toInstant())
          assertThat(responseBody[2].arrivalDate).isEqualTo(date3.toInstant())
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by tier asc`() {
      `Given a User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          val date1 = OffsetDateTime.parse("2022-08-24T15:00:00+01:00")
          val date2 = OffsetDateTime.parse("2022-09-24T15:00:00+01:00")
          val date3 = OffsetDateTime.parse("2022-12-24T15:00:00+01:00")
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val risk1 = PersonRisksFactory()
            .withTier(
              RiskWithStatus(
                RiskTier(
                  level = "M1",
                  lastUpdated = LocalDate.parse("2023-06-26"),
                ),
              ),
            )
            .produce()

          val risk2 = PersonRisksFactory()
            .withTier(
              RiskWithStatus(
                RiskTier(
                  level = "M2",
                  lastUpdated = LocalDate.parse("2023-06-26"),
                ),
              ),
            )
            .produce()

          val risk3 = PersonRisksFactory()
            .withTier(
              RiskWithStatus(
                RiskTier(
                  level = "M3",
                  lastUpdated = LocalDate.parse("2023-06-26"),
                ),
              ),
            )
            .produce()

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date1)
            withRiskRatings(risk1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date2)
            withRiskRatings(risk2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date3)
            withRiskRatings(risk3)
          }

          val rawResponseBody = webTestClient.get()
            .uri("/applications/all?page=1&sortDirection=asc&sortBy=tier")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(
              rawResponseBody,
              object : TypeReference<List<ApprovedPremisesApplicationSummary>>() {},
            )

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].tier).isEqualTo("M1")
          assertThat(responseBody[1].tier).isEqualTo("M2")
          assertThat(responseBody[2].tier).isEqualTo("M3")
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by tier desc`() {
      `Given a User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          val date1 = OffsetDateTime.parse("2022-08-24T15:00:00+01:00")
          val date2 = OffsetDateTime.parse("2022-09-24T15:00:00+01:00")
          val date3 = OffsetDateTime.parse("2022-12-24T15:00:00+01:00")
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val risk1 = PersonRisksFactory()
            .withTier(
              RiskWithStatus(
                RiskTier(
                  level = "M1",
                  lastUpdated = LocalDate.parse("2023-06-26"),
                ),
              ),
            )
            .produce()

          val risk2 = PersonRisksFactory()
            .withTier(
              RiskWithStatus(
                RiskTier(
                  level = "M2",
                  lastUpdated = LocalDate.parse("2023-06-26"),
                ),
              ),
            )
            .produce()

          val risk3 = PersonRisksFactory()
            .withTier(
              RiskWithStatus(
                RiskTier(
                  level = "M3",
                  lastUpdated = LocalDate.parse("2023-06-26"),
                ),
              ),
            )
            .produce()

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date1)
            withRiskRatings(risk1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date2)
            withRiskRatings(risk2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date3)
            withRiskRatings(risk3)
          }

          val rawResponseBody = webTestClient.get()
            .uri("/applications/all?page=1&sortDirection=desc&sortBy=tier")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(
              rawResponseBody,
              object : TypeReference<List<ApprovedPremisesApplicationSummary>>() {},
            )

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].tier).isEqualTo("M3")
          assertThat(responseBody[1].tier).isEqualTo("M2")
          assertThat(responseBody[2].tier).isEqualTo("M1")
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page two and query by status`() {
      `Given a User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(12) {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withStatus(ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(12) {
            withApplicationSchema(applicationSchema)
            withCrn(crn)
            withCreatedByUser(userEntity)
            withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
          }

          val rawResponseBody = webTestClient.get()
            .uri("/applications/all?page=2&sortDirection=desc&status=assesmentInProgress")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(
              rawResponseBody,
              object : TypeReference<List<ApprovedPremisesApplicationSummary>>() {},
            )

          assertThat(responseBody.count()).isEqualTo(2)
          assertThat(responseBody[0].status).isEqualTo(ApiApprovedPremisesApplicationStatus.assesmentInProgress)
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body for a given name`() {
      `Given a User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          `Given an Offender` { offenderDetails2, _ ->

            val crn1 = offenderDetails.otherIds.crn

            val crn2 = offenderDetails2.otherIds.crn

            val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            }

            approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(12) {
              withApplicationSchema(applicationSchema)
              withName("Gareth")
              withCrn(crn1)
              withCreatedByUser(userEntity)
              withStatus(ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS)
            }

            approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(12) {
              withApplicationSchema(applicationSchema)
              withName("Stu")
              withCrn(crn2)
              withCreatedByUser(userEntity)
              withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
            }

            val rawResponseBody = webTestClient.get()
              .uri("/applications/all?page=1&sortDirection=desc&crnOrName=Gareth")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
              .expectHeader().valueEquals("X-Pagination-PageSize", 10)
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody =
              objectMapper.readValue(
                rawResponseBody,
                object : TypeReference<List<ApprovedPremisesApplicationSummary>>() {},
              )

            assertThat(responseBody.count()).isEqualTo(10)
          }
        }
      }
    }

    private fun createTwelveApplications(crn: String, user: UserEntity) {
      val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(12) {
        withApplicationSchema(applicationSchema)
        withCrn(crn)
        withCreatedByUser(user)
        withData(
          """
          {
             "thingId": 123
          }
          """,
        )
      }
    }
  }

  private fun serializableToJsonNode(serializable: Any?): JsonNode {
    if (serializable == null) return NullNode.instance
    if (serializable is String) return objectMapper.readTree(serializable)

    return objectMapper.readTree(objectMapper.writeValueAsString(serializable))
  }

  private fun produceAndPersistBasicApplication(
    crn: String,
    userEntity: UserEntity,
    managingTeamCode: String,
  ): ApplicationEntity {
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

    val application =
      approvedPremisesApplicationEntityFactory.produceAndPersist {
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

  private fun produceAndPersistApplicationAndAssessment(
    applicant: UserEntity,
    assignee: UserEntity,
    offenderDetails: OffenderDetailSummary,
  ): Pair<ApprovedPremisesApplicationEntity, AssessmentEntity> {
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
      withSubmittedAt(OffsetDateTime.now())
    }

    val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(assignee)
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
    }

    assessment.schemaUpToDate = true
    application.assessments.add(assessment)

    return Pair(application, assessment)
  }
}
