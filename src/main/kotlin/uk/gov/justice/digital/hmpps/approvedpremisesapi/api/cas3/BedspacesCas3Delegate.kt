package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link BedspacesCas3Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")interface BedspacesCas3Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see BedspacesCas3#postBedspaceSearch
     */
    fun postBedspaceSearch(cas3BedspaceSearchParameters: Cas3BedspaceSearchParameters): ResponseEntity<Cas3BedspaceSearchResults> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"resultsBedCount\" : 1,  \"results\" : [ {    \"bed\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"premises\" : {      \"characteristics\" : [ {        \"propertyName\" : \"propertyName\",        \"name\" : \"name\"      }, {        \"propertyName\" : \"propertyName\",        \"name\" : \"name\"      } ],      \"notes\" : \"notes\",      \"bookedBedCount\" : 5,      \"town\" : \"town\",      \"name\" : \"name\",      \"postcode\" : \"postcode\",      \"addressLine1\" : \"addressLine1\",      \"addressLine2\" : \"addressLine2\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"probationDeliveryUnitName\" : \"probationDeliveryUnitName\",      \"bedCount\" : 5    },    \"overlaps\" : [ {      \"sex\" : \"sex\",      \"name\" : \"name\",      \"days\" : 2,      \"personType\" : \"FullPerson\",      \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"crn\" : \"crn\",      \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"roomId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isSexualRisk\" : true    }, {      \"sex\" : \"sex\",      \"name\" : \"name\",      \"days\" : 2,      \"personType\" : \"FullPerson\",      \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"crn\" : \"crn\",      \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"roomId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isSexualRisk\" : true    } ],    \"room\" : {      \"characteristics\" : [ {        \"propertyName\" : \"propertyName\",        \"name\" : \"name\"      }, {        \"propertyName\" : \"propertyName\",        \"name\" : \"name\"      } ],      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    }  }, {    \"bed\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"premises\" : {      \"characteristics\" : [ {        \"propertyName\" : \"propertyName\",        \"name\" : \"name\"      }, {        \"propertyName\" : \"propertyName\",        \"name\" : \"name\"      } ],      \"notes\" : \"notes\",      \"bookedBedCount\" : 5,      \"town\" : \"town\",      \"name\" : \"name\",      \"postcode\" : \"postcode\",      \"addressLine1\" : \"addressLine1\",      \"addressLine2\" : \"addressLine2\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"probationDeliveryUnitName\" : \"probationDeliveryUnitName\",      \"bedCount\" : 5    },    \"overlaps\" : [ {      \"sex\" : \"sex\",      \"name\" : \"name\",      \"days\" : 2,      \"personType\" : \"FullPerson\",      \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"crn\" : \"crn\",      \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"roomId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isSexualRisk\" : true    }, {      \"sex\" : \"sex\",      \"name\" : \"name\",      \"days\" : 2,      \"personType\" : \"FullPerson\",      \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"crn\" : \"crn\",      \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"roomId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isSexualRisk\" : true    } ],    \"room\" : {      \"characteristics\" : [ {        \"propertyName\" : \"propertyName\",        \"name\" : \"name\"      }, {        \"propertyName\" : \"propertyName\",        \"name\" : \"name\"      } ],      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    }  } ],  \"resultsRoomCount\" : 0,  \"resultsPremisesCount\" : 6}")
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
