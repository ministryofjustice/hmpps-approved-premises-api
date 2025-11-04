
package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName

@RestController
interface ReportsCas1 {

  fun getDelegate(): ReportsCas1Delegate = object : ReportsCas1Delegate {}

  @Operation(
    tags = ["Reports"],
    summary = "Returns a spreadsheet of all data metrics for the 'reportName'.",
    operationId = "getReportByName",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "Successfully retrieved the report", content = [Content(schema = Schema(implementation = org.springframework.core.io.Resource::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/reports/{reportName}"],
    produces = ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"],
  )
  fun getReportByName(@Parameter(description = "Validates user for this service has access to the report", `in` = ParameterIn.HEADER, required = true, schema = Schema(allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"])) @RequestHeader(value = "X-Service-Name", required = true) xServiceName: ServiceName, @Parameter(description = "Name of the report to download", required = true, schema = Schema(allowableValues = ["applicationsV2", "applicationsV2WithPii", "dailyMetrics", "outOfServiceBeds", "outOfServiceBedsWithPii", "placementMatchingOutcomesV2", "placementMatchingOutcomesV2WithPii", "requestsForPlacement", "requestsForPlacementWithPii", "placements", "placementsWithPii", "overduePlacements"])) @PathVariable("reportName") reportName: Cas1ReportName, @RequestParam(value = "year", required = false) year: kotlin.Int?, @RequestParam(value = "month", required = false) month: kotlin.Int?, @RequestParam(value = "startDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) startDate: java.time.LocalDate?, @RequestParam(value = "endDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) endDate: java.time.LocalDate?): ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> = getDelegate().getReportByName(xServiceName, reportName, year, month, startDate, endDate)
}
