package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForTemporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenSomeOffenders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus.COMPLETED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.nonRepeatingRandomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2

class Cas3AssessmentTest : IntegrationTestBase() {
  @Autowired
  lateinit var userTransformer: UserTransformer

  @Autowired
  lateinit var cas3AssessmentTransformer: Cas3AssessmentTransformer

  @Nested
  inner class GetAssessments {
    @Test
    fun `Get all assessments without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas3/assessments")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get all assessments filters correctly when status is not defined`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val allAssessments = arrayOf(
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated),
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview),
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed),
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected),
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace),
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated),
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview),
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed),
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected),
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace),
          )

          assertAssessmentsReturnedGivenStatus(
            jwt,
            assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(*allAssessments),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = emptyList(),
          )
        }
      }
    }

    @Test
    fun `Get all assessments filters correctly when status is cas3Unallocated`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val unallocated1 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)
          val unallocated2 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(unallocated1, unallocated2),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(AssessmentStatus.cas3Unallocated),
          )
        }
      }
    }

    @Test
    fun `Get all assessments filters correctly when status is cas3InReview`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          val inReview1 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          val inReview2 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(inReview1, inReview2),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(AssessmentStatus.cas3InReview),
          )
        }
      }
    }

    @Test
    fun `Get all assessments filters correctly when status is cas3Closed`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val closed1 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val closed2 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(closed1, closed2),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(AssessmentStatus.cas3Closed),
          )
        }
      }
    }

    @Test
    fun `Get all assessments filters correctly when multiple status cas3Closed, cas3Rejected are requested`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val closed1 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          val rejected1 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val closed2 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          val rejected2 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(closed1, rejected1, closed2, rejected2),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(AssessmentStatus.cas3Closed, AssessmentStatus.cas3Rejected),
          )
        }
      }
    }

    @Test
    fun `Get all assessments filters correctly when multiple status cas3Closed, cas3Rejected with pagination`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val closed1 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          val rejected1 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val closed2 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          val rejected2 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)

          val statusParams = listOf(AssessmentStatus.cas3Closed, AssessmentStatus.cas3Rejected).joinToString("&") { "statuses=${it.value}" }

          val page1Response = assertResponseForUrl(
            jwt,
            "/cas3/assessments?page=1&perPage=3&sortBy=${AssessmentSortField.assessmentCreatedAt.value}&$statusParams",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(closed1, rejected1, closed2),
            ),
          )

          val page2Response = assertResponseForUrl(
            jwt,
            "/cas3/assessments?page=2&perPage=3&sortBy=${AssessmentSortField.assessmentCreatedAt.value}&$statusParams",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(rejected2),
            ),
          )

          page1Response.expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 4)
            .expectHeader().valueEquals("X-Pagination-PageSize", 3)

          page2Response.expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 4)
            .expectHeader().valueEquals("X-Pagination-PageSize", 3)
        }
      }
    }

    @Test
    fun `Get all assessments filters correctly when status is cas3Rejected`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          val rejected1 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          val rejected2 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(rejected1, rejected2),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(AssessmentStatus.cas3Rejected),
          )
        }
      }
    }

    @Test
    fun `Get all assessments filters correctly when status is cas3ReadyToPlace`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          val readyToPlace1 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          val readyToPlace2 =
            createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(readyToPlace1, readyToPlace2),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(AssessmentStatus.cas3ReadyToPlace),
          )
        }
      }
    }

    @ParameterizedTest
    @EnumSource(AssessmentSortField::class, names = ["assessmentDueAt"], mode = EnumSource.Mode.EXCLUDE)
    fun `Get all assessments sorts correctly when 'sortDirection' and 'sortBy' query parameters are provided`(
      sortBy: AssessmentSortField,
    ) {
      givenAUser { user, jwt ->
        givenSomeOffenders { offenderSequence ->
          val offenders = offenderSequence.take(5).toList()
          apDeliusContextAddListCaseSummaryToBulkResponse(offenders.map { (offenderDetails, _) -> offenderDetails.asCaseSummary() })

          data class AssessmentParams(
            val assessment: TemporaryAccommodationAssessmentEntity,
            val offenderDetails: OffenderDetailSummary,
            val inmateDetails: InmateDetail,
          )

          val probationDeliveryUnits =
            probationDeliveryUnitRepository.findAll().take(4) as MutableList<ProbationDeliveryUnitEntity?>
          probationDeliveryUnits.addLast(null)

          val assessments = offenders.mapIndexed { i, (offenderDetails, inmateDetails) ->
            val application = produceAndPersistApplication(offenderDetails.otherIds.crn, user) {
              withArrivalDate(LocalDate.now().randomDateAfter(512))
              withProbationDeliveryUnit(
                probationDeliveryUnits[i],
              )
            }

            val assessment = produceAndPersistAssessmentEntity(user, application)

            AssessmentParams(assessment, offenderDetails, inmateDetails)
          }

          val toSummary = { assessmentParams: AssessmentParams ->
            assessmentSummaryMapper(assessmentParams.offenderDetails, assessmentParams.inmateDetails)
              .toCas3Summary(assessmentParams.assessment)
          }

          val expectedResponse = when (sortBy) {
            AssessmentSortField.personName -> {
              ExpectedResponse.OK(
                assessments
                  .sortedByDescending { "${it.offenderDetails.firstName} ${it.offenderDetails.surname}" }
                  .map(toSummary),
              )
            }

            AssessmentSortField.personCrn -> {
              ExpectedResponse.OK(
                assessments
                  .sortedByDescending { it.assessment.application.crn }
                  .map(toSummary),
              )
            }

            AssessmentSortField.assessmentArrivalDate -> {
              ExpectedResponse.OK(
                assessments
                  .sortedByDescending { (it.assessment.application as TemporaryAccommodationApplicationEntity).arrivalDate }
                  .map(toSummary),
              )
            }

            AssessmentSortField.assessmentStatus -> {
              // Skip test for sorting by assessment status, as it would involve replicating the logic used to determine
              // that status.
              Assumptions.assumeThat(true).isFalse
              // Allow the compiler to ignore this branch without defining a dummy value.
              return@givenSomeOffenders
            }

            AssessmentSortField.assessmentCreatedAt -> {
              ExpectedResponse.OK(
                assessments
                  .sortedByDescending { it.assessment.createdAt }
                  .map(toSummary),
              )
            }

            AssessmentSortField.assessmentDueAt -> {
              ExpectedResponse.Error(HttpStatus.BAD_REQUEST, "Sorting by due date is not supported for CAS3")
            }

            AssessmentSortField.applicationProbationDeliveryUnitName -> {
              ExpectedResponse.OK(
                assessments
                  .sortedWith(
                    compareByDescending(nullsLast()) {
                      (
                        it.assessment.application as
                          TemporaryAccommodationApplicationEntity
                        ).probationDeliveryUnit?.name?.lowercase()
                    },
                  )
                  .map(toSummary),
              )
            }
          }

          assertResponseForUrl(
            jwt,
            "/cas3/assessments?sortDirection=desc&sortBy=${sortBy.value}",
            expectedResponse,
          )
        }
      }
    }

    @Test
    fun `Get all assessments filters correctly when 'page' query parameter is provided`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val assessment1 = createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val assessment2 = createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val assessment3 = createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val assessment4 = createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val assessment5 = createAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)

          val page1Response = assertResponseForUrl(
            jwt,
            "/cas3/assessments?page=1&perPage=3&sortBy=${AssessmentSortField.assessmentCreatedAt.value}",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(
                assessment1,
                assessment2,
                assessment3,
                status = COMPLETED,
              ),
            ),
          )

          val page2Response = assertResponseForUrl(
            jwt,
            "/cas3/assessments?page=2&perPage=3&sortBy=${AssessmentSortField.assessmentCreatedAt.value}",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(
                assessment4,
                assessment5,
                status = COMPLETED,
              ),
            ),
          )

          page1Response.expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 5)
            .expectHeader().valueEquals("X-Pagination-PageSize", 3)

          page2Response.expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
            .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
            .expectHeader().valueEquals("X-Pagination-TotalResults", 5)
            .expectHeader().valueEquals("X-Pagination-PageSize", 3)
        }
      }
    }

    @Test
    fun `Get all assessments sorts by closest arrival date first by default`() {
      givenAUser { user, jwt ->
        givenSomeOffenders { offenderSequence ->
          val offenders = offenderSequence.take(5).toList()

          data class AssessmentParams(
            val assessment: TemporaryAccommodationAssessmentEntity,
            val offenderDetails: OffenderDetailSummary,
            val inmateDetails: InmateDetail,
          )

          val assessments = offenders.map { (offenderDetails, inmateDetails) ->
            val application = produceAndPersistApplication(offenderDetails.otherIds.crn, user) {
              withArrivalDate(LocalDate.now().nonRepeatingRandomDateAfter("assessmentArrivalDate", 512))
            }

            val assessment =
              produceAndPersistAssessmentEntity(user, application)

            AssessmentParams(assessment, offenderDetails, inmateDetails)
          }

          val expectedAssessments = assessments
            .sortedBy { (it.assessment.application as TemporaryAccommodationApplicationEntity).arrivalDate }
            .map { assessmentSummaryMapper(it.offenderDetails, it.inmateDetails).toCas3Summary(it.assessment) }

          apDeliusContextAddListCaseSummaryToBulkResponse(offenders.map { (offenderDetails, inmateDetails) -> offenderDetails.asCaseSummary() })

          assertResponseForUrl(
            jwt,
            "/cas3/assessments",
            ExpectedResponse.OK(expectedAssessments),
          )
        }
      }
    }

    @Test
    fun `Get all assessments filters correctly when a crn is used in the 'query' parameter`() {
      givenAUser { user, jwt ->
        givenSomeOffenders { offenderSequence ->

          val (offender, otherOffender) = offenderSequence.take(2).toList()

          val application = produceAndPersistApplication(offender.first.otherIds.crn, user)

          val otherApplication =
            produceAndPersistApplication(otherOffender.first.otherIds.crn, user)

          val assessment = produceAndPersistAssessmentEntity(user, application)

          val otherAssessment =
            produceAndPersistAssessmentEntity(user, otherApplication)

          // when CRN is upper case
          assertResponseForUrl(
            jwt,
            "/cas3/assessments?crnOrName=${offender.first.otherIds.crn}",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offender.first, offender.second).toSummaries(assessment),
            ),
          )

          // when CRN is lower case
          assertResponseForUrl(
            jwt,
            "/cas3/assessments?crnOrName=${offender.first.otherIds.crn.lowercase()}",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offender.first, offender.second).toSummaries(assessment),
            ),
          )
        }
      }
    }

    @Test
    fun `Get all assessments filters correctly when a name is used in the 'query' parameter`() {
      givenAUser { user, jwt ->
        givenSomeOffenders { offenderSequence ->

          val (offender, otherOffender) = offenderSequence.take(2).toList()

          val application = produceAndPersistApplication(offender.first.otherIds.crn, user) {
            withName("${offender.first.firstName} ${offender.first.surname}")
          }

          val otherApplication =
            produceAndPersistApplication(otherOffender.first.otherIds.crn, user)

          val assessment = produceAndPersistAssessmentEntity(user, application)

          val otherAssessment =
            produceAndPersistAssessmentEntity(user, otherApplication)

          // first name match
          assertResponseForUrl(
            jwt,
            "/cas3/assessments?crnOrName=${offender.first.firstName}",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offender.first, offender.second).toSummaries(assessment),
            ),
          )

          // surname match
          assertResponseForUrl(
            jwt,
            "/cas3/assessments?crnOrName=${offender.first.surname}",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offender.first, offender.second).toSummaries(assessment),
            ),
          )

          // full name match
          assertResponseForUrl(
            jwt,
            "/cas3/assessments?crnOrName=${offender.first.firstName} ${offender.first.surname}",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offender.first, offender.second).toSummaries(assessment),
            ),
          )

          // partial match, last letter of first name, first letter of last name
          assertResponseForUrl(
            jwt,
            "/cas3/assessments?crnOrName=${offender.first.firstName.last()} ${offender.first.surname.first()}",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offender.first, offender.second).toSummaries(assessment),
            ),
          )
        }
      }
    }

    @Test
    fun `Get all assessments returns empty when name and crn do not match`() {
      givenAUser { user, jwt ->
        givenSomeOffenders { offenderSequence ->

          val (offender, otherOffender) = offenderSequence.take(2).toList()

          val application = produceAndPersistApplication(offender.first.otherIds.crn, user) {
            withName("${offender.first.firstName} ${offender.first.surname}")
          }

          val otherApplication =
            produceAndPersistApplication(otherOffender.first.otherIds.crn, user)

          val assessment = produceAndPersistAssessmentEntity(user, application)

          val otherAssessment =
            produceAndPersistAssessmentEntity(user, otherApplication)

          assertResponseForUrl(
            jwt,
            "/cas3/assessments?crnOrName=someone else",
            ExpectedResponse.OK(emptyList()),
          )
        }
      }
    }

    @Test
    fun `Get all assessments returns restricted person information for LAO`() {
      var offenderIndex = 0
      givenAUser { user, jwt ->
        givenSomeOffenders(
          offenderDetailsConfigBlock = {
            withCurrentExclusion(offenderIndex != 0)
            withCurrentRestriction(offenderIndex != 0)
            offenderIndex++
          },
        ) { offenderSequence ->

          val (offender, otherOffender) = offenderSequence.take(2).toList()

          val application = produceAndPersistApplication(
            crn = offender.first.otherIds.crn,
            user = user,
          ) {
            withArrivalDate(LocalDate.now().plusDays(2))
          }

          val otherApplication =
            produceAndPersistApplication(
              crn = otherOffender.first.otherIds.crn,
              user = user,
            ) {
              withArrivalDate(LocalDate.now().plusDays(4))
            }

          val assessment = produceAndPersistAssessmentEntity(user, application)

          val otherAssessment =
            produceAndPersistAssessmentEntity(user, otherApplication)

          apDeliusContextAddListCaseSummaryToBulkResponse(
            listOf(
              offender.first.asCaseSummary(),
              otherOffender.first.asCaseSummary(),
            ),
          )

          assertResponseForUrl(
            jwt,
            "/cas3/assessments",
            ExpectedResponse.OK(
              listOf(
                assessmentSummaryMapper(offender.first, offender.second).toCas3Summary(assessment),
                assessmentSummaryMapper(otherOffender.first, inmateDetails = null).toRestricted(otherAssessment),
              ),
            ),
          )
        }
      }
    }

    private fun assessmentSummaryMapper(
      offenderDetails: OffenderDetailSummary,
      inmateDetails: InmateDetail?,
    ) = AssessmentSummaryMapper(cas3AssessmentTransformer, objectMapper, offenderDetails, inmateDetails)

    private fun createAssessmentForStatus(
      user: UserEntity,
      offenderDetails: OffenderDetailSummary,
      assessmentStatus: AssessmentStatus,
    ): TemporaryAccommodationAssessmentEntity {
      val application = produceAndPersistApplication(offenderDetails.otherIds.crn, user) {
        withArrivalDate(LocalDate.now().randomDateAfter(512))
      }

      val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
        withApplication(application)
        withDecision(null)
        withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
        withReleaseDate(null)
        withAccommodationRequiredFromDate(null)

        when (assessmentStatus) {
          AssessmentStatus.cas3Rejected -> {
            withDecision(AssessmentDecision.REJECTED)
          }

          AssessmentStatus.cas3Closed -> {
            withDecision(AssessmentDecision.ACCEPTED)
            withCompletedAt(OffsetDateTime.now())
          }

          AssessmentStatus.cas3ReadyToPlace -> {
            withDecision(AssessmentDecision.ACCEPTED)
          }

          AssessmentStatus.cas3InReview -> {
            withAllocatedToUser(user)
          }

          AssessmentStatus.cas3Unallocated -> {
          }

          else -> throw IllegalArgumentException("status $assessmentStatus is not supported")
        }
      }

      return assessment
    }

    private fun assertAssessmentsReturnedGivenStatus(
      jwt: String,
      expectedAssessments: List<Cas3AssessmentSummary>,
      sortBy: AssessmentSortField,
      status: List<AssessmentStatus>,
    ) {
      val sortParam = sortBy.value
      val statusParams = status.joinToString("&") { "statuses=${it.value}" }

      assertResponseForUrl(
        jwt,
        "/cas3/assessments?sortBy=$sortParam&$statusParams",
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
      expectedAssessmentSummaries: List<Cas3AssessmentSummary>,
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
  }

  @Nested
  inner class GetAssessment {
    @Test
    fun `Get assessment by ID without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas3/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get assessment by ID returns 403 when Offender is LAO and user does not have LAO qualification or pass the LAO check`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentExclusion(true)
          },
        ) { offenderDetails, inmateDetails ->

          val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
          }

          val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
            withAllocatedToUser(userEntity)
            withApplication(application)
          }

          webTestClient.get()
            .uri("/cas3/assessments/${assessment.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `Get assessment by ID returns 200 when Offender is LAO and user does not have LAO qualification but does pass the LAO check`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCurrentExclusion(true)
          },
        ) { offenderDetails, inmateDetails ->

          apDeliusContextMockUserAccess(
            CaseAccessFactory()
              .withCrn(offenderDetails.otherIds.crn)
              .withUserExcluded(false)
              .withUserRestricted(false)
              .produce(),
            userEntity.deliusUsername,
          )

          val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withArrivalDate(LocalDate.now().plusDays(30))
          }

          val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
            withAllocatedToUser(userEntity)
            withApplication(application)
          }

          webTestClient.get()
            .uri("/cas3/assessments/${assessment.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                cas3AssessmentTransformer.transformJpaToApi(
                  assessment,
                  PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `Get assessment by ID returns 200 with summary data transformed correctly`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val application = produceAndPersistApplication(offenderDetails.otherIds.crn, userEntity) {
            withArrivalDate(LocalDate.now().minusDays(100))
            withPersonReleaseDate(LocalDate.now().minusDays(100))
          }

          val assessment =
            produceAndPersistAssessmentEntity(userEntity, application) {
              withSummaryData("{\"num\":50,\"text\":\"Hello world!\"}")
            }

          webTestClient.get()
            .uri("/cas3/assessments/${assessment.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.summaryData.num").isEqualTo(50)
            .jsonPath("$.summaryData.text").isEqualTo("Hello world!")
        }
      }
    }

    private fun produceAndPersistApplication(
      crn: String,
      user: UserEntity,
      nonDefaultFields: TemporaryAccommodationApplicationEntityFactory.() -> Unit = {},
    ): TemporaryAccommodationApplicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn(crn)
      withCreatedByUser(user)
      withProbationRegion(user.probationRegion)
      nonDefaultFields()
    }

    private fun produceAndPersistAssessmentEntity(
      user: UserEntity,
      application: TemporaryAccommodationApplicationEntity,
      nonDefaultFields: TemporaryAccommodationAssessmentEntityFactory.() -> Unit = {},
    ): TemporaryAccommodationAssessmentEntity {
      val produceAndPersist = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
        withAllocatedToUser(user)
        withApplication(application)
        withReleaseDate(null)
        withAccommodationRequiredFromDate(null)
        nonDefaultFields()
      }
      return produceAndPersist
    }
  }

  @Nested
  inner class UpdateAssessment {
    @Test
    fun `Successfully update release date on assessment`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val accommodationDateFromApplication = LocalDate.now().plusDays(10)
          val releaseDateFromApplication = accommodationDateFromApplication.minusDays(1)
          var newReleaseDate = LocalDate.now()

          val application = produceAndPersistApplication(offenderDetails.otherIds.crn, userEntity) {
            withPersonReleaseDate(releaseDateFromApplication)
            withArrivalDate(accommodationDateFromApplication)
          }

          val assessment = produceAndPersistAssessmentEntity(userEntity, application)

          webTestClient.put()
            .uri("/cas3/assessments/${assessment.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-service-name", "temporary-accommodation")
            .bodyValue(
              Cas3UpdateAssessment(
                releaseDate = newReleaseDate,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.releaseDate").isEqualTo(newReleaseDate.toString())
            .jsonPath("$.accommodationRequiredFromDate").isEqualTo(accommodationDateFromApplication.toString())

          val domainEvents =
            domainEventRepository.findByAssessmentIdAndType(
              assessmentId = assessment.id,
              type = DomainEventType.CAS3_ASSESSMENT_UPDATED,
            )

          assertThat(domainEvents.size).isEqualTo(1)
        }
      }
    }

    @Test
    fun `Successfully update accommodation required from date on assessment`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, UserRole.CAS3_REPORTER)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val accommodationDateFromApplication = LocalDate.now()
          val releaseDateFromApplication = accommodationDateFromApplication.minusDays(1)
          var newAccommodationDate = accommodationDateFromApplication.plusDays(1)

          val application = produceAndPersistApplication(offenderDetails.otherIds.crn, userEntity) {
            withPersonReleaseDate(releaseDateFromApplication)
            withArrivalDate(accommodationDateFromApplication)
          }

          val assessment =
            produceAndPersistAssessmentEntity(userEntity, application)

          webTestClient.put()
            .uri("/cas3/assessments/${assessment.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-service-name", "temporary-accommodation")
            .bodyValue(
              Cas3UpdateAssessment(
                accommodationRequiredFromDate = newAccommodationDate,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.releaseDate").isEqualTo(releaseDateFromApplication.toString())
            .jsonPath("$.accommodationRequiredFromDate").isEqualTo(newAccommodationDate.toString())

          val domainEvents =
            domainEventRepository.findByAssessmentIdAndType(
              assessmentId = assessment.id,
              type = DomainEventType.CAS3_ASSESSMENT_UPDATED,
            )

          assertThat(domainEvents.size).isEqualTo(1)
        }
      }
    }
  }

  @Nested
  inner class AcceptAssessment {
    @Test
    fun `Accept assessment without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas3/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9/acceptance")
        .bodyValue(
          Cas3AssessmentAcceptance(
            document = "{}",
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Accept assessment returns 200, persists decision and add system notes`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(
          probationArea = ProbationArea(
            code = "N21",
            description = randomStringMultiCaseWithNumbers(10),
          ),
        ),
        roles = listOf(UserRole.CAS3_ASSESSOR),
      ) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->

          val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
          }

          val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
            withAllocatedToUser(userEntity)
            withApplication(application)
          }

          webTestClient.post()
            .uri("/cas3/assessments/${assessment.id}/acceptance")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              AssessmentAcceptance(
                document = mapOf("document" to "value"),
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val persistedAssessment = temporaryAccommodationAssessmentRepository.findByIdOrNull(assessment.id)!!
          assertThat(persistedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
          assertThat(persistedAssessment.document).isEqualTo("{\"document\":\"value\"}")
          assertThat(persistedAssessment.submittedAt).isNotNull
          assertThat(persistedAssessment.completedAt).isNull()

          val systemNotes = assessmentReferralSystemNoteRepository.findAll().first { it.assessment.id == assessment.id }
          assertThat(systemNotes).isNotNull
          assertThat(systemNotes.type).isEqualTo(ReferralHistorySystemNoteType.READY_TO_PLACE)
        }
      }
    }
  }

  @Nested
  inner class RejectAssessment {
    @Test
    fun `Reject assessment without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas3/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9/rejection")
        .bodyValue(AssessmentRejection(document = "{}", rejectionRationale = "reasoning"))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Reject assessment returns 200 and persists decision`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val application = produceAndPersistApplication(offenderDetails.otherIds.crn, userEntity)

          val assessment = produceAndPersistAssessmentEntity(userEntity, application)

          val referralRejectionReasonId = UUID.randomUUID()

          val referralRejectionReason = referralRejectionReasonEntityFactory.produceAndPersist {
            withId(referralRejectionReasonId)
          }

          val referralRejectionReasonDetail = "Other referral rejection reason detail"

          webTestClient.post()
            .uri("/cas3/assessments/${assessment.id}/rejection")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              AssessmentRejection(
                document = mapOf("document" to "value"),
                rejectionRationale = "reasoning",
                referralRejectionReasonId = referralRejectionReasonId,
                referralRejectionReasonDetail = referralRejectionReasonDetail,
                isWithdrawn = true,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val persistedAssessment = temporaryAccommodationAssessmentRepository.findByIdOrNull(assessment.id)!!
          assertThat(persistedAssessment.decision).isEqualTo(AssessmentDecision.REJECTED)
          assertThat(persistedAssessment.document).isEqualTo("{\"document\":\"value\"}")
          assertThat(persistedAssessment.submittedAt).isNotNull
          assertThat(persistedAssessment.completedAt).isNull()
          assertThat(persistedAssessment.referralRejectionReason).isEqualTo(referralRejectionReason)
          assertThat(persistedAssessment.referralRejectionReasonDetail).isEqualTo(referralRejectionReasonDetail)
          assertThat(persistedAssessment.isWithdrawn).isTrue()
        }
      }
    }

    @Test
    fun `Reject assessment returns 200 and persists decision when a referral rejection reason exists for the assessment`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val application = produceAndPersistApplication(offenderDetails.otherIds.crn, userEntity)

          val referralRejectionReason1 = referralRejectionReasonEntityFactory.produceAndPersist {
            withId(UUID.randomUUID())
          }

          val assessment =
            produceAndPersistAssessmentEntity(userEntity, application) {
              withReferralRejectionReason(referralRejectionReason1)
              withReferralRejectionReasonDetail("Old referral rejection reason detail")
            }

          val referralRejectionReasonId2 = UUID.randomUUID()

          val referralRejectionReason2 = referralRejectionReasonEntityFactory.produceAndPersist {
            withId(referralRejectionReasonId2)
          }

          val referralRejectionReasonDetail = "New referral rejection reason detail"

          webTestClient.post()
            .uri("/cas3/assessments/${assessment.id}/rejection")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              AssessmentRejection(
                document = mapOf("document" to "value"),
                rejectionRationale = "reasoning",
                referralRejectionReasonId = referralRejectionReasonId2,
                referralRejectionReasonDetail = referralRejectionReasonDetail,
                isWithdrawn = true,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val persistedAssessment = temporaryAccommodationAssessmentRepository.findByIdOrNull(assessment.id)!!
          assertThat(persistedAssessment.decision).isEqualTo(AssessmentDecision.REJECTED)
          assertThat(persistedAssessment.document).isEqualTo("{\"document\":\"value\"}")
          assertThat(persistedAssessment.submittedAt).isNotNull
          assertThat(persistedAssessment.completedAt).isNull()
          assertThat(persistedAssessment.referralRejectionReason).isEqualTo(referralRejectionReason2)
          assertThat(persistedAssessment.referralRejectionReasonDetail).isEqualTo(referralRejectionReasonDetail)
          assertThat(persistedAssessment.isWithdrawn).isTrue()
        }
      }
    }

    @Test
    fun `Reject assessment returns 404 when the referral rejection reason not exists`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val application = produceAndPersistApplication(offenderDetails.otherIds.crn, userEntity)

          val assessment = produceAndPersistAssessmentEntity(userEntity, application)

          val referralRejectionReasonId = UUID.randomUUID()

          webTestClient.post()
            .uri("/cas3/assessments/${assessment.id}/rejection")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              AssessmentRejection(
                document = mapOf("document" to "value"),
                rejectionRationale = "reasoning",
                referralRejectionReasonId = referralRejectionReasonId,
                isWithdrawn = false,
              ),
            )
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody()
            .jsonPath("detail")
            .isEqualTo("No Referral Rejection Reason with an ID of $referralRejectionReasonId could be found")
        }
      }
    }
  }

  @Nested
  inner class CloseAssessment {
    @Test
    fun `Close assessment without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas3/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9/closure")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Close assessment returns 200 OK, persists closure timestamp`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val application = produceAndPersistApplication(offenderDetails.otherIds.crn, userEntity)

          val assessment =
            produceAndPersistAssessmentEntity(userEntity, application)

          webTestClient.post()
            .uri("/cas3/assessments/${assessment.id}/closure")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isOk

          val persistedAssessment = temporaryAccommodationAssessmentRepository.findByIdOrNull(assessment.id)!!
          assertThat(persistedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
          assertThat(persistedAssessment.completedAt).isNotNull
        }
      }
    }
  }

  @Nested
  inner class DeallocateAssessmentTest {
    @Test
    fun `Deallocate assessment without JWT returns 401 Unauthorized`() {
      webTestClient.delete()
        .uri("/cas3/assessments/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Deallocate Temporary Accommodation assessment without CAS3_ASSESSOR role returns 403 Forbidden`() {
      givenAUser { _, jwt ->
        webTestClient.delete()
          .uri("/cas3/assessments/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Deallocate Temporary Accommodation assessment returns 200 and unassigns the allocated user`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          givenAnAssessmentForTemporaryAccommodation(
            allocatedToUser = user,
            createdByUser = user,
            crn = offenderDetails.otherIds.crn,
          ) { existingAssessment, _ ->

            webTestClient.delete()
              .uri("/cas3/assessments/${existingAssessment.id}/allocations")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
              .exchange()
              .expectStatus()
              .isNoContent

            val assessment =
              temporaryAccommodationAssessmentRepository.findAll().first { it.id == existingAssessment.id }
            val note = assessmentReferralSystemNoteRepository.findAll().first { it.assessment.id == assessment.id }

            assertThat(assessment.allocatedToUser).isNull()
            assertThat(assessment.allocatedAt).isNull()
            assertThat(assessment.decision).isNull()
            assertThat(assessment.submittedAt).isNull()
            assertThat(assessment.referralHistoryNotes).isNotNull()
            assertThat(note.type).isEqualTo(ReferralHistorySystemNoteType.UNALLOCATED)
            assertThat(note.createdByUser.id).isEqualTo(user.id)
          }
        }
      }
    }
  }

  @Nested
  inner class ReallocateAssessmentToMeTest {
    @BeforeEach
    fun stubBankHolidaysApi() {
      govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()
    }

    @Test
    fun `Reallocate application to different assessor without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas3/assessments/9c7abdf6-fd39-4670-9704-98a5bbfec95e/reallocateToMe")
        .bodyValue(
          NewReallocation(
            userId = UUID.randomUUID(),
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Reallocating a Temporary Accommodation assessment does not require a request body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { originalUser, _ ->
        givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { expectedUser, jwt ->
          givenAnOffender { offenderDetails, _ ->
            givenAnAssessmentForTemporaryAccommodation(
              allocatedToUser = originalUser,
              createdByUser = originalUser,
              crn = offenderDetails.otherIds.crn,
            ) { assessment, _ ->
              webTestClient.post()
                .uri("/cas3/assessments/${assessment.id}/reallocateToMe")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
                .bodyValue(Unit)
                .exchange()
                .expectStatus()
                .isCreated

              val assessment = temporaryAccommodationAssessmentRepository.findAll().first { it.id == assessment.id }
              val note = assessmentReferralSystemNoteRepository.findAll().first { it.assessment.id == assessment.id }

              assertThat(assessment.allocatedToUser).isNotNull()
              assertThat(assessment.allocatedToUser!!.id).isEqualTo(expectedUser.id)
              assertThat(assessment.allocatedAt).isNotNull()
              assertThat(assessment.decision).isNull()
              assertThat(assessment.referralHistoryNotes).isNotNull()
              assertThat(note.type).isEqualTo(ReferralHistorySystemNoteType.IN_REVIEW)
              assertThat(note.createdByUser.id).isEqualTo(expectedUser.id)
            }
          }
        }
      }
    }
  }

  private fun produceAndPersistApplication(
    crn: String,
    user: UserEntity,
    nonDefaultFields: TemporaryAccommodationApplicationEntityFactory.() -> Unit = {},
  ): TemporaryAccommodationApplicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(user)
    withProbationRegion(user.probationRegion)
    nonDefaultFields()
  }

  private fun produceAndPersistAssessmentEntity(
    user: UserEntity,
    application: TemporaryAccommodationApplicationEntity,
    nonDefaultFields: TemporaryAccommodationAssessmentEntityFactory.() -> Unit = {},
  ): TemporaryAccommodationAssessmentEntity {
    val produceAndPersist = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(user)
      withApplication(application)
      withReleaseDate(null)
      withAccommodationRequiredFromDate(null)
      nonDefaultFields()
    }
    return produceAndPersist
  }

  sealed interface ExpectedResponse {
    data class OK(val expectedAssessmentSummaries: List<Cas3AssessmentSummary>) : ExpectedResponse
    data class Error(val status: HttpStatus, val errorDetail: String) : ExpectedResponse
  }

  class AssessmentSummaryMapper(
    private val cas3AssessmentTransformer: Cas3AssessmentTransformer,
    private val objectMapper: ObjectMapper,
    private val offenderDetails: OffenderDetailSummary,
    private val inmateDetails: InmateDetail?,
  ) {
    fun toSummaries(
      vararg assessments: TemporaryAccommodationAssessmentEntity,
      status: DomainAssessmentSummaryStatus? = null,
    ): List<Cas3AssessmentSummary> = assessments.map { toCas3Summary(it, status) }

    fun toCas3Summary(
      assessment: TemporaryAccommodationAssessmentEntity,
      status: DomainAssessmentSummaryStatus? = null,
    ): Cas3AssessmentSummary = cas3AssessmentTransformer.transformDomainToApiSummary(
      toAssessmentSummaryEntity(assessment, status),
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
    )

    fun toRestricted(
      assessment: TemporaryAccommodationAssessmentEntity,
      status: DomainAssessmentSummaryStatus? = null,
    ): Cas3AssessmentSummary = cas3AssessmentTransformer.transformDomainToApiSummary(
      toAssessmentSummaryEntity(assessment, status),
      PersonInfoResult.Success.Restricted(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber),
    )

    private fun toAssessmentSummaryEntity(
      assessment: TemporaryAccommodationAssessmentEntity,
      status: DomainAssessmentSummaryStatus?,
    ): DomainAssessmentSummary {
      val application = (assessment.application as? TemporaryAccommodationApplicationEntity)
      return DomainAssessmentSummaryImpl(
        type = "temporary-accommodation",
        id = assessment.id,
        applicationId = assessment.application.id,
        createdAt = assessment.createdAt.toInstant(),
        riskRatings = application?.riskRatings?.let { objectMapper.writeValueAsString(it) },
        arrivalDate = application?.arrivalDate?.toInstant(),
        completed = assessment.completedAt != null,
        decision = assessment.decision?.name,
        crn = assessment.application.crn,
        allocated = assessment.allocatedToUser != null,
        status = status,
        dueAt = assessment.dueAt?.toInstant(),
        probationDeliveryUnitName = application?.probationDeliveryUnit?.name,
      )
    }
  }

  @SuppressWarnings("LongParameterList")
  class DomainAssessmentSummaryImpl(
    override val type: String,
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
  ) : DomainAssessmentSummary
}
