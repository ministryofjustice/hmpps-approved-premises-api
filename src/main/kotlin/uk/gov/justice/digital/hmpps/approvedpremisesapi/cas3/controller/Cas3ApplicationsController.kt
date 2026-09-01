package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3ApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID

@Cas3Controller
class Cas3ApplicationsController(
  private val cas3ApplicationService: Cas3ApplicationService,
  private val userService: UserService,
  private val offenderDetailService: OffenderDetailService,
  private val offenderService: OffenderService,
  private val cas3ApplicationTransformer: Cas3ApplicationTransformer,
  private val jsonMapper: JsonMapper,
) {

  @GetMapping("/applications")
  fun getApplicationsForUser(): ResponseEntity<List<Cas3ApplicationSummary>> {
    val user = userService.getUserForRequest()

    val applications = cas3ApplicationService.getApplicationSummariesForUser(user)

    return ResponseEntity.ok(
      getPersonDetailAndTransformToSummary(
        applications = applications,
        laoStrategy = user.cas3LaoStrategy(),
      ),
    )
  }

  @GetMapping("/applications/{applicationId}")
  fun getApplicationById(@PathVariable applicationId: UUID): ResponseEntity<Cas3Application> {
    val user = userService.getUserForRequest()

    val applicationResult = cas3ApplicationService.getApplicationForUsername(applicationId, user.deliusUsername)

    val application = extractEntityFromCasResult(applicationResult)
    return ResponseEntity.ok(
      getPersonDetailAndTransform(
        application = application,
        laoStrategy = user.cas3LaoStrategy(),
      ),
    )
  }

  @Transactional
  @PostMapping(
    "/applications",
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun postApplication(
    @RequestBody body: Cas3NewApplication,
    @RequestParam createWithRisks: Boolean?,
  ): ResponseEntity<Cas3Application> {
    val user = userService.getUserForRequest()

    val personInfo =
      when (val personInfoResult = offenderDetailService.getPersonInfoResult(body.crn, user.cas3LaoStrategy(), includeTier = false)) {
        is PersonInfoResult.NotFound -> throw NotFoundProblem(
          personInfoResult.crn,
          "Offender",
        )

        is PersonInfoResult.Success.Restricted -> throw ForbiddenProblem()
        is PersonInfoResult.Success.Full -> personInfoResult
      }

    val applicationResult = cas3ApplicationService.createApplication(
      body.crn,
      user,
      body.convictionId,
      body.deliusEventNumber,
      body.offenceId,
      createWithRisks,
      personInfo,
      body.referredBy,
    )

    val application = extractEntityFromCasResult(applicationResult)

    return ResponseEntity
      .created(URI.create("/cas3/applications/${application.id}"))
      .body(cas3ApplicationTransformer.transformJpaToApi(application, personInfo))
  }

  @Transactional
  @PutMapping(
    "/applications/{applicationId}",
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun putApplication(
    @PathVariable applicationId: UUID,
    @RequestBody body: Cas3UpdateApplication,
  ): ResponseEntity<Cas3Application> {
    val user = userService.getUserForRequest()

    val serializedData = jsonMapper.writeValueAsString(body.data)

    val applicationResult = cas3ApplicationService.updateApplication(
      applicationId = applicationId,
      data = serializedData,
    )

    val updatedApplication = extractEntityFromCasResult(applicationResult)

    return ResponseEntity.ok(getPersonDetailAndTransform(updatedApplication, user.cas3LaoStrategy()))
  }

  @PostMapping(
    "/applications/{applicationId}/submission",
    consumes = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun postApplicationSubmission(
    @PathVariable applicationId: UUID,
    @RequestBody cas3SubmitApplication: Cas3SubmitApplication,
  ): ResponseEntity<Unit> {
    ensureEntityFromCasResultIsSuccess(cas3ApplicationService.submitApplication(applicationId, cas3SubmitApplication))

    return ResponseEntity(HttpStatus.OK)
  }

  @DeleteMapping("/applications/{applicationId}")
  fun deleteApplication(@PathVariable applicationId: UUID): ResponseEntity<Unit> = ResponseEntity.ok(
    extractEntityFromCasResult(cas3ApplicationService.markApplicationAsDeleted(applicationId)),
  )

  private fun getPersonDetailAndTransform(
    application: TemporaryAccommodationApplicationEntity,
    laoStrategy: LaoStrategy,
  ): Cas3Application {
    val personInfo = offenderDetailService.getPersonInfoResult(application.crn, laoStrategy)

    return cas3ApplicationTransformer.transformJpaToApi(application, personInfo)
  }

  private fun getPersonDetailAndTransformToSummary(
    applications: List<ApplicationSummary>,
    laoStrategy: LaoStrategy,
  ): List<Cas3ApplicationSummary> {
    val crns = applications.map { it.getCrn() }
    val personSummaryInfoResults = offenderService.getPersonSummaryInfoResults(crns.toSet(), laoStrategy)

    val personSummaryInfoResultsByCrn = personSummaryInfoResults.associateBy { it.crn }

    return applications.map {
      val crn = it.getCrn()
      cas3ApplicationTransformer.transformDomainToCas3ApplicationSummary(
        it,
        personSummaryInfoResultsByCrn[crn] ?: PersonSummaryInfoResult.NotFound(crn),
      )
    }
  }
}
