package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus.cas1AwaitingResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus.cas1Completed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus.cas1InProgress
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus.cas1NotStarted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus.cas1Reallocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus.AWAITING_RESPONSE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus.COMPLETED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asOffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class Cas1AssessmentTest : IntegrationTestBase() {
  @Autowired
  lateinit var assessmentTransformer: AssessmentTransformer

  @Nested
  inner class GetAllAssessments {

    @Test
    fun `Get all assessments without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/assessments")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource
    @NullSource
    fun `Get all assessments returns 200 with correct body`(assessmentDecision: AssessmentDecision?) {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
            withAddedAt(OffsetDateTime.now())
          }

          val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
          }

          val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withAllocatedToUser(user)
            withApplication(application)
            withAssessmentSchema(assessmentSchema)
            withDecision(assessmentDecision)
          }

          assessment.schemaUpToDate = true

          val reallocatedAssessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withAllocatedToUser(user)
            withApplication(application)
            withAssessmentSchema(assessmentSchema)
            withReallocatedAt(OffsetDateTime.now())
          }

          reallocatedAssessment.schemaUpToDate = true

          val withdrawnAssessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withAllocatedToUser(user)
            withApplication(application)
            withAssessmentSchema(assessmentSchema)
            withDecision(assessmentDecision)
            withIsWithdrawn(true)
          }

          withdrawnAssessment.schemaUpToDate = true

          val responseStatus = when (assessmentDecision) {
            AssessmentDecision.ACCEPTED -> COMPLETED
            AssessmentDecision.REJECTED -> COMPLETED
            else -> IN_PROGRESS
          }

          assertResponseForUrl(
            jwt,
            "/cas1/assessments",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummaries(assessment, status = responseStatus),
            ),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Approved Premises filters correctly when status is not defined`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val inProgress1 = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1InProgress)
          val awaitingResponse1 = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1AwaitingResponse)
          val notStarted1 = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1NotStarted)
          val completed1 = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1Completed)
          val inProgress2 = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1InProgress)
          val awaitingResponse2 = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1AwaitingResponse)
          val notStarted2 = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1NotStarted)
          val completed2 = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1Completed)

          val mapper = assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            listOf(
              mapper.toSummary(inProgress1, IN_PROGRESS),
              mapper.toSummary(awaitingResponse1, AWAITING_RESPONSE),
              mapper.toSummary(notStarted1, NOT_STARTED),
              mapper.toSummary(completed1, COMPLETED),
              mapper.toSummary(inProgress2, IN_PROGRESS),
              mapper.toSummary(awaitingResponse2, AWAITING_RESPONSE),
              mapper.toSummary(notStarted2, NOT_STARTED),
              mapper.toSummary(completed2, COMPLETED),
            ),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = emptyList(),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Approved Premises filters correctly when status is cas1NotStarted`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1InProgress)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1AwaitingResponse)
          val notStarted1 = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1NotStarted)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1Completed)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1InProgress)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1AwaitingResponse)
          val notStarted2 = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1NotStarted)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1Completed)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummaries(
              notStarted1,
              notStarted2,
              status = NOT_STARTED,
            ),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(cas1NotStarted),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Approved Premises filters correctly when status is cas1InProgress`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val inProgress1 = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1InProgress)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1AwaitingResponse)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1NotStarted)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1Completed)
          val inProgress2 = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1InProgress)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1AwaitingResponse)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1NotStarted)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1Completed)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummaries(
              inProgress1,
              inProgress2,
              status = IN_PROGRESS,
            ),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(cas1InProgress),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Approved Premises filters correctly when status is cas1AwaitingResponse`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1InProgress)
          val awaitingResponse1 = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1AwaitingResponse)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1NotStarted)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1Completed)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1InProgress)
          val awaitingResponse2 = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1AwaitingResponse)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1NotStarted)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1Completed)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummaries(
              awaitingResponse1,
              awaitingResponse2,
              status = AWAITING_RESPONSE,
            ),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(cas1AwaitingResponse),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Approved Premises filters correctly when status is cas1Completed`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1InProgress)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1AwaitingResponse)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1NotStarted)
          val completed1 = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1Completed)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1InProgress)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1AwaitingResponse)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1NotStarted)
          val completed2 = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1Completed)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummaries(
              completed1,
              completed2,
              status = COMPLETED,
            ),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(cas1Completed),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Approved Premises filters correctly when status is cas1Reallocated (none returned)`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1InProgress)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1AwaitingResponse)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1NotStarted)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1Completed)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1InProgress)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1AwaitingResponse)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1NotStarted)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1Completed)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            emptyList(),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(cas1Reallocated),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Approved Premises filters correctly for multiple statuses`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1InProgress)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1AwaitingResponse)
          val notStarted1 =
            createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1NotStarted)
          val completed1 =
            createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1Completed)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1InProgress)
          createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1AwaitingResponse)
          val notStarted2 =
            createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1NotStarted)
          val completed2 =
            createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1Completed)

          val mapper = assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            listOf(
              mapper.toSummary(notStarted1, NOT_STARTED),
              mapper.toSummary(completed1, COMPLETED),
              mapper.toSummary(notStarted2, NOT_STARTED),
              mapper.toSummary(completed2, COMPLETED),
            ),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(cas1NotStarted, cas1Completed),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Approved Premises filters correctly when 'page' query parameter is provided`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val veryOld = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1InProgress,
            applicationMutator = { withArrivalDate(OffsetDateTime.now().minusDays(10)) },
          )

          val veryNew = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1InProgress,
            applicationMutator = { withArrivalDate(OffsetDateTime.now().minusDays(2)) },
          )

          val old = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1InProgress,
            applicationMutator = { withArrivalDate(OffsetDateTime.now().minusDays(5)) },
          )

          val new = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1AwaitingResponse,
            applicationMutator = { withArrivalDate(OffsetDateTime.now().minusDays(3)) },
          )

          val ancient = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1InProgress,
            applicationMutator = { withArrivalDate(OffsetDateTime.now().minusDays(99)) },
          )

          val mapper = assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails)

          val page1Response = assertResponseForUrl(
            jwt,
            "/cas1/assessments?page=1&sortBy=${AssessmentSortField.assessmentArrivalDate.value}&perPage=2&sortDirection=desc",
            ExpectedResponse.OK(
              listOf(
                mapper.toSummary(veryNew, status = IN_PROGRESS),
                mapper.toSummary(new, status = AWAITING_RESPONSE),
              ),
            ),
          )

          page1Response.expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 3)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 5)
            .expectHeader().valueEquals("X-Pagination-PageSize", 2)

          val page2Response = assertResponseForUrl(
            jwt,
            "/cas1/assessments?page=2&sortBy=${AssessmentSortField.assessmentArrivalDate.value}&perPage=2&sortDirection=desc",
            ExpectedResponse.OK(
              listOf(
                mapper.toSummary(old, status = IN_PROGRESS),
                mapper.toSummary(veryOld, status = IN_PROGRESS),
              ),
            ),
          )

          page2Response.expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 3)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 5)
            .expectHeader().valueEquals("X-Pagination-PageSize", 2)

          val page3Response = assertResponseForUrl(
            jwt,
            "/cas1/assessments?page=3&sortBy=${AssessmentSortField.assessmentArrivalDate.value}&perPage=2&sortDirection=desc",
            ExpectedResponse.OK(
              listOf(
                mapper.toSummary(ancient, status = IN_PROGRESS),
              ),
            ),
          )

          page3Response.expectHeader().valueEquals("X-Pagination-CurrentPage", 3)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 3)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 5)
            .expectHeader().valueEquals("X-Pagination-PageSize", 2)
        }
      }
    }

    @Test
    fun `Get all assessments for Approved Premises sorts correctly when sortBy is createdAt`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val new = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1InProgress,
            assessmentMutator = {
              withCreatedAt(
                OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
              )
            },
          )

          val veryNew = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1InProgress,
            assessmentMutator = {
              withCreatedAt(
                OffsetDateTime.now().plusDays(1).roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
              )
            },
          )

          val old = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1InProgress,
            assessmentMutator = {
              withCreatedAt(
                OffsetDateTime.now().minusDays(1).roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
              )
            },
          )

          val veryOld = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1InProgress,
            assessmentMutator = {
              withCreatedAt(
                OffsetDateTime.now().minusDays(5).roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
              )
            },
          )

          assertAssessmentsReturnedGivenStatus(
            jwt,
            assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummaries(
              veryOld,
              old,
              new,
              veryNew,
              status = IN_PROGRESS,
            ),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(cas1InProgress),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Approved Premises sorts correctly when sortBy is status`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val inProgress = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1InProgress)
          val completed = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1Completed)
          val awaitingResponse = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1AwaitingResponse)
          val notStarted = createApprovedPremisesAssessmentForStatus(user, offenderDetails.asCaseSummary(), cas1NotStarted)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            listOf(
              assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummary(
                awaitingResponse,
                status = AWAITING_RESPONSE,
              ),
              assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummary(completed, status = COMPLETED),
              assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummary(inProgress, status = IN_PROGRESS),
              assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummary(notStarted, status = NOT_STARTED),
            ),
            sortBy = AssessmentSortField.assessmentStatus,
            status = listOf(cas1NotStarted, cas1Reallocated, cas1InProgress, cas1Completed, cas1AwaitingResponse),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Approved Premises sorts correctly when sortBy is personName`() {
      givenAUser { user, jwt ->
        val (offender1, inmate1) = givenAnOffender({
          withFirstName("Zendaya")
          withLastName("")
        })
        val assessZendaya = createApprovedPremisesAssessmentForStatus(user, offender1.asCaseSummary(), cas1InProgress)

        val (offender2, inmate2) = givenAnOffender({
          withFirstName("Arthur")
          withLastName("")
        })
        val assessArthur = createApprovedPremisesAssessmentForStatus(user, offender2.asCaseSummary(), cas1InProgress)

        val (offender3, inmate3) = givenAnOffender({
          withFirstName("Agatha")
          withLastName("")
        })
        val assessAgatha = createApprovedPremisesAssessmentForStatus(user, offender3.asCaseSummary(), cas1InProgress)

        val (offender4, inmate4) = givenAnOffender({
          withFirstName("Bagatha")
          withLastName("")
        })
        val assessBagatha = createApprovedPremisesAssessmentForStatus(user, offender4.asCaseSummary(), cas1InProgress)

        apDeliusContextAddListCaseSummaryToBulkResponse(listOf(offender1.asCaseSummary(), offender2.asCaseSummary(), offender3.asCaseSummary(), offender4.asCaseSummary()))

        assertAssessmentsReturnedGivenStatus(
          jwt,
          listOf(
            assessmentSummaryMapper(offender3.asCaseSummary(), inmate3).toSummary(assessAgatha, status = IN_PROGRESS),
            assessmentSummaryMapper(offender2.asCaseSummary(), inmate2).toSummary(assessArthur, status = IN_PROGRESS),
            assessmentSummaryMapper(offender4.asCaseSummary(), inmate4).toSummary(assessBagatha, status = IN_PROGRESS),
            assessmentSummaryMapper(offender1.asCaseSummary(), inmate1).toSummary(assessZendaya, status = IN_PROGRESS),
          ),
          sortBy = AssessmentSortField.personName,
          status = listOf(cas1InProgress),
        )
      }
    }

    @Test
    fun `Get all assessments for Approved Premises sorts correctly when sortBy is personCrn`() {
      givenAUser { user, jwt ->
        val (offender1, inmate1) = givenAnOffender({ withCrn("CRN1") })
        val assessCrn1 = createApprovedPremisesAssessmentForStatus(user, offender1.asCaseSummary(), cas1InProgress)

        val (offender2, inmate2) = givenAnOffender({ withCrn("CRN4") })
        val assessCrn4 = createApprovedPremisesAssessmentForStatus(user, offender2.asCaseSummary(), cas1InProgress)

        val (offender3, inmate3) = givenAnOffender({ withCrn("CRN2") })
        val assessCrn2 = createApprovedPremisesAssessmentForStatus(user, offender3.asCaseSummary(), cas1InProgress)

        val (offender4, inmate4) = givenAnOffender({ withCrn("CRN3") })
        val assessCrn3 = createApprovedPremisesAssessmentForStatus(user, offender4.asCaseSummary(), cas1InProgress)

        apDeliusContextAddListCaseSummaryToBulkResponse(listOf(offender1.asCaseSummary(), offender2.asCaseSummary(), offender3.asCaseSummary(), offender4.asCaseSummary()))

        assertAssessmentsReturnedGivenStatus(
          jwt,
          listOf(
            assessmentSummaryMapper(offender1.asCaseSummary(), inmate1).toSummary(assessCrn1, status = IN_PROGRESS),
            assessmentSummaryMapper(offender3.asCaseSummary(), inmate3).toSummary(assessCrn2, status = IN_PROGRESS),
            assessmentSummaryMapper(offender4.asCaseSummary(), inmate4).toSummary(assessCrn3, status = IN_PROGRESS),
            assessmentSummaryMapper(offender2.asCaseSummary(), inmate2).toSummary(assessCrn4, status = IN_PROGRESS),
          ),
          sortBy = AssessmentSortField.personCrn,
          status = listOf(cas1InProgress),
        )
      }
    }

    @Test
    fun `Get all assessments for Approved Premises sorts correctly when sortBy is arrivalDate`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val veryOld = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1InProgress,
            applicationMutator = { withArrivalDate(OffsetDateTime.now().minusDays(10)) },
          )

          val veryNew = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1InProgress,
            applicationMutator = { withArrivalDate(OffsetDateTime.now().minusDays(2)) },
          )

          val old = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1InProgress,
            applicationMutator = { withArrivalDate(OffsetDateTime.now().minusDays(5)) },
          )

          val new = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1InProgress,
            applicationMutator = { withArrivalDate(OffsetDateTime.now().minusDays(3)) },
          )

          assertAssessmentsReturnedGivenStatus(
            jwt,
            listOf(
              assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummary(veryOld, status = IN_PROGRESS),
              assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummary(old, status = IN_PROGRESS),
              assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummary(new, status = IN_PROGRESS),
              assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummary(veryNew, status = IN_PROGRESS),
            ),
            sortBy = AssessmentSortField.assessmentArrivalDate,
            status = emptyList(),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Approved Premises sorts correctly when sortBy is dueAt`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val dueMuchLater = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1InProgress,
            assessmentMutator = { withDueAt(OffsetDateTime.now().plusDays(10).truncatedTo(ChronoUnit.DAYS)) },
          )

          val dueVerySoon = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1InProgress,
            assessmentMutator = { withDueAt(OffsetDateTime.now().plusDays(2).truncatedTo(ChronoUnit.DAYS)) },
          )

          val dueLater = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1InProgress,
            assessmentMutator = { withDueAt(OffsetDateTime.now().plusDays(5).truncatedTo(ChronoUnit.DAYS)) },
          )

          val dueSoon = createApprovedPremisesAssessmentForStatus(
            user,
            offenderDetails.asCaseSummary(),
            cas1InProgress,
            assessmentMutator = { withDueAt(OffsetDateTime.now().plusDays(3).truncatedTo(ChronoUnit.DAYS)) },
          )

          assertAssessmentsReturnedGivenStatus(
            jwt,
            listOf(
              assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummary(dueVerySoon, status = IN_PROGRESS),
              assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummary(dueSoon, status = IN_PROGRESS),
              assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummary(dueLater, status = IN_PROGRESS),
              assessmentSummaryMapper(offenderDetails.asCaseSummary(), inmateDetails).toSummary(dueMuchLater, status = IN_PROGRESS),
            ),
            sortBy = AssessmentSortField.assessmentDueAt,
            status = emptyList(),
          )
        }
      }
    }

    fun assessmentSummaryMapper(
      offenderDetails: CaseSummary,
      inmateDetails: InmateDetail?,
    ) = AssessmentSummaryMapper(assessmentTransformer, objectMapper, offenderDetails, inmateDetails)

    inner class AssessmentSummaryMapper(
      private val assessmentTransformer: AssessmentTransformer,
      private val objectMapper: ObjectMapper,
      private val offenderDetails: CaseSummary,
      private val inmateDetails: InmateDetail?,
    ) {

      fun toSummaries(
        vararg assessments: AssessmentEntity,
        status: DomainAssessmentSummaryStatus? = null,
      ): List<Cas1AssessmentSummary> = assessments.map { toSummary(it, status) }

      fun toSummary(assessment: AssessmentEntity, status: DomainAssessmentSummaryStatus? = null): Cas1AssessmentSummary = assessmentTransformer.transformDomainToCas1AssessmentSummary(
        toAssessmentSummaryEntity(assessment, status),
        PersonInfoResult.Success.Full(offenderDetails.crn, offenderDetails.asOffenderDetailSummary(), inmateDetails),
      )

      private fun toAssessmentSummaryEntity(
        assessment: AssessmentEntity,
        status: DomainAssessmentSummaryStatus?,
      ): DomainAssessmentSummary = DomainAssessmentSummaryImpl(
        id = assessment.id,

        applicationId = assessment.application.id,

        createdAt = assessment.createdAt.toInstant(),

        riskRatings = (assessment.application as? ApprovedPremisesApplicationEntity)?.riskRatings?.let { objectMapper.writeValueAsString(it) },
        arrivalDate = (assessment.application as? ApprovedPremisesApplicationEntity)?.arrivalDate?.toInstant(),

        completed = assessment.decision != null,
        decision = assessment.decision?.name,
        crn = assessment.application.crn,
        allocated = assessment.allocatedToUser != null,
        status = status,
        dueAt = assessment.dueAt?.toInstant(),
        probationDeliveryUnitName = null,
      )
    }

    private fun assertResponseForUrl(
      jwt: String,
      url: String,
      expectedResponse: ExpectedResponse,
    ): WebTestClient.ResponseSpec = when (expectedResponse) {
      is ExpectedResponse.OK -> assertUrlReturnsAssessments(
        jwt,
        url,
        expectedResponse.expectedAssessmentSummaries,
      )

      is ExpectedResponse.Error -> assertUrlReturnsError(
        jwt,
        url,
        expectedResponse.status,
        expectedResponse.errorDetail,
      )
    }

    private fun assertUrlReturnsError(
      jwt: String,
      url: String,
      status: HttpStatus,
      errorDetail: String,
    ): WebTestClient.ResponseSpec {
      val response = webTestClient.get()
        .uri(url)
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isEqualTo(status)

      val responseBody = response
        .returnResult<Problem>()
        .responseBody
        .blockFirst()

      assertThat(responseBody?.detail).isEqualTo(errorDetail)

      return response
    }
  }

  private fun createApprovedPremisesAssessmentForStatus(
    user: UserEntity,
    offenderDetails: CaseSummary,
    assessmentStatus: AssessmentStatus,
    applicationMutator: (ApprovedPremisesApplicationEntityFactory.() -> Unit) = {},
    assessmentMutator: (ApprovedPremisesAssessmentEntityFactory.() -> Unit) = {},
  ): ApprovedPremisesAssessmentEntity {
    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
      withAddedAt(OffsetDateTime.now())
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.crn)
      withCreatedByUser(user)
      withApplicationSchema(applicationSchema)
      withName("${offenderDetails.name.forename.uppercase()} ${offenderDetails.name.surname.uppercase()}")

      applicationMutator(this)
    }

    val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(user)
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
      withDecision(null)
      withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())

      when (assessmentStatus) {
        cas1Completed -> {
          withDecision(AssessmentDecision.ACCEPTED)
          withData(null)
        }

        cas1AwaitingResponse -> {
          withDecision(null)
          withData(null)
        }

        cas1InProgress -> {
          withDecision(null)
          withData("{ }")
        }

        cas1NotStarted -> {
          withDecision(null)
          withData(null)
        }

        else -> throw IllegalArgumentException("status $assessmentStatus is not supported")
      }

      assessmentMutator(this)
    }

    if (cas1AwaitingResponse == assessmentStatus) {
      // create multiple notes to ensure the left outer join doesn't result in duplicate assessments being returned
      (1..5).forEach { _ ->
        val clarificationNote = assessmentClarificationNoteEntityFactory.produceAndPersist {
          withAssessment(assessment)
          withCreatedBy(user)
          withResponse(null)
        }
        assessment.clarificationNotes.add(clarificationNote)
      }
    }

    assessment.schemaUpToDate = true

    return assessment
  }

  private fun assertAssessmentsReturnedGivenStatus(
    jwt: String,
    expectedAssessments: List<Cas1AssessmentSummary>,
    sortBy: AssessmentSortField,
    status: List<AssessmentStatus>,
  ) {
    val sortParam = sortBy.value
    val statusParams = status.joinToString("&") { "statuses=${it.value}" }

    assertResponseForUrl(
      jwt,
      "/cas1/assessments?sortBy=$sortParam&$statusParams",
      ExpectedResponse.OK(expectedAssessments),
    )
  }

  private fun assertResponseForUrl(
    jwt: String,
    url: String,
    expectedResponse: ExpectedResponse,
  ): WebTestClient.ResponseSpec = when (expectedResponse) {
    is ExpectedResponse.OK -> assertUrlReturnsAssessments(
      jwt,
      url,
      expectedResponse.expectedAssessmentSummaries,
    )

    is ExpectedResponse.Error -> assertUrlReturnsError(
      jwt,
      url,
      expectedResponse.status,
      expectedResponse.errorDetail,
    )
  }

  private fun assertUrlReturnsAssessments(
    jwt: String,
    url: String,
    expectedAssessmentSummaries: List<Cas1AssessmentSummary>,
  ): WebTestClient.ResponseSpec {
    val response = webTestClient.get()
      .uri(url)
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk

    val responseBody = response
      .returnResult<String>()
      .responseBody
      .blockFirst()

    assertThat(responseBody).isEqualTo(objectMapper.writeValueAsString(expectedAssessmentSummaries))

    return response
  }

  private fun assertUrlReturnsError(
    jwt: String,
    url: String,
    status: HttpStatus,
    errorDetail: String,
  ): WebTestClient.ResponseSpec {
    val response = webTestClient.get()
      .uri(url)
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isEqualTo(status)

    val responseBody = response
      .returnResult<Problem>()
      .responseBody
      .blockFirst()

    assertThat(responseBody?.detail).isEqualTo(errorDetail)

    return response
  }

  sealed interface ExpectedResponse {
    data class OK(val expectedAssessmentSummaries: List<Cas1AssessmentSummary>) : ExpectedResponse
    data class Error(val status: HttpStatus, val errorDetail: String) : ExpectedResponse
  }

  @SuppressWarnings("LongParameterList")
  class DomainAssessmentSummaryImpl(
    override val id: UUID,
    override val applicationId: UUID,
    override val createdAt: Instant,
    override val riskRatings: String?,
    override val arrivalDate: Instant?,
    override val completed: Boolean,
    override val allocated: Boolean,
    override val decision: String?,
    override val crn: String,
    override val status: DomainAssessmentSummaryStatus?,
    override val dueAt: Instant?,
    override val probationDeliveryUnitName: String?,
  ) : DomainAssessmentSummary {
    override val type: String = "not needed"
  }
}
