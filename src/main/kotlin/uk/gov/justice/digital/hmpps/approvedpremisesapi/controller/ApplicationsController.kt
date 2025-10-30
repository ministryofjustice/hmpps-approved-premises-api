package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationCreationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

@RestController
@RequestMapping("\${openapi.approvedPremises.base-path:}")
@Suppress("LongParameterList", "TooManyFunctions")
class ApplicationsController(
  private val applicationService: ApplicationService,
  private val applicationsTransformer: ApplicationsTransformer,
  private val objectMapper: ObjectMapper,
  private val userService: UserService,
  private val cas1ApplicationCreationService: Cas1ApplicationCreationService,
  private val offenderDetailService: OffenderDetailService,
) {
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

  private fun getPersonDetailAndTransform(
    application: ApplicationEntity,
    user: UserEntity,
    ignoreLaoRestrictions: Boolean = false,
  ): Application {
    val personInfo = offenderDetailService.getPersonInfoResult(application.crn, user.deliusUsername, ignoreLaoRestrictions)

    return applicationsTransformer.transformJpaToApi(application, personInfo)
  }
}
