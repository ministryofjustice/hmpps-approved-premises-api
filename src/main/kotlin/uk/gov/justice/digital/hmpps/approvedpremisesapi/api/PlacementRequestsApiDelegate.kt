package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBookingConfirmation
import java.util.Optional

/**
 * A delegate to be called by the {@link PlacementRequestsApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")
interface PlacementRequestsApiDelegate {

  fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

  /**
   * @see PlacementRequestsApi#placementRequestsIdBookingPost
   */
  fun placementRequestsIdBookingPost(
    id: java.util.UUID,
    newPlacementRequestBooking: NewPlacementRequestBooking,
  ): ResponseEntity<NewPlacementRequestBookingConfirmation> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"premisesName\" : \"premisesName\",  \"departureDate\" : \"2022-09-30\",  \"arrivalDate\" : \"2022-07-28\"}")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see PlacementRequestsApi#placementRequestsIdBookingNotMadePost
   */
  fun placementRequestsIdBookingNotMadePost(
    id: java.util.UUID,
    newBookingNotMade: NewBookingNotMade,
  ): ResponseEntity<BookingNotMade> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"placementRequestId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"notes\" : \"notes\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }
}
