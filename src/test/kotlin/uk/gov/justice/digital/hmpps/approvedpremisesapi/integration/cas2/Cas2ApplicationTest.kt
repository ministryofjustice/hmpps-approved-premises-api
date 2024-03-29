package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

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
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockNotFoundOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.PrisonAPI_mockNotFoundInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.math.sign

class Cas2ApplicationTest : IntegrationTestBase() {
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
    @Test
    fun `creating an application is forbidden to external users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.post()
        .uri("/cas2/applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `updating an application is forbidden to external users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.put()
        .uri("/cas2/applications/66911cf0-75b1-4361-84bd-501b176fd4fd")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `viewing list of applications is forbidden to external users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.get()
        .uri("/cas2/applications/")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `viewing an application is forbidden to external users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.get()
        .uri("/cas2/applications/66911cf0-75b1-4361-84bd-501b176fd4")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  inner class MissingJwt {
    @Test
    fun `Get all applications without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2/applications")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get single application without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2/applications/9b785e59-b85c-4be0-b271-d9ac287684b6")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Create new application without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas2/applications")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class GetToIndex {

    @Test
    fun `Get all applications returns 200 with correct body`() {
      `Given a CAS2 User` { userEntity, jwt ->
        `Given a CAS2 User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            cas2ApplicationJsonSchemaRepository.deleteAll()

            val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
            }

