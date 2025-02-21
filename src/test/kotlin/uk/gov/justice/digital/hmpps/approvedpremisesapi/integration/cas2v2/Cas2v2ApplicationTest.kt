package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2v2

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2v2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.Cas2v2IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2LicenceCaseAdminUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2Assessor
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2DeliusUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2LicenceCaseAdminUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextEmptyCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockNotFoundInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.math.sign

class Cas2v2ApplicationTest : Cas2v2IntegrationTestBase() {

  @SpykBean
  lateinit var realApplicationRepository: ApplicationRepository

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

  val data = """
          {
             "thingId": 123
          }
          """

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realApplicationRepository)
  }

  @Nested
  inner class ControlsOnExternalUsers {
    @ParameterizedTest
    @ValueSource(strings = ["ROLE_CAS2_ASSESSOR", "ROLE_CAS2_MI"])
    fun `creating a cas2v2 application is forbidden to external users based on role`(role: String) {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf(role),
      )

      webTestClient.post()
        .uri("/cas2v2/applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @ParameterizedTest
    @ValueSource(strings = ["ROLE_CAS2_ASSESSOR", "ROLE_CAS2_MI"])
    fun `updating a cas2v2 application is forbidden to external users based on role`(role: String) {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf(role),
      )

      webTestClient.put()
        .uri("/cas2v2/applications/66911cf0-75b1-4361-84bd-501b176fd4fd")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `viewing list of cas2v2 applications is forbidden to external users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.get()
        .uri("/cas2v2/applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `viewing a cas2v2 application is forbidden to external users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.get()
        .uri("/cas2v2/applications/66911cf0-75b1-4361-84bd-501b176fd4")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  inner class MissingJwt {
    @Test
    fun `Get all cas2v2 applications without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2v2/applications")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get single cas2v2 application without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2v2/applications/9b785e59-b85c-4be0-b271-d9ac287684b6")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Create new cas2v2 application without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas2v2/applications")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class GetToIndex {

    @Test
    fun `return unexpired cas2v2 applications as a delius user when applications GET is requested`() {
      givenACas2v2DeliusUser { deliusUserEntity, jwt ->
        returnsCas2V2UnexpiredApplications(deliusUserEntity, jwt)
      }
    }

    @Test
    fun `return unexpired cas2v2 applications as a nomis user when applications GET is requested`() {
      givenACas2v2PomUser { pomUserEntity, jwt ->
        returnsCas2V2UnexpiredApplications(pomUserEntity, jwt)
      }
    }

    private fun returnsCas2V2UnexpiredApplications(userEntity: Cas2v2UserEntity, jwt: String) {
      val unexpiredSubset = setOf(
        Pair("More information requested", UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905")),
        Pair("Awaiting decision", UUID.fromString("ba4d8432-250b-4ab9-81ec-7eb4b16e5dd1")),
        Pair("On waiting list", UUID.fromString("a919097d-b324-471c-9834-756f255e87ea")),
        Pair("Place offered", UUID.fromString("176bbda0-0766-4d77-8d56-18ed8f9a4ef2")),
        Pair("Offer accepted", UUID.fromString("fe254d88-ce1d-4cd8-8bd6-88de88f39019")),
        Pair("Could not be placed", UUID.fromString("758eee61-2a6d-46b9-8bdd-869536d77f1b")),
        Pair("Incomplete", UUID.fromString("4ad9bbfa-e5b0-456f-b746-146f7fd511dd")),
        Pair("Offer declined or withdrawn", UUID.fromString("9a381bc6-22d3-41d6-804d-4e49f428c1de")),
      )

      val expiredSubset = setOf(
        Pair("Referral withdrawn", UUID.fromString("004e2419-9614-4c1e-a207-a8418009f23d")),
        Pair("Referral cancelled", UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9")),
        Pair("Awaiting arrival", UUID.fromString("89458555-3219-44a2-9584-c4f715d6b565")),
      )

      val applicationSchema = cas2v2ApplicationJsonSchemaEntityFactory.produceAndPersist {
        withAddedAt(OffsetDateTime.now())
        withId(UUID.randomUUID())
      }

      fun createApplication(
        userEntity: Cas2v2UserEntity,
        offenderDetails: OffenderDetailSummary,
      ): Cas2v2ApplicationEntity = cas2v2ApplicationEntityFactory.produceAndPersist {
        withApplicationSchema(applicationSchema)
        withCreatedByUser(userEntity)
        withCrn(offenderDetails.otherIds.crn)
        withCreatedAt(OffsetDateTime.now().minusDays(28))
        withConditionalReleaseDate(LocalDate.now().plusDays(1))
      }

      fun createStatusUpdate(
        status: Pair<String, UUID>,
        application: Cas2v2ApplicationEntity,
      ): Cas2v2StatusUpdateEntity = cas2v2StatusUpdateEntityFactory.produceAndPersist {
        withLabel(status.first)
        withStatusId(status.second)
        withApplication(application)
        withAssessor(cas2v2UserEntityFactory.produceAndPersist { withUserType(Cas2v2UserType.EXTERNAL) })
      }

      fun unexpiredDateTime() = OffsetDateTime.now().randomDateTimeBefore(32)
      fun expiredDateTime() = unexpiredDateTime().minusDays(33)

      val unexpiredApplicationIds = mutableSetOf<UUID>()
      val expiredApplicationIds = mutableSetOf<UUID>()

      givenAnOffender { offenderDetails, _ ->
        repeat(2) {
          unexpiredApplicationIds.add(createApplication(userEntity, offenderDetails).id)
        }

        unexpiredSubset.union(expiredSubset).forEach {
          val application = createApplication(userEntity, offenderDetails)
          val statusUpdate = createStatusUpdate(it, application)
          statusUpdate.createdAt = unexpiredDateTime()
          cas2v2StatusUpdateRepository.save(statusUpdate)
          unexpiredApplicationIds.add(application.id)
        }

        unexpiredSubset.forEach {
          val application = createApplication(userEntity, offenderDetails)
          val statusUpdate = createStatusUpdate(it, application)
          statusUpdate.createdAt = unexpiredDateTime()
          cas2v2StatusUpdateRepository.save(statusUpdate)
          unexpiredApplicationIds.add(application.id)
        }

        expiredSubset.forEach {
          val application = createApplication(userEntity, offenderDetails)
          val statusUpdate = createStatusUpdate(it, application)
          statusUpdate.createdAt = expiredDateTime()
          cas2v2StatusUpdateRepository.save(statusUpdate)
          expiredApplicationIds.add(application.id)
        }

        val rawResponseBody = webTestClient.get()
          .uri("/cas2v2/applications")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.cas2v2.value)
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<String>()
          .responseBody
          .blockFirst()

        val responseBody =
          objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2v2ApplicationSummary>>() {})

        val returnedApplicationIds = responseBody.map { it.id }.toSet()

        Assertions.assertThat(returnedApplicationIds == unexpiredApplicationIds).isTrue()
      }
    }

    @Test
    fun `Get all cas2v2 applications a POM user, POM user returns 200 with correct body`() {
      givenACas2v2PomUser { pomUserEntity, jwt ->
        givenACas2v2PomUser { otherPomUser, _ ->
          getAllCas2v2applications(pomUserEntity, otherPomUser, jwt)
        }
      }
    }

    @Test
    fun `Get all cas2v2 applications a POM user, Delius user returns 200 with correct body`() {
      givenACas2v2PomUser { pomUserEntity, jwt ->
        givenACas2v2DeliusUser { otherDeliusUser, _ ->
          getAllCas2v2applications(pomUserEntity, otherDeliusUser, jwt)
        }
      }
    }

    @Test
    fun `Get all cas2v2 applications a Delius user, POM user returns 200 with correct body`() {
      givenACas2v2DeliusUser { deliusUser, jwt ->
        givenACas2v2PomUser { otherPomUser, _ ->
          getAllCas2v2applications(deliusUser, otherPomUser, jwt)
        }
      }
    }

    @Test
    fun `Get all cas2v2 applications a Delius user, Delius user returns 200 with correct body`() {
      givenACas2v2DeliusUser { deliusUser, jwt ->
        givenACas2v2DeliusUser { otherDeliusUser, _ ->
          getAllCas2v2applications(deliusUser, otherDeliusUser, jwt)
        }
      }
    }

    private fun getAllCas2v2applications(userEntity: Cas2v2UserEntity, otherUser: Cas2v2UserEntity, jwt: String) {
      givenAnOffender { offenderDetails, _ ->
        cas2v2ApplicationJsonSchemaRepository.deleteAll()

        val applicationSchema = cas2v2ApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.now())
          withId(UUID.randomUUID())
        }

        // abandoned application
        val abandonedApplicationEntity = cas2v2ApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(applicationSchema)
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withData("{}")
          withCreatedAt(OffsetDateTime.parse("2024-01-03T16:10:00+01:00"))
          withAbandonedAt(OffsetDateTime.now())
        }

        // unsubmitted application
        val firstApplicationEntity = cas2v2ApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(applicationSchema)
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withData("{}")
          withCreatedAt(OffsetDateTime.parse("2024-01-03T16:10:00+01:00"))
          withHdcEligibilityDate(LocalDate.now().plusMonths(3))
        }

        // submitted application, CRD >= today so should be returned
        val secondApplicationEntity = cas2v2ApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(applicationSchema)
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withData("{}")
          withCreatedAt(OffsetDateTime.parse("2024-02-29T09:00:00+01:00"))
          withSubmittedAt(OffsetDateTime.now())
          withConditionalReleaseDate(LocalDate.now())
        }

        // submitted application, CRD = yesterday, so should not be returned
        val thirdApplicationEntity = cas2v2ApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(applicationSchema)
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withData("{}")
          withCreatedAt(OffsetDateTime.parse("2024-02-29T09:00:00+01:00"))
          withSubmittedAt(OffsetDateTime.now())
          withConditionalReleaseDate(LocalDate.now().minusDays(1))
        }

        val statusUpdate = cas2v2StatusUpdateEntityFactory.produceAndPersist {
          withLabel("More information requested")
          withApplication(secondApplicationEntity)
          withAssessor(cas2v2UserEntityFactory.produceAndPersist { withUserType(Cas2v2UserType.EXTERNAL) })
        }

        val otherCas2v2ApplicationEntity = cas2v2ApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(applicationSchema)
          withCreatedByUser(otherUser)
          withCrn(offenderDetails.otherIds.crn)
          withData("{}")
        }

        val rawResponseBody = webTestClient.get()
          .uri("/cas2v2/applications")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.cas2v2.value)
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<String>()
          .responseBody
          .blockFirst()

        val responseBody =
          objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2v2ApplicationSummary>>() {})

        // check transformers were able to return all fields
        Assertions.assertThat(responseBody).anyMatch {
          firstApplicationEntity.id == it.id &&
            firstApplicationEntity.crn == it.crn &&
            firstApplicationEntity.nomsNumber == it.nomsNumber &&
            "${offenderDetails.firstName} ${offenderDetails.surname}" == it.personName &&
            firstApplicationEntity.createdAt.toInstant() == it.createdAt &&
            firstApplicationEntity.createdByUser.id == it.createdByUserId &&
            firstApplicationEntity.submittedAt?.toInstant() == it.submittedAt &&
            firstApplicationEntity.hdcEligibilityDate == it.hdcEligibilityDate &&
            firstApplicationEntity.createdByUser.name == it.createdByUserName
        }

        Assertions.assertThat(responseBody).noneMatch {
          thirdApplicationEntity.id == it.id
        }

        Assertions.assertThat(responseBody).noneMatch {
          otherCas2v2ApplicationEntity.id == it.id
        }

        Assertions.assertThat(responseBody).noneMatch {
          abandonedApplicationEntity.id == it.id
        }

        Assertions.assertThat(responseBody[0].createdAt)
          .isEqualTo(secondApplicationEntity.createdAt.toInstant())

        Assertions.assertThat(responseBody[0].latestStatusUpdate!!.label)
          .isEqualTo(statusUpdate.label)

        Assertions.assertThat(responseBody[1].createdAt)
          .isEqualTo(firstApplicationEntity.createdAt.toInstant())
      }
    }

    @Test
    fun `Get all applications returns 200 with applicationOrigin`() {
      givenACas2v2PomUser { userEntity, jwt ->
        givenACas2v2PomUser { _, _ ->
          givenAnOffender { offenderDetails, _ ->
            cas2v2ApplicationJsonSchemaRepository.deleteAll()

            val applicationSchema = cas2v2ApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
            }

            val courtBailApplicationEntity = cas2v2ApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(userEntity)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
              withCreatedAt(OffsetDateTime.parse("2024-01-03T16:10:00+01:00"))
              withApplicationOrigin(ApplicationOrigin.courtBail)
            }

            val prisonBailApplicationEntity = cas2v2ApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(userEntity)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
              withCreatedAt(OffsetDateTime.parse("2024-01-03T16:10:00+01:00"))
              withApplicationOrigin(ApplicationOrigin.prisonBail)
            }

            val hdcApplicationEntity = cas2v2ApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(userEntity)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
              withCreatedAt(OffsetDateTime.parse("2024-02-29T09:00:00+01:00"))
              withSubmittedAt(OffsetDateTime.now())
              withApplicationOrigin(ApplicationOrigin.homeDetentionCurfew)
            }

            val rawResponseBody = webTestClient.get()
              .uri("/cas2v2/applications")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody =
              objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2v2ApplicationSummary>>() {})

            Assertions.assertThat(responseBody.count()).isEqualTo(3)
            Assertions.assertThat(responseBody).anyMatch {
              courtBailApplicationEntity.applicationOrigin == it.applicationOrigin
            }

            Assertions.assertThat(responseBody).anyMatch {
              hdcApplicationEntity.applicationOrigin == it.applicationOrigin
            }

            Assertions.assertThat(responseBody).anyMatch {
              prisonBailApplicationEntity.applicationOrigin == it.applicationOrigin
            }
          }
        }
      }
    }

    @Test
    fun `Get all cas2v2 applications as POM user with pagination returns 200 with correct body and header`() {
      givenACas2v2PomUser { pomUserEntity, jwt ->
        getAllApplications(pomUserEntity, jwt)
      }
    }

    @Test
    fun `Get all cas2v2 applications as Delius with pagination returns 200 with correct body and header`() {
      givenACas2v2PomUser { deliusUserEntity, jwt ->
        getAllApplications(deliusUserEntity, jwt)
      }
    }

    private fun getAllApplications(userEntity: Cas2v2UserEntity, jwt: String) {
      givenAnOffender { offenderDetails, _ ->
        cas2v2ApplicationJsonSchemaRepository.deleteAll()

        val applicationSchema = cas2v2ApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.now())
          withId(UUID.randomUUID())
        }

        repeat(12) {
          cas2v2ApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCreatedByUser(userEntity)
            withCrn(offenderDetails.otherIds.crn)
            withData("{}")
            withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
          }
        }

        val rawResponseBodyPage1 = webTestClient.get()
          .uri("/cas2v2/applications?page=1")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.cas2v2.value)
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

        val responseBodyPage1 =
          objectMapper.readValue(rawResponseBodyPage1, object : TypeReference<List<Cas2v2ApplicationSummary>>() {})

        Assertions.assertThat(responseBodyPage1).size().isEqualTo(10)

        Assertions.assertThat(isOrderedByCreatedAtDescending(responseBodyPage1)).isTrue()

        val rawResponseBodyPage2 = webTestClient.get()
          .uri("/cas2v2/applications?page=2")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.cas2.value)
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

        val responseBodyPage2 =
          objectMapper.readValue(rawResponseBodyPage2, object : TypeReference<List<Cas2v2ApplicationSummary>>() {})

        Assertions.assertThat(responseBodyPage2).size().isEqualTo(2)
      }
    }

    @Test
    fun `When a person is not found in cas2v2, returns 200 with placeholder text`() {
      givenACas2v2PomUser { userEntity, jwt ->
        val crn = "X1234"

        apDeliusContextEmptyCaseSummaryToBulkResponse(crn)

        produceAndPersistBasicApplication(crn, userEntity)

        webTestClient
          .get()
          .uri("/cas2v2/applications")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$[0].personName")
          .isEqualTo("Person Not Found")
      }
    }

    @Test
    fun `Court bail users do not need a prison code`() {
      givenACas2v2PomUser { userEntity, _ ->
        produceAndPersistBasicApplication("CRN", userEntity)

        val jwt = jwtAuthHelper.createValidNomisAuthorisationCodeJwt(
          "MY USERNAME",
          listOf("ROLE_COURT_BAIL"),
        )
        webTestClient
          .get()
          .uri("/cas2v2/applications")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
      }
    }

    @Test
    fun `Prison bail users do not need a prison code`() {
      givenACas2v2PomUser { userEntity, _ ->
        produceAndPersistBasicApplication("CRN", userEntity)

        val jwt = jwtAuthHelper.createValidNomisAuthorisationCodeJwt(
          "MY USERNAME",
          listOf("ROLE_PRISON_BAIL"),
        )
        webTestClient
          .get()
          .uri("/cas2v2/applications")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
      }
    }

    /**
     * Returns true if the list of application summaries is sorted by descending created_at
     * or false if not.
     *
     * Works by calculating the difference in seconds between two dates and using the sign
     * of this difference.  If two dates are descending then the difference will be positive.
     * If two dates are ascending the difference will be negative (which is set to 0).
     *
     * For a list of dates, the cumulative multiple of these signs will be 1 if all
     * dates in the range are descending (= 1 x 1 x 1 etc.).
     *
     * If any dates are ascending the multiple will be 0 ( = 1 x 1 x 0 etc.).
     *
     * If all dates are ascending the multiple will also be 0 ( = 1 x 0 x 0 etc.).
     */
    private fun isOrderedByCreatedAtDescending(responseBody: List<Cas2v2ApplicationSummary>): Boolean {
      var allDescending = 1
      for (i in 1..<responseBody.size) {
        val isDescending = (responseBody[i - 1].createdAt.epochSecond - responseBody[i].createdAt.epochSecond).sign
        allDescending *= if (isDescending > 0) 1 else 0
      }
      return allDescending == 1
    }

    @Nested
    inner class WithPrisonCode {
      @Test
      fun `Get all applications using prisonCode returns 200 with correct body`() {
        givenACas2v2Assessor { assessor, _ ->
          givenACas2v2PomUser { userAPrisonA, jwt ->
            givenAnOffender { offenderDetails, _ ->
              cas2v2ApplicationJsonSchemaRepository.deleteAll()

              val applicationSchema = cas2v2ApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
              }

              val userAPrisonAApplicationIds = mutableListOf<UUID>()

              repeat(5) {
                userAPrisonAApplicationIds.add(
                  cas2v2ApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userAPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                    withReferringPrisonCode(userAPrisonA.activeNomisCaseloadId!!)
                  }.id,
                )
              }

              val userBPrisonA = cas2v2UserEntityFactory.produceAndPersist {
                withActiveNomisCaseloadId(userAPrisonA.activeNomisCaseloadId!!)
              }

              val userBPrisonAApplicationIds = mutableListOf<UUID>()

              // submitted applications with conditional release dates in the future
              repeat(6) {
                userBPrisonAApplicationIds.add(
                  cas2v2ApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userBPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong()))
                    withReferringPrisonCode(userAPrisonA.activeNomisCaseloadId!!)
                    withSubmittedAt(OffsetDateTime.now().minusDays(it.toLong()))
                    withConditionalReleaseDate(LocalDate.now().randomDateAfter(14))
                  }.id,
                )
              }

              // submitted applications with conditional release dates today
              repeat(2) {
                userBPrisonAApplicationIds.add(
                  cas2v2ApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userBPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong() + 6))
                    withReferringPrisonCode(userAPrisonA.activeNomisCaseloadId!!)
                    withSubmittedAt(OffsetDateTime.now().minusDays(it.toLong() + 6))
                    withConditionalReleaseDate(LocalDate.now())
                  }.id,
                )
              }

              // submitted application with a conditional release date before today
              val excludedApplicationId = cas2v2ApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(userBPrisonA)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now().minusDays(14))
                withReferringPrisonCode(userAPrisonA.activeNomisCaseloadId!!)
                withSubmittedAt(OffsetDateTime.now())
                withConditionalReleaseDate(LocalDate.now().randomDateBefore(14))
              }.id

              addStatusUpdates(userBPrisonAApplicationIds.first(), assessor)

              val userCPrisonB = cas2v2UserEntityFactory.produceAndPersist {
                withActiveNomisCaseloadId("another prison")
              }

              val otherPrisonApplication = cas2v2ApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(userCPrisonB)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                withReferringPrisonCode(userCPrisonB.activeNomisCaseloadId!!)
              }

              val rawResponseBody = webTestClient.get()
                .uri(
                  "/cas2v2/applications?prisonCode=${userAPrisonA.activeNomisCaseloadId}",
                )
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.cas2v2.value)
                .exchange()
                .expectStatus()
                .isOk
                .returnResult<String>()
                .responseBody
                .blockFirst()

              val responseBody =
                objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2v2ApplicationSummary>>() {})

              Assertions.assertThat(responseBody).noneMatch {
                excludedApplicationId == it.id
              }

              val returnedApplicationIds = responseBody.map { it.id }.toSet()

              Assertions.assertThat(returnedApplicationIds).isEqualTo(
                userAPrisonAApplicationIds.toSet().union(userBPrisonAApplicationIds.toSet()),
              )

              Assertions.assertThat(responseBody).noneMatch {
                otherPrisonApplication.id == it.id
              }

              Assertions.assertThat(responseBody[0].latestStatusUpdate?.label).isEqualTo("Awaiting decision")
              Assertions.assertThat(responseBody[0].latestStatusUpdate?.statusId)
                .isEqualTo(UUID.fromString("c74c3e54-52d8-4aa2-86f6-05190985efee"))
            }
          }
        }
      }

      @Test
      fun `Get all submitted applications using prisonCode returns 200 with correct body`() {
        givenACas2v2Assessor { assessor, _ ->
          givenACas2v2PomUser { userAPrisonA, jwt ->
            givenAnOffender { offenderDetails, _ ->
              cas2v2ApplicationJsonSchemaRepository.deleteAll()

              val applicationSchema = cas2v2ApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
              }

              val userAPrisonAApplicationIds = mutableListOf<UUID>()

              repeat(5) {
                userAPrisonAApplicationIds.add(
                  cas2v2ApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userAPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong()))
                    withReferringPrisonCode(userAPrisonA.activeNomisCaseloadId!!)
                  }.id,
                )
              }

              val userBPrisonA = cas2v2UserEntityFactory.produceAndPersist {
                withActiveNomisCaseloadId(userAPrisonA.activeNomisCaseloadId!!)
              }

              val userBPrisonAApplicationIds = mutableListOf<UUID>()

              repeat(6) {
                userBPrisonAApplicationIds.add(
                  cas2v2ApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userBPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong()))
                    withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                    withReferringPrisonCode(userAPrisonA.activeNomisCaseloadId!!)
                    withConditionalReleaseDate(LocalDate.now().randomDateAfter(14))
                  }.id,
                )
              }

              // submitted applications with conditional release dates today
              repeat(2) {
                userBPrisonAApplicationIds.add(
                  cas2v2ApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userBPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong() + 6))
                    withReferringPrisonCode(userAPrisonA.activeNomisCaseloadId!!)
                    withSubmittedAt(OffsetDateTime.now().minusDays(it.toLong() + 6))
                    withConditionalReleaseDate(LocalDate.now())
                  }.id,
                )
              }

              // submitted application with a conditional release date before today
              val excludedApplicationId =
                cas2v2ApplicationEntityFactory.produceAndPersist {
                  withApplicationSchema(applicationSchema)
                  withCreatedByUser(userBPrisonA)
                  withCrn(offenderDetails.otherIds.crn)
                  withData("{}")
                  withCreatedAt(OffsetDateTime.now().minusDays(14))
                  withReferringPrisonCode(userAPrisonA.activeNomisCaseloadId!!)
                  withSubmittedAt(OffsetDateTime.now())
                  withConditionalReleaseDate(LocalDate.now().randomDateBefore(14))
                }.id

              addStatusUpdates(userBPrisonAApplicationIds.first(), assessor)

              val rawResponseBody = webTestClient.get()
                .uri(
                  "/cas2v2/applications?isSubmitted=true&prisonCode=${userAPrisonA.activeNomisCaseloadId}",
                )
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.cas2v2.value)
                .exchange()
                .expectStatus()
                .isOk
                .returnResult<String>()
                .responseBody
                .blockFirst()

              val responseBody =
                objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2v2ApplicationSummary>>() {})

              Assertions.assertThat(responseBody).noneMatch {
                excludedApplicationId == it.id
              }

              val returnedApplicationIds = responseBody.map { it.id }.toSet()

              Assertions.assertThat(returnedApplicationIds).isEqualTo(userBPrisonAApplicationIds.toSet())
              Assertions.assertThat(responseBody[0].latestStatusUpdate?.label).isEqualTo("Awaiting decision")
              Assertions.assertThat(responseBody[0].latestStatusUpdate?.statusId)
                .isEqualTo(UUID.fromString("c74c3e54-52d8-4aa2-86f6-05190985efee"))
            }
          }
        }
      }

      @Test
      fun `Get all unsubmitted applications using prisonCode returns 200 with correct body`() {
        givenACas2v2PomUser { userAPrisonA, jwt ->
          givenAnOffender { offenderDetails, _ ->
            cas2v2ApplicationJsonSchemaRepository.deleteAll()

            val applicationSchema = cas2v2ApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
            }

            val userAPrisonAApplicationIds = mutableListOf<UUID>()

            repeat(5) {
              userAPrisonAApplicationIds.add(
                cas2v2ApplicationEntityFactory.produceAndPersist {
                  withApplicationSchema(applicationSchema)
                  withCreatedByUser(userAPrisonA)
                  withCrn(offenderDetails.otherIds.crn)
                  withData("{}")
                  withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                  withReferringPrisonCode(userAPrisonA.activeNomisCaseloadId!!)
                }.id,
              )
            }

            val userBPrisonA = cas2v2UserEntityFactory.produceAndPersist {
              withActiveNomisCaseloadId(userAPrisonA.activeNomisCaseloadId!!)
            }

            val userBPrisonAApplicationIds = mutableListOf<UUID>()

            repeat(6) {
              userBPrisonAApplicationIds.add(
                cas2v2ApplicationEntityFactory.produceAndPersist {
                  withApplicationSchema(applicationSchema)
                  withCreatedByUser(userBPrisonA)
                  withCrn(offenderDetails.otherIds.crn)
                  withData("{}")
                  withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                  withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                  withReferringPrisonCode(userAPrisonA.activeNomisCaseloadId!!)
                }.id,
              )
            }

            val rawResponseBody = webTestClient.get()
              .uri("/cas2v2/applications?isSubmitted=false&prisonCode=${userAPrisonA.activeNomisCaseloadId}")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2v2.value)
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody =
              objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2v2ApplicationSummary>>() {})

            val returnedApplicationIds = responseBody.map { it.id }.toSet()

            Assertions.assertThat(returnedApplicationIds).isEqualTo(userAPrisonAApplicationIds.toSet())
          }
        }
      }

      @Test
      fun `Get applications using another prisonCode returns Forbidden 403`() {
        givenACas2v2PomUser { userAPrisonA, jwt ->
          webTestClient.get()
            .uri("/cas2v2/applications?prisonCode=${userAPrisonA.activeNomisCaseloadId!!.reversed()}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2v2.value)
            .exchange()
            .expectStatus()
            .isForbidden()
        }
      }
    }

    @Nested
    inner class AsLicenceCaseAdminUser {
      @Test
      fun `Get all submitted cas2v2 applications using prisonCode returns 200 with correct body`() {
        givenACas2v2Assessor { _, _ ->
          givenACas2LicenceCaseAdminUser { caseAdminPrisonA, jwt ->
            givenAnOffender { offenderDetails, _ ->
              cas2v2ApplicationJsonSchemaRepository.deleteAll()

              val applicationSchema = cas2v2ApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
              }

              val pomUserPrisonA = cas2v2UserEntityFactory.produceAndPersist {
                withActiveNomisCaseloadId(caseAdminPrisonA.activeCaseloadId!!)
              }

              val userBPrisonAApplicationIds = mutableListOf<UUID>()

              // submitted applications with conditional release dates in the future
              repeat(6) {
                userBPrisonAApplicationIds.add(
                  cas2v2ApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(pomUserPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong()))
                    withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                    withReferringPrisonCode(caseAdminPrisonA.activeCaseloadId!!)
                    withConditionalReleaseDate(LocalDate.now().randomDateAfter(14))
                  }.id,
                )
              }

              // submitted applications with conditional release date of today
              repeat(2) {
                userBPrisonAApplicationIds.add(
                  cas2v2ApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(pomUserPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong() + 6))
                    withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                    withReferringPrisonCode(caseAdminPrisonA.activeCaseloadId!!)
                    withConditionalReleaseDate(LocalDate.now())
                  }.id,
                )
              }

              // submitted application with a conditional release date before today
              val excludedApplicationId = cas2v2ApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(pomUserPrisonA)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                withReferringPrisonCode(caseAdminPrisonA.activeCaseloadId!!)
                withConditionalReleaseDate(LocalDate.now().randomDateBefore(14))
              }.id

              val pomUserPrisonB = cas2v2UserEntityFactory.produceAndPersist {
                withActiveNomisCaseloadId("other_prison")
              }

              val otherPrisonApplication = cas2v2ApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(pomUserPrisonB)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now())
                withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                withReferringPrisonCode("other_prison")
              }

              val rawResponseBody = webTestClient.get()
                .uri(
                  "/cas2v2/applications?isSubmitted=true&prisonCode=${caseAdminPrisonA.activeCaseloadId}",
                )
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.cas2v2.value)
                .exchange()
                .expectStatus()
                .isOk
                .returnResult<String>()
                .responseBody
                .blockFirst()

              val responseBody =
                objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2v2ApplicationSummary>>() {})

              Assertions.assertThat(responseBody).noneMatch {
                excludedApplicationId == it.id
              }
              val returnedApplicationIds = responseBody.map { it.id }.toSet()

              Assertions.assertThat(returnedApplicationIds).isEqualTo(userBPrisonAApplicationIds.toSet())
              Assertions.assertThat(returnedApplicationIds).noneMatch {
                otherPrisonApplication.id == it
              }
            }
          }
        }
      }
    }
  }

  private fun addStatusUpdates(applicationId: UUID, assessor: Cas2v2UserEntity) {
    cas2v2StatusUpdateEntityFactory.produceAndPersist {
      withLabel("More information requested")
      withApplication(cas2v2ApplicationRepository.findById(applicationId).get())
      withAssessor(cas2v2UserEntityFactory.produceAndPersist { withUserType(Cas2v2UserType.EXTERNAL) })
    }
    // this is the one that should be returned as latestStatusUpdate
    cas2v2StatusUpdateEntityFactory.produceAndPersist {
      withStatusId(UUID.fromString("c74c3e54-52d8-4aa2-86f6-05190985efee"))
      withLabel("Awaiting decision")
      withApplication(cas2v2ApplicationRepository.findById(applicationId).get())
      withAssessor(assessor)
    }
  }

  @Nested
  inner class GetToIndexUsingIsSubmitted {

    private var jwtForUser: String? = null
    private val submittedIds = mutableSetOf<UUID>()
    private val unSubmittedIds = mutableSetOf<UUID>()
    private lateinit var excludedApplicationId: UUID

    @BeforeEach
    fun setup() {
      givenACas2v2Assessor { assessor, _ ->
        givenACas2v2PomUser { userEntity, jwt ->
          givenACas2v2DeliusUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              cas2v2ApplicationJsonSchemaRepository.deleteAll()

              val applicationSchema = cas2v2ApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
              }

              // create 3 x submitted applications for this user
              // with most recent first and conditional release dates in the future
              repeat(3) {
                submittedIds.add(
                  cas2v2ApplicationEntityFactory.produceAndPersist {
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong()))
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userEntity)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withSubmittedAt(OffsetDateTime.now().minusDays(it.toLong()))
                    withConditionalReleaseDate(LocalDate.now().randomDateAfter(14))
                  }.id,
                )
              }

              // create 2 x submitted applications for this user
              // with most recent first and conditional release dates of today
              repeat(2) {
                submittedIds.add(
                  cas2v2ApplicationEntityFactory.produceAndPersist {
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong() + 3))
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userEntity)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withSubmittedAt(OffsetDateTime.now().minusDays(it.toLong() + 3))
                    withConditionalReleaseDate(LocalDate.now())
                  }.id,
                )
              }

              // submitted application with a conditional release date before today
              excludedApplicationId = cas2v2ApplicationEntityFactory.produceAndPersist {
                withCreatedAt(OffsetDateTime.now().minusDays(14))
                withApplicationSchema(applicationSchema)
                withCreatedByUser(userEntity)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withSubmittedAt(OffsetDateTime.now())
                withConditionalReleaseDate(LocalDate.now().randomDateBefore(14))
              }.id

              addStatusUpdates(submittedIds.first(), assessor)

              // create 4 x un-submitted in-progress applications for this user
              repeat(4) {
                unSubmittedIds.add(
                  cas2v2ApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userEntity)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                  }.id,
                )
              }

              // create a submitted application by another user which should not be in results
              cas2v2ApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(otherUser)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withSubmittedAt(OffsetDateTime.now())
              }

              // create an unsubmitted application by another user which should not be in results
              cas2v2ApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(otherUser)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
              }

              jwtForUser = jwt
            }
          }
        }
      }
    }

    @Test
    fun `returns all cas2v2 applications for user when isSubmitted is null`() {
      val rawResponseBody = webTestClient.get()
        .uri("/cas2v2/applications")
        .header("Authorization", "Bearer $jwtForUser")
        .header("X-Service-Name", ServiceName.cas2v2.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult<String>()
        .responseBody
        .blockFirst()

      val responseBody =
        objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2v2ApplicationSummary>>() {})

      Assertions.assertThat(responseBody).noneMatch {
        excludedApplicationId == it.id
      }

      val uuids = responseBody.map { it.id }.toSet()
      Assertions.assertThat(uuids).isEqualTo(submittedIds.union(unSubmittedIds))
    }

    @Test
    fun `returns submitted cas2v2 applications for user when isSubmitted is true`() {
      val rawResponseBody = webTestClient.get()
        .uri("/cas2v2/applications?isSubmitted=true")
        .header("Authorization", "Bearer $jwtForUser")
        .header("X-Service-Name", ServiceName.cas2.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult<String>()
        .responseBody
        .blockFirst()

      val responseBody =
        objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2v2ApplicationSummary>>() {})

      Assertions.assertThat(responseBody).noneMatch {
        excludedApplicationId == it.id
      }

      val uuids = responseBody.map { it.id }.toSet()
      Assertions.assertThat(uuids).isEqualTo(submittedIds)
      Assertions.assertThat(responseBody[0].latestStatusUpdate?.label).isEqualTo("Awaiting decision")
      Assertions.assertThat(responseBody[0].latestStatusUpdate?.statusId)
        .isEqualTo(UUID.fromString("c74c3e54-52d8-4aa2-86f6-05190985efee"))
    }

    @Test
    fun `returns submitted cas2v2 applications for user when isSubmitted is true and page specified`() {
      val rawResponseBody = webTestClient.get()
        .uri("/cas2v2/applications?isSubmitted=true&page=1")
        .header("Authorization", "Bearer $jwtForUser")
        .header("X-Service-Name", ServiceName.cas2.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult<String>()
        .responseBody
        .blockFirst()

      val responseBody =
        objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2v2ApplicationSummary>>() {})

      val uuids = responseBody.map { it.id }.toSet()
      Assertions.assertThat(uuids).isEqualTo(submittedIds)
      Assertions.assertThat(responseBody[0].latestStatusUpdate?.label).isEqualTo("Awaiting decision")
      Assertions.assertThat(responseBody[0].latestStatusUpdate?.statusId)
        .isEqualTo(UUID.fromString("c74c3e54-52d8-4aa2-86f6-05190985efee"))
    }

    @Test
    fun `returns unsubmitted cas2v2 applications for user when isSubmitted is false`() {
      val rawResponseBody = webTestClient.get()
        .uri("/cas2v2/applications?isSubmitted=false")
        .header("Authorization", "Bearer $jwtForUser")
        .header("X-Service-Name", ServiceName.cas2.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult<String>()
        .responseBody
        .blockFirst()

      val responseBody =
        objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2v2ApplicationSummary>>() {})

      val uuids = responseBody.map { it.id }.toSet()
      Assertions.assertThat(uuids).isEqualTo(unSubmittedIds)
    }
  }

  @Nested
  inner class GetToShow {

    @Nested
    inner class WhenCreatedBySameUser {
      // When the application requested was created by the logged-in user
      @Test
      fun `Get single in progress cas2v2 application returns 200 with correct body`() {
        givenACas2v2PomUser { userEntity, jwt ->
          givenAnOffender { offenderDetails, _ ->
            cas2v2ApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = cas2v2ApplicationJsonSchemaEntityFactory
              .produceAndPersist {
                withAddedAt(OffsetDateTime.parse("2025-01-17T12:45:00+01:00"))
                withSchema(
                  schema,
                )
              }

            val applicationEntity = cas2v2ApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(newestJsonSchema)
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
              withData(
                data,
              )
            }

            val rawResponseBody = webTestClient.get()
              .uri("/cas2v2/applications/${applicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(
              rawResponseBody,
              Cas2v2Application::class.java,
            )

            Assertions.assertThat(responseBody).matches {
              applicationEntity.id == it.id &&
                applicationEntity.crn == it.person.crn &&
                applicationEntity.createdAt.toInstant() == it.createdAt &&
                applicationEntity.createdByUser.id == it.createdBy.id &&
                applicationEntity.submittedAt?.toInstant() == it.submittedAt &&
                serializableToJsonNode(applicationEntity.data) == serializableToJsonNode(it.data) &&
                newestJsonSchema.id == it.schemaVersion &&
                !it.outdatedSchema
            }
          }
        }
      }

      @Test
      fun `Get single cas2v2 application returns successfully when the offender cannot be fetched from the prisons API`() {
        givenACas2v2PomUser { userEntity, jwt ->
          val crn = "X5678"

          givenAnOffender(
            offenderDetailsConfigBlock = {
              withCrn(crn)
              withNomsNumber("ABC123")
            },
          ) { offenderDetails, _ ->
            val application = produceAndPersistBasicApplication(crn, userEntity)

            prisonAPIMockNotFoundInmateDetailsCall(offenderDetails.otherIds.nomsNumber!!)
            loadPreemptiveCacheForInmateDetails(offenderDetails.otherIds.nomsNumber!!)

            val rawResponseBody = webTestClient.get()
              .uri("/cas2v2/applications/${application.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(
              rawResponseBody,
              Cas2v2Application::class.java,
            )

            Assertions.assertThat(responseBody.person is FullPerson).isTrue

            Assertions.assertThat(responseBody).matches {
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
      fun `Get single submitted cas2v2 application returns 200 with timeline events`() {
        givenACas2v2PomUser { userEntity, jwt ->
          givenAnOffender { offenderDetails, _ ->
            cas2v2ApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = cas2v2ApplicationJsonSchemaEntityFactory
              .produceAndPersist {
                withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                withSchema(
                  schema,
                )
              }

            val applicationEntity = cas2v2ApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(newestJsonSchema)
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
              withSubmittedAt(OffsetDateTime.now().minusDays(1))
            }

            cas2v2AssessmentEntityFactory.produceAndPersist {
              withApplication(applicationEntity)
            }

            val rawResponseBody = webTestClient.get()
              .uri("/cas2v2/applications/${applicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(
              rawResponseBody,
              Cas2v2Application::class.java,
            )

            Assertions.assertThat(responseBody.assessment!!.statusUpdates).isEqualTo(emptyList<Cas2v2StatusUpdate>())

            Assertions.assertThat(responseBody.timelineEvents!!.map { event -> event.label })
              .isEqualTo(listOf("Application submitted"))
          }
        }
      }
    }

    @Nested
    inner class WhenCreatedByDifferentUser {

      @Nested
      inner class WhenDifferentPrison {
        @Test
        fun `Get single submitted cas2v2 application is forbidden`() {
          givenACas2v2PomUser { _, jwt ->
            givenAnOffender { offenderDetails, _ ->
              cas2v2ApplicationJsonSchemaRepository.deleteAll()

              val otherUser = cas2v2UserEntityFactory.produceAndPersist {
                withActiveNomisCaseloadId("other_caseload")
              }

              val newestJsonSchema = cas2v2ApplicationJsonSchemaEntityFactory
                .produceAndPersist {
                  withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                  withSchema(
                    schema,
                  )
                }

              val applicationEntity = cas2v2ApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(newestJsonSchema)
                withCrn(offenderDetails.otherIds.crn)
                withSubmittedAt(OffsetDateTime.now())
                withCreatedByUser(otherUser)
                withData(
                  data,
                )
              }

              webTestClient.get()
                .uri("/cas2v2/applications/${applicationEntity.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }

      @Nested
      inner class WhenSamePrison {
        @Test
        fun `Get single submitted cas2v2 application returns 200 with timeline events`() {
          givenACas2v2PomUser { userEntity, jwt ->
            givenAnOffender { offenderDetails, _ ->
              cas2v2ApplicationJsonSchemaRepository.deleteAll()

              val newestJsonSchema = cas2v2ApplicationJsonSchemaEntityFactory
                .produceAndPersist {
                  withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                  withSchema(
                    schema,
                  )
                }

              val otherUser = cas2v2UserEntityFactory.produceAndPersist {
                withActiveNomisCaseloadId(userEntity.activeNomisCaseloadId!!)
              }

              val applicationEntity = cas2v2ApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(newestJsonSchema)
                withCrn(offenderDetails.otherIds.crn)
                withCreatedByUser(otherUser)
                withSubmittedAt(OffsetDateTime.now().minusDays(1))
                withReferringPrisonCode(userEntity.activeNomisCaseloadId!!)
              }

              cas2v2AssessmentEntityFactory.produceAndPersist {
                withApplication(applicationEntity)
              }

              val rawResponseBody = webTestClient.get()
                .uri("/cas2v2/applications/${applicationEntity.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .returnResult<String>()
                .responseBody
                .blockFirst()

              val responseBody = objectMapper.readValue(
                rawResponseBody,
                Cas2v2Application::class.java,
              )

              Assertions.assertThat(responseBody.assessment!!.statusUpdates).isEqualTo(emptyList<Cas2v2StatusUpdate>())

              Assertions.assertThat(responseBody.timelineEvents!!.map { event -> event.label })
                .isEqualTo(listOf("Application submitted"))
            }
          }
        }

        @Test
        fun `Get single unsubmitted cas2v2 application returns 403`() {
          givenACas2v2PomUser { userEntity, jwt ->
            givenAnOffender { offenderDetails, _ ->
              cas2v2ApplicationJsonSchemaRepository.deleteAll()

              val newestJsonSchema = cas2v2ApplicationJsonSchemaEntityFactory
                .produceAndPersist {
                  withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                  withSchema(
                    schema,
                  )
                }

              val otherUser = cas2v2UserEntityFactory.produceAndPersist {
                withActiveNomisCaseloadId(userEntity.activeNomisCaseloadId!!)
              }

              val applicationEntity = cas2v2ApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(newestJsonSchema)
                withCrn(offenderDetails.otherIds.crn)
                withCreatedByUser(otherUser)
                withReferringPrisonCode(userEntity.activeNomisCaseloadId!!)
              }

              webTestClient.get()
                .uri("/cas2v2/applications/${applicationEntity.id}")
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
  inner class PostToCreate {
    @Test
    fun `Create new application for cas2v2 as a POM user returns 201 with correct body and Location header`() {
      givenACas2v2PomUser { _, jwt ->
        createNewApplicationForCas2v2returns201(jwt)
      }
    }

    @Test
    fun `Create new cas2v2 application as a POM user returns 404 when a person cannot be found`() {
      givenACas2v2PomUser { _, jwt ->
        createNewApplicationForCas2v2Returns404WhenAPersonIsNotFound(jwt)
      }
    }

    @Test
    fun `Create new application for cas2v2 as a Delius user returns 201 with correct body and Location header`() {
      givenACas2v2DeliusUser { _, jwt ->
        createNewApplicationForCas2v2returns201(jwt)
      }
    }

    @Test
    fun `Create new cas2v2 application as a Delius user returns 404 when a person cannot be found`() {
      givenACas2v2PomUser { _, jwt ->
        createNewApplicationForCas2v2Returns404WhenAPersonIsNotFound(jwt)
      }
    }

    @Test
    fun `Create new application for cas2v2 as a LicenceCaseAdminUsers user returns 201 with correct body and Location header`() {
      givenACas2v2LicenceCaseAdminUser { _, jwt ->
        createNewApplicationForCas2v2returns201(jwt)
      }
    }

    @Test
    fun `Create new cas2v2 application as a LicenceCaseAdminUsers user returns 404 when a person cannot be found`() {
      givenACas2v2PomUser { _, jwt ->
        createNewApplicationForCas2v2Returns404WhenAPersonIsNotFound(jwt)
      }
    }

    private fun createNewApplicationForCas2v2returns201(jwt: String) {
      givenAnOffender { offenderDetails, _ ->
        val applicationSchema =
          cas2v2ApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
          }

        val result = webTestClient.post()
          .uri("/cas2v2/applications")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.cas2v2.value)
          .bodyValue(
            NewApplication(
              crn = offenderDetails.otherIds.crn,
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .returnResult(Cas2v2Application::class.java)

        Assertions.assertThat(result.responseHeaders["Location"]).anyMatch {
          it.matches(Regex("/cas2v2/applications/.+"))
        }

        Assertions.assertThat(result.responseBody.blockFirst()).matches {
          it.person.crn == offenderDetails.otherIds.crn &&
            it.schemaVersion == applicationSchema.id
        }
      }
    }

    private fun createNewApplicationForCas2v2Returns404WhenAPersonIsNotFound(jwt: String) {
      val crn = "X1234"

      cas2v2ApplicationJsonSchemaEntityFactory.produceAndPersist {
        withAddedAt(OffsetDateTime.now())
        withId(UUID.randomUUID())
      }

      webTestClient.post()
        .uri("/cas2v2/applications")
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

  @Nested
  inner class PutToUpdate {

    @Test
    fun `Update existing cas2v2 application given a POM user returns 200 with correct body`() {
      givenACas2v2PomUser { submittingUser, jwt ->
        updateExistingCas2v2ApplicationReturns200WithCorrectBody(submittingUser, jwt)
      }
    }

    @Test
    fun `Update existing cas2v2 application given a Delius user returns 200 with correct body`() {
      givenACas2v2DeliusUser { submittingUser, jwt ->
        updateExistingCas2v2ApplicationReturns200WithCorrectBody(submittingUser, jwt)
      }
    }

    @Test
    fun `Update existing cas2v2 application given a LicencedCaseAdmin user returns 200 with correct body`() {
      givenACas2v2LicenceCaseAdminUser { submittingUser, jwt ->
        updateExistingCas2v2ApplicationReturns200WithCorrectBody(submittingUser, jwt)
      }
    }

    private fun updateExistingCas2v2ApplicationReturns200WithCorrectBody(submittingUser: Cas2v2UserEntity, jwt: String) {
      givenAnOffender { offenderDetails, _ ->
        val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

        val applicationSchema =
          cas2v2ApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
            withSchema(
              schema,
            )
          }

        cas2v2ApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withId(applicationId)
          withApplicationSchema(applicationSchema)
          withCreatedByUser(submittingUser)
        }

        val resultBody = webTestClient.put()
          .uri("/cas2v2/applications/$applicationId")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateCas2v2Application(
              data = mapOf("thingId" to 123),
              type = UpdateApplicationType.CAS2V2,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .returnResult(String::class.java)
          .responseBody
          .blockFirst()

        val result = objectMapper.readValue(resultBody, Cas2v2Application::class.java)

        Assertions.assertThat(result.person.crn).isEqualTo(offenderDetails.otherIds.crn)
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
    userEntity: Cas2v2UserEntity,
  ): Cas2v2ApplicationEntity {
    val jsonSchema = cas2v2ApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
      withSchema(
        schema,
      )
    }

    val application = cas2v2ApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(jsonSchema)
      withCrn(crn)
      withCreatedByUser(userEntity)
      withData(
        data,
      )
    }

    return application
  }
}
