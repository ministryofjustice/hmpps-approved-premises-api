package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3OASysGroup
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link PeopleCas3Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")interface PeopleCas3Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see PeopleCas3#riskManagement
     */
    fun riskManagement(crn: kotlin.String): ResponseEntity<Cas3OASysGroup> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"assessmentMetadata\" : {    \"hasApplicableAssessment\" : true,    \"dateStarted\" : \"2000-01-23T04:56:07.000+00:00\",    \"dateCompleted\" : \"2000-01-23T04:56:07.000+00:00\"  },  \"answers\" : [ {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  }, {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  } ]}")
                            break
                        }
                                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }

}
