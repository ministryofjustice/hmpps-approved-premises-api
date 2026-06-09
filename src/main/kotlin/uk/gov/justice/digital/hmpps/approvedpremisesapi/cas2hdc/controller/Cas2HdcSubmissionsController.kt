package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.controller

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcSubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcSubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcSubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcOffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcSubmissionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@Cas2HdcController
class Cas2HdcSubmissionsController(
  private val applicationService: Cas2HdcApplicationService,
  private val cas2HdcSubmissionsTransformer: Cas2HdcSubmissionsTransformer,
  private val offenderService: Cas2HdcOffenderService,
  private val cas2HdcUserService: Cas2HdcUserService,
) {

  @GetMapping("/submissions")
  fun submissionsGet(@RequestParam page: Int?): ResponseEntity<List<Cas2HdcSubmittedApplicationSummary>> {
    cas2HdcUserService.getUserForRequest(Cas2ServiceOrigin.HDC)

    val sortDirection = SortDirection.asc
    val sortBy = "submittedAt"

    val (applications, metadata) = applicationService.getAllSubmittedApplicationsForAssessor(PageCriteria(sortBy, sortDirection, page))

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(getPersonNamesAndTransformToSummaries(applications))
  }

  @GetMapping("/submissions/{applicationId}")
  fun submissionsApplicationIdGet(@PathVariable applicationId: UUID): ResponseEntity<Cas2HdcSubmittedApplication> {
    cas2HdcUserService.getUserForRequest(Cas2ServiceOrigin.HDC)

    val application = extractEntityFromCasResult(applicationService.getSubmittedApplicationForAssessor(applicationId))
    return ResponseEntity.ok(getPersonDetailAndTransform(application))
  }

  @PostMapping("/submissions")
  @Transactional
  fun submissionsPost(@RequestBody cas2HdcSubmitApplication: Cas2HdcSubmitApplication): ResponseEntity<Unit> {
    val user = cas2HdcUserService.getUserForRequest(Cas2ServiceOrigin.HDC)
    val submitResult = applicationService.submitApplication(cas2HdcSubmitApplication, user)

    extractEntityFromCasResult(submitResult)

    return ResponseEntity(HttpStatus.OK)
  }

  private fun getPersonNamesAndTransformToSummaries(applicationSummaries: List<Cas2ApplicationSummaryEntity>): List<Cas2HdcSubmittedApplicationSummary> {
    val crns = applicationSummaries.map { it.crn }

    val personNamesMap = offenderService.getMapOfPersonNamesAndCrns(crns)

    return applicationSummaries.map { application ->
      cas2HdcSubmissionsTransformer.transformJpaSummaryToApiRepresentation(application, personNamesMap[application.crn]!!)
    }
  }

  private fun getPersonDetailAndTransform(
    application: Cas2ApplicationEntity,
  ): Cas2HdcSubmittedApplication {
    val personInfo = offenderService.getFullInfoForPersonOrThrow(application.crn)

    return cas2HdcSubmissionsTransformer.transformJpaToApiRepresentation(
      application,
      personInfo,
    )
  }
}
