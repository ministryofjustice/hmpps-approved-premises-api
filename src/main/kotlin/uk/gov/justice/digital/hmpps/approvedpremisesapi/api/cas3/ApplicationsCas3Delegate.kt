package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link ApplicationsCas3Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")interface ApplicationsCas3Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see ApplicationsCas3#deleteApplication
     */
    fun deleteApplication(applicationId: java.util.UUID): ResponseEntity<Unit> {
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
     * @see ApplicationsCas3#getApplicationById
     */
    fun getApplicationById(applicationId: java.util.UUID): ResponseEntity<Cas3Application> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"offenceId\" : \"offenceId\",  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"data\" : \"{}\",  \"document\" : \"{}\",  \"outdatedSchema\" : true,  \"arrivalDate\" : \"2000-01-23T04:56:07.000+00:00\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"status\" : \"inProgress\"}")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }


    /**
     * @see ApplicationsCas3#getApplicationsForUser
     */
    fun getApplicationsForUser(): ResponseEntity<List<Cas3ApplicationSummary>> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "[ {  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"status\" : \"inProgress\"}, {  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"status\" : \"inProgress\"} ]")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }


    /**
     * @see ApplicationsCas3#postApplication
     */
    fun postApplication(body: Cas3NewApplication,
        createWithRisks: kotlin.Boolean?): ResponseEntity<Cas3Application> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"offenceId\" : \"offenceId\",  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"data\" : \"{}\",  \"document\" : \"{}\",  \"outdatedSchema\" : true,  \"arrivalDate\" : \"2000-01-23T04:56:07.000+00:00\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"status\" : \"inProgress\"}")
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
     * @see ApplicationsCas3#postApplicationSubmission
     */
    fun postApplicationSubmission(applicationId: java.util.UUID,
        cas3SubmitApplication: Cas3SubmitApplication): ResponseEntity<Unit> {
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
     * @see ApplicationsCas3#putApplication
     */
    fun putApplication(applicationId: java.util.UUID,
        body: Cas3UpdateApplication): ResponseEntity<Cas3Application> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"offenceId\" : \"offenceId\",  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"data\" : \"{}\",  \"document\" : \"{}\",  \"outdatedSchema\" : true,  \"arrivalDate\" : \"2000-01-23T04:56:07.000+00:00\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"status\" : \"inProgress\"}")
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
