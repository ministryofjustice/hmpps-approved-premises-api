package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Suppress("ReturnCount", "CyclomaticComplexMethod")
class AssessmentTest : IntegrationTestBase() {
  @Autowired
  lateinit var assessmentTransformer: AssessmentTransformer

  sealed interface ExpectedResponse {
    data class OK(val expectedAssessmentSummaries: List<AssessmentSummary>) : ExpectedResponse
    data class Error(val status: HttpStatus, val errorDetail: String) : ExpectedResponse
  }

  @Test
  fun `Get assessment by ID without JWT returns 401`() {
    webTestClient.get()
      .uri("/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9")
      .exchange()
      .expectStatus()
      .isUnauthorized
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
            .uri("/assessments/${assessment.id}/acceptance")
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
