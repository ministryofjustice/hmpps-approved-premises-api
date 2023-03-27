package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime

fun IntegrationTestBase.`Given a Placement Request`(
  allocatedToUser: UserEntity,
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  reallocated: Boolean = false,
  block: (placementRequest: PlacementRequestEntity, application: ApplicationEntity) -> Unit
) {
  val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
    withPermissiveSchema()
  }

  val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(createdByUser)
    withApplicationSchema(applicationSchema)
  }

  val placementRequest = placementRequestFactory.produceAndPersist {
    withAllocatedToUser(allocatedToUser)
    withApplication(application)
    withPostcodeDistrict(
      postCodeDistrictRepository.findAll()[0]
    )
    withDesirableCriteria(
      characteristicEntityFactory.produceAndPersistMultiple(5)
    )
    withEssentialCriteria(
      characteristicEntityFactory.produceAndPersistMultiple(3)
    )
    if (reallocated) {
      withReallocatedAt(OffsetDateTime.now())
    }
  }

  block(placementRequest, application)
}
