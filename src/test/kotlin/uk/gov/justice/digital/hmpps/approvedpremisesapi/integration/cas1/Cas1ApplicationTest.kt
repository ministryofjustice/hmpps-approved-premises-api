package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FlagsEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MappaEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskEnvelopeStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisksEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UnknownPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TeamFactoryDeliusContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus as ApiApprovedPremisesApplicationStatus

class Cas1ApplicationTest : IntegrationTestBase() {

  @Nested
  inner class GetAllApplications {
    @Test
    fun `Get applications all without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/applications/all")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get applications all returns 200 and correct body`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          approvedPremisesApplicationJsonSchemaRepository.deleteAll()
          val applicationId1 = UUID.randomUUID()
          val applicationId2 = UUID.randomUUID()

          val newestJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
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
            .uri("/cas1/applications/all")
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
              object : TypeReference<List<Cas1ApplicationSummary>>() {},
            )

          assertThat(responseBody.count()).isEqualTo(2)
        }
      }
    }

    @Test
    fun `Get applications all LAO without qualification returns 200 and restricted person`() {
      givenAUser { userEntity, jwt ->

        val (offenderDetails, _) = givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentRestriction(true)
          },
        )

        approvedPremisesApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(
            approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          )
          withId(UUID.randomUUID())
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

        val response = webTestClient.get()
          .uri("/cas1/applications/all")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Cas1ApplicationSummary>()

        assertThat(response).hasSize(1)
        assertThat(response[0].person).isInstanceOf(RestrictedPerson::class.java)
      }
    }

    @Test
    fun `Get applications all LAO with LAO qualification returns 200 and full person`() {
      givenAUser(qualifications = listOf(UserQualification.LAO)) { userEntity, jwt ->
        val (offenderDetails, _) = givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentRestriction(true)
          },
        )

        approvedPremisesApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(
            approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          )
          withId(UUID.randomUUID())
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

        val response = webTestClient.get()
          .uri("/cas1/applications/all")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Cas1ApplicationSummary>()

        assertThat(response).hasSize(1)
        assertThat(response[0].person).isInstanceOf(FullPerson::class.java)
      }
    }

    @Test
    fun `Get applications all returns twelve items if no page parameter passed`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          createTwelveApplications(offenderDetails.otherIds.crn, userEntity)

          val responseBody = webTestClient.get()
            .uri("/cas1/applications/all")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<Cas1ApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(12)
        }
      }
    }

    @Test
    fun `Get applications all returns ten items if page parameter passed is one`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          createTwelveApplications(offenderDetails.otherIds.crn, userEntity)

          val responseBody = webTestClient.get()
            .uri("/cas1/applications/all?page=1&sortDirection=asc")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<Cas1ApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(10)
        }
      }
    }

    @Test
    fun `Get applications all returns twelve items if crn parameter passed and no page`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          createTwelveApplications(crn, userEntity)

          val responseBody = webTestClient.get()
            .uri("/cas1/applications/all?crnOrName=$crn")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<Cas1ApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(12)
        }
      }
    }

    @Test
    fun `Get applications all returns twelve items if crn parameter passed and two crns in db no page`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAnOffender { offenderDetails2, _ ->

            val crn1 = offenderDetails.otherIds.crn
            createTwelveApplications(crn1, userEntity)

            val crn2 = offenderDetails2.otherIds.crn
            createTwelveApplications(crn2, userEntity)

            val responseBody = webTestClient.get()
              .uri("/cas1/applications/all?crnOrName=$crn2")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .bodyAsListOfObjects<Cas1ApplicationSummary>()

            assertThat(responseBody.count()).isEqualTo(12)
          }
        }
      }
    }

    @Test
    fun `Get applications all returns two items if crn parameter passed and two crns in db and page one`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAnOffender { offenderDetails2, _ ->

            val crn1 = offenderDetails.otherIds.crn
            createTwelveApplications(crn1, userEntity)

            val crn2 = offenderDetails2.otherIds.crn
            createTwelveApplications(crn2, userEntity)

            val responseBody = webTestClient.get()
              .uri("/cas1/applications/all?page=2&sortDirection=desc&crnOrName=$crn2")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
              .expectHeader().valueEquals("X-Pagination-PageSize", 10)
              .bodyAsListOfObjects<Cas1ApplicationSummary>()

            assertThat(responseBody.count()).isEqualTo(2)
          }
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by createdAt desc`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
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

          val responseBody = webTestClient.get()
            .uri("/cas1/applications/all?page=1&sortDirection=desc&sortBy=createdAt")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<Cas1ApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].createdAt).isEqualTo(date3.toInstant())
          assertThat(responseBody[1].createdAt).isEqualTo(date2.toInstant())
          assertThat(responseBody[2].createdAt).isEqualTo(date1.toInstant())
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by createdAt asc`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
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

          val responseBody = webTestClient.get()
            .uri("/cas1/applications/all?page=1&sortDirection=asc&sortBy=createdAt")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<Cas1ApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].createdAt).isEqualTo(date1.toInstant())
          assertThat(responseBody[1].createdAt).isEqualTo(date2.toInstant())
          assertThat(responseBody[2].createdAt).isEqualTo(date3.toInstant())
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by arrivalDate desc`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
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

          val responseBody = webTestClient.get()
            .uri("/cas1/applications/all?page=1&sortDirection=desc&sortBy=arrivalDate")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<Cas1ApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].arrivalDate).isEqualTo("2022-12-24")
          assertThat(responseBody[1].arrivalDate).isEqualTo("2022-09-24")
          assertThat(responseBody[2].arrivalDate).isEqualTo("2022-08-24")
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by arrivalDate asc`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
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

          val responseBody = webTestClient.get()
            .uri("/cas1/applications/all?page=1&sortDirection=asc&sortBy=arrivalDate")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<Cas1ApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].arrivalDate).isEqualTo("2022-08-24")
          assertThat(responseBody[1].arrivalDate).isEqualTo("2022-09-24")
          assertThat(responseBody[2].arrivalDate).isEqualTo("2022-12-24")
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by tier asc`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
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

          val responseBody = webTestClient.get()
            .uri("/cas1/applications/all?page=1&sortDirection=asc&sortBy=tier")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<Cas1ApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].tier).isEqualTo("M1")
          assertThat(responseBody[1].tier).isEqualTo("M2")
          assertThat(responseBody[2].tier).isEqualTo("M3")
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page one and sorted by tier desc`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
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

          val responseBody = webTestClient.get()
            .uri("/cas1/applications/all?page=1&sortDirection=desc&sortBy=tier")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 3)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<Cas1ApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(3)
          assertThat(responseBody[0].tier).isEqualTo("M3")
          assertThat(responseBody[1].tier).isEqualTo("M2")
          assertThat(responseBody[2].tier).isEqualTo("M1")
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body and page two and query by status`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
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

          val responseBody = webTestClient.get()
            .uri("/cas1/applications/all?page=2&sortDirection=desc&status=assesmentInProgress")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
            .expectHeader().valueEquals("X-Pagination-PageSize", 10)
            .bodyAsListOfObjects<Cas1ApplicationSummary>()

          assertThat(responseBody.count()).isEqualTo(2)
          assertThat(responseBody[0].status).isEqualTo(ApiApprovedPremisesApplicationStatus.assesmentInProgress)
        }
      }
    }

    @Test
    fun `Get applications all returns 200 correct body for a given name`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAnOffender { offenderDetails2, _ ->

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

            val responseBody = webTestClient.get()
              .uri("/cas1/applications/all?page=1&sortDirection=desc&crnOrName=Gareth")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
              .expectHeader().valueEquals("X-Pagination-PageSize", 10)
              .bodyAsListOfObjects<Cas1ApplicationSummary>()

            assertThat(responseBody.count()).isEqualTo(10)
          }
        }
      }
    }
  }

  @Nested
  inner class GetApplicationsMe {
    @Test
    fun `Get applications me without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/applications/me")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get applications me returns twelve items sorted by created_at asc`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val crn = offenderDetails.otherIds.crn
          val createdApplications = createTwelveApplications(crn, userEntity)

          val anotherUser = givenAUser().first
          createTwelveApplications(crn, anotherUser)

          val responseBody = webTestClient.get()
            .uri("/cas1/applications/me")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<Cas1ApplicationSummary>()

          val sortedDates = createdApplications.map { it.createdAt.toString() }.sorted()

          assertThat(responseBody.count()).isEqualTo(12)

          responseBody.forEachIndexed { index, element ->
            assertThat(element.createdAt).isEqualTo(sortedDates[index])
          }
        }
      }
    }

    @Test
    fun `Get applications me LAO without qualification returns 200 and restricted person`() {
      givenAUser { userEntity, jwt ->

        val (offenderDetails, _) = givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentRestriction(true)
          },
        )

        approvedPremisesApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(
            approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          )
          withId(UUID.randomUUID())
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

        val response = webTestClient.get()
          .uri("/cas1/applications/me")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Cas1ApplicationSummary>()

        assertThat(response).hasSize(1)
        assertThat(response[0].person).isInstanceOf(RestrictedPerson::class.java)
      }
    }

    @Test
    fun `Get applications me LAO with LAO qualification returns 200 and full person`() {
      givenAUser(qualifications = listOf(UserQualification.LAO)) { userEntity, jwt ->
        val (offenderDetails, _) = givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentRestriction(true)
          },
        )

        approvedPremisesApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(
            approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          )
          withId(UUID.randomUUID())
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

        val response = webTestClient.get()
          .uri("/cas1/applications/me")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Cas1ApplicationSummary>()

        assertThat(response).hasSize(1)
        assertThat(response[0].person).isInstanceOf(FullPerson::class.java)
      }
    }
  }

  @Nested
  inner class GetApplications {

    @Test
    fun `Get all applications without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/applications")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get all applications returns 200 - when user has no roles returns applications managed by their teams`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(teams = listOf(TeamFactoryDeliusContext.team(code = "TEAM1"))),
      ) { userEntity, jwt ->
        givenAUser { otherUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            givenAnOffender { otherOffenderDetails, _ ->
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

              val outdatedApplicationEntityNotManagedByTeam =
                approvedPremisesApplicationEntityFactory.produceAndPersist {
                  withApplicationSchema(olderJsonSchema)
                  withCreatedByUser(otherUser)
                  withCrn(otherOffenderDetails.otherIds.crn)
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

              val responseBody = webTestClient.get()
                .uri("/cas1/applications")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .bodyAsListOfObjects<Cas1ApplicationSummary>()

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
    fun `Get all applications returns limited information when a person cannot be found`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(teams = listOf(TeamFactoryDeliusContext.team(code = "TEAM1"))),
      ) { userEntity, jwt ->
        val crn = "X1234"

        val application = produceAndPersistBasicApplication(crn, userEntity, "TEAM1")

        apDeliusContextMockUserAccess(
          CaseAccessFactory()
            .withCrn(crn)
            .produce(),
          userEntity.deliusUsername,
        )

        webTestClient.get()
          .uri("/cas1/applications")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              listOf(
                Cas1ApplicationSummary(
                  createdByUserId = userEntity.id,
                  status = ApiApprovedPremisesApplicationStatus.started,
                  id = application.id,
                  person = UnknownPerson(
                    crn = crn,
                    type = PersonType.unknownPerson,
                  ),
                  createdAt = application.createdAt.toInstant(),
                  isWomensApplication = null,
                  isPipeApplication = false,
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
                  hasRequestsForPlacement = false,
                ),
              ),
            ),
          )
      }
    }

    @Test
    fun `Get all applications returns successfully when a person has no NOMS number`() {
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

          val responseBody = webTestClient.get()
            .uri("/cas1/applications")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<Cas1ApplicationSummary>()

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
    fun `Get all applications returns successfully when the person cannot be fetched from the prisons API`() {
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

          val responseBody = webTestClient.get()
            .uri("/cas1/applications")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<Cas1ApplicationSummary>()

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
  }

  private fun produceAndPersistBasicApplication(
    crn: String,
    userEntity: UserEntity,
    managingTeamCode: String,
    submittedAt: OffsetDateTime? = null,
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

  private fun createTwelveApplications(crn: String, user: UserEntity): List<ApprovedPremisesApplicationEntity> {
    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    return approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(12) {
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
