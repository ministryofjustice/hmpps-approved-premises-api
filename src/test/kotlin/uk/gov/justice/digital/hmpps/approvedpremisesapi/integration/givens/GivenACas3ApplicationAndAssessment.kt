package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import java.time.OffsetDateTime

fun IntegrationTestBase.givenCas3ApplicationAndAssessment(
  user: UserEntity,
  offenderDetails: OffenderDetailSummary,
  assessmentRelocatedAt: OffsetDateTime? = null,
): Pair<TemporaryAccommodationApplicationEntity, TemporaryAccommodationAssessmentEntity> {
  val application: TemporaryAccommodationApplicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
    withCreatedByUser(user)
    withCrn(offenderDetails.otherIds.crn)
    withProbationRegion(user.probationRegion)
  }
  val assessment: TemporaryAccommodationAssessmentEntity = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
    withApplication(application)
    withCompletedAt(OffsetDateTime.now())
    withReallocatedAt(assessmentRelocatedAt)
  }
  return application to assessment
}
