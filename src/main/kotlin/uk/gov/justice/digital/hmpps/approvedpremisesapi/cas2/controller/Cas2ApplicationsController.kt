package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.controller

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2.ApplicationsCas2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssignmentType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID

@Service
class Cas2ApplicationsController(
  private val applicationService: Cas2ApplicationService,
  private val cas2ApplicationsTransformer: Cas2ApplicationsTransformer,
  private val objectMapper: ObjectMapper,
  private val offenderService: Cas2OffenderService,
  private val userService: Cas2UserService,
) : ApplicationsCas2Delegate {

  override fun getCas2Applications(
    isSubmitted: Boolean?,
    page: Int?,
    prisonCode: String?,
    assignmentType: AssignmentType?,
  ): ResponseEntity<List<Cas2ApplicationSummary>> {
    val user = userService.getUserForRequest()

    prisonCode?.let { if (user.activeCaseloadId == null || prisonCode != user.activeCaseloadId) throw ForbiddenProblem() }

    val pageCriteria = PageCriteria("createdAt", SortDirection.desc, page)

    if (assignmentType != null) {
      val (results, metadata) = applicationService.getApplicationSummaries(
        user,
        pageCriteria,
        assignmentType,
        forPrison = prisonCode != null,
      )
      return ResponseEntity.ok().headers(
        metadata?.toHeaders(),
      ).body(getPersonNamesAndTransformToSummaries(results))
    }

    val (applications, metadata) = applicationService.getApplications(prisonCode, isSubmitted, user, pageCriteria)

    return ResponseEntity.ok().headers(
      metadata?.toHeaders(),
    ).body(getPersonNamesAndTransformToSummaries(applications))
  }

  override fun getCas2Application(applicationId: UUID): ResponseEntity<Cas2Application> {
    val user = userService.getUserForRequest()
    val application = extractEntityFromCasResult(applicationService.getApplicationForUser(applicationId, user))
    return ResponseEntity.ok(getPersonDetailAndTransform(application))
  }

  @Transactional
  override fun createCas2Application(body: NewApplication): ResponseEntity<Cas2Application> {
    val user = userService.getUserForRequest()
    val personInfo = offenderService.getFullInfoForPersonOrThrow(body.crn)
    val applicationResult = applicationService.createApplication(personInfo, user)

    val application = extractEntityFromCasResult(applicationResult)

    return ResponseEntity
      .created(URI.create("/cas2/applications/${application.id}"))
      .body(cas2ApplicationsTransformer.transformJpaToApi(application, personInfo))
  }

  @Transactional
  override fun updateCas2Application(
    applicationId: UUID,
    body: UpdateCas2Application,
  ): ResponseEntity<Cas2Application> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(body.data)

    val applicationResult = applicationService.updateApplication(
      applicationId =
      applicationId,
      data = serializedData,
      user,
    )

    return ResponseEntity.ok(getPersonDetailAndTransform(extractEntityFromCasResult(applicationResult)))
  }

  @Transactional
  override fun abandonCas2Application(applicationId: UUID): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()
    extractEntityFromCasResult(applicationService.abandonApplication(applicationId, user))
    return ResponseEntity.ok(Unit)
  }

  private fun getPersonNamesAndTransformToSummaries(applicationSummaries: List<Cas2ApplicationSummaryEntity>): List<uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationSummary> {
    val crns = applicationSummaries.map { it.crn }

    val personNamesMap = offenderService.getMapOfPersonNamesAndCrns(crns)

    return applicationSummaries.map { application ->
      cas2ApplicationsTransformer.transformJpaSummaryToSummary(application, personNamesMap[application.crn]!!)
    }
  }

  private fun getPersonDetailAndTransform(
    application: Cas2ApplicationEntity,
  ): Cas2Application {
    val personInfo = offenderService.getFullInfoForPersonOrThrow(application.crn)

    return cas2ApplicationsTransformer.transformJpaToApi(application, personInfo)
  }
}
