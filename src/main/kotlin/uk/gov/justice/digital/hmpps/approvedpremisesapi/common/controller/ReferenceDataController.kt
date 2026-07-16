package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CancellationReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DepartureReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DestinationProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NonArrivalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralRejectionReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3VoidBedspaceReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralRejectionReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DestinationProviderTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LocalAuthorityAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.MoveOnCategoryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NonArrivalReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationDeliveryUnitTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationRegionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ReferralRejectionReasonTransformer
import java.util.UUID

@RestController
@Tag(name = "Reference Data")
class ReferenceDataController(
  private val departureReasonRepository: DepartureReasonRepository,
  private val moveOnCategoryRepository: MoveOnCategoryRepository,
  private val destinationProviderRepository: DestinationProviderRepository,
  private val cancellationReasonRepository: CancellationReasonRepository,
  private val cas3VoidBedspaceReasonRepository: Cas3VoidBedspaceReasonRepository,
  private val localAuthorityAreaRepository: LocalAuthorityAreaRepository,
  private val characteristicRepository: CharacteristicRepository,
  private val probationRegionRepository: ProbationRegionRepository,
  private val nonArrivalReasonRepository: NonArrivalReasonRepository,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
  private val referralRejectionReasonRepository: ReferralRejectionReasonRepository,
  private val departureReasonTransformer: DepartureReasonTransformer,
  private val moveOnCategoryTransformer: MoveOnCategoryTransformer,
  private val destinationProviderTransformer: DestinationProviderTransformer,
  private val cancellationReasonTransformer: CancellationReasonTransformer,
  private val cas3VoidBedspaceReasonTransformer: Cas3VoidBedspaceReasonTransformer,
  private val localAuthorityAreaTransformer: LocalAuthorityAreaTransformer,
  private val characteristicTransformer: CharacteristicTransformer,
  private val probationRegionTransformer: ProbationRegionTransformer,
  private val nonArrivalReasonTransformer: NonArrivalReasonTransformer,
  private val probationDeliveryUnitTransformer: ProbationDeliveryUnitTransformer,
  private val referralRejectionReasonTransformer: ReferralRejectionReasonTransformer,
  private val apAreaRepository: ApAreaRepository,
  private val apAreaTransformer: ApAreaTransformer,
) {

  @Operation(
    summary = "Lists all available characteristics",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = Characteristic::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/characteristics"],
    produces = ["application/json"],
  )
  fun referenceDataCharacteristicsGet(
    @Parameter(
      description = "If given, only characteristics for this service will be returned",
      `in` = ParameterIn.HEADER,
      schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"]),
    )
    @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?,
    @RequestParam(value = "modelScope", required = false) modelScope: String?,
  ): ResponseEntity<List<Characteristic>> = ResponseEntity.ok(
    characteristicRepository.findActiveByServiceScopeAndModelScope(
      serviceScope = xServiceName?.value ?: "*",
      modelScope = toModelScopeEnum(modelScope).value,
    ).map(characteristicTransformer::transformJpaToApi),
  )

  /*
  this can be refactored to use the enum directly in the controller
  when we move away from the manually written openApiSpec files
   */
  private fun toModelScopeEnum(modelScope: String?): Characteristic.ModelScope = when (modelScope) {
    "PREMISES" -> Characteristic.ModelScope.premises
    "BEDSPACE", "ROOM" -> Characteristic.ModelScope.room
    else -> Characteristic.ModelScope.star
  }

  @Operation(
    summary = "Lists all local authorities",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = LocalAuthorityArea::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/local-authority-areas"],
    produces = ["application/json"],
  )
  fun referenceDataLocalAuthorityAreasGet(): ResponseEntity<List<LocalAuthorityArea>> {
    val localAuthorities = localAuthorityAreaRepository.findAll()

    return ResponseEntity.ok(localAuthorities.map(localAuthorityAreaTransformer::transformJpaToApi))
  }

  @Operation(
    summary = "Lists all departure reasons",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = DepartureReason::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/departure-reasons"],
    produces = ["application/json"],
  )
  fun referenceDataDepartureReasonsGet(
    @Parameter(
      description = "If given, only departure reasons for this service will be returned",
      `in` = ParameterIn.HEADER,
      schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"]),
    )
    @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?,
    @RequestParam(value = "includeInactive", required = false) includeInactive: Boolean?,
  ): ResponseEntity<List<DepartureReason>> {
    val reasons = when (xServiceName != null) {
      true -> when (includeInactive) {
        true -> departureReasonRepository.findAllByServiceScope(xServiceName.value)
        else -> departureReasonRepository.findActiveByServiceScope(xServiceName.value)
      }

      false -> when (includeInactive) {
        true -> departureReasonRepository.findAll()
        else -> departureReasonRepository.findActive()
      }
    }

    return ResponseEntity.ok(reasons.map(departureReasonTransformer::transformJpaToApi))
  }

  @Operation(
    summary = "Lists all move-on categories for departures",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = MoveOnCategory::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/move-on-categories"],
    produces = ["application/json"],
  )
  fun referenceDataMoveOnCategoriesGet(
    @Parameter(
      description = "If given, only move-on categories for this service will be returned",
      `in` = ParameterIn.HEADER,
      schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"]),
    )
    @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?,
    @RequestParam(value = "includeInactive", required = false) includeInactive: Boolean?,
  ): ResponseEntity<List<MoveOnCategory>> {
    val moveOnCategories = when {
      xServiceName == null && includeInactive == true -> moveOnCategoryRepository.findAll()
      xServiceName == null -> moveOnCategoryRepository.findActive()
      includeInactive == true -> moveOnCategoryRepository.findAllByServiceScope(xServiceName.value)
      else -> moveOnCategoryRepository.findActiveByServiceScope(xServiceName.value)
    }.let {
      if (xServiceName == ServiceName.temporaryAccommodation) it.sortedBy { category -> category.name } else it
    }

    return ResponseEntity.ok(moveOnCategories.map(moveOnCategoryTransformer::transformJpaToApi))
  }

  @Operation(
    summary = "Lists all destination providers for departures",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = DestinationProvider::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/destination-providers"],
    produces = ["application/json"],
  )
  fun referenceDataDestinationProvidersGet(): ResponseEntity<List<DestinationProvider>> {
    val destinationProviders = destinationProviderRepository.findAll()

    return ResponseEntity.ok(destinationProviders.map(destinationProviderTransformer::transformJpaToApi))
  }

  @Operation(
    summary = "Lists all cancellation reasons",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = CancellationReason::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/cancellation-reasons"],
    produces = ["application/json"],
  )
  fun referenceDataCancellationReasonsGet(
    @Parameter(
      description = "If given, only departure reasons for this service will be returned",
      `in` = ParameterIn.HEADER,
      schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"]),
    )
    @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?,
  ): ResponseEntity<List<CancellationReason>> {
    val cancellationReasons = when (xServiceName != null) {
      true -> if (xServiceName == ServiceName.temporaryAccommodation) {
        cancellationReasonRepository.findAllByServiceScopeIsActive(xServiceName.value)
      } else {
        cancellationReasonRepository.findAllByServiceScope(xServiceName.value)
      }

      false -> cancellationReasonRepository.findAll()
    }

    return ResponseEntity.ok(cancellationReasons.map(cancellationReasonTransformer::transformJpaToApi))
  }

  @Deprecated("use /cas3/reference-data?VOID_BEDSPACE_REASONS")
  @Operation(
    summary = "Lists all reasons for losing beds",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = LostBedReason::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/lost-bed-reasons"],
    produces = ["application/json"],
  )
  fun referenceDataLostBedReasonsGet(
    @Parameter(
      description = "If given, only lost bed reasons for this service will be returned",
      `in` = ParameterIn.HEADER,
      schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"]),
    )
    @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?,
  ): ResponseEntity<List<LostBedReason>> {
    val voidBedspaceReasons = when (xServiceName == ServiceName.temporaryAccommodation) {
      true -> cas3VoidBedspaceReasonRepository.findAllActive()
      false -> throw ForbiddenProblem()
    }

    return ResponseEntity.ok(voidBedspaceReasons.map(cas3VoidBedspaceReasonTransformer::transformJpaToApi))
  }

  @Operation(
    summary = "Lists all probation regions",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = ProbationRegion::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/probation-regions"],
    produces = ["application/json"],
  )
  fun referenceDataProbationRegionsGet(): ResponseEntity<List<ProbationRegion>> {
    val probationRegions = probationRegionRepository.findAll()

    return ResponseEntity.ok(probationRegions.map(probationRegionTransformer::transformJpaToApi))
  }

  @Operation(
    summary = "Lists all probation regions",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = ApArea::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/ap-areas"],
    produces = ["application/json"],
  )
  fun referenceDataApAreasGet(): ResponseEntity<List<ApArea>> {
    val apAreas = apAreaRepository.findAll()

    return ResponseEntity.ok(apAreas.map(apAreaTransformer::transformJpaToApi))
  }

  @Operation(
    summary = "Lists reasons for non-arrivals",
    description = """deprecated, use /cas1/reference-data/non-arrival-reasons""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = NonArrivalReason::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/non-arrival-reasons"],
    produces = ["application/json"],
  )
  fun referenceDataNonArrivalReasonsGet(): ResponseEntity<List<NonArrivalReason>> {
    val reasons = nonArrivalReasonRepository.findAll().filter { it.isActive }

    return ResponseEntity.ok(reasons.map(nonArrivalReasonTransformer::transformJpaToApi))
  }

  @Operation(
    summary = "Lists probation delivery units, optionally filtered by region",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = ProbationDeliveryUnit::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/probation-delivery-units"],
    produces = ["application/json"],
  )
  fun referenceDataProbationDeliveryUnitsGet(
    @RequestParam(value = "probationRegionId", required = false) probationRegionId: UUID?,
  ): ResponseEntity<List<ProbationDeliveryUnit>> {
    val probationDeliveryUnits = when (probationRegionId) {
      null -> probationDeliveryUnitRepository.findAll()
      else -> probationDeliveryUnitRepository.findAllByProbationRegionId(probationRegionId)
    }

    return ResponseEntity.ok(probationDeliveryUnits.map(probationDeliveryUnitTransformer::transformJpaToApi))
  }

  @Operation(
    summary = "Lists all referral rejection reasons",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = ReferralRejectionReason::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/referral-rejection-reasons"],
    produces = ["application/json"],
  )
  fun referenceDataReferralRejectionReasonsGet(
    @Parameter(
      description = "If given, only referral rejection reasons for this service will be returned",
      `in` = ParameterIn.HEADER,
      schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"]),
    )
    @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?,
  ): ResponseEntity<List<ReferralRejectionReason>> {
    val referralRejectionReasons = when (xServiceName == ServiceName.temporaryAccommodation) {
      true -> referralRejectionReasonRepository.findAllActive()
      else -> throw ForbiddenProblem()
    }

    return ResponseEntity.ok(referralRejectionReasons.map(referralRejectionReasonTransformer::transformJpaToApi))
  }
}
