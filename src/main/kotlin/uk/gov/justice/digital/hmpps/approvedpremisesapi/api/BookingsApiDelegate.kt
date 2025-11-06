package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")
interface BookingsApiDelegate {

  fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

  fun bookingsSearchGet(
    status: BookingStatus?,
    sortOrder: SortOrder?,
    sortField: BookingSearchSortField?,
    page: kotlin.Int?,
    crnOrName: kotlin.String?,
  ): ResponseEntity<BookingSearchResults> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"resultsCount\" : 0,  \"results\" : [ {    \"bed\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"booking\" : {      \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",      \"endDate\" : \"2000-01-23\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"startDate\" : \"2000-01-23\",      \"status\" : \"arrived\"    },    \"person\" : {      \"name\" : \"name\",      \"crn\" : \"crn\"    },    \"premises\" : {      \"town\" : \"town\",      \"name\" : \"name\",      \"postcode\" : \"postcode\",      \"addressLine1\" : \"addressLine1\",      \"addressLine2\" : \"addressLine2\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"room\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    }  }, {    \"bed\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"booking\" : {      \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",      \"endDate\" : \"2000-01-23\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"startDate\" : \"2000-01-23\",      \"status\" : \"arrived\"    },    \"person\" : {      \"name\" : \"name\",      \"crn\" : \"crn\"    },    \"premises\" : {      \"town\" : \"town\",      \"name\" : \"name\",      \"postcode\" : \"postcode\",      \"addressLine1\" : \"addressLine1\",      \"addressLine2\" : \"addressLine2\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"room\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    }  } ]}")
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
