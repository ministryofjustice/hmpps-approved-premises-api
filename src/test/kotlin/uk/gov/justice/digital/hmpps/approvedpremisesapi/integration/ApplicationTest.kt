package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TeamFactoryDeliusContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextEmptyCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulTeamsManagingCaseCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulNeedsDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationTest : IntegrationTestBase() {

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

  @Nested
  inner class Cas1GetApplication {

    @Test
    fun `Get single non LAO application returns 200 with correct body`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->

          val applicationEntity = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withData("""{"thingId":123}""")
            withReleaseType(ReleaseTypeOption.inCommunity.name)
            withSentenceType(SentenceTypeOption.bailPlacement.name)
          }

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

          assertThat(responseBody.id).isEqualTo(applicationEntity.id)
          assertThat(responseBody.person.crn).isEqualTo(applicationEntity.crn)
          assertThat(responseBody.createdAt).isEqualTo(applicationEntity.createdAt.toInstant())
          assertThat(responseBody.createdByUserId).isEqualTo(applicationEntity.createdByUser.id)
          assertThat(responseBody.submittedAt).isEqualTo(applicationEntity.submittedAt?.toInstant())
          assertThat(responseBody.releaseType).isEqualTo(ReleaseTypeOption.inCommunity)
          assertThat(responseBody.sentenceType).isEqualTo(SentenceTypeOption.bailPlacement)
          assertThat(serializableToJsonNode(responseBody.data)).isEqualTo(serializableToJsonNode(applicationEntity.data))
        }
      }
    }

    @Test
    fun `Get single LAO application for creator, LAO Access, no LAO Qualification returns 200`() {
      givenAUser { applicationCreator, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentRestriction(true)
          },
        ) { offenderDetails, _ ->
          val applicationEntity = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(applicationCreator)
            withData(
              """
          {
             "thingId": 123
          }
          """,
            )
          }

          apDeliusContextMockUserAccess(
            caseAccess = CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .withUserExcluded(false)
              .withUserRestricted(false)
              .produce(),
            username = applicationCreator.deliusUsername,
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

          assertThat(responseBody.person).isInstanceOf(FullPerson::class.java)
        }
      }
    }

    @Test
    fun `Get single LAO application for creator, no LAO Access, has LAO Qualification returns 200`() {
      givenAUser(qualifications = listOf(UserQualification.LAO)) { applicationCreator, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentRestriction(true)
          },
        ) { offenderDetails, _ ->
          val applicationEntity = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(applicationCreator)
            withData(
              """
          {
             "thingId": 123
          }
          """,
            )
          }

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

          assertThat(responseBody.person).isInstanceOf(FullPerson::class.java)
        }
      }
    }

    @Test
    fun `Get single LAO application for non creator, no LAO Access, no LAO Qualification returns 403`() {
      givenAUser { applicationCreator, _ ->
        givenAUser { _, otherUserJwt ->
          givenAnOffender(
            offenderDetailsConfigBlock = {
              withCurrentRestriction(true)
            },
          ) { offenderDetails, _ ->
            val applicationEntity = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(applicationCreator)
              withData(
                """
          {
             "thingId": 123
          }
          """,
              )
            }

            webTestClient.get()
              .uri("/applications/${applicationEntity.id}")
              .header("Authorization", "Bearer $otherUserJwt")
              .exchange()
              .expectStatus()
              .isForbidden
          }
        }
      }
    }

    @Test
    fun `Get single LAO application for non creator, no LAO Access, has LAO Qualification returns 200`() {
      givenAUser { applicationCreator, _ ->
        givenAUser(qualifications = listOf(UserQualification.LAO)) { _, otherUserJwt ->
          givenAnOffender(
            offenderDetailsConfigBlock = {
              withCurrentRestriction(true)
            },
          ) { offenderDetails, _ ->
            val applicationEntity = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(applicationCreator)
              withData(
                """
          {
             "thingId": 123
          }
          """,
              )
            }

            val rawResponseBody = webTestClient.get()
              .uri("/applications/${applicationEntity.id}")
              .header("Authorization", "Bearer $otherUserJwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(rawResponseBody, ApprovedPremisesApplication::class.java)

            assertThat(responseBody.person).isInstanceOf(FullPerson::class.java)
          }
        }
      }
    }

    @Test
    fun `Get single non LAO application for non creator returns 200`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(teams = listOf(TeamFactoryDeliusContext.team(code = "TEAM2"))),
      ) { _, jwt ->
        givenAUser { otherUser, _ ->
          val crn = "X1234"
          givenAnOffender(offenderDetailsConfigBlock = { withCrn(crn) }) { offenderDetails, _ ->
            val application = produceAndPersistBasicApplication(crn, otherUser, "TEAM1")

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

            assertThat(responseBody.person).isInstanceOf(FullPerson::class.java)
          }
        }
      }
    }

    @Test
    fun `Get single non LAO application returns 200 when a person has no NOMS number`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(teams = listOf(TeamFactoryDeliusContext.team(code = "TEAM1"))),
      ) { userEntity, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = { withoutNomsNumber() },
        ) { offenderDetails, _ ->
          val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, userEntity, "TEAM1")

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            userEntity.deliusUsername,
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
    fun `Get single non LAO application returns 200 when the person cannot be fetched from the prisons API`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(teams = listOf(TeamFactoryDeliusContext.team(code = "TEAM1"))),
      ) { userEntity, jwt ->
        val crn = "X1234"

        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCrn(crn)
            withNomsNumber("ABC123")
          },
        ) { _, _ ->
          val application = produceAndPersistBasicApplication(crn, userEntity, "TEAM1")

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
    fun `Get single online application returns 200 non-upgradable outdated application marked as such`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val nonUpgradableApplicationEntity = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withData("{}")
          }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            userEntity.deliusUsername,
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
              serializableToJsonNode(nonUpgradableApplicationEntity.data) == serializableToJsonNode(it.data)
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CRU_MEMBER", "CAS1_ASSESSOR", "CAS1_FUTURE_MANAGER"],
    )
    fun `Get single offline application returns 200 with correct body`(role: UserRole) {
      givenAUser(roles = listOf(role)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val offlineApplicationEntity = offlineApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
          }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            userEntity.deliusUsername,
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
  }

  @Nested
  inner class Cas1CreateApplication {

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
      givenAUser { _, jwt ->
        val crn = "X1234"

        apDeliusContextEmptyCaseSummaryToBulkResponse(crn)

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
      givenAUser { _, jwt ->
        givenAnOffender { offenderDetails, _ ->

          apDeliusContextMockSuccessfulTeamsManagingCaseCall(
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
      givenAUser { _, jwt ->
        givenAnOffender { offenderDetails, _ ->

          apDeliusContextMockSuccessfulTeamsManagingCaseCall(
            offenderDetails.otherIds.crn,
            ManagingTeamsResponse(
              teamCodes = listOf("TEAM1"),
            ),
          )

          apOASysContextMockSuccessfulNeedsDetailsCall(
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
              cas1OffenderRepository.findByCrn(it.person.crn) != null
          }
        }
      }
    }

    @Test
    fun `Create new application without risks returns 201 with correct body and Location header`() {
      givenAUser { _, jwt ->
        givenAnOffender { offenderDetails, _ ->

          apDeliusContextMockSuccessfulTeamsManagingCaseCall(
            offenderDetails.otherIds.crn,
            ManagingTeamsResponse(
              teamCodes = listOf("TEAM1"),
            ),
          )

          apOASysContextMockSuccessfulNeedsDetailsCall(
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
              cas1OffenderRepository.findByCrn(it.person.crn) != null
          }
        }
      }
    }

    @Test
    fun `Create new application returns successfully when a person has no NOMS number`() {
      givenAUser { _, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = { withoutNomsNumber() },
        ) { offenderDetails, _ ->
          apDeliusContextMockSuccessfulTeamsManagingCaseCall(
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
            cas1OffenderRepository.findByCrn(it.person.crn) != null
          }
        }
      }
    }

    @Test
    fun `Create new application returns successfully when the person cannot be fetched from the prisons API`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withNomsNumber("ABC123")
          },
        ) { offenderDetails, _ ->
          apDeliusContextMockSuccessfulTeamsManagingCaseCall(
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
            cas1OffenderRepository.findByCrn(it.person.crn) != null
          }
        }
      }
    }
  }

  @Nested
  inner class Cas1UpdateApplicationCas {
    @Test
    fun `Update existing AP application returns 200 with correct body`() {
      givenAUser { submittingUser, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS1_ASSESSOR),
          qualifications = listOf(UserQualification.PIPE),
        ) { _, _ ->
          givenAnOffender { offenderDetails, _ ->
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

            approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withId(applicationId)
              withCreatedByUser(submittingUser)
            }

            val resultBody = webTestClient.put()
              .uri("/applications/$applicationId")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                UpdateApprovedPremisesApplication(
                  data = mapOf("thingId" to 123),
                  isWomensApplication = false,
                  apType = ApType.pipe,
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
  }

  @Nested
  inner class PostTimelineNotesForApplication {
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
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        val applicationId = UUID.randomUUID()

        approvedPremisesApplicationEntityFactory.produceAndPersist {
          withId(applicationId)
          withCreatedByUser(user)
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
          applicationTimelineNoteRepository.findApplicationTimelineNoteEntitiesByApplicationIdAndDeletedAtIsNull(applicationId)
        savedNote.map {
          assertThat(it.body).isEqualTo("some note")
          assertThat(it.applicationId).isEqualTo(applicationId)
          assertThat(it.createdBy!!.id).isEqualTo(user.id)
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
      givenAUser { user, jwt ->
        val application = produceAndPersistBasicApplication(
          crn = "ABC123",
          userEntity = user,
          managingTeamCode = "TEAM",
          submittedAt = OffsetDateTime.now(),
        )

        val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAllocatedToUser(user)
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
          Cas1NotifyTemplates.APPLICATION_WITHDRAWN_V2,
        )
      }
    }

    @Test
    fun `Withdraw Application 200 withdraws application and sends an email to assessor if assessment is pending`() {
      givenAUser { applicant, jwt ->
        givenAUser { assessor, _ ->
          val application = produceAndPersistBasicApplication(
            crn = "ABC123",
            userEntity = applicant,
            managingTeamCode = "TEAM",
            submittedAt = OffsetDateTime.now(),
          )

          approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withAllocatedToUser(assessor)
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
            Cas1NotifyTemplates.APPLICATION_WITHDRAWN_V2,
          )
          emailAsserter.assertEmailRequested(
            assessor.email!!,
            Cas1NotifyTemplates.ASSESSMENT_WITHDRAWN_V2,
          )
        }
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
    submittedAt: OffsetDateTime? = null,
  ): ApplicationEntity {
    val application =
      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(crn)
        withCreatedByUser(userEntity)
        withData(
          """
          {
             "thingId": 123
          }
          """,
        )
        withSubmittedAt(submittedAt)
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
