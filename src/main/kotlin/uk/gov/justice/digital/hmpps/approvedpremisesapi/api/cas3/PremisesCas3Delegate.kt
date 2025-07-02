package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Bedspaces
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3NewBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSortBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3UpdateBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FutureBooking
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link PremisesCas3Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")interface PremisesCas3Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see PremisesCas3#createBedspace
     */
    fun createBedspace(premisesId: java.util.UUID,
        body: Cas3NewBedspace): ResponseEntity<Cas3Bedspace> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"reference\" : \"reference\",  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"notes\" : \"notes\",  \"endDate\" : \"2024-12-30\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bedspaceCharacteristics\" : [ {    \"name\" : \"isCatered\",    \"description\" : \"Is this premises catered (rather than self-catered)?\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"  }, {    \"name\" : \"isCatered\",    \"description\" : \"Is this premises catered (rather than self-catered)?\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"  } ],  \"startDate\" : \"2024-07-30\",  \"status\" : \"online\"}")
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
     * @see PremisesCas3#createPremises
     */
    fun createPremises(body: Cas3NewPremises): ResponseEntity<Cas3Premises> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"totalUpcomingBedspaces\" : 1,  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"notes\" : \"some notes about this property\",  \"town\" : \"Braintree\",  \"probationRegion\" : {    \"name\" : \"NPS North East Central Referrals\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"  },  \"totalArchivedBedspaces\" : 2,  \"postcode\" : \"LS1 3AD\",  \"probationDeliveryUnit\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"totalOnlineBedspaces\" : 5,  \"reference\" : \"Hope House\",  \"turnaroundWorkingDays\" : 2,  \"localAuthorityArea\" : {    \"identifier\" : \"LEEDS\",    \"name\" : \"Leeds City Council\",    \"id\" : \"6abb5fa3-e93f-4445-887b-30d081688f44\"  },  \"addressLine1\" : \"one something street\",  \"addressLine2\" : \"Blackmore End\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"startDate\" : \"2024-03-30\",  \"status\" : \"online\"}")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }


    /**
     * @see PremisesCas3#getPremisesBedspace
     */
    fun getPremisesBedspace(premisesId: java.util.UUID,
        bedspaceId: java.util.UUID): ResponseEntity<Cas3Bedspace> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"reference\" : \"reference\",  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"notes\" : \"notes\",  \"endDate\" : \"2024-12-30\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bedspaceCharacteristics\" : [ {    \"name\" : \"isCatered\",    \"description\" : \"Is this premises catered (rather than self-catered)?\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"  }, {    \"name\" : \"isCatered\",    \"description\" : \"Is this premises catered (rather than self-catered)?\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"  } ],  \"startDate\" : \"2024-07-30\",  \"status\" : \"online\"}")
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
     * @see PremisesCas3#getPremisesBedspaces
     */
    fun getPremisesBedspaces(premisesId: java.util.UUID): ResponseEntity<Cas3Bedspaces> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"totalUpcomingBedspaces\" : 1,  \"bedspaces\" : [ {    \"reference\" : \"reference\",    \"characteristics\" : [ {      \"propertyName\" : \"isCatered\",      \"name\" : \"Is this premises catered (rather than self-catered)?\",      \"serviceScope\" : \"approved-premises\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",      \"modelScope\" : \"premises\"    }, {      \"propertyName\" : \"isCatered\",      \"name\" : \"Is this premises catered (rather than self-catered)?\",      \"serviceScope\" : \"approved-premises\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",      \"modelScope\" : \"premises\"    } ],    \"notes\" : \"notes\",    \"endDate\" : \"2024-12-30\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedspaceCharacteristics\" : [ {      \"name\" : \"isCatered\",      \"description\" : \"Is this premises catered (rather than self-catered)?\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    }, {      \"name\" : \"isCatered\",      \"description\" : \"Is this premises catered (rather than self-catered)?\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    } ],    \"startDate\" : \"2024-07-30\",    \"status\" : \"online\"  }, {    \"reference\" : \"reference\",    \"characteristics\" : [ {      \"propertyName\" : \"isCatered\",      \"name\" : \"Is this premises catered (rather than self-catered)?\",      \"serviceScope\" : \"approved-premises\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",      \"modelScope\" : \"premises\"    }, {      \"propertyName\" : \"isCatered\",      \"name\" : \"Is this premises catered (rather than self-catered)?\",      \"serviceScope\" : \"approved-premises\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",      \"modelScope\" : \"premises\"    } ],    \"notes\" : \"notes\",    \"endDate\" : \"2024-12-30\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedspaceCharacteristics\" : [ {      \"name\" : \"isCatered\",      \"description\" : \"Is this premises catered (rather than self-catered)?\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    }, {      \"name\" : \"isCatered\",      \"description\" : \"Is this premises catered (rather than self-catered)?\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    } ],    \"startDate\" : \"2024-07-30\",    \"status\" : \"online\"  } ],  \"totalArchivedBedspaces\" : 2,  \"totalOnlineBedspaces\" : 5}")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }


    /**
     * @see PremisesCas3#getPremisesById
     */
    fun getPremisesById(premisesId: java.util.UUID): ResponseEntity<Cas3Premises> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"totalUpcomingBedspaces\" : 1,  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"notes\" : \"some notes about this property\",  \"town\" : \"Braintree\",  \"probationRegion\" : {    \"name\" : \"NPS North East Central Referrals\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"  },  \"totalArchivedBedspaces\" : 2,  \"postcode\" : \"LS1 3AD\",  \"probationDeliveryUnit\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"totalOnlineBedspaces\" : 5,  \"reference\" : \"Hope House\",  \"turnaroundWorkingDays\" : 2,  \"localAuthorityArea\" : {    \"identifier\" : \"LEEDS\",    \"name\" : \"Leeds City Council\",    \"id\" : \"6abb5fa3-e93f-4445-887b-30d081688f44\"  },  \"addressLine1\" : \"one something street\",  \"addressLine2\" : \"Blackmore End\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"startDate\" : \"2024-03-30\",  \"status\" : \"online\"}")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }


    /**
     * @see PremisesCas3#getPremisesFutureBookings
     */
    fun getPremisesFutureBookings(premisesId: java.util.UUID,
        statuses: kotlin.collections.List<BookingStatus>): ResponseEntity<List<FutureBooking>> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "[ {  \"bed\" : {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  },  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"departureDate\" : \"2000-01-23\",  \"arrivalDate\" : \"2000-01-23\"}, {  \"bed\" : {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  },  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"departureDate\" : \"2000-01-23\",  \"arrivalDate\" : \"2000-01-23\"} ]")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }


    /**
     * @see PremisesCas3#getPremisesSummary
     */
    fun getPremisesSummary(postcodeOrAddress: kotlin.String?,
        sortBy: Cas3PremisesSortBy?): ResponseEntity<List<Cas3PremisesSummary>> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "[ {  \"pdu\" : \"pdu\",  \"bedspaceCount\" : 22,  \"name\" : \"Hope House\",  \"postcode\" : \"LS1 3AD\",  \"addressLine1\" : \"one something street\",  \"addressLine2\" : \"Blackmore End\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"localAuthorityAreaName\" : \"localAuthorityAreaName\",  \"status\" : \"pending\"}, {  \"pdu\" : \"pdu\",  \"bedspaceCount\" : 22,  \"name\" : \"Hope House\",  \"postcode\" : \"LS1 3AD\",  \"addressLine1\" : \"one something street\",  \"addressLine2\" : \"Blackmore End\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"localAuthorityAreaName\" : \"localAuthorityAreaName\",  \"status\" : \"pending\"} ]")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }


    /**
     * @see PremisesCas3#postPremisesBookingDeparture
     */
    fun postPremisesBookingDeparture(premisesId: java.util.UUID,
        bookingId: java.util.UUID,
        body: Cas3NewDeparture): ResponseEntity<Cas3Departure> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"dateTime\" : \"2000-01-23T04:56:07.000+00:00\",  \"reason\" : {    \"parentReasonId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"name\" : \"Admitted to Hospital\",    \"serviceScope\" : \"serviceScope\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"notes\" : \"notes\",  \"moveOnCategory\" : {    \"name\" : \"Housing Association - Rented\",    \"serviceScope\" : \"serviceScope\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
     * @see PremisesCas3#searchPremises
     */
    fun searchPremises(postcodeOrAddress: kotlin.String?,
        premisesStatus: Cas3PremisesStatus?): ResponseEntity<Cas3PremisesSearchResults> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"totalUpcomingBedspaces\" : 3,  \"results\" : [ {    \"reference\" : \"Hope House\",    \"pdu\" : \"pdu\",    \"bedspaces\" : [ {      \"reference\" : \"reference\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"status\" : \"online\"    }, {      \"reference\" : \"reference\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"status\" : \"online\"    } ],    \"town\" : \"Leeds\",    \"totalArchivedBedspaces\" : 4,    \"postcode\" : \"LS1 3AD\",    \"addressLine1\" : \"one something street\",    \"addressLine2\" : \"Blackmore End\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"localAuthorityAreaName\" : \"localAuthorityAreaName\"  }, {    \"reference\" : \"Hope House\",    \"pdu\" : \"pdu\",    \"bedspaces\" : [ {      \"reference\" : \"reference\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"status\" : \"online\"    }, {      \"reference\" : \"reference\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"status\" : \"online\"    } ],    \"town\" : \"Leeds\",    \"totalArchivedBedspaces\" : 4,    \"postcode\" : \"LS1 3AD\",    \"addressLine1\" : \"one something street\",    \"addressLine2\" : \"Blackmore End\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"localAuthorityAreaName\" : \"localAuthorityAreaName\"  } ],  \"totalOnlineBedspaces\" : 15,  \"totalPremises\" : 50}")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }


    /**
     * @see PremisesCas3#updateBedspace
     */
    fun updateBedspace(premisesId: java.util.UUID,
        bedspaceId: java.util.UUID,
        cas3UpdateBedspace: Cas3UpdateBedspace): ResponseEntity<Cas3Bedspace> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"reference\" : \"reference\",  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"notes\" : \"notes\",  \"endDate\" : \"2024-12-30\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bedspaceCharacteristics\" : [ {    \"name\" : \"isCatered\",    \"description\" : \"Is this premises catered (rather than self-catered)?\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"  }, {    \"name\" : \"isCatered\",    \"description\" : \"Is this premises catered (rather than self-catered)?\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"  } ],  \"startDate\" : \"2024-07-30\",  \"status\" : \"online\"}")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }

}
