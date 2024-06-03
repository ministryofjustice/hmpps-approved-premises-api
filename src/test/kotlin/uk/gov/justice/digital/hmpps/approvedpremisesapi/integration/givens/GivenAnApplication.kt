package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime

fun IntegrationTestBase.givenAnApplication(
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  submittedAt: OffsetDateTime? = null,
  block: (application: ApplicationEntity) -> Unit,
) {
  val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
    withPermissiveSchema()
  }

  val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(createdByUser)
    withApplicationSchema(applicationSchema)
    withSubmittedAt(submittedAt)
  }

  block(application)
}

fun IntegrationTestBase.givenASubmittedApplication(
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  block: (application: ApplicationEntity) -> Unit,
) {
  givenAnApplication(
    createdByUser,
    crn,
    OffsetDateTime.now(),
  ) { application ->
    block(application)
  }
}
