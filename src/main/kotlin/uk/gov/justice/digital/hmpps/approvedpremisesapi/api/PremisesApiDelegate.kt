package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DateChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDateChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Room
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateRoom
import java.util.Optional

/**
 * A delegate to be called by the {@link PremisesApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")
interface PremisesApiDelegate {

  fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

  /**
   * @see PremisesApi#premisesPremisesIdBookingsBookingIdDateChangesPost
   */
  fun premisesPremisesIdBookingsBookingIdDateChangesPost(
    premisesId: java.util.UUID,
    bookingId: java.util.UUID,
    body: NewDateChange,
  ): ResponseEntity<DateChange> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"newArrivalDate\" : \"2000-01-23\",  \"previousDepartureDate\" : \"2000-01-23\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"previousArrivalDate\" : \"2000-01-23\",  \"newDepartureDate\" : \"2000-01-23\",  \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see PremisesApi#premisesPremisesIdBookingsBookingIdGet
   */
  fun premisesPremisesIdBookingsBookingIdGet(
    premisesId: java.util.UUID,
    bookingId: java.util.UUID,
  ): ResponseEntity<Booking> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"bed\" : {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  },  \"effectiveEndDate\" : \"2000-01-23\",  \"arrival\" : \"\",  \"turnaround\" : \"\",  \"turnaroundStartDate\" : \"2000-01-23\",  \"originalDepartureDate\" : \"2000-01-23\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"premises\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"departureDate\" : \"2000-01-23\",  \"nonArrival\" : \"\",  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"originalArrivalDate\" : \"2000-01-23\",  \"confirmation\" : \"\",  \"serviceName\" : \"approved-premises\",  \"departures\" : [ {    \"dateTime\" : \"2000-01-23T04:56:07.000+00:00\",    \"reason\" : {      \"parentReasonId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"name\" : \"Admitted to Hospital\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"destinationProvider\" : {      \"name\" : \"Ext - North East Region\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"moveOnCategory\" : {      \"name\" : \"Housing Association - Rented\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"dateTime\" : \"2000-01-23T04:56:07.000+00:00\",    \"reason\" : {      \"parentReasonId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"name\" : \"Admitted to Hospital\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"destinationProvider\" : {      \"name\" : \"Ext - North East Region\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"moveOnCategory\" : {      \"name\" : \"Housing Association - Rented\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"turnarounds\" : [ {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"workingDays\" : 0,    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"workingDays\" : 0,    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"arrivalDate\" : \"2000-01-23\",  \"cancellations\" : [ {    \"date\" : \"2000-01-23\",    \"reason\" : {      \"name\" : \"Recall\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"otherReason\" : \"otherReason\",    \"notes\" : \"notes\",    \"premisesName\" : \"premisesName\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"date\" : \"2000-01-23\",    \"reason\" : {      \"name\" : \"Recall\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"otherReason\" : \"otherReason\",    \"notes\" : \"notes\",    \"premisesName\" : \"premisesName\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"extensions\" : [ {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"previousDepartureDate\" : \"2000-01-23\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"newDepartureDate\" : \"2000-01-23\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"previousDepartureDate\" : \"2000-01-23\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"newDepartureDate\" : \"2000-01-23\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"cancellation\" : \"\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"keyWorker\" : \"\",  \"departure\" : \"\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"status\" : \"arrived\"}")
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
   * @see PremisesApi#premisesPremisesIdGet
   */
  fun premisesPremisesIdGet(premisesId: java.util.UUID): ResponseEntity<Premises> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"notes\" : \"some notes about this property\",  \"town\" : \"Braintree\",  \"probationRegion\" : {    \"name\" : \"NPS North East Central Referrals\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"  },  \"postcode\" : \"LS1 3AD\",  \"availableBedsForToday\" : 20,  \"apArea\" : {    \"identifier\" : \"LON\",    \"name\" : \"Yorkshire & The Humber\",    \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"  },  \"localAuthorityArea\" : {    \"identifier\" : \"LEEDS\",    \"name\" : \"Leeds City Council\",    \"id\" : \"6abb5fa3-e93f-4445-887b-30d081688f44\"  },  \"service\" : \"service\",  \"name\" : \"Hope House\",  \"addressLine1\" : \"one something street\",  \"addressLine2\" : \"Blackmore End\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bedCount\" : 22,  \"status\" : \"pending\"}")
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
   * @see PremisesApi#premisesPremisesIdPut
   */
  fun premisesPremisesIdPut(
    premisesId: java.util.UUID,
    body: UpdatePremises,
  ): ResponseEntity<Premises> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"notes\" : \"some notes about this property\",  \"town\" : \"Braintree\",  \"probationRegion\" : {    \"name\" : \"NPS North East Central Referrals\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"  },  \"postcode\" : \"LS1 3AD\",  \"availableBedsForToday\" : 20,  \"apArea\" : {    \"identifier\" : \"LON\",    \"name\" : \"Yorkshire & The Humber\",    \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"  },  \"localAuthorityArea\" : {    \"identifier\" : \"LEEDS\",    \"name\" : \"Leeds City Council\",    \"id\" : \"6abb5fa3-e93f-4445-887b-30d081688f44\"  },  \"service\" : \"service\",  \"name\" : \"Hope House\",  \"addressLine1\" : \"one something street\",  \"addressLine2\" : \"Blackmore End\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bedCount\" : 22,  \"status\" : \"pending\"}")
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

  /**
   * @see PremisesApi#premisesPremisesIdRoomsPost
   */
  fun premisesPremisesIdRoomsPost(
    premisesId: java.util.UUID,
    newRoom: NewRoom,
  ): ResponseEntity<Room> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"code\" : \"NEABC-4\",  \"notes\" : \"notes\",  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"beds\" : [ {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  }, {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  } ]}")
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
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see PremisesApi#premisesPremisesIdRoomsRoomIdGet
   */
  fun premisesPremisesIdRoomsRoomIdGet(
    premisesId: java.util.UUID,
    roomId: java.util.UUID,
  ): ResponseEntity<Room> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"code\" : \"NEABC-4\",  \"notes\" : \"notes\",  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"beds\" : [ {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  }, {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  } ]}")
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
   * @see PremisesApi#premisesPremisesIdRoomsRoomIdPut
   */
  fun premisesPremisesIdRoomsRoomIdPut(
    premisesId: java.util.UUID,
    roomId: java.util.UUID,
    updateRoom: UpdateRoom,
  ): ResponseEntity<Room> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"code\" : \"NEABC-4\",  \"notes\" : \"notes\",  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"beds\" : [ {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  }, {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  } ]}")
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
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }
}
