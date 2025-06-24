package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3.PeopleCas3Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3OASysGroup
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3OASysOffenceDetailsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Service
class Cas3PeopleController(
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val oaSysService: OASysService,
  private val oaSysSectionsTransformer: OASysSectionsTransformer,
  private val oaSysOffenceDetailsTransformer: Cas3OASysOffenceDetailsTransformer,
) : PeopleCas3Delegate {

  override fun riskManagement(crn: String): ResponseEntity<Cas3OASysGroup> {
    userAccessService.ensureUserCanAccessOffender(
      crn = crn,
      strategy = userService.getUserForRequest().cas3LaoStrategy(),
      throwNotFound = true,
    )

    val offenceDetails = extractEntityFromCasResult(oaSysService.getOASysOffenceDetails(crn))

    val assessmentMetadata = oaSysOffenceDetailsTransformer.toAssessmentMetadata(offenceDetails)

    val answers = oaSysSectionsTransformer.riskManagementPlanAnswers(
      extractEntityFromCasResult(oaSysService.getOASysRiskManagementPlan(crn)).riskManagementPlan,
    )

    return ResponseEntity.ok(
      Cas3OASysGroup(
        assessmentMetadata = assessmentMetadata,
        answers = answers,
      ),
    )
  }
}
