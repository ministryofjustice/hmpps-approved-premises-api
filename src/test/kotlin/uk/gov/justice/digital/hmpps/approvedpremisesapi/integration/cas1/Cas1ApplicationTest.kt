package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventCas
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.DomainEventSummaryImpl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.Cas1DomainEventsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus as ApiApprovedPremisesApplicationStatus

class Cas1ApplicationTest : IntegrationTestBase() {

  @Autowired
  lateinit var cas1applicationTimelineTransformer: Cas1ApplicationTimelineTransformer

  @Autowired
  lateinit var applicationTimelineNoteTransformer: ApplicationTimelineNoteTransformer

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
          val applicationId1 = UUID.randomUUID()
          val applicationId2 = UUID.randomUUID()

          approvedPremisesApplicationEntityFactory.produceAndPersist {
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

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(crn)
            withCreatedByUser(userEntity)
            withCreatedAt(date1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(crn)
            withCreatedByUser(userEntity)
            withCreatedAt(date2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
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
          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(crn)
            withCreatedByUser(userEntity)
            withCreatedAt(date1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(crn)
            withCreatedByUser(userEntity)
            withCreatedAt(date2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
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

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
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

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
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
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date1)
            withRiskRatings(risk1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date2)
            withRiskRatings(risk2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
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
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date1)
            withRiskRatings(risk1)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(crn)
            withCreatedByUser(userEntity)
            withArrivalDate(date2)
            withRiskRatings(risk2)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersist {
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

          approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(12) {
            withCrn(crn)
            withCreatedByUser(userEntity)
            withStatus(ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS)
          }

          approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(12) {
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

            approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(12) {
              withName("Gareth")
              withCrn(crn1)
              withCreatedByUser(userEntity)
              withStatus(ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS)
            }

            approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(12) {
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

          val sortedDates = createdApplications.map { it.createdAt.toInstant() }.sorted()

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
  inner class GetApplicationTimeline : InitialiseDatabasePerClassTestBase() {

    lateinit var user: UserEntity
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var assessment: ApprovedPremisesAssessmentEntity
    lateinit var domainEvents: List<DomainEventEntity>
    lateinit var notes: List<ApplicationTimelineNoteEntity>

    @BeforeAll
    fun setup() {
      val userArgs = givenAUser()

      user = userArgs.first

      application = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(user)
      }

      val otherApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(user)
      }

      assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
        withApplication(application)
        withAllocatedToUser(user)
      }

      val otherAssessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
        withApplication(otherApplication)
        withAllocatedToUser(user)
      }

      domainEvents = DomainEventType.entries
        .filter { it.cas == DomainEventCas.CAS1 }
        .filter { it != DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED }.map {
          createDomainEvent(it, otherApplication, otherAssessment, user)
          return@map createDomainEvent(it, application, assessment, user)
        }

      notes = createTimelineNotes(application, 5, isDeleted = false)
      createTimelineNotes(application, 2, isDeleted = true)
      createTimelineNotes(otherApplication, 3, isDeleted = false)
    }

    @Test
    fun `Get application timeline without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/applications/${application.id}/timeline")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get application timeline returns all expected items`() {
      givenAUser { _, jwt ->
        val rawResponseBody = webTestClient.get()
          .uri("/cas1/applications/${application.id}/timeline")
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
            object : TypeReference<List<Cas1TimelineEvent>>() {},
          )

        val expectedItems = mutableListOf<Cas1TimelineEvent>()

        expectedItems.addAll(
          domainEvents.map {
            cas1applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(
              DomainEventSummaryImpl(
                it.id.toString(),
                it.type,
                it.occurredAt,
                it.applicationId,
                it.assessmentId,
                null,
                null,
                null,
                null,
                it.triggerSource,
                user,
              ),
            )
          },
        )
        expectedItems.addAll(notes.map { applicationTimelineNoteTransformer.transformToCas1TimelineEvents(it) })

        assertThat(responseBody.count()).isEqualTo(expectedItems.count())
        assertThat(responseBody).hasSameElementsAs(expectedItems)
      }
    }

    private fun createDomainEvent(
      type: DomainEventType,
      applicationEntity: ApprovedPremisesApplicationEntity,
      assessmentEntity: ApprovedPremisesAssessmentEntity,
      userEntity: UserEntity,
    ): DomainEventEntity {
      val domainEventsFactory = Cas1DomainEventsFactory(objectMapper)

      val data = if (type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED) {
        val clarificationNote = assessmentClarificationNoteEntityFactory.produceAndPersist {
          withAssessment(assessmentEntity)
          withCreatedBy(userEntity)
        }

        domainEventsFactory.createEnvelopeForLatestSchemaVersion(type = type, requestId = clarificationNote.id)
      } else {
        domainEventsFactory.createEnvelopeForLatestSchemaVersion(type = type)
      }

      return domainEventFactory.produceAndPersist {
        withType(type)
        withApplicationId(applicationEntity.id)
        withData(data)
        withTriggeredByUserId(userEntity.id)
        withTriggerSourceSystem()
      }
    }

    private fun createTimelineNotes(
      applicationEntity: ApprovedPremisesApplicationEntity,
      count: Int,
      isDeleted: Boolean,
    ) = applicationTimelineNoteEntityFactory.produceAndPersistMultiple(count) {
      withApplicationId(applicationEntity.id)
      if (isDeleted) withDeletedAt(OffsetDateTime.now())
    }.toMutableList()
  }

  private fun createTwelveApplications(crn: String, user: UserEntity): List<ApprovedPremisesApplicationEntity> = approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(12) {
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
