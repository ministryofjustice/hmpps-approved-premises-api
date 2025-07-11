package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DepartureReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NonArrivalReason
import java.util.Optional

/**
 * A delegate to be called by the {@link ReferenceDataCas1Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")
interface ReferenceDataCas1Delegate {

  fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

  /**
   * @see ReferenceDataCas1#getChangeRequestReasons
   */
  fun getChangeRequestReasons(changeRequestType: Cas1ChangeRequestType): ResponseEntity<List<NamedId>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}, {  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"} ]")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see ReferenceDataCas1#getChangeRequestRejectionReasons
   */
  fun getChangeRequestRejectionReasons(changeRequestType: Cas1ChangeRequestType): ResponseEntity<List<NamedId>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}, {  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"} ]")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see ReferenceDataCas1#getCruManagementAreas
   */
  fun getCruManagementAreas(): ResponseEntity<List<Cas1CruManagementArea>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}, {  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"} ]")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see ReferenceDataCas1#getDepartureReasons
   */
  fun getDepartureReasons(): ResponseEntity<List<DepartureReason>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"parentReasonId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"name\" : \"Admitted to Hospital\",  \"serviceScope\" : \"serviceScope\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"isActive\" : true}, {  \"parentReasonId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"name\" : \"Admitted to Hospital\",  \"serviceScope\" : \"serviceScope\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"isActive\" : true} ]")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see ReferenceDataCas1#getMoveOnCategories
   */
  fun getMoveOnCategories(): ResponseEntity<List<MoveOnCategory>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"name\" : \"Housing Association - Rented\",  \"serviceScope\" : \"serviceScope\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"isActive\" : true}, {  \"name\" : \"Housing Association - Rented\",  \"serviceScope\" : \"serviceScope\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"isActive\" : true} ]")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see ReferenceDataCas1#getNonArrivalReasons
   */
  fun getNonArrivalReasons(): ResponseEntity<List<NonArrivalReason>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"name\" : \"Recall\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"isActive\" : true}, {  \"name\" : \"Recall\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"isActive\" : true} ]")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see ReferenceDataCas1#getOutOfServiceBedReasons
   */
  fun getOutOfServiceBedReasons(): ResponseEntity<List<Cas1OutOfServiceBedReason>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"isActive\" : true}, {  \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"isActive\" : true} ]")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }
}
