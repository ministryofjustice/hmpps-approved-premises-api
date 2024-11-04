package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime

fun IntegrationTestBase.`Given a CAS1 Application`(
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  submittedAt: OffsetDateTime? = null,
  block: (application: ApplicationEntity) -> Unit = {},
) = `Given an Application`(createdByUser, crn, submittedAt, block)

fun IntegrationTestBase.`Given an Application`(
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  submittedAt: OffsetDateTime? = null,
  block: (application: ApplicationEntity) -> Unit = {},
): ApprovedPremisesApplicationEntity {
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

  return application
}

fun IntegrationTestBase.`Given a Submitted Application`(
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  block: (application: ApplicationEntity) -> Unit = {},
): ApprovedPremisesApplicationEntity {
  return `Given an Application`(
    createdByUser,
    crn,
    OffsetDateTime.now(),
  ) { application ->
    block(application)
  }
}
