package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationPlaceholderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Cas1DataScienceReportEntities(
  val user: UserEntity,
  val apArea: ApAreaEntity,
  val probationRegion: ProbationRegionEntity,
  val application: ApprovedPremisesApplicationEntity,
  val case: CaseEntity,
  val placementApplication: PlacementApplicationEntity,
  val placementApplicationPlaceholder: PlacementApplicationPlaceholderEntity,
)

data class Cas2DataScienceReportEntities(
  val user: Cas2UserEntity,
  val application: Cas2ApplicationEntity,
  val case: CaseEntity,
  val statusUpdate: Cas2StatusUpdateEntity,
)

data class Cas3DataScienceReportEntities(
  val user: UserEntity,
  val probationRegion: ProbationRegionEntity,
  val probationDeliveryUnit: ProbationDeliveryUnitEntity,
  val application: TemporaryAccommodationApplicationEntity,
  val assessment: TemporaryAccommodationAssessmentEntity,
  val case: CaseEntity,
)

data class AllCasDataScienceReportEntities(
  val cas1: Cas1DataScienceReportEntities,
  val cas2: Cas2DataScienceReportEntities,
  val cas3: Cas3DataScienceReportEntities,
)

fun IntegrationTestBase.givenCas1DataScienceReportEntities(
  crn: String = "CAS1CRN01",
  createdAt: OffsetDateTime = OffsetDateTime.parse("2026-02-01T10:00:00Z"),
  submittedAt: OffsetDateTime = OffsetDateTime.parse("2026-02-02T10:00:00Z"),
): Cas1DataScienceReportEntities {
  val apArea = givenAnApArea()
  val probationRegion = givenAProbationRegion(apArea = apArea)
  val uniqueId = UUID.randomUUID().toString().take(8)
  val user = givenAUser(
    staffDetail = StaffDetailFactory.staffDetail(
      deliusUsername = "CAS1_USER_$uniqueId",
      code = "CAS1_$uniqueId",
    ),
    probationRegion = probationRegion,
    mockStaffUserDetailsCall = false,
  ).first
  val case = givenACase(crn = crn)

  val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedAt(createdAt)
    withSubmittedAt(submittedAt)
    withCreatedByUser(user)
    withApArea(apArea)
    withArrivalDate(OffsetDateTime.parse("2026-03-01T10:00:00Z"))
    withStatus(ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT)
  }

  val placementApplication = placementApplicationFactory.produceAndPersist {
    withApplication(application)
    withCreatedByUser(user)
    withSubmittedAt(OffsetDateTime.parse("2026-02-03T10:00:00Z"))
    withExpectedArrival(LocalDate.parse("2026-03-01"))
    withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
    withAutomatic(false)
  }

  val placeholder = givenAPlacementApplicationPlaceholder(application)

  return Cas1DataScienceReportEntities(
    user = user,
    apArea = apArea,
    probationRegion = probationRegion,
    application = application,
    case = case,
    placementApplication = placementApplication,
    placementApplicationPlaceholder = placeholder,
  )
}

fun IntegrationTestBase.givenCas2DataScienceReportEntities(
  crn: String = "CAS2CRN01",
  createdAt: OffsetDateTime = OffsetDateTime.parse("2026-10-02T10:00:00Z"),
  submittedAt: OffsetDateTime = OffsetDateTime.parse("2026-10-03T10:00:00Z"),
): Cas2DataScienceReportEntities {
  val uniqueId = UUID.randomUUID().toString().take(8)
  val user = cas2UserEntityFactory.produceAndPersist {
    withUsername("CAS2_USER_$uniqueId")
    withServiceOrigin(Cas2ServiceOrigin.HDC)
  }
  val case = givenACase(crn = crn)

  val application = cas2ApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(user)
    withCreatedAt(createdAt)
    withSubmittedAt(submittedAt)
    withHdcEligibilityDate(LocalDate.parse("2026-11-01"))
    withConditionalReleaseDate(LocalDate.parse("2026-12-01"))
    withBailHearingDate(LocalDate.parse("2026-10-15"))
    withServiceOrigin(Cas2ServiceOrigin.HDC)
  }

  val statusUpdate = cas2StatusUpdateEntityFactory.produceAndPersist {
    withApplication(application)
    withAssessor(user)
    withStatusId(UUID.randomUUID())
    withDescription("More information requested")
    withLabel("More info")
    withCreatedAt(OffsetDateTime.parse("2026-10-04T10:00:00Z"))
  }

  return Cas2DataScienceReportEntities(
    user = user,
    application = application,
    case = case,
    statusUpdate = statusUpdate,
  )
}

fun IntegrationTestBase.givenCas3DataScienceReportEntities(
  crn: String = "CAS3CRN01",
  createdAt: OffsetDateTime = OffsetDateTime.parse("2026-10-02T10:00:00Z"),
  submittedAt: OffsetDateTime = OffsetDateTime.parse("2026-10-03T10:00:00Z"),
): Cas3DataScienceReportEntities {
  val probationRegion = givenAProbationRegion()
  val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
    withProbationRegion(probationRegion)
  }
  val uniqueId = UUID.randomUUID().toString().take(8)
  val user = givenAUser(
    staffDetail = StaffDetailFactory.staffDetail(
      deliusUsername = "CAS3_USER_$uniqueId",
      code = "CAS3_$uniqueId",
    ),
    probationRegion = probationRegion,
    mockStaffUserDetailsCall = false,
  ).first
  val case = givenACase(crn = crn)

  val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(user)
    withProbationRegion(probationRegion)
    withProbationDeliveryUnit(probationDeliveryUnit)
    withCreatedAt(createdAt)
    withSubmittedAt(submittedAt)
    withArrivalDate(OffsetDateTime.parse("2026-11-01T10:00:00Z"))
  }

  val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
    withApplication(application)
    withAllocatedToUser(user)
    withCreatedAt(OffsetDateTime.parse("2026-10-04T10:00:00Z"))
    withSubmittedAt(OffsetDateTime.parse("2026-10-05T10:00:00Z"))
    withDecision(AssessmentDecision.ACCEPTED)
  }

  return Cas3DataScienceReportEntities(
    user = user,
    probationRegion = probationRegion,
    probationDeliveryUnit = probationDeliveryUnit,
    application = application,
    assessment = assessment,
    case = case,
  )
}

fun IntegrationTestBase.givenAllCasDataScienceReportEntities(): AllCasDataScienceReportEntities = AllCasDataScienceReportEntities(
  cas1 = givenCas1DataScienceReportEntities(),
  cas2 = givenCas2DataScienceReportEntities(),
  cas3 = givenCas3DataScienceReportEntities(),
)
