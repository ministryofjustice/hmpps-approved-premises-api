
package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysGroup
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysGroupName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem

@RestController
interface OAsysCas1 {

  fun getDelegate(): OAsysCas1Delegate = object : OAsysCas1Delegate {}

  @Operation(
    tags = ["OAsys"],
    summary = "Returns OASys answers for the requested group. The Supporting Information answers are returned if linked to harm and optionally if their section number appears in the selected-sections query parameter.",
    operationId = "answers",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas1OASysGroup::class))]),
      ApiResponse(responseCode = "404", description = "invalid CRN", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/people/{crn}/oasys/answers"],
    produces = ["application/json"],
  )
  fun answers(@Parameter(description = "CRN of the Person to fetch latest OASys selection", required = true) @PathVariable("crn") crn: kotlin.String, @RequestParam(value = "group", required = true) group: Cas1OASysGroupName, @RequestParam(value = "includeOptionalSections", required = false) includeOptionalSections: kotlin.collections.List<kotlin.Int>?): ResponseEntity<Cas1OASysGroup> = getDelegate().answers(crn, group, includeOptionalSections)

  @Operation(
    tags = ["OAsys"],
    summary = "Returns metadata about supporting information questions for a given CRN",
    operationId = "metadata",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas1OASysMetadata::class))]),
      ApiResponse(responseCode = "404", description = "invalid CRN", content = [Content(schema = Schema(implementation = Problem::class))]),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/people/{crn}/oasys/metadata"],
    produces = ["application/json"],
  )
  fun metadata(@Parameter(description = "", required = true) @PathVariable("crn") crn: kotlin.String): ResponseEntity<Cas1OASysMetadata> = getDelegate().metadata(crn)
}