            val firstApplicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(userEntity)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
              withCreatedAt(OffsetDateTime.parse("2024-01-03T16:10:00+01:00"))
            }

            val secondApplicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(userEntity)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
              withCreatedAt(OffsetDateTime.parse("2024-02-29T09:00:00+01:00"))
            }

            val otherCas2ApplicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(otherUser)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
            }

            val rawResponseBody = webTestClient.get()
              .uri("/cas2/applications")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody =
              objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2ApplicationSummary>>() {})

            Assertions.assertThat(responseBody).anyMatch {
              firstApplicationEntity.id == it.id &&
                firstApplicationEntity.crn == it.person.crn &&
                firstApplicationEntity.createdAt.toInstant() == it.createdAt &&
                firstApplicationEntity.createdByUser.id == it.createdByUserId &&
                firstApplicationEntity.submittedAt?.toInstant() == it.submittedAt
            }

            Assertions.assertThat(responseBody).noneMatch {
              otherCas2ApplicationEntity.id == it.id
            }

            Assertions.assertThat(responseBody[0].createdAt)
              .isEqualTo(secondApplicationEntity.createdAt.toInstant())

            Assertions.assertThat(responseBody[1].createdAt)
              .isEqualTo(firstApplicationEntity.createdAt.toInstant())
          }
        }
      }
    }

    @Test
    fun `Get all applications with pagination returns 200 with correct body and header`() {
      `Given a CAS2 User` { userEntity, jwt ->
        `Given a CAS2 User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            cas2ApplicationJsonSchemaRepository.deleteAll()

            val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
            }

            repeat(12) {
              cas2ApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(userEntity)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now().randomDateTimeBefore())
              }
            }

            val rawResponseBodyPage1 = webTestClient.get()
              .uri("/cas2/applications?page=1")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
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
              objectMapper.readValue(rawResponseBodyPage1, object : TypeReference<List<Cas2ApplicationSummary>>() {})

            Assertions.assertThat(responseBodyPage1).size().isEqualTo(10)

            Assertions.assertThat(isOrderedByCreatedAtDescending(responseBodyPage1)).isTrue()

            val rawResponseBodyPage2 = webTestClient.get()
              .uri("/cas2/applications?page=2")
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
              objectMapper.readValue(rawResponseBodyPage2, object : TypeReference<List<Cas2ApplicationSummary>>() {})

            Assertions.assertThat(responseBodyPage2).size().isEqualTo(2)
          }
        }
      }
    }

    @Test
    fun `Get list of applications returns 500 when a person cannot be found`() {
      `Given a CAS2 User`() { userEntity, jwt ->
        val crn = "X1234"

        produceAndPersistBasicApplication(crn, userEntity)
        CommunityAPI_mockNotFoundOffenderDetailsCall(crn)
        loadPreemptiveCacheForOffenderDetails(crn)

        webTestClient.get()
          .uri("/cas2/applications")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .is5xxServerError
          .expectBody()
          .jsonPath("$.detail").isEqualTo("Unable to get Person via crn: $crn")
      }
    }

    @Test
    fun `Get list of applications returns successfully when the person cannot be fetched from the prisons API`() {
      `Given a CAS2 User` { userEntity, jwt ->
        val crn = "X1234"

        `Given an Offender`(
          offenderDetailsConfigBlock = {
            withCrn(crn)
            withNomsNumber("ABC123")
          },
        ) { offenderDetails, _ ->
          val application = produceAndPersistBasicApplication(crn, userEntity)

          PrisonAPI_mockNotFoundInmateDetailsCall(offenderDetails.otherIds.nomsNumber!!)
          loadPreemptiveCacheForInmateDetails(offenderDetails.otherIds.nomsNumber!!)

          val rawResponseBody = webTestClient.get()
            .uri("/cas2/applications")
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
              object :
                TypeReference<List<Cas2ApplicationSummary>>() {},
            )

          Assertions.assertThat(responseBody).matches {
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
    private fun isOrderedByCreatedAtDescending(responseBody: List<Cas2ApplicationSummary>): Boolean {
      var allDescending = 1
      for (i in 1..(responseBody.size - 1)) {
        val isDescending = (responseBody[i - 1].createdAt.epochSecond - responseBody[i].createdAt.epochSecond).sign
        allDescending *= if (isDescending > 0) 1 else 0
      }
      return allDescending == 1
    }
  }

  @Nested
  inner class GetToIndexUsingIsSubmitted {

    var username: String? = null
    val submittedIds = mutableSetOf<UUID>()
    val unSubmittedIds = mutableSetOf<UUID>()

    @BeforeEach
    fun setup() {
      `Given a CAS2 User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          cas2ApplicationJsonSchemaRepository.deleteAll()

          val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
          }

          // create 3 x submitted applications for this user
          repeat(3) {
            submittedIds.add(
              cas2ApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(userEntity)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(30))
              }.id,
            )
          }

          // create 4 x un-submitted in-progress applications for this user
          repeat(4) {
            unSubmittedIds.add(
              cas2ApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(userEntity)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
              }.id,
            )
          }

          username = userEntity.nomisUsername
        }
      }
    }

    @Test
    fun `returns all applications for user when isSubmitted is null`() {
      val jwt = jwtAuthHelper.createValidNomisAuthorisationCodeJwt(username!!)

      val rawResponseBody = webTestClient.get()
        .uri("/cas2/applications")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.cas2.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult<String>()
        .responseBody
        .blockFirst()

      val responseBody =
        objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2ApplicationSummary>>() {})

      val uuids = responseBody.map { it.id }.toSet()
      Assertions.assertThat(uuids).isEqualTo(submittedIds.union(unSubmittedIds))
    }

    @Test
    fun `returns submitted applications for user when isSubmitted is true`() {
      val jwt = jwtAuthHelper.createValidNomisAuthorisationCodeJwt(username!!)

      val rawResponseBody = webTestClient.get()
        .uri("/cas2/applications?isSubmitted=true")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.cas2.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult<String>()
        .responseBody
        .blockFirst()

      val responseBody =
        objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2ApplicationSummary>>() {})

      val uuids = responseBody.map { it.id }.toSet()
      Assertions.assertThat(uuids).isEqualTo(submittedIds)
    }

    @Test
    fun `returns unsubmitted applications for user when isSubmitted is false`() {
      val jwt = jwtAuthHelper.createValidNomisAuthorisationCodeJwt(username!!)

      val rawResponseBody = webTestClient.get()
        .uri("/cas2/applications?isSubmitted=false")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.cas2.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult<String>()
        .responseBody
        .blockFirst()

      val responseBody =
        objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2ApplicationSummary>>() {})

      val uuids = responseBody.map { it.id }.toSet()
      Assertions.assertThat(uuids).isEqualTo(unSubmittedIds)
    }
  }

  @Nested
  inner class GetToShow {

    @Test
    fun `Get single in progress application returns 200 with correct body`() {
      `Given a CAS2 User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          cas2ApplicationJsonSchemaRepository.deleteAll()

          val newestJsonSchema = cas2ApplicationJsonSchemaEntityFactory
            .produceAndPersist {
              withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
              withSchema(
                schema,
              )
            }

          val applicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(newestJsonSchema)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withData(
              data,
            )
          }

          val rawResponseBody = webTestClient.get()
            .uri("/cas2/applications/${applicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody = objectMapper.readValue(
            rawResponseBody,
            Cas2Application::class.java,
          )

          Assertions.assertThat(responseBody).matches {
            applicationEntity.id == it.id &&
              applicationEntity.crn == it.person.crn &&
              applicationEntity.createdAt.toInstant() == it.createdAt &&
              applicationEntity.createdByUser.id == it.createdBy.id &&
              applicationEntity.submittedAt?.toInstant() == it.submittedAt &&
              serializableToJsonNode(applicationEntity.data) == serializableToJsonNode(it.data) &&
              newestJsonSchema.id == it.schemaVersion && !it.outdatedSchema
          }
        }
      }
    }

    @Test
    fun `Get single application returns successfully when the person cannot be fetched from the prisons API`() {
      `Given a CAS2 User` { userEntity, jwt ->
        val crn = "X1234"

        `Given an Offender`(
          offenderDetailsConfigBlock = {
            withCrn(crn)
            withNomsNumber("ABC123")
          },
        ) { offenderDetails, _ ->
          val application = produceAndPersistBasicApplication(crn, userEntity)

          PrisonAPI_mockNotFoundInmateDetailsCall(offenderDetails.otherIds.nomsNumber!!)
          loadPreemptiveCacheForInmateDetails(offenderDetails.otherIds.nomsNumber!!)

          val rawResponseBody = webTestClient.get()
            .uri("/cas2/applications/${application.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody = objectMapper.readValue(
            rawResponseBody,
            Cas2Application::class.java,
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
    fun `Get single submitted application returns 200 with timeline events`() {
      `Given a CAS2 User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          cas2ApplicationJsonSchemaRepository.deleteAll()

          val newestJsonSchema = cas2ApplicationJsonSchemaEntityFactory
            .produceAndPersist {
              withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
              withSchema(
                schema,
              )
            }

          val applicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(newestJsonSchema)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withSubmittedAt(OffsetDateTime.now().minusDays(1))
          }

          val rawResponseBody = webTestClient.get()
            .uri("/cas2/applications/${applicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody = objectMapper.readValue(
            rawResponseBody,
            Cas2Application::class.java,
          )

          Assertions.assertThat(responseBody.statusUpdates).isEqualTo(emptyList<Cas2StatusUpdate>())

          Assertions.assertThat(responseBody.timelineEvents!!.map { event -> event.label })
            .isEqualTo(listOf("Application submitted"))
        }
      }
    }
  }

  @Nested
  inner class PostToCreate {
    @Test
    fun `Create new application for CAS-2 returns 201 with correct body and Location header`() {
      `Given a CAS2 User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val applicationSchema =
            cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
            }

          val result = webTestClient.post()
            .uri("/cas2/applications")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2.value)
            .bodyValue(
              NewApplication(
                crn = offenderDetails.otherIds.crn,
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated
            .returnResult(Cas2Application::class.java)

          Assertions.assertThat(result.responseHeaders["Location"]).anyMatch {
            it.matches(Regex("/cas2/applications/.+"))
          }

          Assertions.assertThat(result.responseBody.blockFirst()).matches {
            it.person.crn == offenderDetails.otherIds.crn &&
              it.schemaVersion == applicationSchema.id
          }
        }
      }
    }

    @Test
    fun `Create new application returns 404 when a person cannot be found`() {
      `Given a CAS2 User` { userEntity, jwt ->
        val crn = "X1234"

        CommunityAPI_mockNotFoundOffenderDetailsCall(crn)
        loadPreemptiveCacheForOffenderDetails(crn)

        cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.now())
          withId(UUID.randomUUID())
        }

        webTestClient.post()
          .uri("/cas2/applications")
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
  }

  @Nested
  inner class PutToUpdate {
    @Test
    fun `Update existing CAS2 application returns 200 with correct body`() {
      `Given a CAS2 User` { submittingUser, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

          val applicationSchema =
            cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
              withSchema(
                schema,
              )
            }

          cas2ApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withId(applicationId)
            withApplicationSchema(applicationSchema)
            withCreatedByUser(submittingUser)
          }

          val resultBody = webTestClient.put()
            .uri("/cas2/applications/$applicationId")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdateCas2Application(
                data = mapOf("thingId" to 123),
                type = UpdateApplicationType.CAS2,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(String::class.java)
            .responseBody
            .blockFirst()

          val result = objectMapper.readValue(resultBody, Application::class.java)

          Assertions.assertThat(result.person.crn).isEqualTo(offenderDetails.otherIds.crn)
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
    userEntity: NomisUserEntity,
  ): Cas2ApplicationEntity {
    val jsonSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
      withSchema(
        schema,
      )
    }

    val application = cas2ApplicationEntityFactory.produceAndPersist {
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
