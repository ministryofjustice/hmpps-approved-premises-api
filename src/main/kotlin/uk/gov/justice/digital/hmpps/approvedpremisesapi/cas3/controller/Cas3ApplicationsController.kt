package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import com.fasterxml.jackson.databind.ObjectMapper
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3ApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy.CheckUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID

@Cas3Controller
class Cas3ApplicationsController(
  private val cas3ApplicationService: Cas3ApplicationService,
  private val applicationService: ApplicationService,
  private val userService: UserService,
  private val offenderDetailService: OffenderDetailService,
  private val cas3ApplicationTransformer: Cas3ApplicationTransformer,
  private val objectMapper: ObjectMapper,
) {

  @GetMapping("/applications")
  fun getApplicationsForUser(): ResponseEntity<List<Cas3ApplicationSummary>> {
    val user = userService.getUserForRequest()

    val applications = applicationService.getAllApplicationsForUsername(user, ServiceName.temporaryAccommodation)

    return ResponseEntity.ok(
      getPersonDetailAndTransformToSummary(
        applications = applications,
        laoStrategy = CheckUserAccess(user.deliusUsername),
      ),
    )
  }

  @GetMapping("/applications/{applicationId}")
  fun getApplicationById(@PathVariable applicationId: UUID): ResponseEntity<Cas3Application> {
    val user = userService.getUserForRequest()

    val applicationResult = applicationService.getApplicationForUsername(applicationId, user.deliusUsername)

    val application = extractEntityFromCasResult(applicationResult) as TemporaryAccommodationApplicationEntity
    return ResponseEntity.ok(
      getPersonDetailAndTransform(
        application = application,
        user = user,
        ignoreLaoRestrictions = false,
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
      when (val personInfoResult = offenderDetailService.getPersonInfoResult(body.crn, user.deliusUsername, false)) {
        is PersonInfoResult.NotFound, is PersonInfoResult.Unknown -> throw NotFoundProblem(
          personInfoResult.crn,
          "Offender",
        )

        is PersonInfoResult.Success.Restricted -> throw ForbiddenProblem()
        is PersonInfoResult.Success.Full -> personInfoResult
      }

    val applicationResult = createApplication(
      personInfo,
      user,
      body,
      createWithRisks,
    )

    val application = extractEntityFromCasResult(applicationResult)

    return ResponseEntity
      .created(URI.create("/applications/${application.id}"))
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

    val serializedData = objectMapper.writeValueAsString(body.data)

    val applicationResult = cas3ApplicationService.updateApplication(
      applicationId = applicationId,
      data = serializedData,
    )

    val updatedApplication = extractEntityFromCasResult(applicationResult)

    return ResponseEntity.ok(getPersonDetailAndTransform(updatedApplication, user, false))
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

  private fun createApplication(
    personInfo: PersonInfoResult.Success.Full,
    user: UserEntity,
    body: Cas3NewApplication,
    createWithRisks: Boolean?,
  ): CasResult<out TemporaryAccommodationApplicationEntity> = applicationService.createTemporaryAccommodationApplication(
    body.crn,
    user,
    body.convictionId,
    body.deliusEventNumber,
    body.offenceId,
    createWithRisks,
    personInfo,
  )

  private fun getPersonDetailAndTransform(
    application: TemporaryAccommodationApplicationEntity,
    user: UserEntity,
    ignoreLaoRestrictions: Boolean,
  ): Cas3Application {
    val personInfo = offenderDetailService.getPersonInfoResult(application.crn, user.deliusUsername, ignoreLaoRestrictions)

    return cas3ApplicationTransformer.transformJpaToApi(application, personInfo)
  }

  private fun getPersonDetailAndTransformToSummary(
    applications: List<ApplicationSummary>,
    laoStrategy: LaoStrategy,
  ): List<Cas3ApplicationSummary> {
    val crns = applications.map { it.getCrn() }
    val personInfoResults = offenderDetailService.getPersonInfoResults(crns.toSet(), laoStrategy)

    return applications.map {
      val crn = it.getCrn()
      cas3ApplicationTransformer.transformDomainToCas3ApplicationSummary(
        it,
        personInfoResults.firstOrNull { it.crn == crn } ?: PersonInfoResult.Unknown(crn),
      )
    }
  }
}
