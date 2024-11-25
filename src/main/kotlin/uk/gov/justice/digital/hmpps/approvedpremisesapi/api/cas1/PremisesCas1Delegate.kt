package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesBasicSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSummary
import java.util.Optional

/**
 * A delegate to be called by the {@link PremisesCas1Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.7.0")
interface PremisesCas1Delegate {

  fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

  /**
   * @see PremisesCas1#getCapacity
   */
  fun getCapacity(
    premisesId: java.util.UUID,
    startDate: java.time.LocalDate,
    endDate: java.time.LocalDate,
  ): ResponseEntity<Cas1PremiseCapacity> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"premisesId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"endDate\" : \"2000-01-23\",  \"startDate\" : \"2000-01-23\",  \"capacity\" : [ {    \"totalBedCount\" : 0,    \"availableBedCount\" : 6,    \"characteristicAvailability\" : [ {      \"bookingsCount\" : 5,      \"availableBedsCount\" : 5,      \"characteristic\" : \"acceptsChildSexOffenders\"    }, {      \"bookingsCount\" : 5,      \"availableBedsCount\" : 5,      \"characteristic\" : \"acceptsChildSexOffenders\"    } ],    \"bookingCount\" : 1  }, {    \"totalBedCount\" : 0,    \"availableBedCount\" : 6,    \"characteristicAvailability\" : [ {      \"bookingsCount\" : 5,      \"availableBedsCount\" : 5,      \"characteristic\" : \"acceptsChildSexOffenders\"    }, {      \"bookingsCount\" : 5,      \"availableBedsCount\" : 5,      \"characteristic\" : \"acceptsChildSexOffenders\"    } ],    \"bookingCount\" : 1  } ]}")
          break
        }
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
          ApiUtil.setExampleResponse(request, "application/problem+json", "Custom MIME type example not yet supported: application/problem+json")
          break
        }
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
          break
        }
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
          break
        }
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
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

  /**
   * @see PremisesCas1#getPremisesById
   */
  fun getPremisesById(premisesId: java.util.UUID): ResponseEntity<Cas1PremisesSummary> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"apArea\" : {    \"identifier\" : \"LON\",    \"name\" : \"Yorkshire & The Humber\",    \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"  },  \"availableBeds\" : 20,  \"name\" : \"Hope House\",  \"postcode\" : \"LS1 3AD\",  \"supportsSpaceBookings\" : true,  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"apCode\" : \"NEHOPE1\",  \"bedCount\" : 22,  \"outOfServiceBeds\" : 2}")
          break
        }
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
          break
        }
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
          break
        }
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
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

  /**
   * @see PremisesCas1#getPremisesSummaries
   */
  fun getPremisesSummaries(
    gender: Cas1ApprovedPremisesGender?,
    apAreaId: java.util.UUID?,
  ): ResponseEntity<List<Cas1PremisesBasicSummary>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"apArea\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"name\" : \"Hope House\",  \"supportsSpaceBookings\" : true,  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"apCode\" : \"NEHOPE1\",  \"bedCount\" : 22}, {  \"apArea\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"name\" : \"Hope House\",  \"supportsSpaceBookings\" : true,  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"apCode\" : \"NEHOPE1\",  \"bedCount\" : 22} ]")
          break
        }
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
          ApiUtil.setExampleResponse(request, "application/problem+json", "Custom MIME type example not yet supported: application/problem+json")
          break
        }
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
          break
        }
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
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
