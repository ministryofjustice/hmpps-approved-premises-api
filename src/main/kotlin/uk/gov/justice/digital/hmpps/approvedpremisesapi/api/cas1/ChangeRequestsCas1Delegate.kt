package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1RejectChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import java.util.Optional

/**
 * A delegate to be called by the {@link ChangeRequestsCas1Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")
interface ChangeRequestsCas1Delegate {

  fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

  /**
   * @see ChangeRequestsCas1#createPlacementAppeal
   */
  fun createPlacementAppeal(
    placementRequestId: java.util.UUID,
    cas1NewChangeRequest: Cas1NewChangeRequest,
  ): ResponseEntity<Unit> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
          ApiUtil.setExampleResponse(request, "application/problem+json", "Custom MIME type example not yet supported: application/problem+json")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see ChangeRequestsCas1#createPlacementExtension
   */
  fun createPlacementExtension(
    placementRequestId: java.util.UUID,
    cas1NewChangeRequest: Cas1NewChangeRequest,
  ): ResponseEntity<Unit> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
          ApiUtil.setExampleResponse(request, "application/problem+json", "Custom MIME type example not yet supported: application/problem+json")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see ChangeRequestsCas1#createPlannedTransfer
   */
  fun createPlannedTransfer(
    placementRequestId: java.util.UUID,
    cas1NewChangeRequest: Cas1NewChangeRequest,
  ): ResponseEntity<Unit> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
          ApiUtil.setExampleResponse(request, "application/problem+json", "Custom MIME type example not yet supported: application/problem+json")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see ChangeRequestsCas1#findOpen
   */
  fun findOpen(
    page: kotlin.Int?,
    cruManagementAreaId: java.util.UUID?,
    sortBy: Cas1ChangeRequestSortField?,
    sortDirection: SortDirection?,
  ): ResponseEntity<List<Cas1ChangeRequestSummary>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"actualArrivalDate\" : \"2000-01-23\",  \"placementRequestId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"tier\" : \"tier\",  \"expectedArrivalDate\" : \"2000-01-23\",  \"person\" : {    \"personType\" : \"FullPersonSummary\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"type\" : \"placementAppeal\"}, {  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"actualArrivalDate\" : \"2000-01-23\",  \"placementRequestId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"tier\" : \"tier\",  \"expectedArrivalDate\" : \"2000-01-23\",  \"person\" : {    \"personType\" : \"FullPersonSummary\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"type\" : \"placementAppeal\"} ]")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see ChangeRequestsCas1#get
   */
  fun get(
    placementRequestId: java.util.UUID,
    changeRequestId: java.util.UUID,
  ): ResponseEntity<Cas1ChangeRequest> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"requestReason\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"decision\" : \"approved\",  \"spaceBookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"type\" : \"placementAppeal\",  \"decisionJson\" : \"{}\",  \"rejectionReason\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"requestJson\" : \"{}\",  \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"}")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see ChangeRequestsCas1#reject
   */
  fun reject(
    placementRequestId: java.util.UUID,
    changeRequestId: java.util.UUID,
    cas1RejectChangeRequest: Cas1RejectChangeRequest,
  ): ResponseEntity<Unit> = ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
}
