package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Submitted Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Assessment for Approved Premises`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockOffenderUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.CommunityAPI_mockSuccessfulOffenderDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision as JpaPlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType as JpaPlacementType

class PlacementApplicationsTest : IntegrationTestBase() {

  @Nested
  inner class CreatePlacementApplicationTest {
    @Test
    fun `creating a placement application JWT returns 401`() {
      webTestClient.post()
        .uri("/placement-applications")
        .bodyValue(
          NewPlacementApplication(
            applicationId = UUID.randomUUID(),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `creating a placement application when the application does not exist returns 404`() {
      `Given a User` { _, jwt ->
        webTestClient.post()
          .uri("/placement-applications")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewPlacementApplication(
              applicationId = UUID.randomUUID(),
            ),
          )
          .exchange()
          .expectStatus()
          .isNotFound()
      }
    }

    @Test
    fun `creating a placement application when the application does not belong to the user returns 401`() {
      `Given a User` { _, jwt ->
        `Given a User` { otherUser, _ ->
          `Given a Submitted Application`(createdByUser = otherUser) { application ->
            webTestClient.post()
              .uri("/placement-applications")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                NewPlacementApplication(
                  applicationId = application.id,
                ),
              )
              .exchange()
              .expectStatus()
              .isForbidden()
          }
        }
      }
    }

    @Test
    fun `creating a placement application when the application does not have an assessment returns an error`() {
      `Given a User` { user, jwt ->
        `Given a Submitted Application`(createdByUser = user) { application ->
          webTestClient.post()
            .uri("/placement-applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewPlacementApplication(
                applicationId = application.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest()
        }
      }
    }

    @Test
    fun `creating a placement application when the assessment has been rejected returns an error`() {
      `Given a User` { user, jwt ->
        `Given an Assessment for Approved Premises`(decision = AssessmentDecision.REJECTED, allocatedToUser = user, createdByUser = user) { _, application ->
          approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          webTestClient.post()
            .uri("/placement-applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewPlacementApplication(
                applicationId = application.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest()
        }
      }
    }

    @Test
    fun `creating a placement application when the application belongs to the user returns successfully`() {
      `Given a User` { user, jwt ->
        `Given an Assessment for Approved Premises`(decision = AssessmentDecision.ACCEPTED, allocatedToUser = user, createdByUser = user, submittedAt = OffsetDateTime.now()) { _, application ->
          val schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val rawResult = webTestClient.post()
            .uri("/placement-applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewPlacementApplication(
                applicationId = application.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)

          assertThat(body.applicationId).isEqualTo(application.id)
          assertThat(body.applicationId).isEqualTo(application.id)
          assertThat(body.outdatedSchema).isEqualTo(false)
          assertThat(body.createdAt).isNotNull()
          assertThat(body.schemaVersion).isEqualTo(schema.id)
        }
      }
    }
  }

  @Nested
  inner class GetPlacementApplicationTest {
    @Test
    fun `getting a placement request application JWT returns 401`() {
      `Given a User` { user, _ ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        ) { placementApplicationEntity ->
          webTestClient.get()
            .uri("/placement-request-applications/${placementApplicationEntity.id}")
            .exchange()
            .expectStatus()
            .isUnauthorized
        }
      }
    }

    @Test
    fun `getting a nonexistent placement request application returns 404`() {
      `Given a User` { _, jwt ->
        webTestClient.get()
          .uri("/placement-applications/${UUID.randomUUID()}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `getting a placement application where the Offender is LAO but user does not pass LAO check or have LAO qualification returns 403`() {
      `Given a User` { user, jwt ->
        `Given an Offender`(
          offenderDetailsConfigBlock = {
            withCurrentExclusion(true)
          },
        ) { offenderDetails, _ ->
          `Given a Placement Application`(
            crn = offenderDetails.otherIds.crn,
            createdByUser = user,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          ) { placementApplicationEntity ->
            CommunityAPI_mockOffenderUserAccessCall(
              username = user.deliusUsername,
              crn = offenderDetails.otherIds.crn,
              inclusion = false,
              exclusion = true,
            )

            webTestClient.get()
              .uri("/placement-applications/${placementApplicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isForbidden
          }
        }
      }
    }

    @Test
    fun `getting a placement application where the Offender is LAO, does not have LAO qualification but does pass LAO check or returns 200`() {
      `Given a User` { user, jwt ->
        `Given an Offender`(
          offenderDetailsConfigBlock = {
            withCurrentExclusion(true)
          },
        ) { offenderDetails, _ ->
          `Given a Placement Application`(
            crn = offenderDetails.otherIds.crn,
            createdByUser = user,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          ) { placementApplicationEntity ->
            CommunityAPI_mockOffenderUserAccessCall(
              username = user.deliusUsername,
              crn = offenderDetails.otherIds.crn,
              inclusion = false,
              exclusion = false,
            )

            val rawResult = webTestClient.get()
              .uri("/placement-applications/${placementApplicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)

            assertThat(body.id).isEqualTo(placementApplicationEntity.id)
            assertThat(body.applicationId).isEqualTo(placementApplicationEntity.application.id)
            assertThat(body.createdByUserId).isEqualTo(placementApplicationEntity.createdByUser.id)
            assertThat(body.schemaVersion).isEqualTo(placementApplicationEntity.schemaVersion.id)
            assertThat(body.createdAt).isEqualTo(placementApplicationEntity.createdAt.toInstant())
            assertThat(body.submittedAt).isNull()
          }
        }
      }
    }

    @Test
    fun `getting a placement application where the Offender is LAO, does not pass LAO check but does have LAO qualification returns 200`() {
      `Given a User`(qualifications = listOf(UserQualification.LAO)) { user, jwt ->
        `Given an Offender`(
          offenderDetailsConfigBlock = {
            withCurrentExclusion(true)
          },
        ) { offenderDetails, _ ->
          `Given a Placement Application`(
            crn = offenderDetails.otherIds.crn,
            createdByUser = user,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          ) { placementApplicationEntity ->
            CommunityAPI_mockOffenderUserAccessCall(
              username = user.deliusUsername,
              crn = offenderDetails.otherIds.crn,
              inclusion = false,
              exclusion = true,
            )

            val rawResult = webTestClient.get()
              .uri("/placement-applications/${placementApplicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)

            assertThat(body.id).isEqualTo(placementApplicationEntity.id)
            assertThat(body.applicationId).isEqualTo(placementApplicationEntity.application.id)
            assertThat(body.createdByUserId).isEqualTo(placementApplicationEntity.createdByUser.id)
            assertThat(body.schemaVersion).isEqualTo(placementApplicationEntity.schemaVersion.id)
            assertThat(body.createdAt).isEqualTo(placementApplicationEntity.createdAt.toInstant())
            assertThat(body.submittedAt).isNull()
          }
        }
      }
    }

    @Test
    fun `getting a placement application returns the transformed object`() {
      `Given a User` { user, jwt ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        ) { placementApplicationEntity ->
          val rawResult = webTestClient.get()
            .uri("/placement-applications/${placementApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)

          assertThat(body.id).isEqualTo(placementApplicationEntity.id)
          assertThat(body.applicationId).isEqualTo(placementApplicationEntity.application.id)
          assertThat(body.createdByUserId).isEqualTo(placementApplicationEntity.createdByUser.id)
          assertThat(body.schemaVersion).isEqualTo(placementApplicationEntity.schemaVersion.id)
          assertThat(body.createdAt).isEqualTo(placementApplicationEntity.createdAt.toInstant())
          assertThat(body.submittedAt).isNull()
        }
      }
    }
  }

  @Nested
  inner class UpdatePlacementApplicationTest {
    @Test
    fun `updating a placement request application without a JWT returns 401`() {
      webTestClient.put()
        .uri("/placement-applications/${UUID.randomUUID()}")
        .bodyValue(
          UpdatePlacementApplication(
            data = mapOf("thingId" to 123),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `updating a submitted placement request application returns an error`() {
      `Given a User` { user, jwt ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          webTestClient.put()
            .uri("/placement-applications/${placementApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdatePlacementApplication(
                data = mapOf("thingId" to 123),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    @Test
    fun `updating a placement request application created by a different user returns an error`() {
      `Given a User` { _, jwt ->
        `Given a Placement Application`(
          createdByUser = userEntityFactory.produceAndPersist {
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist {
                withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
              }
            }
          },
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          webTestClient.put()
            .uri("/placement-applications/${placementApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdatePlacementApplication(
                data = mapOf("thingId" to 123),
              ),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `updating a placement request application with an outdated schema returns an error`() {
      `Given a User` { user, jwt ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          webTestClient.put()
            .uri("/placement-applications/${placementApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdatePlacementApplication(
                data = mapOf("thingId" to 123),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    @Test
    fun `updating an in-progress placement request application returns successfully and updates the application`() {
      `Given a User` { user, jwt ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        ) { placementApplicationEntity ->
          val rawResult = webTestClient.put()
            .uri("/placement-applications/${placementApplicationEntity.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdatePlacementApplication(
                data = mapOf("thingId" to 123),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)
          val expectedUpdatedPlacementApplication = placementApplicationEntity.copy(
            schemaUpToDate = true,
            data = "{\"thingId\":123}",
          )

          assertThat(body).matches {
            expectedUpdatedPlacementApplication.id == it.id &&
              expectedUpdatedPlacementApplication.application.id == it.applicationId &&
              expectedUpdatedPlacementApplication.createdByUser.id == it.createdByUserId &&
              expectedUpdatedPlacementApplication.schemaVersion.id == it.schemaVersion &&
              expectedUpdatedPlacementApplication.createdAt.toInstant() == it.createdAt &&
              serializableToJsonNode(expectedUpdatedPlacementApplication.data) == serializableToJsonNode(it.data)
          }

          val updatedPlacementApplication = placementApplicationRepository.findByIdOrNull(placementApplicationEntity.id)

          assertThat(updatedPlacementApplication!!.data).isEqualTo(expectedUpdatedPlacementApplication.data)
        }
      }
    }
  }

  @Nested
  inner class SubmitPlacementApplicationTest {
    @Test
    fun `submitting a placement request application without a JWT returns 401`() {
      webTestClient.post()
        .uri("/placement-applications/${UUID.randomUUID()}/submission")
        .bodyValue(
          SubmitPlacementApplication(
            translatedDocument = mapOf("thingId" to 123),
            placementType = PlacementType.additionalPlacement,
            placementDates = listOf(
              PlacementDates(
                expectedArrival = LocalDate.now(),
                duration = 12,
              ),
            ),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `submitting a submitted placement request application returns an error`() {
      `Given a User` { user, jwt ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          webTestClient.post()
            .uri("/placement-applications/${placementApplicationEntity.id}/submission")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              SubmitPlacementApplication(
                translatedDocument = mapOf("thingId" to 123),
                placementType = PlacementType.additionalPlacement,
                placementDates = listOf(
                  PlacementDates(
                    expectedArrival = LocalDate.now(),
                    duration = 12,
                  ),
                ),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    @Test
    fun `submitting a placement request application created by a different user returns an error`() {
      `Given a User` { _, jwt ->
        `Given a Placement Application`(
          createdByUser = userEntityFactory.produceAndPersist {
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist {
                withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
              }
            }
          },
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          webTestClient.post()
            .uri("/placement-applications/${placementApplicationEntity.id}/submission")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              SubmitPlacementApplication(
                translatedDocument = mapOf("thingId" to 123),
                placementType = PlacementType.additionalPlacement,
                placementDates = listOf(
                  PlacementDates(
                    expectedArrival = LocalDate.now(),
                    duration = 12,
                  ),
                ),
              ),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `submitting a placement request application with an outdated schema returns an error`() {
      `Given a User` { user, jwt ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->
          approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          webTestClient.post()
            .uri("/placement-applications/${placementApplicationEntity.id}/submission")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              SubmitPlacementApplication(
                translatedDocument = mapOf("thingId" to 123),
                placementType = PlacementType.additionalPlacement,
                placementDates = listOf(
                  PlacementDates(
                    expectedArrival = LocalDate.now(),
                    duration = 12,
                  ),
                ),
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    @Test
    fun `submitting an in-progress placement request application returns successfully and updates the application`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER), qualifications = listOf()) { matcherUser, _ ->
        `Given a User` { user, jwt ->
          `Given a Placement Application`(
            createdByUser = user,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          ) { placementApplicationEntity ->
            CommunityAPI_mockSuccessfulOffenderDetailsCall(
              OffenderDetailsSummaryFactory()
                .withCrn(placementApplicationEntity.application.crn)
                .produce(),
            )

            GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

            val placementDates = listOf(
              PlacementDates(
                expectedArrival = LocalDate.now(),
                duration = 12,
              ),
            )
            val rawResult = webTestClient.post()
              .uri("/placement-applications/${placementApplicationEntity.id}/submission")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                SubmitPlacementApplication(
                  translatedDocument = mapOf("thingId" to 123),
                  placementType = PlacementType.additionalPlacement,
                  placementDates = placementDates,
                ),
              )
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)
            val expectedUpdatedPlacementApplication = placementApplicationEntity.copy(
              schemaUpToDate = true,
              document = "{\"thingId\":123}",
            )

            assertThat(body).matches {
              expectedUpdatedPlacementApplication.id == it.id &&
                expectedUpdatedPlacementApplication.application.id == it.applicationId &&
                expectedUpdatedPlacementApplication.createdByUser.id == it.createdByUserId &&
                expectedUpdatedPlacementApplication.schemaVersion.id == it.schemaVersion &&
                expectedUpdatedPlacementApplication.createdAt.toInstant() == it.createdAt &&
                serializableToJsonNode(expectedUpdatedPlacementApplication.document) == serializableToJsonNode(it.document)
            }

            val updatedPlacementApplication =
              placementApplicationRepository.findByIdOrNull(placementApplicationEntity.id)!!

            assertThat(updatedPlacementApplication.document).isEqualTo(expectedUpdatedPlacementApplication.document)
            assertThat(updatedPlacementApplication.submittedAt).isNotNull()
            assertThat(updatedPlacementApplication.allocatedToUser!!.id).isEqualTo(matcherUser.id)
            assertThat(updatedPlacementApplication.allocatedAt).isNotNull()

            val createdPlacementDates = placementDateRepository.findAllByPlacementApplication(placementApplicationEntity)

            assertThat(createdPlacementDates.size).isEqualTo(1)

            assertThat(createdPlacementDates[0].placementApplication.id).isEqualTo(placementApplicationEntity.id)
            assertThat(createdPlacementDates[0].duration).isEqualTo(placementDates[0].duration)
            assertThat(createdPlacementDates[0].expectedArrival).isEqualTo(placementDates[0].expectedArrival)
          }
        }
      }
    }
  }

  @Nested
  inner class CreatePlacementApplicationDecisionTest {

    @Test
    fun `submitting a placement request application decision without a JWT returns 401`() {
      webTestClient.post()
        .uri("/placement-applications/${UUID.randomUUID()}/decision")
        .bodyValue(
          PlacementApplicationDecisionEnvelope(
            decision = PlacementApplicationDecision.accepted,
            summaryOfChanges = "ChangeSummary",
            decisionSummary = "DecisionSummary",
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `submitting a placement application decision when the placement application does not exist returns 404`() {
      `Given a User` { _, jwt ->
        webTestClient.post()
          .uri("/placement-applications/${UUID.randomUUID()}/decision")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            PlacementApplicationDecisionEnvelope(
              decision = PlacementApplicationDecision.accepted,
              summaryOfChanges = "ChangeSummary",
              decisionSummary = "DecisionSummary",
            ),
          )
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `submitting a placement request application decision with a decision already set returns an error`() {
      `Given a User` { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          `Given a submitted Placement Application`(
            allocatedToUser = user,
            offenderDetails = offenderDetails,
            decision = JpaPlacementApplicationDecision.REJECTED,
          ) { placementApplicationEntity ->
            webTestClient.post()
              .uri("/placement-applications/${placementApplicationEntity.id}/decision")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                PlacementApplicationDecisionEnvelope(
                  decision = PlacementApplicationDecision.accepted,
                  summaryOfChanges = "ChangeSummary",
                  decisionSummary = "DecisionSummary",
                ),
              )
              .exchange()
              .expectStatus()
              .isBadRequest
          }
        }
      }
    }

    @Test
    fun `submitting a placement request application that is not assigned to me returns an error`() {
      `Given a User` { _, jwt ->
        `Given a User` { otherUser, _ ->
          `Given an Offender` { offenderDetails, _ ->
            `Given a submitted Placement Application`(
              allocatedToUser = otherUser,
              offenderDetails = offenderDetails,
              decision = JpaPlacementApplicationDecision.REJECTED,
            ) { placementApplicationEntity ->
              webTestClient.post()
                .uri("/placement-applications/${placementApplicationEntity.id}/decision")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  PlacementApplicationDecisionEnvelope(
                    decision = PlacementApplicationDecision.accepted,
                    summaryOfChanges = "ChangeSummary",
                    decisionSummary = "DecisionSummary",
                  ),
                )
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }
    }

    @Test
    fun `submitting a placement application decision when the placement requirements do not exist returns 404 and does not update the decision`() {
      `Given a User` { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          `Given a submitted Placement Application`(allocatedToUser = user, offenderDetails = offenderDetails) { placementApplicationEntity ->
            webTestClient.post()
              .uri("/placement-applications/${placementApplicationEntity.id}/decision")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                PlacementApplicationDecisionEnvelope(
                  decision = PlacementApplicationDecision.accepted,
                  summaryOfChanges = "ChangeSummary",
                  decisionSummary = "DecisionSummary",
                ),
              )
              .exchange()
              .expectStatus()
              .isNotFound

            val updatedPlacementApplication =
              placementApplicationRepository.findByIdOrNull(placementApplicationEntity.id)!!

            assertThat(updatedPlacementApplication.decision).isEqualTo(null)
          }
        }
      }
    }

    @Test
    fun `submitting a placement application decision when the placement dates do not exist returns 404 and does not update the decision`() {
      `Given a User` { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          `Given a submitted Placement Application`(allocatedToUser = user, offenderDetails = offenderDetails) { placementApplicationEntity ->
            `Given placement requirements`(placementApplicationEntity = placementApplicationEntity) { _ ->
              webTestClient.post()
                .uri("/placement-applications/${placementApplicationEntity.id}/decision")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  PlacementApplicationDecisionEnvelope(
                    decision = PlacementApplicationDecision.accepted,
                    summaryOfChanges = "ChangeSummary",
                    decisionSummary = "DecisionSummary",
                  ),
                )
                .exchange()
                .expectStatus()
                .isNotFound

              val updatedPlacementApplication =
                placementApplicationRepository.findByIdOrNull(placementApplicationEntity.id)!!

              assertThat(updatedPlacementApplication.decision).isEqualTo(null)
            }
          }
        }
      }
    }

    @ParameterizedTest
    @CsvSource("ROTL,false", "ADDITIONAL_PLACEMENT,false", "RELEASE_FOLLOWING_DECISION,true")
    fun `accepting a placement request application decision records the decision and creates and assigns a placement request`(placementType: JpaPlacementType, isParole: Boolean) {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { matcher1, _ ->
        `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { matcher2, _ ->
          `Given a User` { user, jwt ->
            `Given an Offender` { offenderDetails, _ ->
              `Given a submitted Placement Application`(allocatedToUser = user, offenderDetails = offenderDetails, placementType = placementType) { placementApplicationEntity ->
                `Given placement requirements`(placementApplicationEntity = placementApplicationEntity, createdAt = OffsetDateTime.now()) { placementRequirements ->
                  `Given placement requirements`(placementApplicationEntity = placementApplicationEntity, createdAt = OffsetDateTime.now().minusDays(4)) { _ ->
                    `Given placement dates`(placementApplicationEntity = placementApplicationEntity) { placementDates ->
                      GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

                      webTestClient.post()
                        .uri("/placement-applications/${placementApplicationEntity.id}/decision")
                        .header("Authorization", "Bearer $jwt")
                        .bodyValue(
                          PlacementApplicationDecisionEnvelope(
                            decision = PlacementApplicationDecision.accepted,
                            summaryOfChanges = "ChangeSummary",
                            decisionSummary = "DecisionSummary",
                          ),
                        )
                        .exchange()
                        .expectStatus()
                        .isOk

                      val updatedPlacementApplication =
                        placementApplicationRepository.findByIdOrNull(placementApplicationEntity.id)!!

                      assertThat(updatedPlacementApplication.decision).isEqualTo(JpaPlacementApplicationDecision.ACCEPTED)
                      assertThat(updatedPlacementApplication.decisionMadeAt).isWithinTheLastMinute()

                      val createdPlacementRequests =
                        placementRequestTestRepository.findAllByApplication(placementApplicationEntity.application)

                      assertThat(createdPlacementRequests.size).isEqualTo(1)

                      val createdPlacementApplication = createdPlacementRequests[0]
                      assertThat(updatedPlacementApplication.placementDates[0].placementRequest!!.id).isEqualTo(createdPlacementApplication.id)

                      assertThat(createdPlacementApplication.allocatedToUser!!.id).isIn(listOf(matcher1.id, matcher2.id))
                      assertThat(createdPlacementApplication.application.id).isEqualTo(placementApplicationEntity.application.id)
                      assertThat(createdPlacementApplication.expectedArrival).isEqualTo(placementDates.expectedArrival)
                      assertThat(createdPlacementApplication.duration).isEqualTo(placementDates.duration)
                      assertThat(createdPlacementApplication.isParole).isEqualTo(isParole)
                      assertThat(createdPlacementApplication.placementRequirements.id).isEqualTo(placementRequirements.id)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    private fun `Given a submitted Placement Application`(
      allocatedToUser: UserEntity,
      offenderDetails: OffenderDetailSummary,
      decision: JpaPlacementApplicationDecision? = null,
      placementType: JpaPlacementType? = JpaPlacementType.ADDITIONAL_PLACEMENT,
      block: (placementApplicationEntity: PlacementApplicationEntity) -> Unit,
    ) {
      val placementApplication = `Given a Placement Application`(
        allocatedToUser = allocatedToUser,
        createdByUser = userEntityFactory.produceAndPersist {
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist {
              withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
            }
          }
        },
        schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
        submittedAt = OffsetDateTime.now(),
        crn = offenderDetails.otherIds.crn,
        decision = decision,
        reallocated = false,
        placementType = placementType,
      )

      block(placementApplication)
    }

    private fun `Given placement requirements`(
      placementApplicationEntity: PlacementApplicationEntity,
      createdAt: OffsetDateTime = OffsetDateTime.now(),
      block: (placementRequirements: PlacementRequirementsEntity) -> Unit,
    ) {
      val placementRequirements = placementRequirementsFactory.produceAndPersist {
        withApplication(placementApplicationEntity.application)
        withAssessment(placementApplicationEntity.application.getLatestAssessment()!!)
        withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
        withDesirableCriteria(
          characteristicEntityFactory.produceAndPersistMultiple(5),
        )
        withEssentialCriteria(
          characteristicEntityFactory.produceAndPersistMultiple(3),
        )
        withCreatedAt(createdAt)
      }

      block(placementRequirements)
    }

    private fun `Given placement dates`(
      placementApplicationEntity: PlacementApplicationEntity,
      block: (placementDates: PlacementDateEntity) -> Unit,
    ) {
      block(
        placementDateFactory.produceAndPersist {
          withPlacementApplication(placementApplicationEntity)
        },
      )
    }
  }

  /**
   * Note - Withdrawal cascading is tested in [WithdrawalTest]
   */
  @Nested
  inner class WithdrawPlacementApplicationTest {

    @Test
    fun `withdrawing a placement application JWT returns 401`() {
      webTestClient.post()
        .uri("/placement-applications/${UUID.randomUUID()}/withdraw")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `withdrawing a placement application decision when the placement application does not exist returns 404`() {
      `Given a User` { _, jwt ->
        webTestClient.post()
          .uri("/placement-applications/${UUID.randomUUID()}/withdraw")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            WithdrawPlacementApplication(WithdrawPlacementRequestReason.duplicatePlacementRequest),
          )
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `withdrawing a placement application created by a different user returns an error`() {
      `Given a User` { _, jwt ->
        `Given a Placement Application`(
          createdByUser = userEntityFactory.produceAndPersist {
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist {
                withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
              }
            }
          },
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
          submittedAt = OffsetDateTime.now(),
        ) { placementApplicationEntity ->

          webTestClient.post()
            .uri("/placement-applications/${placementApplicationEntity.id}/withdraw")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              WithdrawPlacementApplication(WithdrawPlacementRequestReason.duplicatePlacementRequest),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `withdrawing an in-progress placement application returns successfully and updates decision field`() {
      `Given a User` { user, jwt ->
        `Given a Placement Application`(
          createdByUser = user,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        ) { placementApplicationEntity ->
          CommunityAPI_mockSuccessfulOffenderDetailsCall(
            OffenderDetailsSummaryFactory()
              .withCrn(placementApplicationEntity.application.crn)
              .produce(),
          )

          val rawResult = webTestClient.post()
            .uri("/placement-applications/${placementApplicationEntity.id}/withdraw")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              WithdrawPlacementApplication(WithdrawPlacementRequestReason.duplicatePlacementRequest),
            )
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)

          assertThat(body).matches {
            placementApplicationEntity.id == it.id &&
              placementApplicationEntity.application.id == it.applicationId &&
              placementApplicationEntity.createdByUser.id == it.createdByUserId &&
              placementApplicationEntity.schemaVersion.id == it.schemaVersion &&
              placementApplicationEntity.createdAt.toInstant() == it.createdAt &&
              serializableToJsonNode(placementApplicationEntity.document) == serializableToJsonNode(it.document)
          }

          val updatedPlacementApplication =
            placementApplicationRepository.findByIdOrNull(placementApplicationEntity.id)!!

          assertThat(updatedPlacementApplication.decision).isEqualTo(uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision.WITHDRAW)
          assertThat(updatedPlacementApplication.decisionMadeAt).isWithinTheLastMinute()
        }
      }
    }

    @Test
    fun `withdrawing a submitted placement application as the applicant sends emails, raises a domain event and returns successfully`() {
      `Given a User` { applicant, jwt ->

        `Given a User` { assessor, _ ->
          `Given a Placement Application`(
            createdByUser = applicant,
            schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
            decision = null,
            submittedAt = OffsetDateTime.now(),
            allocatedToUser = assessor,
          ) { placementApplicationEntity ->

            CommunityAPI_mockSuccessfulOffenderDetailsCall(
              OffenderDetailsSummaryFactory()
                .withCrn(placementApplicationEntity.application.crn)
                .produce(),
            )

            val application = placementApplicationEntity.application

            val rawResult = webTestClient.post()
              .uri("/placement-applications/${placementApplicationEntity.id}/withdraw")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                WithdrawPlacementApplication(WithdrawPlacementRequestReason.duplicatePlacementRequest),
              )
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)

            assertThat(body).matches {
              placementApplicationEntity.id == it.id &&
                application.id == it.applicationId &&
                placementApplicationEntity.createdByUser.id == it.createdByUserId &&
                placementApplicationEntity.schemaVersion.id == it.schemaVersion &&
                placementApplicationEntity.createdAt.toInstant() == it.createdAt &&
                serializableToJsonNode(placementApplicationEntity.document) == serializableToJsonNode(it.document)
            }

            val emittedMessage = snsDomainEventListener.blockForMessage()
            assertThat(emittedMessage.eventType).isEqualTo("approved-premises.placement-application.withdrawn")

            emailAsserter.assertEmailsRequestedCount(2)
            emailAsserter.assertEmailRequested(
              placementApplicationEntity.createdByUser.email!!,
              notifyConfig.templates.placementRequestWithdrawn,
            )
            emailAsserter.assertEmailRequested(
              placementApplicationEntity.allocatedToUser!!.email!!,
              notifyConfig.templates.placementRequestWithdrawn,
            )
          }
        }
      }
    }

    @Test
    fun `withdrawing a submitted placement application as a workflow manager sends emails, raises a domain event and returns successfully`() {
      `Given a User` { applicant, _ ->
        `Given a User` { assessor, _ ->
          `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
            `Given a Placement Application`(
              createdByUser = applicant,
              schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
              decision = null,
              submittedAt = OffsetDateTime.now(),
              allocatedToUser = assessor,
            ) { placementApplicationEntity ->

              CommunityAPI_mockSuccessfulOffenderDetailsCall(
                OffenderDetailsSummaryFactory()
                  .withCrn(placementApplicationEntity.application.crn)
                  .produce(),
              )

              val application = placementApplicationEntity.application

              val rawResult = webTestClient.post()
                .uri("/placement-applications/${placementApplicationEntity.id}/withdraw")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  WithdrawPlacementApplication(WithdrawPlacementRequestReason.duplicatePlacementRequest),
                )
                .exchange()
                .expectStatus()
                .isOk
                .returnResult<String>()
                .responseBody
                .blockFirst()

              val body = objectMapper.readValue(rawResult, PlacementApplication::class.java)

              assertThat(body).matches {
                placementApplicationEntity.id == it.id &&
                  application.id == it.applicationId &&
                  placementApplicationEntity.createdByUser.id == it.createdByUserId &&
                  placementApplicationEntity.schemaVersion.id == it.schemaVersion &&
                  placementApplicationEntity.createdAt.toInstant() == it.createdAt &&
                  serializableToJsonNode(placementApplicationEntity.document) == serializableToJsonNode(it.document)
              }

              val emittedMessage = snsDomainEventListener.blockForMessage()
              assertThat(emittedMessage.eventType).isEqualTo("approved-premises.placement-application.withdrawn")

              emailAsserter.assertEmailsRequestedCount(2)
              emailAsserter.assertEmailRequested(
                placementApplicationEntity.createdByUser.email!!,
                notifyConfig.templates.placementRequestWithdrawn,
              )
              emailAsserter.assertEmailRequested(
                placementApplicationEntity.allocatedToUser!!.email!!,
                notifyConfig.templates.placementRequestWithdrawn,
              )
            }
          }
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
