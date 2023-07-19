package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingNotMadeEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PlacementRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.time.OffsetDateTime
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision

class PlacementRequestTransformerTest {
  private val mockAssessmentTransformer = mockk<AssessmentTransformer>()
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockRisksTransformer = mockk<RisksTransformer>()
  private val mockUserTransformer = mockk<UserTransformer>()

  private val placementRequestTransformer = PlacementRequestTransformer(
    mockPersonTransformer,
    mockRisksTransformer,
    mockAssessmentTransformer,
    mockUserTransformer,
  )

  private val offenderDetailSummary = OffenderDetailsSummaryFactory().produce()
  private val inmateDetail = InmateDetailFactory().produce()

  private val user = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .produce()

  private val applicationSubmittedAt = OffsetDateTime.now().minusDays(12)
  private val assessmentSubmittedAt = OffsetDateTime.now().minusDays(3)

  private val application = ApprovedPremisesApplicationEntityFactory()
    .withReleaseType("licence")
    .withCreatedByUser(user)
    .withSubmittedAt(applicationSubmittedAt)
    .produce()

  private val assessment = AssessmentEntityFactory()
    .withAllocatedToUser(user)
    .withApplication(application)
    .withSubmittedAt(assessmentSubmittedAt)
    .produce()

  private val placementRequirementsFactory = PlacementRequirementsEntityFactory()
    .withApplication(application)
    .withAssessment(assessment)
    .withEssentialCriteria(
      listOf(
        CharacteristicEntityFactory().withPropertyName("isSemiSpecialistMentalHealth").produce(),
        CharacteristicEntityFactory().withPropertyName("isRecoveryFocussed").produce(),
        CharacteristicEntityFactory().withPropertyName("someOtherPropertyName").produce(),
      ),
    )
    .withDesirableCriteria(
      listOf(
        CharacteristicEntityFactory().withPropertyName("isWheelchairDesignated").produce(),
        CharacteristicEntityFactory().withPropertyName("isSingle").produce(),
        CharacteristicEntityFactory().withPropertyName("hasEnSuite").produce(),
        CharacteristicEntityFactory().withPropertyName("somethingElse").produce(),
      ),
    )

  private val placementRequestFactory = PlacementRequestEntityFactory()
    .withApplication(application)
    .withAssessment(assessment)
    .withAllocatedToUser(user)

  private val mockRisks = mockk<PersonRisks>()
  private val mockPerson = mockk<Person>()

  private val mockUser = mockk<ApprovedPremisesUser>()
  private val decision = ApiAssessmentDecision.accepted

  @BeforeEach
  fun setup() {
    every { mockAssessmentTransformer.transformJpaDecisionToApi(assessment.decision) } returns decision
    every { mockPersonTransformer.transformModelToApi(offenderDetailSummary, inmateDetail) } returns mockPerson
    every { mockRisksTransformer.transformDomainToApi(application.riskRatings!!, application.crn) } returns mockRisks
    every { mockUserTransformer.transformJpaToApi(user, ServiceName.approvedPremises) } returns mockUser
  }

  @Test
  fun `transformJpaToApi transforms a basic placement request entity`() {
    val placementRequirementsEntity = placementRequirementsFactory
      .withEssentialCriteria(
        listOf(
          CharacteristicEntityFactory().withPropertyName("isSemiSpecialistMentalHealth").produce(),
          CharacteristicEntityFactory().withPropertyName("isRecoveryFocussed").produce(),
          CharacteristicEntityFactory().withPropertyName("someOtherPropertyName").produce(),
        ),
      )
      .withDesirableCriteria(
        listOf(
          CharacteristicEntityFactory().withPropertyName("isWheelchairDesignated").produce(),
          CharacteristicEntityFactory().withPropertyName("isSingle").produce(),
          CharacteristicEntityFactory().withPropertyName("hasEnSuite").produce(),
          CharacteristicEntityFactory().withPropertyName("somethingElse").produce(),
        ),
      )
      .produce()

    val placementRequestEntity = placementRequestFactory
      .withPlacementRequirements(placementRequirementsEntity)
      .withNotes("Some notes")
      .produce()

    val result = placementRequestTransformer.transformJpaToApi(placementRequestEntity, offenderDetailSummary, inmateDetail)

    assertThat(result).isEqualTo(
      PlacementRequest(
        id = placementRequestEntity.id,
        gender = placementRequirementsEntity.gender,
        type = placementRequirementsEntity.apType,
        expectedArrival = placementRequestEntity.expectedArrival,
        duration = placementRequestEntity.duration,
        location = placementRequirementsEntity.postcodeDistrict.outcode,
        radius = placementRequirementsEntity.radius,
        essentialCriteria = listOf(PlacementCriteria.isSemiSpecialistMentalHealth, PlacementCriteria.isRecoveryFocussed),
        desirableCriteria = listOf(PlacementCriteria.isWheelchairDesignated, PlacementCriteria.isSingle, PlacementCriteria.hasEnSuite),
        person = mockPerson,
        risks = mockRisks,
        applicationId = application.id,
        assessmentId = assessment.id,
        releaseType = ReleaseTypeOption.licence,
        status = PlacementRequestStatus.notMatched,
        assessmentDecision = decision,
        assessmentDate = assessmentSubmittedAt.toInstant(),
        applicationDate = applicationSubmittedAt.toInstant(),
        assessor = mockUser,
        notes = placementRequestEntity.notes,
      ),
    )
  }

  @Test
  fun `transformJpaToApi returns a status of matched when a placement request has a booking`() {
    val premises = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withPremises(premises)
      .produce()

    val placementRequirementsEntity = placementRequirementsFactory.produce()

    val placementRequestEntity = placementRequestFactory
      .withPlacementRequirements(placementRequirementsEntity)
      .withBooking(booking)
      .produce()

    val result = placementRequestTransformer.transformJpaToApi(placementRequestEntity, offenderDetailSummary, inmateDetail)

    assertThat(result.status).isEqualTo(PlacementRequestStatus.matched)
  }

  @Test
  fun `transformJpaToApi returns a status of unableToMatch when a placement request has a bookingNotMade entity`() {
    val placementRequirementsEntity = placementRequirementsFactory.produce()

    val placementRequestEntity = placementRequestFactory
      .withPlacementRequirements(placementRequirementsEntity)
      .produce()

    val bookingNotMade = BookingNotMadeEntityFactory()
      .withPlacementRequest(placementRequestEntity)
      .produce()

    placementRequestEntity.bookingNotMades = mutableListOf(bookingNotMade)

    val result = placementRequestTransformer.transformJpaToApi(placementRequestEntity, offenderDetailSummary, inmateDetail)

    assertThat(result.status).isEqualTo(PlacementRequestStatus.unableToMatch)
  }
}
