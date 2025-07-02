package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3OASysGroup
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.media.*
import io.swagger.v3.oas.annotations.responses.*
import org.springframework.http.ResponseEntity

import org.springframework.web.bind.annotation.*

@RestController
interface PeopleCas3 {

    fun getDelegate(): PeopleCas3Delegate = object: PeopleCas3Delegate {}

    @Operation(
        tags = ["OAsys",],
        summary = "",
        operationId = "riskManagement",
        description = """""",
        responses = [
            ApiResponse(responseCode = "200", description = "successful operation", content = [Content(schema = Schema(implementation = Cas3OASysGroup::class))]),
            ApiResponse(responseCode = "404", description = "invalid CRN", content = [Content(schema = Schema(implementation = Problem::class))])
        ]
    )
    @RequestMapping(
            method = [RequestMethod.GET],
            value = ["/people/{crn}/oasys/riskManagement"],
            produces = ["application/json"]
    )
    fun riskManagement(@Parameter(description = "CRN of the Person to fetch latest OASys selection", required = true) @PathVariable("crn") crn: kotlin.String): ResponseEntity<Cas3OASysGroup> {
        return getDelegate().riskManagement(crn)
    }
}
