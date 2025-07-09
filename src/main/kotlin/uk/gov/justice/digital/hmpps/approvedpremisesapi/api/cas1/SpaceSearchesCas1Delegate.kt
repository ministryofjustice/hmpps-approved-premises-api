package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link SpaceSearchesCas1Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")interface SpaceSearchesCas1Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see SpaceSearchesCas1#spaceSearch
     */
    fun spaceSearch(cas1SpaceSearchParameters: Cas1SpaceSearchParameters): ResponseEntity<Cas1SpaceSearchResults> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"resultsCount\" : 4,  \"results\" : [ {    \"distanceInMiles\" : 2.1,    \"premises\" : {      \"apType\" : \"normal\",      \"characteristics\" : [ \"acceptsChildSexOffenders\", \"acceptsChildSexOffenders\" ],      \"apArea\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"name\" : \"Hope House\",      \"fullAddress\" : \"fullAddress\",      \"postcode\" : \"LS1 3AD\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    }  }, {    \"distanceInMiles\" : 2.1,    \"premises\" : {      \"apType\" : \"normal\",      \"characteristics\" : [ \"acceptsChildSexOffenders\", \"acceptsChildSexOffenders\" ],      \"apArea\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"name\" : \"Hope House\",      \"fullAddress\" : \"fullAddress\",      \"postcode\" : \"LS1 3AD\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    }  } ]}")
                            break
                        }
                                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
                            ApiUtil.setExampleResponse(request, "application/problem+json", "Custom MIME type example not yet supported: application/problem+json")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }

}
