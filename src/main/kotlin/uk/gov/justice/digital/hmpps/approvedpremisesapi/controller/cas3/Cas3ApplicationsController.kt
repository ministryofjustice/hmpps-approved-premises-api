package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3.ApplicationsCas3Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy.CheckUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3ApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Service
class Cas3ApplicationsController(
  private val cas3ApplicationService: Cas3ApplicationService,
  private val applicationService: ApplicationService,
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val cas3ApplicationTransformer: Cas3ApplicationTransformer,
) : ApplicationsCas3Delegate {

  override fun getApplicationsForUser(): ResponseEntity<List<Cas3ApplicationSummary>> {
    val user = userService.getUserForRequest()

    val applications = applicationService.getAllApplicationsForUsername(user, ServiceName.temporaryAccommodation)

    return ResponseEntity.ok(
      getPersonDetailAndTransformToSummary(
        applications = applications,
        laoStrategy = CheckUserAccess(user.deliusUsername),
      ),
    )
  }

  private fun getPersonDetailAndTransformToSummary(
    applications: List<ApplicationSummary>,
    laoStrategy: LaoStrategy,
  ): List<Cas3ApplicationSummary> {
    val crns = applications.map { it.getCrn() }
    val personInfoResults = offenderService.getPersonInfoResults(crns.toSet(), laoStrategy)

    return applications.map {
      val crn = it.getCrn()
      cas3ApplicationTransformer.transformDomainToCas3ApplicationSummary(
        it,
        personInfoResults.firstOrNull { it.crn == crn } ?: PersonInfoResult.Unknown(crn),
      )
    }
  }

  override fun deleteApplication(applicationId: UUID): ResponseEntity<Unit> = ResponseEntity.ok(
    extractEntityFromCasResult(cas3ApplicationService.markApplicationAsDeleted(applicationId)),
  )
}
