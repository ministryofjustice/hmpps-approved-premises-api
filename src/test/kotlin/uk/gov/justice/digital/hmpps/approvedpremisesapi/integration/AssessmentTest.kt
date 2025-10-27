package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assumptions
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus.cas3Rejected
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReferralHistoryUserNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CacheKeyResolver
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnAssessmentForTemporaryAccommodation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenSomeOffenders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus.COMPLETED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JpaApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.nonRepeatingRandomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Suppress("ReturnCount", "CyclomaticComplexMethod")
class AssessmentTest : IntegrationTestBase() {
  @Autowired
  lateinit var assessmentTransformer: AssessmentTransformer

  @SuppressWarnings("LargeClass")
  @Nested
  inner class AllAssessments {
    @Test
    fun `Get all assessments without JWT returns 401`() {
      webTestClient.get()
        .uri("/assessments")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource(ServiceName::class, names = ["cas2", "cas2v2", "approvedPremises"], mode = EnumSource.Mode.EXCLUDE)
    @Suppress("TooGenericExceptionThrown") // The RuntimeException here will never be reached
    fun `Get all assessments returns successfully when an inmate details cache failure occurs`(serviceName: ServiceName) {
      val givenAnAssessment = when (serviceName) {
        ServiceName.temporaryAccommodation -> { user: UserEntity, crn: String, block: (assessment: AssessmentEntity, application: ApplicationEntity) -> Unit ->
          givenAnAssessmentForTemporaryAccommodation(
            createdByUser = user,
            allocatedToUser = user,
            crn = crn,
            block = block,
          )
        }

        else -> throw RuntimeException()
      }

      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          givenAnAssessment(
            user,
            offenderDetails.otherIds.crn,
          ) { assessment, application ->
            // Simulate https://ministryofjustice.sentry.io/issues/4479884804 by deleting the data key from the cache while
            // preserving the metadata key.
            val cacheKeys = CacheKeyResolver(preemptiveCacheKeyPrefix, "inmateDetails", inmateDetails.offenderNo)
            redisTemplate.delete(cacheKeys.dataKey)

            val url = "/assessments"
            val expectedAssessments =
              assessmentSummaryMapper(offenderDetails, inmateDetails = null).toSummaries(
                assessment,
                status = IN_PROGRESS,
              )

            assertResponseForUrl(
              jwt,
              serviceName,
              url,
              ExpectedResponse.OK(expectedAssessments),
            )
          }
        }
      }
    }

