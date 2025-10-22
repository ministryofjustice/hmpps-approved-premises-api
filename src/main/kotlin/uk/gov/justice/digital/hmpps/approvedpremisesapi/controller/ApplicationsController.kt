package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationCreationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("\${openapi.approvedPremises.base-path:}")
@Suppress("LongParameterList", "TooManyFunctions")
class ApplicationsController(
  private val applicationService: ApplicationService,
  private val applicationsTransformer: ApplicationsTransformer,
  private val objectMapper: ObjectMapper,
  private val userService: UserService,
  private val cas1WithdrawableService: Cas1WithdrawableService,
  private val cas1RequestForPlacementService: Cas1RequestForPlacementService,
  private val cas1ApplicationTimelineNoteService: Cas1ApplicationTimelineNoteService,
  private val applicationTimelineNoteTransformer: ApplicationTimelineNoteTransformer,
  private val cas1ApplicationCreationService: Cas1ApplicationCreationService,
  private val offenderDetailService: OffenderDetailService,
) {
  @Operation(
    summary = "Creates an application",
    description = """deprecated for cas3, use /cas3/applications""",
  )
  @PostMapping("/applications")
  @Transactional
  fun applicationsPost(
    @RequestBody body: NewApplication,
    @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?,
    @RequestParam(value = "createWithRisks", required = false) createWithRisks: Boolean?,
  ): ResponseEntity<Application> {
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
      xServiceName ?: ServiceName.approvedPremises,
      personInfo,
      user,
      body,
      createWithRisks,
    )

    val application = extractEntityFromCasResult(applicationResult)

    return ResponseEntity
      .created(URI.create("/applications/${application.id}"))
      .body(applicationsTransformer.transformJpaToApi(application, personInfo))
  }

  @Suppress("TooGenericExceptionThrown")
  private fun createApplication(
    serviceName: ServiceName,
    personInfo: PersonInfoResult.Success.Full,
    user: UserEntity,
    body: NewApplication,
    createWithRisks: Boolean?,
  ): CasResult<out ApplicationEntity> = when (serviceName) {
    ServiceName.approvedPremises ->
      cas1ApplicationCreationService.createApprovedPremisesApplication(
        personInfo.offenderDetailSummary,
        user,
        body.convictionId,
        body.deliusEventNumber,
        body.offenceId,
      )

    ServiceName.temporaryAccommodation -> {
      applicationService.createTemporaryAccommodationApplication(
        body.crn,
        user,
        body.convictionId,
        body.deliusEventNumber,
        body.offenceId,
        createWithRisks,
        personInfo,
      )
    }

    ServiceName.cas2 -> throw RuntimeException(
      "CAS2 now has its own " +
        "Cas2ApplicationsController",
    )

    ServiceName.cas2v2 -> throw RuntimeException(
      "CAS2v2 now has its own " +
        "Cas2v2ApplicationsController",
    )
  }

  @Operation(summary = "Updates an application")
  @PutMapping("/applications/{applicationId}")
  @Transactional
  fun applicationsApplicationIdPut(
    @PathVariable applicationId: UUID,
    @RequestBody body: UpdateApplication,
  ): ResponseEntity<Application> {
    val user = userService.getUserForRequest()

    val serializedData = objectMapper.writeValueAsString(body.data)

    val applicationResult = when (body) {
      is UpdateApprovedPremisesApplication -> cas1ApplicationCreationService.updateApplication(
        applicationId = applicationId,
        Cas1ApplicationCreationService.Cas1ApplicationUpdateFields(
          data = serializedData,
          isWomensApplication = body.isWomensApplication,
          isEmergencyApplication = body.isEmergencyApplication,
          apType = body.apType,
          releaseType = body.releaseType?.name,
          arrivalDate = body.arrivalDate,
          isInapplicable = body.isInapplicable,
          noticeType = body.noticeType,
        ),
        userForRequest = user,
      )

      is UpdateTemporaryAccommodationApplication -> applicationService.updateTemporaryAccommodationApplication(
        applicationId = applicationId,
        data = serializedData,
      )

      else -> throw RuntimeException("Unsupported UpdateApplication type: ${body::class.qualifiedName}")
    }

    val updatedApplication = extractEntityFromCasResult(applicationResult)

    return ResponseEntity.ok(getPersonDetailAndTransform(updatedApplication, user))
  }

  @Operation(summary = "Add a note on applications")
  @PostMapping("/applications/{applicationId}/notes")
  fun applicationsApplicationIdNotesPost(
    @PathVariable applicationId: UUID,
    @RequestBody body: NewApplicationTimelineNote,
  ): ResponseEntity<ApplicationTimelineNote> {
    val user = userService.getUserForRequest()
    val savedNote = cas1ApplicationTimelineNoteService.saveApplicationTimelineNote(applicationId, body.note, user)

    return ResponseEntity.ok(applicationTimelineNoteTransformer.transformJpaToApi(savedNote))
  }

  @Operation(summary = "Withdraws an application with a reason")
  @PostMapping("/applications/{applicationId}/withdrawal")
  fun applicationsApplicationIdWithdrawalPost(
    @PathVariable applicationId: UUID,
    @RequestBody body: NewWithdrawal,
  ): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    return ResponseEntity.ok(
      extractEntityFromCasResult(
        cas1WithdrawableService.withdrawApplication(
          applicationId = applicationId,
          user = user,
          withdrawalReason = body.reason.value,
          otherReason = body.otherReason,
        ),
      ),
    )
  }

  @Operation(summary = "Returns a list of Requests for Placement for the given application.")
  @GetMapping("/applications/{applicationId}/requests-for-placement")
  fun applicationsApplicationIdRequestsForPlacementGet(
    @PathVariable applicationId: UUID,
  ): ResponseEntity<List<RequestForPlacement>> = ResponseEntity.ok(
    extractEntityFromCasResult(cas1RequestForPlacementService.getRequestsForPlacementByApplication(applicationId, userService.getUserForRequest())),
  )

  private fun getPersonDetailAndTransform(
    application: ApplicationEntity,
    user: UserEntity,
    ignoreLaoRestrictions: Boolean = false,
  ): Application {
    val personInfo = offenderDetailService.getPersonInfoResult(application.crn, user.deliusUsername, ignoreLaoRestrictions)

    return applicationsTransformer.transformJpaToApi(application, personInfo)
  }
}
