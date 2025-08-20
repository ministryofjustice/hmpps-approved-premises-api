package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.ExternalUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.SubmissionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.transformCas2UserEntityToNomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas2Controller
class Cas2SubmissionsController(
  private val httpAuthService: HttpAuthService,
  private val applicationService: Cas2ApplicationService,
  private val submissionsTransformer: SubmissionsTransformer,
  private val offenderService: Cas2OffenderService,
  private val externalUserService: ExternalUserService,
  private val cas2UserService: Cas2UserService,
) {

  @GetMapping("/submissions")
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

  @GetMapping("/submissions/{applicationId}")
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

  @PostMapping("/submissions")
  @Transactional
  fun submissionsPost(@RequestBody submitCas2Application: SubmitCas2Application): ResponseEntity<Unit> {
    val user = cas2UserService.getCas2UserForRequest()
    val submitResult = applicationService.submitApplication(submitCas2Application, transformCas2UserEntityToNomisUserEntity(user))

    extractEntityFromCasResult(submitResult)

    return ResponseEntity(HttpStatus.OK)
  }

  private fun ensureExternalUserPersisted() {
    externalUserService.getUserForRequest()
  }

  private fun ensureNomisUserPersisted() {
    cas2UserService.getCas2UserForRequest()
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
