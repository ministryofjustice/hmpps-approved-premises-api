package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import java.time.LocalDate

fun IntegrationTestBase.givenCas3PremisesAndBedspaceAndApplicationAndAssessment(
  user: UserEntity,
  offenderDetails: OffenderDetailSummary,
  startDate: LocalDate = LocalDate.now(),
  premises: Cas3PremisesEntity = givenACas3Premises(
    probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(user.probationRegion)
    },
  ),
  bedspace: Cas3BedspacesEntity = cas3BedspaceEntityFactory.produceAndPersist {
    withReference("test-bed")
    withPremises(premises)
    withStartDate(startDate)
  },
): Triple<Cas3PremisesEntity, Cas3BedspacesEntity, AssessmentEntity> {
  val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
    withCreatedByUser(user)
    withCrn(offenderDetails.otherIds.crn)
    withProbationRegion(user.probationRegion)
  }

  val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
    withApplication(application)
  }

  return Triple(premises, bedspace, assessment)
}
