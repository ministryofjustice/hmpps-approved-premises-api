package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProfileResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link ProfileApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")interface ProfileApiDelegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see ProfileApi#profileV2Get
     */
    fun profileV2Get(xServiceName: ServiceName,
        readOnly: kotlin.Boolean?): ResponseEntity<ProfileResponse> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"loadError\" : \"staff_record_not_found\",  \"deliusUsername\" : \"deliusUsername\",  \"user\" : {    \"telephoneNumber\" : \"telephoneNumber\",    \"service\" : \"service\",    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\"  }}")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }

}
