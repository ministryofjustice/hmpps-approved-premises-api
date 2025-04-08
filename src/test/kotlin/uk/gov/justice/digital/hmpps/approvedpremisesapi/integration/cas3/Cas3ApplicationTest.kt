package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3ApplicationTest : InitialiseDatabasePerClassTestBase() {

  @Nested
  inner class GetApplications {
    @ParameterizedTest
    @CsvSource("/applications", "/cas3/applications")
    fun `Get all applications without JWT returns 401`(baseUrl: String) {
      webTestClient.get()
        .uri(baseUrl)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @CsvSource(
      "CAS3_REFERRER,/applications",
      "CAS3_ASSESSOR,/applications",
      "CAS3_REFERRER,/cas3/applications",
      "CAS3_ASSESSOR,/cas3/applications",
    )
    fun `Get all applications returns 200 and returns all applications for user`(userRole: UserRole, baseUrl: String) {
      givenAProbationRegion { probationRegion ->
        givenAUser(roles = listOf(userRole), probationRegion = probationRegion) { otherUser, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS3_REFERRER),
            probationRegion = probationRegion,
          ) { referrerUser, jwt ->
            givenAnOffender { offenderDetails, _ ->
              temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

              val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
              }

              val application =
                createApplicationEntity(applicationSchema, referrerUser, offenderDetails, probationRegion, null)

              val anotherUsersApplication =
                createApplicationEntity(applicationSchema, otherUser, offenderDetails, probationRegion, null)

              apDeliusContextAddResponseToUserAccessCall(
                listOf(
                  CaseAccessFactory()
                    .withCrn(offenderDetails.otherIds.crn)
                    .produce(),
                ),
                referrerUser.deliusUsername,
              )

              when (baseUrl) {
                "/applications" -> {
                  val responseBody = webTestClient.get()
                    .uri(baseUrl)
                    .header("Authorization", "Bearer $jwt")
                    .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .bodyAsListOfObjects<TemporaryAccommodationApplicationSummary>()

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
                else -> {
                  val responseBody = webTestClient.get()
                    .uri(baseUrl)
                    .header("Authorization", "Bearer $jwt")
                    .exchange()
                    .expectStatus()
                    .isOk
                    .bodyAsListOfObjects<Cas3ApplicationSummary>()

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
      }
    }

    private fun createApplicationEntity(
      applicationSchema: TemporaryAccommodationApplicationJsonSchemaEntity,
      user: UserEntity,
      offenderDetails: OffenderDetailSummary,
      probationRegion: ProbationRegionEntity,
      submittedAt: OffsetDateTime?,
    ): TemporaryAccommodationApplicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(applicationSchema)
      withCreatedByUser(user)
      withSubmittedAt(submittedAt)
      withCrn(offenderDetails.otherIds.crn)
      withData("{}")
      withProbationRegion(probationRegion)
    }
  }

  @Nested
  inner class GetApplication {
    @Test
    fun `Get single application returns 200 with correct body when requesting user created application`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
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

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            userEntity.deliusUsername,
          )

          callCasApiAndAssertApiResponse(jwt, applicationEntity, newestJsonSchema.id)
          callCas3ApiAndAssertApiResponse(jwt, applicationEntity, newestJsonSchema.id)
        }
      }
    }

    @Test
    fun `Get single application returns 200 with correct body when a user with the CAS3_ASSESSOR role requests a submitted application in their region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAUser(probationRegion = userEntity.probationRegion) { createdByUser, _ ->
          givenAnOffender { offenderDetails, _ ->
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

            apDeliusContextAddResponseToUserAccessCall(
              listOf(
                CaseAccessFactory()
                  .withCrn(offenderDetails.otherIds.crn)
                  .produce(),
              ),
              userEntity.deliusUsername,
            )

            callCasApiAndAssertApiResponse(jwt, applicationEntity, newestJsonSchema.id)
            callCas3ApiAndAssertApiResponse(jwt, applicationEntity, newestJsonSchema.id)
          }
        }
      }
    }

    @Test
    fun `Get single LAO application for application creator with LAO access returns 200`() {
      givenAUser { createdByUser, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentRestriction(true)
          },
        ) { offenderDetails, _ ->
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

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            createdByUser.deliusUsername,
          )

          val casApiResponseBody = callCasApi(jwt, applicationEntity.id)
            .expectStatus()
            .isOk
            .expectBody(TemporaryAccommodationApplication::class.java)
            .returnResult()
            .responseBody

          assertThat(casApiResponseBody?.person).isInstanceOf(FullPerson::class.java)

          val cas3ApiResponseBody = callCas3Api(jwt, applicationEntity.id)
            .expectStatus()
            .isOk
            .expectBody(Cas3Application::class.java)
            .returnResult()
            .responseBody

          assertThat(cas3ApiResponseBody?.person).isInstanceOf(FullPerson::class.java)
        }
      }
    }

    @Test
    fun `Get single LAO application for user who is not creator but has LAO Qualification returns RestrictedPerson`() {
      givenAUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        qualifications = listOf(UserQualification.LAO),
      ) { otherUser, otherUserJwt ->
        givenAUser(probationRegion = otherUser.probationRegion) { createdByUser, _ ->
          givenAnOffender(
            offenderDetailsConfigBlock = {
              withCurrentRestriction(true)
            },
          ) { offenderDetails, _ ->
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

            val casApiResult = callCasApi(otherUserJwt, applicationEntity.id)
              .expectStatus()
              .isOk
              .expectBody(TemporaryAccommodationApplication::class.java)
              .returnResult()
              .responseBody

            assertThat(casApiResult!!.person.type).isEqualTo(PersonType.restrictedPerson)

            val cas3ApiResult = callCas3Api(otherUserJwt, applicationEntity.id)
              .expectStatus()
              .isOk
              .expectBody(Cas3Application::class.java)
              .returnResult()
              .responseBody

            assertThat(cas3ApiResult!!.person.type).isEqualTo(PersonType.restrictedPerson)
          }
        }
      }
    }

    @Test
    fun `Get single application returns 403 Forbidden when a user with the CAS3_ASSESSOR role requests an application not in their region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAUser { createdByUser, _ ->
          givenAnOffender { offenderDetails, _ ->
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

            apDeliusContextAddResponseToUserAccessCall(
              listOf(
                CaseAccessFactory()
                  .withCrn(offenderDetails.otherIds.crn)
                  .produce(),
              ),
              userEntity.deliusUsername,
            )

            callCasApi(jwt, applicationEntity.id)
              .expectStatus()
              .isForbidden

            callCas3Api(jwt, applicationEntity.id)
              .expectStatus()
              .isForbidden
          }
        }
      }
    }

    @Test
    fun `Get single application returns 403 Forbidden when a user without the CAS3_ASSESSOR role requests an application not created by them`() {
      givenAUser { userEntity, jwt ->
        givenAUser(probationRegion = userEntity.probationRegion) { createdByUser, _ ->
          givenAnOffender { offenderDetails, _ ->
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

            apDeliusContextAddResponseToUserAccessCall(
              listOf(
                CaseAccessFactory()
                  .withCrn(offenderDetails.otherIds.crn)
                  .produce(),
              ),
              userEntity.deliusUsername,
            )

            callCasApi(jwt, applicationEntity.id)
              .expectStatus()
              .isForbidden

            callCas3Api(jwt, applicationEntity.id)
              .expectStatus()
              .isForbidden
          }
        }
      }
    }

    @Test
    fun `Get single application returns 404 Not Found when the application was deleted`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

          val newestJsonSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.parse("2024-12-11T13:21:00+01:00"))
            withSchema("{}")
          }

          val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(newestJsonSchema)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withProbationRegion(userEntity.probationRegion)
            withDeletedAt(OffsetDateTime.now().minusDays(15))
            withData(
              """
            {
               "thingId": 123
            }
            """,
            )
          }

          apDeliusContextAddResponseToUserAccessCall(
            listOf(
              CaseAccessFactory()
                .withCrn(offenderDetails.otherIds.crn)
                .produce(),
            ),
            userEntity.deliusUsername,
          )

          callCasApi(jwt, applicationEntity.id)
            .expectStatus()
            .isNotFound

          callCas3Api(jwt, applicationEntity.id)
            .expectStatus()
            .isNotFound
        }
      }
    }

    @Test
    fun `GET submitted CAS3 application includes assessmentId in the response`() {
      givenAUser { submittingUser, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")
          val offenderName = "${offenderDetails.firstName} ${offenderDetails.surname}"

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
            withName(offenderName)
            withData("{}")
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

          val casApiResult = callCasApi(jwt, applicationId)
            .expectStatus()
            .isOk
            .expectBody(TemporaryAccommodationApplication::class.java)
            .returnResult()
            .responseBody

          assertThat(casApiResult!!.assessmentId).isNotNull()

          val cas3ApiResult = callCas3Api(jwt, applicationId)
            .expectStatus()
            .isOk
            .expectBody(Cas3Application::class.java)
            .returnResult()
            .responseBody

          assertThat(cas3ApiResult!!.assessmentId).isNotNull()
        }
      }
    }

    private fun callCasApi(jwt: String, applicationId: UUID) = webTestClient.get()
      .uri("/applications/$applicationId")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .exchange()

    private fun callCas3Api(jwt: String, applicationId: UUID) = webTestClient.get()
      .uri("/cas3/applications/$applicationId")
      .header("Authorization", "Bearer $jwt")
      .exchange()

    private fun callCasApiAndAssertApiResponse(
      jwt: String,
      applicationEntity: TemporaryAccommodationApplicationEntity,
      applicationSchemaId: UUID,
    ) {
      val responseBody = callCasApi(jwt, applicationEntity.id)
        .expectStatus()
        .isOk
        .expectBody(TemporaryAccommodationApplication::class.java)
        .returnResult()
        .responseBody

      assertThat(responseBody).matches {
        applicationEntity.id == it.id &&
          applicationEntity.crn == it.person.crn &&
          applicationEntity.createdAt.toInstant() == it.createdAt &&
          applicationEntity.createdByUser.id == it.createdByUserId &&
          applicationEntity.submittedAt?.toInstant() == it.submittedAt &&
          serializableToJsonNode(applicationEntity.data) == serializableToJsonNode(it.data) &&
          applicationSchemaId == it.schemaVersion &&
          !it.outdatedSchema
      }
    }

    private fun callCas3ApiAndAssertApiResponse(
      jwt: String,
      applicationEntity: TemporaryAccommodationApplicationEntity,
      applicationSchemaId: UUID,
    ) {
      val responseBody = callCas3Api(jwt, applicationEntity.id)
        .expectStatus()
        .isOk
        .expectBody(Cas3Application::class.java)
        .returnResult()
        .responseBody

      assertThat(responseBody).matches {
        applicationEntity.id == it.id &&
          applicationEntity.crn == it.person.crn &&
          applicationEntity.createdAt.toInstant() == it.createdAt &&
          applicationEntity.createdByUser.id == it.createdByUserId &&
          applicationEntity.submittedAt?.toInstant() == it.submittedAt &&
          serializableToJsonNode(applicationEntity.data) == serializableToJsonNode(it.data) &&
          applicationSchemaId == it.schemaVersion &&
          !it.outdatedSchema
      }
    }

    private fun schemaText(): String = """
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
  inner class CreateApplication {

    @Test
    fun `Create new application returns 403 when user isn't  CAS3_REFERRER role`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        givenAnOffender { offenderDetails, _ ->

          callCasApi(jwt, offenderDetails.otherIds.crn, "789")
            .expectStatus()
            .isForbidden

          callCas3Api(jwt, offenderDetails.otherIds.crn, "789")
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `Should get 403 forbidden error when create new application with user CAS3_REPORTER`() {
      givenAUser(roles = listOf(UserRole.CAS3_REPORTER)) { _, jwt ->
        givenAnOffender { offenderDetails, _ ->

          callCasApi(jwt, offenderDetails.otherIds.crn, "789")
            .expectStatus()
            .isForbidden

          callCas3Api(jwt, offenderDetails.otherIds.crn, "789")
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `Create new application returns 201 with correct body and Location header`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { _, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
          }

          callCasApiAndAssertResponse(jwt, offenderDetails.otherIds.crn, applicationSchema.id)
          callCas3ApiAndAssertResponse(jwt, offenderDetails.otherIds.crn, applicationSchema.id)
        }
      }
    }

    @Test
    fun `Create new application returns successfully when a person has no NOMS number`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { _, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = { withoutNomsNumber() },
        ) { offenderDetails, _ ->
          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
          }

          callCasApiAndAssertResponse(jwt, offenderDetails.otherIds.crn, applicationSchema.id)
          callCas3ApiAndAssertResponse(jwt, offenderDetails.otherIds.crn, applicationSchema.id)
        }
      }
    }

    @Test
    fun `Create new application returns 201 with correct body and store prison-name in DB`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { _, jwt ->
        val agencyName = "HMP Bristol"
        givenAnOffender(
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
            withCustodyStatus(InmateStatus.IN)
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

          callCasApiAndAssertResponse(jwt, offenderDetails.otherIds.crn, applicationSchema.id, agencyName)
          callCas3ApiAndAssertResponse(jwt, offenderDetails.otherIds.crn, applicationSchema.id, agencyName)
        }
      }
    }

    private fun callCasApiAndAssertResponse(
      jwt: String,
      crn: String,
      applicationSchemaId: UUID,
      agencyName: String? = null,
    ) {
      val offenceId = "789"

      val result = callCasApi(jwt, crn, offenceId)
        .expectStatus()
        .isCreated
        .returnResult(TemporaryAccommodationApplication::class.java)

      assertThat(result.responseHeaders["Location"]).anyMatch {
        it.matches(Regex("/applications/.+"))
      }

      val blockFirst = result.responseBody.blockFirst()
      assertThat(blockFirst).matches {
        it.person.crn == crn &&
          it.schemaVersion == applicationSchemaId &&
          it.offenceId == offenceId
      }

      if (agencyName != null) {
        val accommodationApplicationEntity =
          temporaryAccommodationApplicationRepository.findByIdOrNull(blockFirst.id)
        assertThat(accommodationApplicationEntity!!.prisonNameOnCreation).isNotNull()
        assertThat(accommodationApplicationEntity!!.prisonNameOnCreation).isEqualTo(agencyName)
      }
    }

    private fun callCasApi(jwt: String, crn: String, offenceId: String) = webTestClient.post()
      .uri("/applications")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .bodyValue(
        NewApplication(
          crn = crn,
          convictionId = 123,
          deliusEventNumber = "1",
          offenceId = offenceId,
        ),
      )
      .exchange()

    private fun callCas3ApiAndAssertResponse(
      jwt: String,
      crn: String,
      applicationSchemaId: UUID,
      agencyName: String? = null,
    ) {
      val offenceId = "789"

      val result = callCas3Api(jwt, crn, offenceId)
        .expectStatus()
        .isCreated
        .returnResult(Cas3Application::class.java)

      assertThat(result.responseHeaders["Location"]).anyMatch {
        it.matches(Regex("/applications/.+"))
      }

      val blockFirst = result.responseBody.blockFirst()
      assertThat(blockFirst).matches {
        it.person.crn == crn &&
          it.schemaVersion == applicationSchemaId &&
          it.offenceId == offenceId
      }

      if (agencyName != null) {
        val accommodationApplicationEntity =
          temporaryAccommodationApplicationRepository.findByIdOrNull(blockFirst.id)
        assertThat(accommodationApplicationEntity!!.prisonNameOnCreation).isNotNull()
        assertThat(accommodationApplicationEntity!!.prisonNameOnCreation).isEqualTo(agencyName)
      }
    }

    private fun callCas3Api(jwt: String, crn: String, offenceId: String) = webTestClient.post()
      .uri("/cas3/applications")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        Cas3NewApplication(
          crn = crn,
          convictionId = 123,
          deliusEventNumber = "1",
          offenceId = offenceId,
        ),
      )
      .exchange()
  }

  @Nested
  inner class UpdateApplication {
    @Test
    fun `Update existing application returns 200 with correct body`() {
      givenAUser { userEntity, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS3_REFERRER),
        ) { _, _ ->
          givenAnOffender { offenderDetails, _ ->
            temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.parse("2024-12-11T13:21:00+01:00"))
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

            callCasApiAndAssertResponse(jwt, applicationEntity.id, offenderDetails.otherIds.crn)
            callCas3ApiAndAssertResponse(jwt, applicationEntity.id, offenderDetails.otherIds.crn)
          }
        }
      }
    }

    @Test
    fun `Update existing application which was deleted returns 400`() {
      givenAUser { userEntity, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS3_REFERRER),
        ) { _, _ ->
          givenAnOffender { offenderDetails, _ ->
            temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.parse("2024-12-11T13:21:00+01:00"))
              withSchema("{}")
            }

            val applicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(newestJsonSchema)
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
              withProbationRegion(userEntity.probationRegion)
              withDeletedAt(OffsetDateTime.now().minusDays(26))
              withData(
                """
            {
               "thingId": 123
            }
            """,
              )
            }

            callCasApi(jwt, applicationEntity.id)
              .expectStatus()
              .isBadRequest
              .expectBody()
              .jsonPath("$.status").isEqualTo("400")
              .jsonPath("$.detail").isEqualTo("This application has already been deleted")

            callCas3Api(jwt, applicationEntity.id)
              .expectStatus()
              .isBadRequest
              .expectBody()
              .jsonPath("$.status").isEqualTo("400")
              .jsonPath("$.detail").isEqualTo("This application has already been deleted")
          }
        }
      }
    }

    private fun callCasApiAndAssertResponse(
      jwt: String,
      applicationId: UUID,
      crn: String,
    ) {
      val casApiResult = callCasApi(jwt, applicationId)
        .expectStatus()
        .isOk
        .returnResult(String::class.java)
        .responseBody
        .blockFirst()

      val result = objectMapper.readValue(casApiResult, TemporaryAccommodationApplication::class.java)

      assertThat(result.person.crn).isEqualTo(crn)
      assertThat(result.data.toString()).isEqualTo("""{thingId=345}""")
    }

    private fun callCasApi(jwt: String, applicationId: UUID) = webTestClient.put()
      .uri("/applications/$applicationId")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        UpdateTemporaryAccommodationApplication(
          data = mapOf("thingId" to 345),
          type = UpdateApplicationType.CAS3,
        ),
      )
      .exchange()

    private fun callCas3ApiAndAssertResponse(
      jwt: String,
      applicationId: UUID,
      crn: String,
    ) {
      val casApiResult = callCas3Api(jwt, applicationId)
        .expectStatus()
        .isOk
        .returnResult(String::class.java)
        .responseBody
        .blockFirst()

      val result = objectMapper.readValue(casApiResult, Cas3Application::class.java)

      assertThat(result.person.crn).isEqualTo(crn)
      assertThat(result.data.toString()).isEqualTo("""{thingId=345}""")
    }

    private fun callCas3Api(jwt: String, applicationId: UUID) = webTestClient.put()
      .uri("/cas3/applications/$applicationId")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        Cas3UpdateApplication(
          data = mapOf("thingId" to 345),
        ),
      )
      .exchange()
  }

  @Nested
  inner class SubmitApplication {

    @ParameterizedTest
    @CsvSource("CAS", "CAS3")
    fun `Submit Temporary Accommodation application returns 200`(apiEndpoint: String) {
      givenAUser { submittingUser, jwt ->
        givenAUser { _, _ ->
          givenAnOffender { offenderDetails, _ ->
            val applicationId = UUID.randomUUID()
            val offenderName = "${offenderDetails.firstName} ${offenderDetails.surname}"

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
              withName(offenderName)
              withData(
                """
                {}
              """,
              )
            }

            if (apiEndpoint == "CAS") {
              val submitApplication = SubmitTemporaryAccommodationApplication(
                translatedDocument = {},
                type = "CAS3",
                arrivalDate = LocalDate.now(),
                summaryData = object {
                  val num = 50
                  val text = "Hello world!"
                },
              )

              callCasApiAndAssertApiResponse(jwt, applicationId, submitApplication)
                .expectStatus()
                .isOk
            } else {
              val cas3SubmitApplication = Cas3SubmitApplication(
                translatedDocument = {},
                arrivalDate = LocalDate.now(),
                summaryData = object {
                  val num = 50
                  val text = "Hello world!"
                },
              )

              callCas3ApiAndAssertApiResponse(jwt, applicationId, cas3SubmitApplication)
                .expectStatus()
                .isOk
            }

            val persistedApplication = temporaryAccommodationApplicationRepository.findByIdOrNull(applicationId)!!
            val persistedAssessment = persistedApplication.getLatestAssessment() as TemporaryAccommodationAssessmentEntity

            assertThat(persistedAssessment.summaryData).isEqualTo("{\"num\":50,\"text\":\"Hello world!\"}")
            assertThat(persistedApplication.name).isEqualTo(offenderName)
          }
        }
      }
    }

    @CsvSource("CAS", "CAS3")
    fun `Submit Temporary Accommodation application returns 200 with optional elements in the request`(apiEndpoint: String) {
      givenAUser { submittingUser, jwt ->
        givenAUser { _, _ ->
          givenAnOffender { offenderDetails, _ ->
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
              withPdu("Probation Delivery Unit Test")
              withHasHistoryOfSexualOffence(true)
              withIsConcerningSexualBehaviour(true)
              withIsConcerningArsonBehaviour(true)
              withData(
                """
              {}
            """,
              )
            }

            val pdu = probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(
                probationRegionEntityFactory.produceAndPersist {
                  withId(submittingUser.probationRegion.id)
                },
              )
            }

            if (apiEndpoint == "CAS") {
              val submitApplication = SubmitTemporaryAccommodationApplication(
                translatedDocument = {},
                type = "CAS3",
                arrivalDate = LocalDate.now(),
                summaryData = object {
                  val num = 50
                  val text = "Hello world!"
                },
                personReleaseDate = LocalDate.now(),
                pdu = "Probation Delivery Unit Test",
                isHistoryOfSexualOffence = true,
                isConcerningSexualBehaviour = true,
                isConcerningArsonBehaviour = true,
                dutyToReferOutcome = "Accepted – Prevention/ Relief Duty",
                prisonReleaseTypes = listOf(
                  "Parole",
                  "CRD licence",
                ),
                probationDeliveryUnitId = pdu.id,
              )

              callCasApiAndAssertApiResponse(jwt, applicationId, submitApplication)
                .expectStatus()
                .isOk
            } else {
              val cas3SubmitApplication = Cas3SubmitApplication(
                translatedDocument = {},
                arrivalDate = LocalDate.now(),
                summaryData = object {
                  val num = 50
                  val text = "Hello world!"
                },
                personReleaseDate = LocalDate.now(),
                pdu = "Probation Delivery Unit Test",
                isHistoryOfSexualOffence = true,
                isConcerningSexualBehaviour = true,
                isConcerningArsonBehaviour = true,
                dutyToReferOutcome = "Accepted – Prevention/ Relief Duty",
                prisonReleaseTypes = listOf(
                  "Parole",
                  "CRD licence",
                ),
                probationDeliveryUnitId = pdu.id,
              )

              callCas3ApiAndAssertApiResponse(jwt, applicationId, cas3SubmitApplication)
                .expectStatus()
                .isOk
            }

            val persistedApplication = temporaryAccommodationApplicationRepository.findByIdOrNull(applicationId)!!
            val persistedAssessment =
              persistedApplication.getLatestAssessment() as TemporaryAccommodationAssessmentEntity

            assertThat(persistedAssessment.summaryData).isEqualTo("{\"num\":50,\"text\":\"Hello world!\"}")
            assertThat(persistedApplication.personReleaseDate).isEqualTo(LocalDate.now())
            assertThat(persistedApplication.dutyToReferOutcome).isEqualTo("Accepted – Prevention/ Relief Duty")
            assertThat(persistedApplication.prisonReleaseTypes).isEqualTo("Parole,CRD licence")
            assertThat(persistedApplication.probationDeliveryUnit!!.id).isEqualTo(pdu.id)
            assertThat(persistedApplication.probationDeliveryUnit!!.name).isEqualTo(pdu.name)
          }
        }
      }
    }

    @Test
    fun `Submit Temporary Accommodation application returns 400 when the application was deleted`() {
      givenAUser { submittingUser, jwt ->
        givenAUser { _, _ ->
          givenAnOffender { offenderDetails, _ ->
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")
            val offenderName = "${offenderDetails.firstName} ${offenderDetails.surname}"

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
              withName(offenderName)
              withDeletedAt(OffsetDateTime.now().minusDays(32))
              withData(
                """
                {}
              """,
              )
            }

            val submitApplication = SubmitTemporaryAccommodationApplication(
              translatedDocument = {},
              type = "CAS3",
              arrivalDate = LocalDate.now(),
              summaryData = object {
                val num = 50
                val text = "Hello world!"
              },
            )

            callCasApiAndAssertApiResponse(jwt, applicationId, submitApplication)
              .expectStatus()
              .isBadRequest
              .expectBody()
              .jsonPath("$.status").isEqualTo("400")
              .jsonPath("$.detail").isEqualTo("This application has already been deleted")

            val cas3SubmitApplication = Cas3SubmitApplication(
              translatedDocument = {},
              arrivalDate = LocalDate.now(),
              summaryData = object {
                val num = 50
                val text = "Hello world!"
              },
            )

            callCas3ApiAndAssertApiResponse(jwt, applicationId, cas3SubmitApplication)
              .expectStatus()
              .isBadRequest
              .expectBody()
              .jsonPath("$.status").isEqualTo("400")
              .jsonPath("$.detail").isEqualTo("This application has already been deleted")
          }
        }
      }
    }

    private fun callCasApiAndAssertApiResponse(jwt: String, applicationId: UUID, submitApplication: SubmitTemporaryAccommodationApplication) = webTestClient.post()
      .uri("/applications/$applicationId/submission")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .bodyValue(submitApplication)
      .exchange()

    private fun callCas3ApiAndAssertApiResponse(jwt: String, applicationId: UUID, submitApplication: Cas3SubmitApplication) = webTestClient.post()
      .uri("/cas3/applications/$applicationId/submission")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .bodyValue(submitApplication)
      .exchange()

    private fun schemaText(): String = """
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
  inner class SoftDelete {
    @Test
    fun `soft delete application without JWT returns 401`() {
      webTestClient.delete()
        .uri("/cas3/applications/${UUID.randomUUID()}")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `soft delete inProgress application successfully returns 200`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->

          val application =
            persistApplication(crn = offenderDetails.otherIds.crn, user = userEntity)

          webTestClient.delete()
            .uri("/cas3/applications/${application.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk

          Assertions.assertThat(temporaryAccommodationApplicationRepository.findById(application.id).get().deletedAt)
            .isNotNull()

          val domainEvents =
            domainEventRepository.findByApplicationIdAndType(
              applicationId = application.id,
              type = DomainEventType.CAS3_DRAFT_REFERRAL_DELETED,
            )

          assertThat(domainEvents.size).isEqualTo(1)
        }
      }
    }
  }

  private fun persistApplication(crn: String, user: UserEntity) = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(user)
    withApplicationSchema(persistApplicationSchema())
    withProbationRegion(user.probationRegion)
    withArrivalDate(LocalDate.now().plusDays(30))
    withSubmittedAt(null)
  }

  private fun persistApplicationSchema() = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
    temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }
  }

  private fun serializableToJsonNode(serializable: Any?): JsonNode {
    if (serializable == null) return NullNode.instance
    if (serializable is String) return objectMapper.readTree(serializable)

    return objectMapper.readTree(objectMapper.writeValueAsString(serializable))
  }
}
