package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementRequestDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummaryDiscriminator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestBookingSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestDetailTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ChangeRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class PlacementRequestDetailTransformerTest {
  private val mockPlacementRequestTransformer = mockk<PlacementRequestTransformer>()
  private val mockBookingSummaryTransformer = mockk<PlacementRequestBookingSummaryTransformer>()
  private val mockApplicationsTransformer = mockk<ApplicationsTransformer>()
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockCas1SpaceBookingTransformer = mockk<Cas1SpaceBookingTransformer>()
  private val mockCas1ChangeRequestTransformer = mockk<Cas1ChangeRequestTransformer>()

  private val placementRequestDetailTransformer = PlacementRequestDetailTransformer(
    mockPlacementRequestTransformer,
    mockBookingSummaryTransformer,
    mockApplicationsTransformer,
    mockPersonTransformer,
    mockCas1SpaceBookingTransformer,
    mockCas1ChangeRequestTransformer,
  )

  private val mockPlacementRequestEntity = mockk<PlacementRequestEntity>()
  private val mockPersonInfoResult = mockk<PersonInfoResult.Success>()
  private val mockPersonSummaryInfoResult = mockk<PersonSummaryInfoResult.Success>()

  private val transformedPlacementRequest = getTransformedPlacementRequest()
  private val mockApplicationEntity = mockk<ApprovedPremisesApplicationEntity>()
  private val mockCas1Application = mockk<Cas1Application>()
  private val mockCas1ChangeRequestEntity = mockk<Cas1ChangeRequestEntity>()
  private val mockCas1SpaceBookingEntity = mockk<Cas1SpaceBookingEntity>()
  private val mockPlacementApplicationEntity = mockk<PlacementApplicationEntity>()

  @BeforeEach
  fun setup() {
    every { mockPlacementRequestTransformer.transformJpaToApi(mockPlacementRequestEntity, mockPersonInfoResult) } returns transformedPlacementRequest
  }

  @Test
  fun `transforms correctly to Cas1PlacementRequestDetail`() {
    val transformedPlacementRequest = getTransformedPlacementRequest()

    val changeRequests = listOf(
      Cas1ChangeRequestSummary(
        id = UUID.randomUUID(),
        person = FullPersonSummary(
          crn = "CRN1",
          name = "NAME",
          isRestricted = false,
          personType = PersonSummaryDiscriminator.fullPersonSummary,
        ),
        type = Cas1ChangeRequestType.PLACEMENT_APPEAL,
        createdAt = Instant.now(),
        tier = "TierA",
        expectedArrivalDate = LocalDate.parse("2023-01-01"),
        actualArrivalDate = LocalDate.parse("2023-01-01"),
        placementRequestId = UUID.randomUUID(),
      ),
    )

    val prExpectedArrival = LocalDate.parse("2024-01-15")
    val prDuration = 12
    val paExpectedArrival = LocalDate.parse("2024-02-01")
    val paRequestedDuration = 10
    val paAuthorisedDuration = 10
    val paArrivalFlexible = true

    every { mockPlacementRequestEntity.spaceBookings } returns mutableListOf()
    every { mockPlacementRequestEntity.isParole } returns false
    every { mockPlacementRequestEntity.application } returns mockApplicationEntity
    every { mockPlacementRequestEntity.isWithdrawn } returns true
    every { mockPlacementRequestEntity.expectedArrival } returns prExpectedArrival
    every { mockPlacementRequestEntity.duration } returns prDuration
    every { mockPlacementRequestEntity.isForLegacyInitialRequestForPlacement() } returns false
    every { mockPlacementRequestEntity.placementApplication } returns mockPlacementApplicationEntity

    every { mockPlacementApplicationEntity.expectedArrival } returns paExpectedArrival
    every { mockPlacementApplicationEntity.requestedDuration } returns paRequestedDuration
    every { mockPlacementApplicationEntity.expectedArrivalFlexible } returns paArrivalFlexible
    every { mockPlacementApplicationEntity.authorisedDuration } returns paAuthorisedDuration

    every { mockPlacementRequestTransformer.transformJpaToApi(mockPlacementRequestEntity, mockPersonInfoResult) } returns transformedPlacementRequest
    every { mockApplicationsTransformer.transformJpaToCas1Application(mockApplicationEntity, mockPersonInfoResult) } returns mockCas1Application
    every { mockPersonTransformer.personInfoResultToPersonSummaryInfoResult(mockPersonInfoResult) } returns mockPersonSummaryInfoResult
    every { mockCas1ChangeRequestTransformer.transformToChangeRequestSummaries(listOf(mockCas1ChangeRequestEntity), mockPersonInfoResult) } returns changeRequests
    every { mockCas1ChangeRequestEntity.spaceBooking } returns mockCas1SpaceBookingEntity

    val result = placementRequestDetailTransformer.transformJpaToCas1PlacementRequestDetail(
      mockPlacementRequestEntity,
      mockPersonInfoResult,
      listOf(mockCas1ChangeRequestEntity),
    )
    assertThat(result).isInstanceOf(Cas1PlacementRequestDetail::class.java)

    assertThat(result.id).isEqualTo(transformedPlacementRequest.id)
    assertThat(result.type).isEqualTo(transformedPlacementRequest.type)
    assertThat(result.location).isEqualTo(transformedPlacementRequest.location)
    assertThat(result.radius).isEqualTo(transformedPlacementRequest.radius)
    assertThat(result.essentialCriteria).isEqualTo(transformedPlacementRequest.essentialCriteria)
    assertThat(result.person).isEqualTo(transformedPlacementRequest.person)
    assertThat(result.risks).isEqualTo(transformedPlacementRequest.risks)
    assertThat(result.applicationId).isEqualTo(transformedPlacementRequest.applicationId)
    assertThat(result.assessmentId).isEqualTo(transformedPlacementRequest.assessmentId)
    assertThat(result.releaseType).isEqualTo(transformedPlacementRequest.releaseType)
    assertThat(result.status).isEqualTo(transformedPlacementRequest.status)
    assertThat(result.assessmentDecision).isEqualTo(transformedPlacementRequest.assessmentDecision)
    assertThat(result.assessmentDate).isEqualTo(transformedPlacementRequest.assessmentDate)
    assertThat(result.applicationDate).isEqualTo(transformedPlacementRequest.applicationDate)
    assertThat(result.assessor).isEqualTo(transformedPlacementRequest.assessor)
    assertThat(result.notes).isEqualTo(transformedPlacementRequest.notes)
    assertThat(result.legacyBooking).isNull()
    assertThat(result.spaceBookings).isEmpty()
    assertThat(result.isWithdrawn).isEqualTo(true)
    assertThat(result.isParole).isEqualTo(false)
    assertThat(result.application).isEqualTo(mockCas1Application)
    assertThat(result.openChangeRequests.size).isEqualTo(1)
    assertThat(result.openChangeRequests[0]).isEqualTo(changeRequests[0])

    assertThat(result.requestedPlacementPeriod.arrival).isEqualTo(paExpectedArrival)
    assertThat(result.requestedPlacementPeriod.duration).isEqualTo(paRequestedDuration)
    assertThat(result.requestedPlacementPeriod.arrivalFlexible).isEqualTo(paArrivalFlexible)

    assertThat(result.authorisedPlacementPeriod.arrival).isEqualTo(paExpectedArrival)
    assertThat(result.authorisedPlacementPeriod.duration).isEqualTo(paAuthorisedDuration)
    assertThat(result.expectedArrival).isEqualTo(transformedPlacementRequest.expectedArrival)
    assertThat(result.duration).isEqualTo(transformedPlacementRequest.duration)

    verify(exactly = 1) {
      mockPlacementRequestTransformer.transformJpaToApi(mockPlacementRequestEntity, mockPersonInfoResult)
    }
  }

  @Test
  fun `transforms correctly to Cas1PlacementRequestDetail when legacyInitialRequestForPlacement is true`() {
    val transformedPlacementRequest = getTransformedPlacementRequest()

    val changeRequests = emptyList<Cas1ChangeRequestEntity>()

    val prExpectedArrival = LocalDate.parse("2024-03-10")
    val paRequestedDuration = 14

    every { mockPlacementRequestEntity.spaceBookings } returns mutableListOf()
    every { mockPlacementRequestEntity.isParole } returns false
    every { mockPlacementRequestEntity.application } returns mockApplicationEntity
    every { mockPlacementRequestEntity.isWithdrawn } returns false
    every { mockPlacementRequestEntity.expectedArrival } returns prExpectedArrival
    every { mockPlacementRequestEntity.duration } returns 99

    every { mockPlacementRequestEntity.isForLegacyInitialRequestForPlacement() } returns true
    every { mockPlacementRequestEntity.placementApplication } returns mockPlacementApplicationEntity

    every { mockPlacementApplicationEntity.requestedDuration } returns paRequestedDuration
    every { mockPlacementApplicationEntity.expectedArrival } returns null
    every { mockPlacementApplicationEntity.expectedArrivalFlexible } returns null

    every { mockPlacementRequestTransformer.transformJpaToApi(mockPlacementRequestEntity, mockPersonInfoResult) } returns transformedPlacementRequest
    every { mockApplicationsTransformer.transformJpaToCas1Application(mockApplicationEntity, mockPersonInfoResult) } returns mockCas1Application
    every { mockPersonTransformer.personInfoResultToPersonSummaryInfoResult(mockPersonInfoResult) } returns mockPersonSummaryInfoResult
    every { mockCas1ChangeRequestTransformer.transformToChangeRequestSummaries(changeRequests, mockPersonInfoResult) } returns emptyList()

    val result = placementRequestDetailTransformer.transformJpaToCas1PlacementRequestDetail(
      mockPlacementRequestEntity,
      mockPersonInfoResult,
      emptyList(),
    )

    assertThat(result.requestedPlacementPeriod.arrival).isEqualTo(prExpectedArrival)
    assertThat(result.requestedPlacementPeriod.duration).isEqualTo(99)
    assertThat(result.requestedPlacementPeriod.arrivalFlexible).isNull()

    assertThat(result.authorisedPlacementPeriod.arrival).isEqualTo(prExpectedArrival)
    assertThat(result.authorisedPlacementPeriod.duration).isEqualTo(99)
    assertThat(result.authorisedPlacementPeriod.arrivalFlexible).isNull()

    assertThat(result.expectedArrival).isEqualTo(transformedPlacementRequest.expectedArrival)
    assertThat(result.duration).isEqualTo(transformedPlacementRequest.duration)

    verify(exactly = 1) {
      mockPlacementRequestTransformer.transformJpaToApi(mockPlacementRequestEntity, mockPersonInfoResult)
    }
  }

  private fun getTransformedPlacementRequest(): PlacementRequest {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withReleaseType("licence")
      .withCreatedByUser(user)
      .withSubmittedAt(OffsetDateTime.now().minusDays(3))
      .produce()

    val submittedAt = OffsetDateTime.now()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withAllocatedToUser(user)
      .withApplication(application)
      .withSubmittedAt(submittedAt)
      .produce()

    val placementRequirementsEntity = PlacementRequirementsEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withEssentialCriteria(
        listOf(
          CharacteristicEntityFactory().withPropertyName("isSemiSpecialistMentalHealth").produce(),
        ),
      )
      .withDesirableCriteria(
        listOf(
          CharacteristicEntityFactory().withPropertyName("isWheelchairDesignated").produce(),
        ),
      )
      .produce()

    val placementRequestEntity = PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirementsEntity)
      .produce()

    val mockAssessmentTransformer = mockk<AssessmentTransformer>()
    val mockPersonTransformer = mockk<PersonTransformer>()
    val mockRisksTransformer = mockk<RisksTransformer>()
    val mockUserTransformer = mockk<UserTransformer>()
    val mockBookingSummaryTransformer = mockk<PlacementRequestBookingSummaryTransformer>()

    val realPlacementRequestTransformer = PlacementRequestTransformer(
      mockPersonTransformer,
      mockRisksTransformer,
      mockAssessmentTransformer,
      mockUserTransformer,
      mockBookingSummaryTransformer,
    )

    every { mockAssessmentTransformer.transformJpaDecisionToApi(assessment.decision) } returns AssessmentDecision.accepted
    every { mockPersonTransformer.transformModelToPersonApi(mockPersonInfoResult) } returns mockk<Person>()
    every { mockRisksTransformer.transformDomainToApi(application.riskRatings!!, application.crn) } returns mockk<PersonRisks>()
    every { mockUserTransformer.transformJpaToApi(user, ServiceName.approvedPremises) } returns mockk<ApprovedPremisesUser>()

    return realPlacementRequestTransformer.transformJpaToApi(placementRequestEntity, mockPersonInfoResult)
  }
}
