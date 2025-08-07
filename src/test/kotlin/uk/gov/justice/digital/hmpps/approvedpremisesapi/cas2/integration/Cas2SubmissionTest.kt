package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateDetailRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.NomisUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.Agency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.toHttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ExternalUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2Admin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2Assessor
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.manageUsersMockSuccessfulExternalUsersCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockSuccessfulAgencyDetailsCall
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2SubmissionTest(
  @Value("\${url-templates.frontend.cas2.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2.submitted-application-overview}") private val submittedApplicationUrlTemplate: String,
) : IntegrationTestBase() {
  @SpykBean
  lateinit var realApplicationRepository: Cas2ApplicationRepository

  @SpykBean
  lateinit var realAssessmentRepository: Cas2AssessmentRepository

  @SpykBean
  lateinit var realStatusUpdateRepository: Cas2StatusUpdateRepository

  @SpykBean
  lateinit var realStatusUpdateDetailRepository: Cas2StatusUpdateDetailRepository

  @Autowired
  lateinit var nomisUserTransformer: NomisUserTransformer

  @Autowired
  lateinit var applicationAssignmentRepository: Cas2ApplicationAssignmentRepository

  val schema = """
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

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realApplicationRepository)
    clearMocks(realAssessmentRepository)
    clearMocks(realStatusUpdateRepository)
    clearMocks(realStatusUpdateDetailRepository)
  }

  @Nested
  inner class ControlsOnExternalUsers {
    @Test
    fun `submitting an application is forbidden to external users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "auth",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.post()
        .uri("/cas2/submissions?applicationId=de6512fc-a225-4109-bdcd-86c6307a5237")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `viewing submitted applications is forbidden to internal users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_POM"),
      )

      webTestClient.get()
        .uri("/cas2/submissions")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `viewing a single submitted application is forbidden to internal users based on     role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_POM"),
      )

      webTestClient.get()
        .uri("/cas2/submissions/66911cf0-75b1-4361-84bd-501b176fd4fd")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  inner class MissingJwt {
    @Test
    fun `Get all submitted applications without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2/submissions")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get single submitted application without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2/submissions/9b785e59-b85c-4be0-b271-d9ac287684b6")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class GetToIndex {
    @Test
    fun `Previously unknown Assessor has an ExternalUser record created from details retrieved from Manage-Users API `() {
      externalUserRepository.deleteAll()

      val username = "PREVIOUSLY_UNKNOWN_ASSESSOR"
      val externalUserDetails = ExternalUserDetailsFactory()
        .withUsername(username)
        .produce()

      manageUsersMockSuccessfulExternalUsersCall(
        username = username,
        externalUserDetails = externalUserDetails,
      )

      val jwt = jwtAuthHelper.createValidExternalAuthorisationCodeJwt(username)

      webTestClient.get()
        .uri("/cas2/submissions")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk

      assertThat(
        externalUserRepository.findByUsername(username),
      ).isNotNull
    }

    @Test
    fun `Assessor can view ALL submitted applications`() {
      givenACas2Assessor { _, jwt ->
        givenACas2PomUser { user, _ ->
          givenAnOffender { offenderDetails, _ ->
            val submittedcas2applicationentitySecond = cas2ApplicationEntityFactory
              .produceAndPersist {
                withCreatedByUser(user)
                withCrn(offenderDetails.otherIds.crn)
                withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
                withSubmittedAt(OffsetDateTime.parse("2023-01-02T09:00:00+01:00"))
                withData("{}")
              }

            val submittedcas2applicationentityFirst = cas2ApplicationEntityFactory
              .produceAndPersist {
                withCreatedByUser(user)
                withCrn(offenderDetails.otherIds.crn)
                withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
                withSubmittedAt(OffsetDateTime.parse("2023-01-01T09:00:00+01:00"))
                withData("{}")
              }

            val submittedcas2applicationentityThird = cas2ApplicationEntityFactory
              .produceAndPersist {
                withCreatedByUser(user)
                withCrn(offenderDetails.otherIds.crn)
                withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
                withSubmittedAt(OffsetDateTime.parse("2023-01-03T09:00:00+01:00"))
                withData("{}")
              }

            val inProgressCas2ApplicationEntity = cas2ApplicationEntityFactory
              .produceAndPersist {
                withCreatedByUser(user)
                withCrn(offenderDetails.otherIds.crn)
                withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
                withSubmittedAt(null)
                withData("{}")
              }

            val rawResponseBody = webTestClient.get()
              .uri("/cas2/submissions?page=1")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
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
                object : TypeReference<List<Cas2SubmittedApplicationSummary>>() {},
              )

            assertApplicationResponseMatchesExpected(
              responseBody[0],
              submittedcas2applicationentityFirst,
              offenderDetails,
            )

            assertApplicationResponseMatchesExpected(
              responseBody[1],
              submittedcas2applicationentitySecond,
              offenderDetails,
            )

            assertApplicationResponseMatchesExpected(
              responseBody[2],
              submittedcas2applicationentityThird,
              offenderDetails,
            )

            assertThat(responseBody).noneMatch {
              inProgressCas2ApplicationEntity.id == it.id
            }
          }
        }
      }
    }

    private fun assertApplicationResponseMatchesExpected(
      response: Cas2SubmittedApplicationSummary,
      expectedSubmittedApplication: Cas2ApplicationEntity,
      offenderDetails: OffenderDetailSummary,
    ) {
      assertThat(response).matches {
        expectedSubmittedApplication.id == it.id &&
          expectedSubmittedApplication.crn == it.crn &&
          expectedSubmittedApplication.nomsNumber == it.nomsNumber &&
          expectedSubmittedApplication.createdAt.toInstant() == it.createdAt &&
          expectedSubmittedApplication.createdByUser.id == it.createdByUserId &&
          expectedSubmittedApplication.submittedAt?.toInstant() == it.submittedAt
      }

      assertThat(response.personName)
        .isEqualTo("${offenderDetails.firstName} ${offenderDetails.surname}")
    }
  }

  @Nested
  inner class GetToShow {

    private fun createInProgressApplication(
      crn: String,
      user: NomisUserEntity,
    ): Cas2ApplicationEntity {
      val applicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
        withCrn(crn)
        withCreatedByUser(user)
        withSubmittedAt(null)
        withData(
          """
                              {
                                 "thingId": 123
                              }
                              """,
        )
      }

      return applicationEntity
    }

    @Test
    fun `Previously unknown Assessor has an ExternalUser record created from details retrieved from Manage-Users API`() {
      externalUserRepository.deleteAll()

      val username = "PREVIOUSLY_UNKNOWN_ASSESSOR"
      val externalUserDetails = ExternalUserDetailsFactory()
        .withUsername(username)
        .produce()

      manageUsersMockSuccessfulExternalUsersCall(
        username = username,
        externalUserDetails = externalUserDetails,
      )

      val jwt = jwtAuthHelper.createValidExternalAuthorisationCodeJwt(username)

      webTestClient.get()
        .uri("/cas2/submissions/fea7986d-cae6-4a7a-8420-5b31376ce787")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound

      assertThat(
        externalUserRepository.findByUsername("PREVIOUSLY_UNKNOWN_ASSESSOR"),
      ).isNotNull
    }

    @Test
    fun `Assessor can view single submitted application (transferred)`() {
      givenACas2Assessor { assessor, jwt ->
        givenACas2PomUser { user, _ ->
          givenAnOffender { offenderDetails, _ ->
            val omuEmail = "test@test.com"

            val applicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(user)
              withSubmittedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
              withData(
                """
            {
               "thingId": 123
            }
            """,
              )
            }

            val assessmentEntity = cas2AssessmentEntityFactory.produceAndPersist {
              withApplication(applicationEntity)
              withNacroReferralId("OH123")
              withAssessorName("Assessor name")
            }

            val update1 = cas2StatusUpdateEntityFactory.produceAndPersist {
              withApplication(applicationEntity)
              withAssessment(assessmentEntity)
              withAssessor(assessor)
              withLabel("1st update")
            }

            val update2 = cas2StatusUpdateEntityFactory.produceAndPersist {
              withApplication(applicationEntity)
              withAssessment(assessmentEntity)
              withAssessor(assessor)
              withLabel("2nd update")
            }

            val update3 = cas2StatusUpdateEntityFactory.produceAndPersist {
              withApplication(applicationEntity)
              withAssessment(assessmentEntity)
              withAssessor(assessor)
              withStatusId(UUID.fromString("9a381bc6-22d3-41d6-804d-4e49f428c1de"))
              withLabel("3rd update")
            }

            val statusUpdateDetail = Cas2StatusUpdateDetailEntity(
              id = UUID.fromString("5f89ec4d-1a3e-4ec3-a48b-52959d6fcc6a"),
              statusUpdate = update3,
              statusDetailId = UUID.fromString("62645779-242d-4601-a8f8-d2cbf1d41dfa"),
              label = "Detail on 3rd update",
            )

            update1.apply { this.createdAt = OffsetDateTime.now().minusDays(20) }
            realStatusUpdateRepository.save(update1)

            update2.apply { this.createdAt = OffsetDateTime.now().minusDays(14) }
            realStatusUpdateRepository.save(update2)

            update3.apply { this.createdAt = OffsetDateTime.now().minusDays(1) }
            realStatusUpdateRepository.save(update3)

            statusUpdateDetail.apply { this.createdAt = OffsetDateTime.now().minusDays(1) }
            realStatusUpdateDetailRepository.save(statusUpdateDetail)

            val assignmentDate = OffsetDateTime.now().minusDays(5)

            val newPom = nomisUserEntityFactory.produceAndPersist()
            applicationEntity.applicationAssignments.addAll(
              mutableListOf(
                Cas2ApplicationAssignmentEntity(
                  id = UUID.randomUUID(),
                  application = applicationEntity,
                  prisonCode = "LON",
                  allocatedPomUser = user,
                  createdAt = OffsetDateTime.now().minusDays(18),
                ),
                Cas2ApplicationAssignmentEntity(
                  id = UUID.randomUUID(),
                  application = applicationEntity,
                  prisonCode = "PBI",
                  allocatedPomUser = null,
                  createdAt = OffsetDateTime.now().minusDays(16),
                ),
                Cas2ApplicationAssignmentEntity(
                  id = UUID.randomUUID(),
                  application = applicationEntity,
                  prisonCode = "PBI",
                  allocatedPomUser = newPom,
                  createdAt = assignmentDate,
                ),
              ),
            )

            realApplicationRepository.save(applicationEntity)

            val prisonLon = Agency(agencyId = "LON", description = "London Prison", agencyType = "prison")
            prisonAPIMockSuccessfulAgencyDetailsCall(prisonLon)

            val prisonPbi = Agency(agencyId = "PBI", description = "HMP Peterborough (Male)", agencyType = "prison")
            prisonAPIMockSuccessfulAgencyDetailsCall(prisonPbi)

            offenderManagementUnitEntityFactory.produceAndPersist {
              withPrisonName(prisonPbi.description)
              withPrisonCode(prisonPbi.agencyId)
              withEmail(omuEmail)
            }

            val rawResponseBody = webTestClient.get()
              .uri("/cas2/submissions/${applicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(
              rawResponseBody,
              Cas2SubmittedApplication::class.java,
            )

            val applicant = nomisUserTransformer.transformJpaToApi(
              applicationEntity
                .createdByUser,
            )

            assertThat(responseBody).matches {
              applicationEntity.id == it.id &&
                applicationEntity.crn == it.person.crn &&
                applicationEntity.createdAt.toInstant() == it.createdAt &&
                applicant == it.submittedBy &&
                applicationEntity.submittedAt?.toInstant() == it.submittedAt &&
                serializableToJsonNode(applicationEntity.document) == serializableToJsonNode(it.document)
              assessmentEntity.assessorName == it.assessment.assessorName &&
                assessmentEntity.nacroReferralId == it.assessment.nacroReferralId
            }

            assertThat(responseBody.assessment.statusUpdates!!.map { update -> update.label })
              .isEqualTo(listOf("3rd update", "2nd update", "1st update"))

            assertThat(responseBody.assessment.statusUpdates!!.map { update -> update.label })
              .isEqualTo(listOf("3rd update", "2nd update", "1st update"))

            assertThat(
              responseBody.assessment.statusUpdates!!.first().statusUpdateDetails!!
                .map { detail -> detail.label },
            )
              .isEqualTo(listOf("Detail on 3rd update"))

            assertThat(responseBody.timelineEvents.map { event -> event.label })
              .isEqualTo(
                listOf(
                  "3rd update",
                  "New Prison offender manager ${newPom.name} allocated",
                  "2nd update",
                  "Prison transfer from London Prison to HMP Peterborough (Male)",
                  "1st update",
                  "Application submitted",
                ),
              )

            assertThat(responseBody.allocatedPomEmailAddress).isEqualTo(newPom.email)
            assertThat(responseBody.allocatedPomName).isEqualTo(newPom.name)
            assertThat(responseBody.assignmentDate).isEqualTo(assignmentDate.toLocalDate())
            assertThat(responseBody.currentPrisonName).isEqualTo(prisonPbi.description)
            assertThat(responseBody.isTransferredApplication).isTrue()
            assertThat(responseBody.omuEmailAddress).isEqualTo(omuEmail)
          }
        }
      }
    }

    @Test
    fun `Assessor can NOT view single in-progress application`() {
      givenACas2Assessor { _, jwt ->
        givenACas2PomUser { user, _ ->
          givenAnOffender { offenderDetails, _ ->

            val applicationEntity = createInProgressApplication(
              offenderDetails.otherIds.crn,
              user,
            )

            webTestClient.get()
              .uri("/cas2/submissions/${applicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isNotFound
          }
        }
      }
    }

    @Nested
    inner class ControlsOnCas2Admin {
      @Test
      fun `Admin can view single submitted application (not transferred)`() {
        givenACas2Assessor { assessor, _ ->
          givenACas2Admin { admin, jwt ->
            givenACas2PomUser { user, _ ->
              givenAnOffender { offenderDetails, _ ->

                val omu = offenderManagementUnitEntityFactory.produceAndPersist {
                  withPrisonName("PRISON")
                  withPrisonCode("PRI")
                  withEmail("test@test.com")
                }

                val applicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
                  withCrn(offenderDetails.otherIds.crn)
                  withCreatedByUser(user)
                  withReferringPrisonCode("PRI")
                  withSubmittedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                  withData(
                    """
                    {
                        "thingId": 123
                    }
                  """,
                  )
                }

                val assessmentEntity = cas2AssessmentEntityFactory.produceAndPersist {
                  withApplication(applicationEntity)
                  withNacroReferralId("OH123")
                  withAssessorName("Assessor name")
                }

                val update1 = cas2StatusUpdateEntityFactory.produceAndPersist {
                  withApplication(applicationEntity)
                  withAssessor(assessor)
                  withAssessment(assessmentEntity)
                  withLabel("1st update")
                }

                val update2 = cas2StatusUpdateEntityFactory.produceAndPersist {
                  withApplication(applicationEntity)
                  withAssessment(assessmentEntity)
                  withAssessor(assessor)
                  withLabel("2nd update")
                }

                val update3 = cas2StatusUpdateEntityFactory.produceAndPersist {
                  withApplication(applicationEntity)
                  withAssessment(assessmentEntity)
                  withAssessor(assessor)
                  withLabel("3rd update")
                }

                update1.apply { this.createdAt = OffsetDateTime.now().minusDays(20) }
                realStatusUpdateRepository.save(update1)

                update2.apply { this.createdAt = OffsetDateTime.now().minusDays(15) }
                realStatusUpdateRepository.save(update2)

                update3.apply { this.createdAt = OffsetDateTime.now().minusDays(1) }
                realStatusUpdateRepository.save(update3)

                applicationEntity.applicationAssignments.add(
                  Cas2ApplicationAssignmentEntity(
                    id = UUID.randomUUID(),
                    application = applicationEntity,
                    prisonCode = "PRI",
                    allocatedPomUser = user,
                    createdAt = applicationEntity.submittedAt!!,
                  ),
                )

                realApplicationRepository.save(applicationEntity)

                val rawResponseBody = webTestClient.get()
                  .uri("/cas2/submissions/${applicationEntity.id}")
                  .header("Authorization", "Bearer $jwt")
                  .exchange()
                  .expectStatus()
                  .isOk
                  .returnResult<String>()
                  .responseBody
                  .blockFirst()

                val responseBody = objectMapper.readValue(
                  rawResponseBody,
                  Cas2SubmittedApplication::class.java,
                )

                val applicant = nomisUserTransformer.transformJpaToApi(
                  applicationEntity
                    .createdByUser,
                )

                assertThat(responseBody).matches {
                  applicationEntity.id == it.id &&
                    applicationEntity.crn == it.person.crn &&
                    applicationEntity.createdAt.toInstant() == it.createdAt &&
                    applicant == it.submittedBy &&
                    applicationEntity.submittedAt?.toInstant() == it.submittedAt &&
                    serializableToJsonNode(applicationEntity.document) == serializableToJsonNode(it.document)
                }

                assertThat(responseBody.assessment.statusUpdates!!.map { update -> update.label })
                  .isEqualTo(listOf("3rd update", "2nd update", "1st update"))

                assertThat(responseBody.timelineEvents.map { event -> event.label })
                  .isEqualTo(listOf("3rd update", "2nd update", "1st update", "Application submitted"))

                assertThat(responseBody.allocatedPomEmailAddress).isEqualTo(user.email)
                assertThat(responseBody.allocatedPomName).isEqualTo(user.name)
                assertThat(responseBody.assignmentDate).isEqualTo(applicationEntity.submittedAt?.toLocalDate())
                assertThat(responseBody.currentPrisonName).isEqualTo(omu.prisonName)
                assertThat(responseBody.isTransferredApplication).isFalse()
                assertThat(responseBody.omuEmailAddress).isEqualTo(omu.email)
              }
            }
          }
        }
      }

      @Test
      fun `Admin can NOT view single in-progress application`() {
        givenACas2Admin { _, jwt ->
          givenACas2PomUser { user, _ ->
            givenAnOffender { offenderDetails, _ ->

              val applicationEntity = createInProgressApplication(
                offenderDetails.otherIds.crn,
                user,
              )

              webTestClient.get()
                .uri("/cas2/submissions/${applicationEntity.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isNotFound
            }
          }
        }
      }
    }
  }

  @Nested
  inner class PostToSubmit {

    @Test
    fun `Submit Cas2 application creates prisoner location and returns 200`() {
      val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")
      val telephoneNumber = "123 456 7891"

      givenACas2PomUser { submittingUser, jwt ->
        givenAnOffender(
          inmateDetailsConfigBlock = {
            withAssignedLivingUnit(
              AssignedLivingUnit(
                agencyId = "agency_id",
                locationId = 123.toLong(),
                agencyName = "agency_name",
                description = null,
              ),
            )
          },
        ) { offenderDetails, _ ->

          cas2ApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withNomsNumber(offenderDetails.otherIds.nomsNumber.toString())
            withId(applicationId)
            withCreatedByUser(submittingUser)
            withData(
              """
                        {
                           "thingId": 123
                        }
               """,
            )
          }

          assertThat(realAssessmentRepository.count()).isEqualTo(0)

          webTestClient.post()
            .uri("/cas2/submissions")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2.value)
            .bodyValue(
              SubmitCas2Application(
                applicationId = applicationId,
                translatedDocument = {},
                preferredAreas = "Leeds | Bradford",
                hdcEligibilityDate = LocalDate.parse("2023-03-30"),
                conditionalReleaseDate = LocalDate.parse("2023-04-29"),
                telephoneNumber = telephoneNumber,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
        }

        val applicationAssignment =
          applicationAssignmentRepository.findFirstByApplicationIdOrderByCreatedAtDesc(applicationId)
        assertThat(applicationAssignment!!.allocatedPomUser?.id).isEqualTo(submittingUser.id)

        // verify that generated 'application.submitted' domain event links to the CAS2 domain
        val expectedFrontEndUrl = applicationUrlTemplate.replace("#id", applicationId.toString())
        val persistedDomainEvent = domainEventRepository.findFirstByOrderByCreatedAtDesc()
        val domainEventFromJson = objectMapper.readValue(
          persistedDomainEvent!!.data,
          Cas2ApplicationSubmittedEvent::class.java,
        )
        assertThat(domainEventFromJson.eventDetails.applicationUrl)
          .isEqualTo(expectedFrontEndUrl)

        val persistedAssessment = realAssessmentRepository.findAll().first()
        assertThat(persistedAssessment!!.application.id).isEqualTo(applicationId)

        val expectedEmailUrl = submittedApplicationUrlTemplate.replace("#applicationId", applicationId.toString())
        emailAsserter.assertEmailsRequestedCount(1)
        emailAsserter.assertEmailRequested(
          notifyConfig.emailAddresses.cas2Assessors,
          Cas2NotifyTemplates.cas2ApplicationSubmitted,
          personalisation = mapOf(
            "name" to submittingUser.name,
            "email" to submittingUser.email!!,
            "prisonNumber" to persistedAssessment.application.nomsNumber!!,
            "telephoneNumber" to telephoneNumber,
            "applicationUrl" to expectedEmailUrl,
          ),
          replyToEmailId = notifyConfig.emailAddresses.cas2ReplyToId,
        )
      }
    }

    @Test
    fun `When several concurrent submit application requests occur, only one is successful, all others return 400`() {
      givenACas2PomUser { submittingUser, jwt ->
        givenAnOffender(
          inmateDetailsConfigBlock = {
            withAssignedLivingUnit(
              AssignedLivingUnit(
                agencyId = "agency_id",
                locationId = 123.toLong(),
                agencyName = "agency_name",
                description = null,
              ),
            )
          },
        ) { offenderDetails, _ ->
          val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

          cas2ApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withNomsNumber(offenderDetails.otherIds.nomsNumber.toString())
            withId(applicationId)
            withCreatedByUser(submittingUser)
            withData(
              """
            {
               "thingId": 123
            }
            """,
            )
          }

          every { realApplicationRepository.save(any()) } answers {
            Thread.sleep(1000)
            it.invocation.args[0] as Cas2ApplicationEntity
          }

          val responseStatuses = mutableListOf<HttpStatus>()

          (1..10).map {
            val thread = Thread {
              webTestClient.post()
                .uri("/cas2/submissions")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  SubmitCas2Application(
                    applicationId = applicationId,
                    translatedDocument = {},
                    telephoneNumber = "123 456 7891",
                  ),
                )
                .exchange()
                .returnResult<String>()
                .consumeWith {
                  synchronized(responseStatuses) {
                    responseStatuses += it.status.toHttpStatus()
                  }
                }
            }

            thread.start()

            thread
          }.forEach(Thread::join)

          assertThat(responseStatuses.count { it.value() == 200 }).isEqualTo(1)
          assertThat(responseStatuses.count { it.value() == 400 }).isEqualTo(9)
        }
      }
    }

    @Test
    fun `When there's an error fetching the referred person's prison code, the application is not saved`() {
      givenACas2PomUser { submittingUser, jwt ->
        givenAnOffender(mockNotFoundErrorForPrisonApi = true) { offenderDetails, _ ->
          val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

          cas2ApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
            withId(applicationId)
            withCreatedByUser(submittingUser)
            withData(
              """
            {
               "thingId": 123
            }
            """,
            )
          }

          assertThat(domainEventRepository.count()).isEqualTo(0)
          assertThat(realAssessmentRepository.count()).isEqualTo(0)

          webTestClient.post()
            .uri("/cas2/submissions")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2.value)
            .bodyValue(
              SubmitCas2Application(
                applicationId = applicationId,
                translatedDocument = {},
                preferredAreas = "Leeds | Bradford",
                hdcEligibilityDate = LocalDate.parse("2023-03-30"),
                conditionalReleaseDate = LocalDate.parse("2023-04-29"),
                telephoneNumber = "123 456 789",
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest

          assertThat(domainEventRepository.count()).isEqualTo(0)
          assertThat(realAssessmentRepository.count()).isEqualTo(0)
          assertThat(realApplicationRepository.findById(applicationId).get().submittedAt).isNull()
        }
      }
    }
  }

  private fun serializableToJsonNode(serializable: Any?): JsonNode {
    if (serializable == null) return NullNode.instance
    if (serializable is String) return objectMapper.readTree(serializable)

    return objectMapper.readTree(objectMapper.writeValueAsString(serializable))
  }
}
