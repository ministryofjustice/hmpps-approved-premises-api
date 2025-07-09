package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link ApplicationsCas1Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")interface ApplicationsCas1Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see ApplicationsCas1#getAllApplications
     */
    fun getAllApplications(page: kotlin.Int?,
        crnOrName: kotlin.String?,
        sortDirection: SortDirection?,
        status: kotlin.collections.List<ApprovedPremisesApplicationStatus>?,
        sortBy: ApplicationSortField?,
        apAreaId: java.util.UUID?,
        releaseType: ReleaseTypeOption?): ResponseEntity<List<Cas1ApplicationSummary>> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "[ {  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"isEmergencyApplication\" : true,  \"hasRequestsForPlacement\" : true,  \"isWomensApplication\" : true,  \"isPipeApplication\" : true,  \"isEsapApplication\" : true,  \"arrivalDate\" : \"2000-01-23\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"isWithdrawn\" : true,  \"tier\" : \"tier\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"releaseType\" : \"licence\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"status\" : \"started\"}, {  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"isEmergencyApplication\" : true,  \"hasRequestsForPlacement\" : true,  \"isWomensApplication\" : true,  \"isPipeApplication\" : true,  \"isEsapApplication\" : true,  \"arrivalDate\" : \"2000-01-23\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"isWithdrawn\" : true,  \"tier\" : \"tier\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"releaseType\" : \"licence\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"status\" : \"started\"} ]")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }


    /**
     * @see ApplicationsCas1#getApplicationTimeLine
     */
    fun getApplicationTimeLine(applicationId: java.util.UUID): ResponseEntity<List<Cas1TimelineEvent>> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "[ {  \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"schemaVersion\" : 0,  \"payload\" : { },  \"associatedUrls\" : [ {    \"type\" : \"application\",    \"url\" : \"url\"  }, {    \"type\" : \"application\",    \"url\" : \"url\"  } ],  \"createdBySummary\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"id\",  \"type\" : \"application_submitted\",  \"triggerSource\" : \"triggerSource\",  \"content\" : \"content\"}, {  \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"schemaVersion\" : 0,  \"payload\" : { },  \"associatedUrls\" : [ {    \"type\" : \"application\",    \"url\" : \"url\"  }, {    \"type\" : \"application\",    \"url\" : \"url\"  } ],  \"createdBySummary\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"id\",  \"type\" : \"application_submitted\",  \"triggerSource\" : \"triggerSource\",  \"content\" : \"content\"} ]")
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
     * @see ApplicationsCas1#getApplicationsForUser
     */
    fun getApplicationsForUser(): ResponseEntity<List<Cas1ApplicationSummary>> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "[ {  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"isEmergencyApplication\" : true,  \"hasRequestsForPlacement\" : true,  \"isWomensApplication\" : true,  \"isPipeApplication\" : true,  \"isEsapApplication\" : true,  \"arrivalDate\" : \"2000-01-23\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"isWithdrawn\" : true,  \"tier\" : \"tier\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"releaseType\" : \"licence\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"status\" : \"started\"}, {  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"isEmergencyApplication\" : true,  \"hasRequestsForPlacement\" : true,  \"isWomensApplication\" : true,  \"isPipeApplication\" : true,  \"isEsapApplication\" : true,  \"arrivalDate\" : \"2000-01-23\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"isWithdrawn\" : true,  \"tier\" : \"tier\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"releaseType\" : \"licence\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"status\" : \"started\"} ]")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }


    /**
     * @see ApplicationsCas1#getApplicationsMe
     */
    fun getApplicationsMe(): ResponseEntity<List<Cas1ApplicationSummary>> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "[ {  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"isEmergencyApplication\" : true,  \"hasRequestsForPlacement\" : true,  \"isWomensApplication\" : true,  \"isPipeApplication\" : true,  \"isEsapApplication\" : true,  \"arrivalDate\" : \"2000-01-23\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"isWithdrawn\" : true,  \"tier\" : \"tier\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"releaseType\" : \"licence\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"status\" : \"started\"}, {  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"isEmergencyApplication\" : true,  \"hasRequestsForPlacement\" : true,  \"isWomensApplication\" : true,  \"isPipeApplication\" : true,  \"isEsapApplication\" : true,  \"arrivalDate\" : \"2000-01-23\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"isWithdrawn\" : true,  \"tier\" : \"tier\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"releaseType\" : \"licence\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"status\" : \"started\"} ]")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }

}
