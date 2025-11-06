
package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
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

@RestController
interface ReferenceDataApi {

  fun getDelegate(): ReferenceDataApiDelegate = object : ReferenceDataApiDelegate {}

  @Operation(
    tags = ["Reference Data"],
    summary = "Lists all probation regions",
    operationId = "referenceDataApAreasGet",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = ApArea::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/ap-areas"],
    produces = ["application/json"],
  )
  fun referenceDataApAreasGet(): ResponseEntity<List<ApArea>> = getDelegate().referenceDataApAreasGet()

  @Operation(
    tags = ["Reference Data"],
    summary = "Lists all cancellation reasons",
    operationId = "referenceDataCancellationReasonsGet",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = CancellationReason::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/cancellation-reasons"],
    produces = ["application/json"],
  )
  fun referenceDataCancellationReasonsGet(@Parameter(description = "If given, only departure reasons for this service will be returned", `in` = ParameterIn.HEADER, schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"])) @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?): ResponseEntity<List<CancellationReason>> = getDelegate().referenceDataCancellationReasonsGet(xServiceName)

  @Operation(
    tags = ["Characteristics"],
    summary = "Lists all available characteristics",
    operationId = "referenceDataCharacteristicsGet",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = Characteristic::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/characteristics"],
    produces = ["application/json"],
  )
  fun referenceDataCharacteristicsGet(@Parameter(description = "If given, only characteristics for this service will be returned", `in` = ParameterIn.HEADER, schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"])) @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?, @RequestParam(value = "modelScope", required = false) modelScope: kotlin.String?): ResponseEntity<List<Characteristic>> = getDelegate().referenceDataCharacteristicsGet(xServiceName, modelScope)

  @Operation(
    tags = ["Reference Data"],
    summary = "Lists all departure reasons",
    operationId = "referenceDataDepartureReasonsGet",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = DepartureReason::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/departure-reasons"],
    produces = ["application/json"],
  )
  fun referenceDataDepartureReasonsGet(@Parameter(description = "If given, only departure reasons for this service will be returned", `in` = ParameterIn.HEADER, schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"])) @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?, @RequestParam(value = "includeInactive", required = false) includeInactive: kotlin.Boolean?): ResponseEntity<List<DepartureReason>> = getDelegate().referenceDataDepartureReasonsGet(xServiceName, includeInactive)

  @Operation(
    tags = ["Reference Data"],
    summary = "Lists all destination providers for departures",
    operationId = "referenceDataDestinationProvidersGet",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = DestinationProvider::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/destination-providers"],
    produces = ["application/json"],
  )
  fun referenceDataDestinationProvidersGet(): ResponseEntity<List<DestinationProvider>> = getDelegate().referenceDataDestinationProvidersGet()

  @Operation(
    tags = ["Local Authorities"],
    summary = "Lists all local authorities",
    operationId = "referenceDataLocalAuthorityAreasGet",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = LocalAuthorityArea::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/local-authority-areas"],
    produces = ["application/json"],
  )
  fun referenceDataLocalAuthorityAreasGet(): ResponseEntity<List<LocalAuthorityArea>> = getDelegate().referenceDataLocalAuthorityAreasGet()

  @Operation(
    tags = ["Reference Data"],
    summary = "Lists all reasons for losing beds",
    operationId = "referenceDataLostBedReasonsGet",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = LostBedReason::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/lost-bed-reasons"],
    produces = ["application/json"],
  )
  fun referenceDataLostBedReasonsGet(@Parameter(description = "If given, only lost bed reasons for this service will be returned", `in` = ParameterIn.HEADER, schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"])) @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?): ResponseEntity<List<LostBedReason>> = getDelegate().referenceDataLostBedReasonsGet(xServiceName)

  @Operation(
    tags = ["Reference Data"],
    summary = "Lists all move-on categories for departures",
    operationId = "referenceDataMoveOnCategoriesGet",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = MoveOnCategory::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/move-on-categories"],
    produces = ["application/json"],
  )
  fun referenceDataMoveOnCategoriesGet(@Parameter(description = "If given, only move-on categories for this service will be returned", `in` = ParameterIn.HEADER, schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"])) @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?, @RequestParam(value = "includeInactive", required = false) includeInactive: kotlin.Boolean?): ResponseEntity<List<MoveOnCategory>> = getDelegate().referenceDataMoveOnCategoriesGet(xServiceName, includeInactive)

  @Operation(
    tags = ["Reference Data"],
    summary = "Lists reasons for non-arrivals",
    operationId = "referenceDataNonArrivalReasonsGet",
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
  fun referenceDataNonArrivalReasonsGet(): ResponseEntity<List<NonArrivalReason>> = getDelegate().referenceDataNonArrivalReasonsGet()

  @Operation(
    tags = ["Reference Data"],
    summary = "Lists probation delivery units, optionally filtered by region",
    operationId = "referenceDataProbationDeliveryUnitsGet",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = ProbationDeliveryUnit::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/probation-delivery-units"],
    produces = ["application/json"],
  )
  fun referenceDataProbationDeliveryUnitsGet(@RequestParam(value = "probationRegionId", required = false) probationRegionId: java.util.UUID?): ResponseEntity<List<ProbationDeliveryUnit>> = getDelegate().referenceDataProbationDeliveryUnitsGet(probationRegionId)

  @Operation(
    tags = ["Reference Data"],
    summary = "Lists all probation regions",
    operationId = "referenceDataProbationRegionsGet",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = ProbationRegion::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/probation-regions"],
    produces = ["application/json"],
  )
  fun referenceDataProbationRegionsGet(): ResponseEntity<List<ProbationRegion>> = getDelegate().referenceDataProbationRegionsGet()

  @Operation(
    tags = ["Reference Data"],
    summary = "Lists all referral rejection reasons",
    operationId = "referenceDataReferralRejectionReasonsGet",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(array = ArraySchema(schema = Schema(implementation = ReferralRejectionReason::class)))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reference-data/referral-rejection-reasons"],
    produces = ["application/json"],
  )
  fun referenceDataReferralRejectionReasonsGet(@Parameter(description = "If given, only referral rejection reasons for this service will be returned", `in` = ParameterIn.HEADER, schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"])) @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?): ResponseEntity<List<ReferralRejectionReason>> = getDelegate().referenceDataReferralRejectionReasonsGet(xServiceName)
}
