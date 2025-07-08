package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import jakarta.transaction.Transactional
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.ExternalUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.SubmissionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@RestController
@RequestMapping(
  "\${openapi.communityAccommodationServicesTier2CAS2.base-path:/cas2}",
  produces = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE],
)
class Cas2SubmissionsController(
  private val httpAuthService: HttpAuthService,
  private val applicationService: Cas2ApplicationService,
  private val submissionsTransformer: SubmissionsTransformer,
  private val offenderService: Cas2OffenderService,
  private val externalUserService: ExternalUserService,
  private val cas2UserService: Cas2UserService,
) {

  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/submissions"],
  )
  fun submissionsGet(@RequestParam page: Int?): ResponseEntity<List<Cas2SubmittedApplicationSummary>> {
    val principal = httpAuthService.getCas2AuthenticatedPrincipalOrThrow()
    if (principal.isExternalUser()) {
      ensureExternalUserPersisted()
    } else {
      ensureNomisUserPersisted()
    }

    val sortDirection = SortDirection.asc
    val sortBy = "submittedAt"

    val (applications, metadata) = applicationService.getAllSubmittedApplicationsForAssessor(PageCriteria(sortBy, sortDirection, page))

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(getPersonNamesAndTransformToSummaries(applications))
  }

  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/submissions/{applicationId}"],
  )
  fun submissionsApplicationIdGet(@PathVariable applicationId: UUID): ResponseEntity<Cas2SubmittedApplication> {
    val principal = httpAuthService.getCas2AuthenticatedPrincipalOrThrow()
    if (principal.isExternalUser()) {
      ensureExternalUserPersisted()
    } else {
      ensureNomisUserPersisted()
    }

    val application = extractEntityFromCasResult(applicationService.getSubmittedApplicationForAssessor(applicationId))
    return ResponseEntity.ok(getPersonDetailAndTransform(application))
  }

  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/submissions"],
  )
  @Transactional
  fun submissionsPost(@RequestBody submitCas2Application: SubmitCas2Application): ResponseEntity<Unit> {
    val user = cas2UserService.getUserForRequest()
    val submitResult = applicationService.submitApplication(submitCas2Application, user)

    extractEntityFromCasResult(submitResult)

    return ResponseEntity(HttpStatus.OK)
  }

  private fun ensureExternalUserPersisted() {
    externalUserService.getUserForRequest()
  }

  private fun ensureNomisUserPersisted() {
    cas2UserService.getUserForRequest()
  }

  private fun getPersonNamesAndTransformToSummaries(applicationSummaries: List<Cas2ApplicationSummaryEntity>): List<Cas2SubmittedApplicationSummary> {
    val crns = applicationSummaries.map { it.crn }

    val personNamesMap = offenderService.getMapOfPersonNamesAndCrns(crns)

    return applicationSummaries.map { application ->
      submissionsTransformer.transformJpaSummaryToApiRepresentation(application, personNamesMap[application.crn]!!)
    }
  }

  private fun getPersonDetailAndTransform(
    application: Cas2ApplicationEntity,
  ): Cas2SubmittedApplication {
    val personInfo = offenderService.getFullInfoForPersonOrThrow(application.crn)

    return submissionsTransformer.transformJpaToApiRepresentation(
      application,
      personInfo,
    )
  }
}
