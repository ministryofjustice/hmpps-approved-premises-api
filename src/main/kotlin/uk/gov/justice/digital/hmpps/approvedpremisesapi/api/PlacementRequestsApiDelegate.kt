package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBookingConfirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
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
   * @see PlacementRequestsApi#placementRequestsDashboardGet
   */
  fun placementRequestsDashboardGet(
    status: PlacementRequestStatus?,
    crnOrName: kotlin.String?,
    tier: RiskTierLevel?,
    arrivalDateStart: java.time.LocalDate?,
    arrivalDateEnd: java.time.LocalDate?,
    requestType: PlacementRequestRequestType?,
    cruManagementAreaId: java.util.UUID?,
    page: kotlin.Int?,
    sortBy: PlacementRequestSortField?,
    sortDirection: SortDirection?,
  ): ResponseEntity<List<PlacementRequest>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"desirableCriteria\" : [ null, null ],  \"booking\" : {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"characteristics\" : [ \"acceptsChildSexOffenders\", \"acceptsChildSexOffenders\" ],    \"premisesId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"premisesName\" : \"premisesName\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"departureDate\" : \"2000-01-23\",    \"type\" : \"space\",    \"arrivalDate\" : \"2000-01-23\"  },  \"notes\" : \"notes\",  \"requestType\" : \"parole\",  \"assessmentDate\" : \"2000-01-23T04:56:07.000+00:00\",  \"assessor\" : {    \"telephoneNumber\" : \"telephoneNumber\",    \"roles\" : [ \"assessor\", \"assessor\" ],    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"cruManagementArea\" : \"\",    \"cruManagementAreaDefault\" : \"\",    \"isActive\" : true,    \"version\" : 0,    \"cruManagementAreaOverride\" : \"\",    \"qualifications\" : [ \"pipe\", \"pipe\" ],    \"apArea\" : {      \"identifier\" : \"LON\",      \"name\" : \"Yorkshire & The Humber\",      \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"    },    \"service\" : \"service\",    \"permissions\" : [ \"cas1_adhoc_booking_create\", \"cas1_adhoc_booking_create\" ],    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\"  },  \"isParole\" : true,  \"type\" : \"normal\",  \"duration\" : 6,  \"essentialCriteria\" : [ \"isPIPE\", \"isPIPE\" ],  \"expectedArrival\" : \"2000-01-23\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"isWithdrawn\" : true,  \"assessmentDecision\" : \"accepted\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"releaseType\" : \"licence\",  \"location\" : \"B74\",  \"withdrawalReason\" : \"DuplicatePlacementRequest\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"radius\" : 0,  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"status\" : \"notMatched\",  \"applicationDate\" : \"2000-01-23T04:56:07.000+00:00\"}, {  \"desirableCriteria\" : [ null, null ],  \"booking\" : {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"characteristics\" : [ \"acceptsChildSexOffenders\", \"acceptsChildSexOffenders\" ],    \"premisesId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"premisesName\" : \"premisesName\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"departureDate\" : \"2000-01-23\",    \"type\" : \"space\",    \"arrivalDate\" : \"2000-01-23\"  },  \"notes\" : \"notes\",  \"requestType\" : \"parole\",  \"assessmentDate\" : \"2000-01-23T04:56:07.000+00:00\",  \"assessor\" : {    \"telephoneNumber\" : \"telephoneNumber\",    \"roles\" : [ \"assessor\", \"assessor\" ],    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"cruManagementArea\" : \"\",    \"cruManagementAreaDefault\" : \"\",    \"isActive\" : true,    \"version\" : 0,    \"cruManagementAreaOverride\" : \"\",    \"qualifications\" : [ \"pipe\", \"pipe\" ],    \"apArea\" : {      \"identifier\" : \"LON\",      \"name\" : \"Yorkshire & The Humber\",      \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"    },    \"service\" : \"service\",    \"permissions\" : [ \"cas1_adhoc_booking_create\", \"cas1_adhoc_booking_create\" ],    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\"  },  \"isParole\" : true,  \"type\" : \"normal\",  \"duration\" : 6,  \"essentialCriteria\" : [ \"isPIPE\", \"isPIPE\" ],  \"expectedArrival\" : \"2000-01-23\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"isWithdrawn\" : true,  \"assessmentDecision\" : \"accepted\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"releaseType\" : \"licence\",  \"location\" : \"B74\",  \"withdrawalReason\" : \"DuplicatePlacementRequest\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"radius\" : 0,  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"status\" : \"notMatched\",  \"applicationDate\" : \"2000-01-23T04:56:07.000+00:00\"} ]")
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
