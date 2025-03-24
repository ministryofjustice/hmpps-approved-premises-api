package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime

fun IntegrationTestBase.givenACas1Application(
  createdByUser: UserEntity,
  offender: CaseSummary,
  cruManagementArea: Cas1CruManagementAreaEntity? = null,
  tier: String? = null,
) = givenACas1Application(
  createdByUser = createdByUser,
  crn = offender.crn,
  cruManagementArea = cruManagementArea,
  name = "${offender.name.forename} ${offender.name.surname}",
  tier = tier,
)

@Suppress("LongParameterList")
fun IntegrationTestBase.givenACas1Application(
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  submittedAt: OffsetDateTime? = null,
  eventNumber: String = randomInt(1, 9).toString(),
  isWomensApplication: Boolean? = null,
  cruManagementArea: Cas1CruManagementAreaEntity? = null,
  name: String = "${randomStringUpperCase(4)} ${randomStringUpperCase(6)}",
  tier: String? = null,
  block: (application: ApplicationEntity) -> Unit = {},
) = givenAnApplication(createdByUser, crn, submittedAt, eventNumber, isWomensApplication, cruManagementArea, name, tier, block)

@Suppress("LongParameterList")
fun IntegrationTestBase.givenAnApplication(
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  submittedAt: OffsetDateTime? = null,
  eventNumber: String = randomInt(1, 9).toString(),
  isWomensApplication: Boolean? = null,
  cruManagementArea: Cas1CruManagementAreaEntity? = null,
  name: String = "${randomStringUpperCase(4)} ${randomStringUpperCase(6)}",
  tier: String? = null,
  block: (application: ApplicationEntity) -> Unit = {},
): ApprovedPremisesApplicationEntity {
  val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
    withPermissiveSchema()
  }

  val riskRatings = tier?.let {
    PersonRisksFactory().withTier(RiskWithStatus(RiskTier(level = it, lastUpdated = LocalDate.now()))).produce()
  } ?: PersonRisksFactory().produce()

  val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(createdByUser)
    withApplicationSchema(applicationSchema)
    withSubmittedAt(submittedAt)
    withEventNumber(eventNumber)
    withCruManagementArea(cruManagementArea)
    withIsWomensApplication(isWomensApplication)
    withName(name)
    withRiskRatings(riskRatings)
  }

  block(application)

  return application
}

fun IntegrationTestBase.givenASubmittedApplication(
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  block: (application: ApplicationEntity) -> Unit = {},
): ApprovedPremisesApplicationEntity = givenAnApplication(
  createdByUser,
  crn,
  OffsetDateTime.now(),
) { application ->
  block(application)
}