    @Test
    fun `Get all assessments for Temporary Accommodation filters correctly when status is not defined`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val allAssessments = arrayOf(
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated),
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview),
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed),
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected),
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace),
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated),
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview),
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed),
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected),
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace),
          )

          assertAssessmentsReturnedGivenStatus(
            jwt,
            ServiceName.temporaryAccommodation,
            assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(*allAssessments),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = emptyList(),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Temporary Accommodation filters correctly when status is cas3Unallocated`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val unallocated1 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)
          val unallocated2 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            ServiceName.temporaryAccommodation,
            assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(unallocated1, unallocated2),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(AssessmentStatus.cas3Unallocated),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Temporary Accommodation filters correctly when status is cas3InReview`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          val inReview1 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          val inReview2 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            ServiceName.temporaryAccommodation,
            assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(inReview1, inReview2),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(AssessmentStatus.cas3InReview),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Temporary Accommodation filters correctly when status is cas3Closed`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val closed1 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val closed2 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            ServiceName.temporaryAccommodation,
            assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(closed1, closed2),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(AssessmentStatus.cas3Closed),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Temporary Accommodation filters correctly when multiple status cas3Closed, cas3Rejected are requested`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val closed1 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          val rejected1 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val closed2 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          val rejected2 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            ServiceName.temporaryAccommodation,
            assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(closed1, rejected1, closed2, rejected2),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(AssessmentStatus.cas3Closed, cas3Rejected),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Temporary Accommodation filters correctly when multiple status cas3Closed, cas3Rejected with pagination`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val closed1 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          val rejected1 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val closed2 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          val rejected2 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)

          val statusParams =
            listOf(AssessmentStatus.cas3Closed, cas3Rejected).map { "statuses=${it.value}" }.joinToString("&")

          val page1Response = assertResponseForUrl(
            jwt,
            ServiceName.temporaryAccommodation,
            "/assessments?page=1&perPage=3&sortBy=${AssessmentSortField.assessmentCreatedAt.value}&$statusParams",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(closed1, rejected1, closed2),
            ),
          )

          val page2Response = assertResponseForUrl(
            jwt,
            ServiceName.temporaryAccommodation,
            "/assessments?page=2&perPage=3&sortBy=${AssessmentSortField.assessmentCreatedAt.value}&$statusParams",
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
    fun `Get all assessments for Temporary Accommodation filters correctly when status is cas3Rejected`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          val rejected1 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          val rejected2 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            ServiceName.temporaryAccommodation,
            assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(rejected1, rejected2),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(AssessmentStatus.cas3Rejected),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Temporary Accommodation filters correctly when status is cas3ReadyToPlace`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          val readyToPlace1 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Unallocated)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Closed)
          createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3Rejected)
          val readyToPlace2 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3ReadyToPlace)

          assertAssessmentsReturnedGivenStatus(
            jwt,
            ServiceName.temporaryAccommodation,
            assessmentSummaryMapper(offenderDetails, inmateDetails).toSummaries(readyToPlace1, readyToPlace2),
            sortBy = AssessmentSortField.assessmentCreatedAt,
            status = listOf(AssessmentStatus.cas3ReadyToPlace),
          )
        }
      }
    }

    private fun assertAssessmentsReturnedGivenStatus(
      jwt: String,
      serviceName: ServiceName,
      expectedAssessments: List<AssessmentSummary>,
      sortBy: AssessmentSortField,
      status: List<AssessmentStatus>,
    ) {
      val sortParam = sortBy.value
      val statusParams = status.map { "statuses=${it.value}" }.joinToString("&")

      assertResponseForUrl(
        jwt,
        serviceName,
        "/assessments?sortBy=$sortParam&$statusParams",
        ExpectedResponse.OK(expectedAssessments),
      )
    }

    private fun createTemporaryAccommodationAssessmentForStatus(
      user: UserEntity,
      offenderDetails: OffenderDetailSummary,
      assessmentStatus: AssessmentStatus,
    ): TemporaryAccommodationAssessmentEntity {
      val application = produceAndPersistTemporaryAccommodationApplication(offenderDetails.otherIds.crn, user) {
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

    @ParameterizedTest
    @EnumSource
    fun `Get all assessments for Temporary Accommodation sorts correctly when 'sortDirection' and 'sortBy' query parameters are provided`(
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

          val probationDeliveryUnits = probationDeliveryUnitRepository.findAll().take(4) as MutableList<ProbationDeliveryUnitEntity?>
          probationDeliveryUnits.addLast(null)

          val assessments = offenders.mapIndexed { i, (offenderDetails, inmateDetails) ->
            val application = produceAndPersistTemporaryAccommodationApplication(offenderDetails.otherIds.crn, user) {
              withArrivalDate(LocalDate.now().randomDateAfter(512))
              withProbationDeliveryUnit(
                probationDeliveryUnits[i],
              )
            }

            val assessment =
              produceAndPersistTemporaryAccommodationAssessmentEntity(user, application)

            AssessmentParams(assessment, offenderDetails, inmateDetails)
          }

          val toSummary = { assessmentParams: AssessmentParams ->
            assessmentSummaryMapper(assessmentParams.offenderDetails, assessmentParams.inmateDetails)
              .toSummary(assessmentParams.assessment)
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
            ServiceName.temporaryAccommodation,
            "/assessments?sortDirection=desc&sortBy=${sortBy.value}",
            expectedResponse,
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Temporary Accommodation filters correctly when 'page' query parameter is provided`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val assessment1 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val assessment2 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val assessment3 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val assessment4 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)
          val assessment5 =
            createTemporaryAccommodationAssessmentForStatus(user, offenderDetails, AssessmentStatus.cas3InReview)

          val page1Response = assertResponseForUrl(
            jwt,
            ServiceName.temporaryAccommodation,
            "/assessments?page=1&perPage=3&sortBy=${AssessmentSortField.assessmentCreatedAt.value}",
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
            ServiceName.temporaryAccommodation,
            "/assessments?page=2&perPage=3&sortBy=${AssessmentSortField.assessmentCreatedAt.value}",
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
    fun `Get all assessments for Temporary Accommodation sorts by closest arrival date first by default`() {
      givenAUser { user, jwt ->
        givenSomeOffenders { offenderSequence ->
          val offenders = offenderSequence.take(5).toList()

          data class AssessmentParams(
            val assessment: TemporaryAccommodationAssessmentEntity,
            val offenderDetails: OffenderDetailSummary,
            val inmateDetails: InmateDetail,
          )

          val assessments = offenders.map { (offenderDetails, inmateDetails) ->
            val application = produceAndPersistTemporaryAccommodationApplication(offenderDetails.otherIds.crn, user) {
              withArrivalDate(LocalDate.now().nonRepeatingRandomDateAfter("assessmentArrivalDate", 512))
            }

            val assessment =
              produceAndPersistTemporaryAccommodationAssessmentEntity(user, application)

            AssessmentParams(assessment, offenderDetails, inmateDetails)
          }

          val expectedAssessments = assessments
            .sortedBy { (it.assessment.application as TemporaryAccommodationApplicationEntity).arrivalDate }
            .map { assessmentSummaryMapper(it.offenderDetails, it.inmateDetails).toSummary(it.assessment) }

          apDeliusContextAddListCaseSummaryToBulkResponse(offenders.map { (offenderDetails, inmateDetails) -> offenderDetails.asCaseSummary() })

          assertResponseForUrl(
            jwt,
            ServiceName.temporaryAccommodation,
            "/assessments",
            ExpectedResponse.OK(expectedAssessments),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Temporary Accommodation filters correctly when a crn is used in the 'query' parameter`() {
      givenAUser { user, jwt ->
        givenSomeOffenders { offenderSequence ->

          val (offender, otherOffender) = offenderSequence.take(2).toList()

          val application = produceAndPersistTemporaryAccommodationApplication(offender.first.otherIds.crn, user)

          val otherApplication =
            produceAndPersistTemporaryAccommodationApplication(otherOffender.first.otherIds.crn, user)

          val assessment = produceAndPersistTemporaryAccommodationAssessmentEntity(user, application)

          val otherAssessment =
            produceAndPersistTemporaryAccommodationAssessmentEntity(user, otherApplication)

          // when CRN is upper case
          assertResponseForUrl(
            jwt,
            ServiceName.temporaryAccommodation,
            "/assessments?crnOrName=${offender.first.otherIds.crn}",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offender.first, offender.second).toSummaries(assessment),
            ),
          )

          // when CRN is lower case
          assertResponseForUrl(
            jwt,
            ServiceName.temporaryAccommodation,
            "/assessments?crnOrName=${offender.first.otherIds.crn.lowercase()}",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offender.first, offender.second).toSummaries(assessment),
            ),
          )
        }
      }
    }

    @Test
    fun `Get all assessments for Temporary Accommodation filters correctly when a name is used in the 'query' parameter`() {
      givenAUser { user, jwt ->
        givenSomeOffenders { offenderSequence ->

          val (offender, otherOffender) = offenderSequence.take(2).toList()

          val application = produceAndPersistTemporaryAccommodationApplication(offender.first.otherIds.crn, user) {
            withName("${offender.first.firstName} ${offender.first.surname}")
          }

          val otherApplication =
            produceAndPersistTemporaryAccommodationApplication(otherOffender.first.otherIds.crn, user)

          val assessment = produceAndPersistTemporaryAccommodationAssessmentEntity(user, application)

          val otherAssessment =
            produceAndPersistTemporaryAccommodationAssessmentEntity(user, otherApplication)

          // first name match
          assertResponseForUrl(
            jwt,
            ServiceName.temporaryAccommodation,
            "/assessments?crnOrName=${offender.first.firstName}",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offender.first, offender.second).toSummaries(assessment),
            ),
          )

          // surname match
          assertResponseForUrl(
            jwt,
            ServiceName.temporaryAccommodation,
            "/assessments?crnOrName=${offender.first.surname}",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offender.first, offender.second).toSummaries(assessment),
            ),
          )

          // full name match
          assertResponseForUrl(
            jwt,
            ServiceName.temporaryAccommodation,
            "/assessments?crnOrName=${offender.first.firstName} ${offender.first.surname}",
            ExpectedResponse.OK(
              assessmentSummaryMapper(offender.first, offender.second).toSummaries(assessment),
            ),
          )

          // partial match, last letter of first name, first letter of last name
          assertResponseForUrl(
            jwt,
            ServiceName.temporaryAccommodation,
            "/assessments?crnOrName=${offender.first.firstName.last()} ${offender.first.surname.first()}",
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

          val application = produceAndPersistTemporaryAccommodationApplication(offender.first.otherIds.crn, user) {
            withName("${offender.first.firstName} ${offender.first.surname}")
          }

          val otherApplication =
            produceAndPersistTemporaryAccommodationApplication(otherOffender.first.otherIds.crn, user)

          val assessment = produceAndPersistTemporaryAccommodationAssessmentEntity(user, application)

          val otherAssessment =
            produceAndPersistTemporaryAccommodationAssessmentEntity(user, otherApplication)

          assertResponseForUrl(
            jwt,
            ServiceName.temporaryAccommodation,
            "/assessments?crnOrName=someone else",
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

          val application = produceAndPersistTemporaryAccommodationApplication(
            crn = offender.first.otherIds.crn,
            user = user,
          ) {
            withArrivalDate(LocalDate.now().plusDays(2))
          }

          val otherApplication =
            produceAndPersistTemporaryAccommodationApplication(
              crn = otherOffender.first.otherIds.crn,
              user = user,
            ) {
              withArrivalDate(LocalDate.now().plusDays(4))
            }

          val assessment = produceAndPersistTemporaryAccommodationAssessmentEntity(user, application)

          val otherAssessment =
            produceAndPersistTemporaryAccommodationAssessmentEntity(user, otherApplication)

          apDeliusContextAddListCaseSummaryToBulkResponse(listOf(offender.first.asCaseSummary(), otherOffender.first.asCaseSummary()))

          assertResponseForUrl(
            jwt,
            ServiceName.temporaryAccommodation,
            "/assessments",
            ExpectedResponse.OK(
              listOf(
                assessmentSummaryMapper(offender.first, offender.second).toSummary(assessment),
                assessmentSummaryMapper(otherOffender.first, inmateDetails = null).toRestricted(otherAssessment),
              ),
            ),
          )
        }
      }
    }
  }

  sealed interface ExpectedResponse {
    data class OK(val expectedAssessmentSummaries: List<AssessmentSummary>) : ExpectedResponse
    data class Error(val status: HttpStatus, val errorDetail: String) : ExpectedResponse
  }

  private fun assertUrlReturnsError(
    jwt: String,
    serviceName: ServiceName,
    url: String,
    status: HttpStatus,
    errorDetail: String,
  ): WebTestClient.ResponseSpec {
    val response = webTestClient.get()
      .uri(url)
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", serviceName.value)
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

  private fun assertResponseForUrl(
    jwt: String,
    serviceName: ServiceName,
    url: String,
    expectedResponse: ExpectedResponse,
  ): WebTestClient.ResponseSpec = when (expectedResponse) {
    is ExpectedResponse.OK -> assertUrlReturnsAssessments(
      jwt,
      serviceName,
      url,
      expectedResponse.expectedAssessmentSummaries,
    )

    is ExpectedResponse.Error -> assertUrlReturnsError(
      jwt,
      serviceName,
      url,
      expectedResponse.status,
      expectedResponse.errorDetail,
    )
  }

  private fun assertUrlReturnsAssessments(
    jwt: String,
    serviceName: ServiceName,
    url: String,
    expectedAssessmentSummaries: List<AssessmentSummary>,
  ): WebTestClient.ResponseSpec {
    val response = webTestClient.get()
      .uri(url)
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", serviceName.value)
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

  @Test
  fun `Get assessment by ID without JWT returns 401`() {
    webTestClient.get()
      .uri("/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class)
  fun `Get assessment by ID returns 200 with correct body for all roles`(role: UserRole) {
    givenAUser(roles = listOf(role)) { _, jwt ->
      givenAUser { userEntity, _ ->
        givenAnOffender { offenderDetails, inmateDetails ->

          val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
          }

          val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withAllocatedToUser(userEntity)
            withApplication(application)
          }

          webTestClient.get()
            .uri("/assessments/${assessment.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                assessmentTransformer.transformJpaToApi(
                  assessment,
                  PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                ),
              ),
            )
        }
      }
    }
  }

  @Test
  fun `Get assessment by ID returns 403 when Offender is LAO and user does not have LAO qualification or pass the LAO check`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender(
        offenderDetailsConfigBlock = {
          withCurrentExclusion(true)
        },
      ) { offenderDetails, inmateDetails ->

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
        }

        val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
        }

        webTestClient.get()
          .uri("/assessments/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Test
  fun `Get assessment by ID returns 200 when Offender is LAO and user does not have LAO qualification but does pass the LAO check`() {
    givenAUser { userEntity, jwt ->
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

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
        }

        val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
        }

        webTestClient.get()
          .uri("/assessments/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              assessmentTransformer.transformJpaToApi(
                assessment,
                PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
              ),
            ),
          )
      }
    }
  }

  @Test
  fun `Get assessment by ID returns 200 when Offender is LAO and user does have LAO qualification but does not pass the LAO check`() {
    givenAUser(qualifications = listOf(UserQualification.LAO)) { userEntity, jwt ->
      givenAnOffender(
        offenderDetailsConfigBlock = {
          withCurrentRestriction(true)
        },
      ) { offenderDetails, inmateDetails ->

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
        }

        val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
        }

        webTestClient.get()
          .uri("/assessments/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              assessmentTransformer.transformJpaToApi(
                assessment,
                PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
              ),
            ),
          )
      }
    }
  }

  @Test
  fun `Get Temporary Accommodation assessment by ID returns 200 with summary data transformed correctly`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

        val application = produceAndPersistTemporaryAccommodationApplication(offenderDetails.otherIds.crn, userEntity) {
          withArrivalDate(LocalDate.now().minusDays(100))
          withPersonReleaseDate(LocalDate.now().minusDays(100))
        }

        val assessment =
          produceAndPersistTemporaryAccommodationAssessmentEntity(userEntity, application) {
            withSummaryData("{\"num\":50,\"text\":\"Hello world!\"}")
          }

        webTestClient.get()
          .uri("/assessments/${assessment.id}")
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

  @Nested
  inner class AcceptAssessment {

    @Test
    fun `Accept assessment without JWT returns 401`() {
      val placementDates = PlacementDates(
        expectedArrival = LocalDate.now(),
        duration = 12,
      )

      val placementRequirements = PlacementRequirements(
        type = ApType.normal,
        location = "B74",
        radius = 50,
        essentialCriteria = listOf(PlacementCriteria.isRecoveryFocussed, PlacementCriteria.hasEnSuite),
        desirableCriteria = listOf(PlacementCriteria.isCatered, PlacementCriteria.acceptsSexOffenders),
      )

      webTestClient.post()
        .uri("/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9/acceptance")
        .bodyValue(
          AssessmentAcceptance(
            document = "{}",
            requirements = placementRequirements,
            placementDates = placementDates,
            notes = "Some Notes",
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Accept assessment with placement date returns 200, persists decision, creates and allocates a placement request, emits domain event and emails`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(
          probationArea = ProbationArea(
            code = "N21",
            description = randomStringMultiCaseWithNumbers(10),
          ),
        ),
      ) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
          }

          val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withAllocatedToUser(userEntity)
            withApplication(application)
          }

          val postcodeDistrict = postCodeDistrictFactory.produceAndPersist()

          val essentialCriteria = listOf(PlacementCriteria.hasEnSuite, PlacementCriteria.isRecoveryFocussed)
          val desirableCriteria =
            listOf(PlacementCriteria.acceptsNonSexualChildOffenders, PlacementCriteria.acceptsSexOffenders)

          val placementDates = PlacementDates(
            expectedArrival = LocalDate.now(),
            duration = 12,
          )

          val placementRequirements = PlacementRequirements(
            type = ApType.normal,
            location = postcodeDistrict.outcode,
            radius = 50,
            essentialCriteria = essentialCriteria,
            desirableCriteria = desirableCriteria,
          )

          webTestClient.post()
            .uri("/assessments/${assessment.id}/acceptance")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              AssessmentAcceptance(
                document = mapOf("document" to "value"),
                requirements = placementRequirements,
                placementDates = placementDates,
                notes = "Some Notes",
                agreeWithShortNoticeReason = true,
                agreeWithShortNoticeReasonComments = "comments",
                reasonForLateApplication = "medical condition",
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val persistedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
          assertThat(persistedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
          assertThat(persistedAssessment.document).isEqualTo("{\"document\":\"value\"}")
          assertThat(persistedAssessment.submittedAt).isNotNull
          assertThat(persistedAssessment.agreeWithShortNoticeReason).isTrue
          assertThat(persistedAssessment.agreeWithShortNoticeReasonComments).isEqualTo("comments")
          assertThat(persistedAssessment.reasonForLateApplication).isEqualTo("medical condition")

          val emittedMessage =
            domainEventAsserter.blockForEmittedDomainEvent(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)

          assertThat(emittedMessage.description).isEqualTo("An application has been assessed for an Approved Premises placement")
          assertThat(emittedMessage.detailUrl).matches("http://api/events/application-assessed/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}")
          assertThat(emittedMessage.additionalInformation.applicationId).isEqualTo(assessment.application.id)
          assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
            SnsEventPersonReference("CRN", offenderDetails.otherIds.crn),
            SnsEventPersonReference("NOMS", offenderDetails.otherIds.nomsNumber!!),
          )

          domainEventAsserter.assertDomainEventOfTypeStored(
            application.id,
            DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED,
          )

          val persistedPlacementRequest = placementRequestTestRepository.findByApplication(application)!!

          assertThat(persistedPlacementRequest.application.id).isEqualTo(application.id)
          assertThat(persistedPlacementRequest.expectedArrival).isEqualTo(placementDates.expectedArrival)
          assertThat(persistedPlacementRequest.duration).isEqualTo(placementDates.duration)
          assertThat(persistedPlacementRequest.notes).isEqualTo("Some Notes")

          val persistedPlacementRequirements = persistedPlacementRequest.placementRequirements

          assertThat(persistedPlacementRequirements.apType).isEqualTo(JpaApType.NORMAL)
          assertThat(persistedPlacementRequirements.postcodeDistrict.outcode).isEqualTo(placementRequirements.location)
          assertThat(persistedPlacementRequirements.radius).isEqualTo(placementRequirements.radius)

          assertThat(persistedPlacementRequirements.desirableCriteria.map { it.propertyName }).containsExactlyInAnyOrderElementsOf(
            placementRequirements.desirableCriteria.map { it.toString() },
          )
          assertThat(persistedPlacementRequirements.essentialCriteria.map { it.propertyName }).containsExactlyInAnyOrderElementsOf(
            placementRequirements.essentialCriteria.map { it.toString() },
          )

          emailAsserter.assertEmailsRequestedCount(2)
          emailAsserter.assertEmailRequested(application.createdByUser.email!!, Cas1NotifyTemplates.ASSESSMENT_ACCEPTED)
          emailAsserter.assertEmailRequested(application.createdByUser.email!!, Cas1NotifyTemplates.PLACEMENT_REQUEST_SUBMITTED)
        }
      }
    }

    @Test
    fun `Accept assessment without placement date returns 200, persists decision, does not create a Placement Request, creates Placement Requirements, emits domain event and emails`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(
          probationArea = ProbationArea(
            code = "N21",
            description = randomStringMultiCaseWithNumbers(10),
          ),
        ),
      ) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->

          val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
          }

          val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withAllocatedToUser(userEntity)
            withApplication(application)
          }

          val postcodeDistrict = postCodeDistrictFactory.produceAndPersist()

          val essentialCriteria = listOf(PlacementCriteria.hasEnSuite, PlacementCriteria.isRecoveryFocussed)
          val desirableCriteria =
            listOf(PlacementCriteria.acceptsNonSexualChildOffenders, PlacementCriteria.acceptsSexOffenders)

          val placementRequirements = PlacementRequirements(
            type = ApType.normal,
            location = postcodeDistrict.outcode,
            radius = 50,
            essentialCriteria = essentialCriteria,
            desirableCriteria = desirableCriteria,
          )

          webTestClient.post()
            .uri("/assessments/${assessment.id}/acceptance")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              AssessmentAcceptance(
                document = mapOf("document" to "value"),
                requirements = placementRequirements,
                notes = "Some Notes",
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          val persistedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
          assertThat(persistedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
          assertThat(persistedAssessment.document).isEqualTo("{\"document\":\"value\"}")
          assertThat(persistedAssessment.submittedAt).isNotNull

          val emittedMessage =
            domainEventAsserter.blockForEmittedDomainEvent(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)

          assertThat(emittedMessage.description).isEqualTo("An application has been assessed for an Approved Premises placement")
          assertThat(emittedMessage.detailUrl).matches("http://api/events/application-assessed/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}")
          assertThat(emittedMessage.additionalInformation.applicationId).isEqualTo(assessment.application.id)
          assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
            SnsEventPersonReference("CRN", offenderDetails.otherIds.crn),
            SnsEventPersonReference("NOMS", offenderDetails.otherIds.nomsNumber!!),
          )

          domainEventAsserter.assertDomainEventOfTypeStored(
            application.id,
            DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED,
          )

          assertThat(placementRequestTestRepository.findByApplication(application)).isNull()

          val persistedPlacementRequirements =
            placementRequirementsRepository.findTopByApplicationOrderByCreatedAtDesc(application)!!

          assertThat(persistedPlacementRequirements.apType).isEqualTo(JpaApType.NORMAL)
          assertThat(persistedPlacementRequirements.postcodeDistrict.outcode).isEqualTo(placementRequirements.location)
          assertThat(persistedPlacementRequirements.radius).isEqualTo(placementRequirements.radius)

          assertThat(persistedPlacementRequirements.desirableCriteria.map { it.propertyName }).containsExactlyInAnyOrderElementsOf(
            placementRequirements.desirableCriteria.map { it.toString() },
          )
          assertThat(persistedPlacementRequirements.essentialCriteria.map { it.propertyName }).containsExactlyInAnyOrderElementsOf(
            placementRequirements.essentialCriteria.map { it.toString() },
          )

          emailAsserter.assertEmailsRequestedCount(1)
          emailAsserter.assertEmailRequested(application.createdByUser.email!!, Cas1NotifyTemplates.ASSESSMENT_ACCEPTED)
        }
      }
    }

    @Test
    fun `Accept assessment with an outstanding clarification note sets the application status correctly`() {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(
          probationArea = ProbationArea(
            code = "N21",
            description = randomStringMultiCaseWithNumbers(10),
          ),
        ),
      ) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withStatus(ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION)
          }

          val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withAllocatedToUser(userEntity)
            withApplication(application)
            withDecision(null)
          }

          assessmentClarificationNoteEntityFactory.produceAndPersist {
            withAssessment(assessment)
            withResponse(null)
            withResponseReceivedOn(null)
            withCreatedBy(userEntity)
          }.apply {
            assessment.clarificationNotes = mutableListOf(this)
          }

          var persistedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
          assertThat((persistedAssessment.application as ApprovedPremisesApplicationEntity).status)
            .isEqualTo(ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION)

          val postcodeDistrict = postCodeDistrictFactory.produceAndPersist()

          val essentialCriteria = listOf(
            PlacementCriteria.hasEnSuite,
            PlacementCriteria.isRecoveryFocussed,
          )
          val desirableCriteria = listOf(
            PlacementCriteria.acceptsNonSexualChildOffenders,
            PlacementCriteria.acceptsSexOffenders,
          )

          val placementDates = PlacementDates(
            expectedArrival = LocalDate.now(),
            duration = 12,
          )

          val placementRequirements = PlacementRequirements(
            type = ApType.normal,
            location = postcodeDistrict.outcode,
            radius = 50,
            essentialCriteria = essentialCriteria,
            desirableCriteria = desirableCriteria,
          )

          webTestClient.post()
            .uri("/assessments/${assessment.id}/acceptance")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              AssessmentAcceptance(
                document = mapOf("document" to "value"),
                requirements = placementRequirements,
                placementDates = placementDates,
                notes = "Some Notes",
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          persistedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
          assertThat((persistedAssessment.application as ApprovedPremisesApplicationEntity).status)
            .isEqualTo(ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)
        }
      }
    }
  }

  @Test
  fun `Reject assessment without JWT returns 401`() {
    webTestClient.post()
      .uri("/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9/rejection")
      .bodyValue(AssessmentRejection(document = "{}", rejectionRationale = "reasoning"))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Reject assessment returns 200, persists decision, emits SNS domain event message`() {
    givenAUser(
      staffDetail = StaffDetailFactory.staffDetail(
        probationArea = ProbationArea(
          code = "N21",
          description = randomStringMultiCaseWithNumbers(10),
        ),
      ),
    ) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
        }

        val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
        }

        webTestClient.post()
          .uri("/assessments/${assessment.id}/rejection")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            AssessmentRejection(
              agreeWithShortNoticeReason = false,
              agreeWithShortNoticeReasonComments = "rejection comments",
              reasonForLateApplication = "medical condition",
              document = mapOf("document" to "value"),
              rejectionRationale = "reasoning",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk

        val persistedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
        assertThat(persistedAssessment.decision).isEqualTo(AssessmentDecision.REJECTED)
        assertThat(persistedAssessment.document).isEqualTo("{\"document\":\"value\"}")
        assertThat(persistedAssessment.submittedAt).isNotNull
        assertThat(persistedAssessment.agreeWithShortNoticeReason).isFalse
        assertThat(persistedAssessment.agreeWithShortNoticeReasonComments).isEqualTo("rejection comments")
        assertThat(persistedAssessment.reasonForLateApplication).isEqualTo("medical condition")

        val emittedMessage =
          snsDomainEventListener.blockForMessage(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)

        assertThat(emittedMessage.description).isEqualTo("An application has been assessed for an Approved Premises placement")
        assertThat(emittedMessage.detailUrl).matches("http://api/events/application-assessed/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}")
        assertThat(emittedMessage.additionalInformation.applicationId).isEqualTo(assessment.application.id)
        assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
          SnsEventPersonReference("CRN", offenderDetails.otherIds.crn),
          SnsEventPersonReference("NOMS", offenderDetails.otherIds.nomsNumber!!),
        )

        emailAsserter.assertEmailsRequestedCount(1)
        emailAsserter.assertEmailRequested(application.createdByUser.email!!, Cas1NotifyTemplates.ASSESSMENT_REJECTED)
      }
    }
  }

  @Test
  fun `Reject assessment with an outstanding clarification note sets the application status correctly`() {
    givenAUser(
      staffDetail = StaffDetailFactory.staffDetail(
        probationArea = ProbationArea(
          code = "N21",
          description = randomStringMultiCaseWithNumbers(10),
        ),
      ),
    ) { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
        }

        val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
          withDecision(null)
        }

        assessmentClarificationNoteEntityFactory.produceAndPersist {
          withAssessment(assessment)
          withResponse(null)
          withResponseReceivedOn(null)
          withCreatedBy(userEntity)
        }.apply {
          assessment.clarificationNotes = mutableListOf(this)
        }

        val postcodeDistrict = postCodeDistrictFactory.produceAndPersist()

        webTestClient.post()
          .uri("/assessments/${assessment.id}/rejection")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            AssessmentRejection(
              document = mapOf("document" to "value"),
              rejectionRationale = "reasoning",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk

        val persistedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
        assertThat((persistedAssessment.application as ApprovedPremisesApplicationEntity).status)
          .isEqualTo(ApprovedPremisesApplicationStatus.REJECTED)
      }
    }
  }

  @Test
  fun `PUT Update release date on temporary accommodation assessment `() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, UserRole.CAS3_REPORTER)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

        val accommodationDateFromApplication = LocalDate.now().plusDays(10)
        val releaseDateFromApplication = accommodationDateFromApplication.minusDays(1)
        var newReleaseDate = LocalDate.now()

        val application = produceAndPersistTemporaryAccommodationApplication(offenderDetails.otherIds.crn, userEntity) {
          withPersonReleaseDate(releaseDateFromApplication)
          withArrivalDate(accommodationDateFromApplication)
        }

        val assessment =
          produceAndPersistTemporaryAccommodationAssessmentEntity(userEntity, application)

        webTestClient.put()
          .uri("/assessments/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-service-name", "temporary-accommodation")
          .bodyValue(
            UpdateAssessment(
              data = emptyMap(),
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
  fun `PUT Update accommodation required from date on temporary accommodation assessment`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, UserRole.CAS3_REPORTER)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

        val accommodationDateFromApplication = LocalDate.now()
        val releaseDateFromApplication = accommodationDateFromApplication.minusDays(1)
        var newAccommodationDate = accommodationDateFromApplication.plusDays(1)

        val application = produceAndPersistTemporaryAccommodationApplication(offenderDetails.otherIds.crn, userEntity) {
          withPersonReleaseDate(releaseDateFromApplication)
          withArrivalDate(accommodationDateFromApplication)
        }

        val assessment =
          produceAndPersistTemporaryAccommodationAssessmentEntity(userEntity, application)

        webTestClient.put()
          .uri("/assessments/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-service-name", "temporary-accommodation")
          .bodyValue(
            UpdateAssessment(
              data = emptyMap(),
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

  @Test
  fun `GET temporary accommodation assessment contains updated release date and accommodation required from date`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, UserRole.CAS3_REPORTER)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

        val originalDate = LocalDate.now().plusDays(10)

        val application = produceAndPersistTemporaryAccommodationApplication(offenderDetails.otherIds.crn, userEntity)

        val assessment =
          produceAndPersistTemporaryAccommodationAssessmentEntity(userEntity, application) {
            withAccommodationRequiredFromDate(originalDate)
            withReleaseDate(originalDate)
          }

        webTestClient.get()
          .uri("/assessments/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-service-name", "temporary-accommodation")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.releaseDate").isEqualTo(originalDate.toString())
          .jsonPath("$.accommodationRequiredFromDate").isEqualTo(originalDate.toString())
      }
    }
  }

  @Test
  fun `GET temporary accommodation assessment contains original release date and accommodation required from date`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, UserRole.CAS3_REPORTER)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

        var releaseDate = LocalDate.now().minusDays(1)
        var accommodationDate = LocalDate.now().plusDays(1)
        val application = produceAndPersistTemporaryAccommodationApplication(offenderDetails.otherIds.crn, userEntity) {
          withPersonReleaseDate(releaseDate)
          withArrivalDate(accommodationDate)
        }

        val assessment =
          produceAndPersistTemporaryAccommodationAssessmentEntity(userEntity, application) {
            withAccommodationRequiredFromDate(null)
            withReleaseDate(null)
          }

        webTestClient.get()
          .uri("/assessments/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-service-name", "temporary-accommodation")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.releaseDate").isEqualTo(releaseDate.toString())
          .jsonPath("$.accommodationRequiredFromDate").isEqualTo(accommodationDate.toString())
      }
    }
  }

  @Test
  fun `Reject Temporary Accommodation assessment returns 200 and persists decision`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

        val application = produceAndPersistTemporaryAccommodationApplication(offenderDetails.otherIds.crn, userEntity)

        val assessment =
          produceAndPersistTemporaryAccommodationAssessmentEntity(userEntity, application)

        val referralRejectionReasonId = UUID.randomUUID()

        val referralRejectionReason = referralRejectionReasonEntityFactory.produceAndPersist {
          withId(referralRejectionReasonId)
        }

        val referralRejectionReasonDetail = "Other referral rejection reason detail"

        webTestClient.post()
          .uri("/assessments/${assessment.id}/rejection")
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
  fun `Reject Temporary Accommodation assessment returns 200 and persists decision when a referral rejection reason exists for the assessment`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

        val application = produceAndPersistTemporaryAccommodationApplication(offenderDetails.otherIds.crn, userEntity)

        val referralRejectionReason1 = referralRejectionReasonEntityFactory.produceAndPersist {
          withId(UUID.randomUUID())
        }

        val assessment =
          produceAndPersistTemporaryAccommodationAssessmentEntity(userEntity, application) {
            withReferralRejectionReason(referralRejectionReason1)
            withReferralRejectionReasonDetail("Old referral rejection reason detail")
          }

        val referralRejectionReasonId2 = UUID.randomUUID()

        val referralRejectionReason2 = referralRejectionReasonEntityFactory.produceAndPersist {
          withId(referralRejectionReasonId2)
        }

        val referralRejectionReasonDetail = "New referral rejection reason detail"

        webTestClient.post()
          .uri("/assessments/${assessment.id}/rejection")
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
  fun `Reject Temporary Accommodation assessment returns 404 when the referral rejection reason not exists`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

        val application = produceAndPersistTemporaryAccommodationApplication(offenderDetails.otherIds.crn, userEntity)

        val assessment =
          produceAndPersistTemporaryAccommodationAssessmentEntity(userEntity, application)

        val referralRejectionReasonId = UUID.randomUUID()

        webTestClient.post()
          .uri("/assessments/${assessment.id}/rejection")
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

  @Test
  fun `Close assessment without JWT returns 401`() {
    webTestClient.post()
      .uri("/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9/closure")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Close assessment returns 200 OK, persists closure timestamp`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

        val application = produceAndPersistTemporaryAccommodationApplication(offenderDetails.otherIds.crn, userEntity)

        val assessment =
          produceAndPersistTemporaryAccommodationAssessmentEntity(userEntity, application)

        webTestClient.post()
          .uri("/assessments/${assessment.id}/closure")
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

  @Test
  fun `Update does not let withdrawn assessments be updated`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withIsWithdrawn(true)
        }

        val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
          withIsWithdrawn(true)
        }

        webTestClient.put()
          .uri("/assessments/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateAssessment(
              data = mapOf("some text" to 5),
            ),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
      }
    }
  }

  @Test
  fun `Update assessment with an outstanding clarification note does not change the application status`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withStatus(ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION)
        }

        val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
          withDecision(null)
        }

        assessmentClarificationNoteEntityFactory.produceAndPersist {
          withAssessment(assessment)
          withResponse(null)
          withResponseReceivedOn(null)
          withCreatedBy(userEntity)
        }.apply {
          assessment.clarificationNotes = mutableListOf(this)
        }

        webTestClient.put()
          .uri("/assessments/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateAssessment(
              data = mapOf("some text" to 5),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk

        val persistedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
        assertThat((persistedAssessment.application as ApprovedPremisesApplicationEntity).status)
          .isEqualTo(ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION)
      }
    }
  }

  @Test
  fun `Create referral history user note returns 200 with correct body`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->

        val application = produceAndPersistTemporaryAccommodationApplication(offenderDetails.otherIds.crn, userEntity)

        val assessment =
          produceAndPersistTemporaryAccommodationAssessmentEntity(userEntity, application)

        webTestClient.post()
          .uri("/assessments/${assessment.id}/referral-history-notes")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewReferralHistoryUserNote(
              message = "Some text",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
      }
    }
  }

  fun assessmentSummaryMapper(
    offenderDetails: OffenderDetailSummary,
    inmateDetails: InmateDetail?,
  ) = AssessmentSummaryMapper(assessmentTransformer, objectMapper, offenderDetails, inmateDetails)

  class AssessmentSummaryMapper(
    private val assessmentTransformer: AssessmentTransformer,
    private val objectMapper: ObjectMapper,
    private val offenderDetails: OffenderDetailSummary,
    private val inmateDetails: InmateDetail?,
  ) {

    fun toSummaries(
      vararg assessments: AssessmentEntity,
      status: DomainAssessmentSummaryStatus? = null,
    ): List<AssessmentSummary> = assessments.map { toSummary(it, status) }

    fun toSummary(assessment: AssessmentEntity, status: DomainAssessmentSummaryStatus? = null): AssessmentSummary = assessmentTransformer.transformDomainToApiSummary(
      toAssessmentSummaryEntity(assessment, status),
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
    )

    fun toRestricted(assessment: AssessmentEntity, status: DomainAssessmentSummaryStatus? = null): AssessmentSummary = assessmentTransformer.transformDomainToApiSummary(
      toAssessmentSummaryEntity(assessment, status),
      PersonInfoResult.Success.Restricted(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber),
    )

    private fun toAssessmentSummaryEntity(
      assessment: AssessmentEntity,
      status: DomainAssessmentSummaryStatus?,
    ): DomainAssessmentSummary = DomainAssessmentSummaryImpl(
      type = when (assessment.application) {
        is ApprovedPremisesApplicationEntity -> "approved-premises"
        is TemporaryAccommodationApplicationEntity -> "temporary-accommodation"
        else -> fail()
      },

      id = assessment.id,

      applicationId = assessment.application.id,

      createdAt = assessment.createdAt.toInstant(),

      riskRatings = when (val reified = assessment.application) {
        is ApprovedPremisesApplicationEntity -> reified.riskRatings?.let { objectMapper.writeValueAsString(it) }
        is TemporaryAccommodationApplicationEntity -> reified.riskRatings?.let { objectMapper.writeValueAsString(it) }
        else -> null
      },

      arrivalDate = when (val application = assessment.application) {
        is ApprovedPremisesApplicationEntity -> application.arrivalDate?.toInstant()
        is TemporaryAccommodationApplicationEntity -> application.arrivalDate?.toInstant()
        else -> null
      },

      completed = when (assessment) {
        is TemporaryAccommodationAssessmentEntity -> assessment.completedAt != null
        else -> assessment.decision != null
      },
      decision = assessment.decision?.name,
      crn = assessment.application.crn,
      allocated = assessment.allocatedToUser != null,
      status = status,
      dueAt = assessment.dueAt?.toInstant(),

        /*
        If assessment.application is not TemporaryAccommodationApplicationEntity this returns null due to cast failing.
        If assessment.application is not null but probationDeliveryUnit is null, then null is also returned,
        which makes sense for applications for which the PDU hasn't been specified (and would therefore need to be null
         */
      probationDeliveryUnitName = (assessment.application as? TemporaryAccommodationApplicationEntity)?.probationDeliveryUnit?.name,
    )
  }

  private fun produceAndPersistTemporaryAccommodationAssessmentEntity(
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

  private fun produceAndPersistTemporaryAccommodationApplication(
    crn: String,
    user: UserEntity,
    nonDefaultFields: TemporaryAccommodationApplicationEntityFactory.() -> Unit = {},
  ): TemporaryAccommodationApplicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(user)
    withProbationRegion(user.probationRegion)
    nonDefaultFields()
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
