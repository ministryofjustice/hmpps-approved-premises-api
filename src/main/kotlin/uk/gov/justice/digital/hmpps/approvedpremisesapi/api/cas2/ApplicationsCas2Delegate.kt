package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssignmentType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Application
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link ApplicationsCas2Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")interface ApplicationsCas2Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see ApplicationsCas2#abandonCas2Application
     */
    fun abandonCas2Application(applicationId: java.util.UUID): ResponseEntity<Unit> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }


    /**
     * @see ApplicationsCas2#createCas2Application
     */
    fun createCas2Application(body: NewApplication): ResponseEntity<Cas2Application> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"currentPrisonName\" : \"currentPrisonName\",  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"telephoneNumber\" : \"telephoneNumber\",  \"allocatedPomEmailAddress\" : \"allocatedPomEmailAddress\",  \"omuEmailAddress\" : \"omuEmailAddress\",  \"isTransferredApplication\" : true,  \"bailHearingDate\" : \"2000-01-23\",  \"outdatedSchema\" : true,  \"type\" : \"type\",  \"cas2CreatedBy\" : {    \"authSource\" : \"nomis\",    \"name\" : \"Roger Smith\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"email\" : \"Roger.Smith@justice.gov.uk\",    \"username\" : \"SMITHR_GEN\"  },  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"assessment\" : {    \"assessorName\" : \"assessorName\",    \"nacroReferralId\" : \"nacroReferralId\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"statusUpdates\" : [ {      \"updatedBy\" : {        \"origin\" : \"NACRO\",        \"name\" : \"Roger Smith\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"email\" : \"roger@external.example.com\",        \"username\" : \"CAS2_ASSESSOR_USER\"      },      \"statusUpdateDetails\" : [ {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      }, {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      } ],      \"name\" : \"moreInfoRequested\",      \"description\" : \"More information about the application has been requested from the POM (Prison Offender Manager).\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\",      \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"    }, {      \"updatedBy\" : {        \"origin\" : \"NACRO\",        \"name\" : \"Roger Smith\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"email\" : \"roger@external.example.com\",        \"username\" : \"CAS2_ASSESSOR_USER\"      },      \"statusUpdateDetails\" : [ {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      }, {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      } ],      \"name\" : \"moreInfoRequested\",      \"description\" : \"More information about the application has been requested from the POM (Prison Offender Manager).\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\",      \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"    } ]  },  \"timelineEvents\" : [ {    \"createdByName\" : \"createdByName\",    \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"label\" : \"label\",    \"type\" : \"cas3_person_arrived\",    \"body\" : \"body\"  }, {    \"createdByName\" : \"createdByName\",    \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"label\" : \"label\",    \"type\" : \"cas3_person_arrived\",    \"body\" : \"body\"  } ],  \"applicationOrigin\" : \"homeDetentionCurfew\",  \"createdBy\" : {    \"name\" : \"Roger Smith\",    \"nomisUsername\" : \"SMITHR_GEN\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"email\" : \"Roger.Smith@justice.gov.uk\"  },  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"assignmentDate\" : \"2000-01-23\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"allocatedPomName\" : \"allocatedPomName\",  \"status\" : \"inProgress\"}")
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
     * @see ApplicationsCas2#getCas2Application
     */
    fun getCas2Application(applicationId: java.util.UUID): ResponseEntity<Cas2Application> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"currentPrisonName\" : \"currentPrisonName\",  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"telephoneNumber\" : \"telephoneNumber\",  \"allocatedPomEmailAddress\" : \"allocatedPomEmailAddress\",  \"omuEmailAddress\" : \"omuEmailAddress\",  \"isTransferredApplication\" : true,  \"bailHearingDate\" : \"2000-01-23\",  \"outdatedSchema\" : true,  \"type\" : \"type\",  \"cas2CreatedBy\" : {    \"authSource\" : \"nomis\",    \"name\" : \"Roger Smith\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"email\" : \"Roger.Smith@justice.gov.uk\",    \"username\" : \"SMITHR_GEN\"  },  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"assessment\" : {    \"assessorName\" : \"assessorName\",    \"nacroReferralId\" : \"nacroReferralId\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"statusUpdates\" : [ {      \"updatedBy\" : {        \"origin\" : \"NACRO\",        \"name\" : \"Roger Smith\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"email\" : \"roger@external.example.com\",        \"username\" : \"CAS2_ASSESSOR_USER\"      },      \"statusUpdateDetails\" : [ {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      }, {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      } ],      \"name\" : \"moreInfoRequested\",      \"description\" : \"More information about the application has been requested from the POM (Prison Offender Manager).\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\",      \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"    }, {      \"updatedBy\" : {        \"origin\" : \"NACRO\",        \"name\" : \"Roger Smith\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"email\" : \"roger@external.example.com\",        \"username\" : \"CAS2_ASSESSOR_USER\"      },      \"statusUpdateDetails\" : [ {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      }, {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      } ],      \"name\" : \"moreInfoRequested\",      \"description\" : \"More information about the application has been requested from the POM (Prison Offender Manager).\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\",      \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"    } ]  },  \"timelineEvents\" : [ {    \"createdByName\" : \"createdByName\",    \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"label\" : \"label\",    \"type\" : \"cas3_person_arrived\",    \"body\" : \"body\"  }, {    \"createdByName\" : \"createdByName\",    \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"label\" : \"label\",    \"type\" : \"cas3_person_arrived\",    \"body\" : \"body\"  } ],  \"applicationOrigin\" : \"homeDetentionCurfew\",  \"createdBy\" : {    \"name\" : \"Roger Smith\",    \"nomisUsername\" : \"SMITHR_GEN\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"email\" : \"Roger.Smith@justice.gov.uk\"  },  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"assignmentDate\" : \"2000-01-23\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"allocatedPomName\" : \"allocatedPomName\",  \"status\" : \"inProgress\"}")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }


    /**
     * @see ApplicationsCas2#getCas2ApplicationSummaries
     */
    fun getCas2ApplicationSummaries(assignmentType: AssignmentType,
        page: kotlin.Int?): ResponseEntity<List<Cas2ApplicationSummary>> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "[ {  \"allocatedPomUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"currentPrisonName\" : \"currentPrisonName\",  \"bailHearingDate\" : \"2000-01-23\",  \"type\" : \"type\",  \"hdcEligibilityDate\" : \"2000-01-23\",  \"personName\" : \"personName\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"createdByUserName\" : \"createdByUserName\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"applicationOrigin\" : \"homeDetentionCurfew\",  \"nomsNumber\" : \"nomsNumber\",  \"assignmentDate\" : \"2000-01-23\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"allocatedPomName\" : \"allocatedPomName\",  \"crn\" : \"crn\",  \"status\" : \"inProgress\",  \"latestStatusUpdate\" : {    \"statusId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"label\" : \"More information requested\"  }}, {  \"allocatedPomUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"currentPrisonName\" : \"currentPrisonName\",  \"bailHearingDate\" : \"2000-01-23\",  \"type\" : \"type\",  \"hdcEligibilityDate\" : \"2000-01-23\",  \"personName\" : \"personName\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"createdByUserName\" : \"createdByUserName\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"applicationOrigin\" : \"homeDetentionCurfew\",  \"nomsNumber\" : \"nomsNumber\",  \"assignmentDate\" : \"2000-01-23\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"allocatedPomName\" : \"allocatedPomName\",  \"crn\" : \"crn\",  \"status\" : \"inProgress\",  \"latestStatusUpdate\" : {    \"statusId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"label\" : \"More information requested\"  }} ]")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }


    /**
     * @see ApplicationsCas2#updateCas2Application
     */
    fun updateCas2Application(applicationId: java.util.UUID,
        body: UpdateCas2Application): ResponseEntity<Cas2Application> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"currentPrisonName\" : \"currentPrisonName\",  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"telephoneNumber\" : \"telephoneNumber\",  \"allocatedPomEmailAddress\" : \"allocatedPomEmailAddress\",  \"omuEmailAddress\" : \"omuEmailAddress\",  \"isTransferredApplication\" : true,  \"bailHearingDate\" : \"2000-01-23\",  \"outdatedSchema\" : true,  \"type\" : \"type\",  \"cas2CreatedBy\" : {    \"authSource\" : \"nomis\",    \"name\" : \"Roger Smith\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"email\" : \"Roger.Smith@justice.gov.uk\",    \"username\" : \"SMITHR_GEN\"  },  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"assessment\" : {    \"assessorName\" : \"assessorName\",    \"nacroReferralId\" : \"nacroReferralId\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"statusUpdates\" : [ {      \"updatedBy\" : {        \"origin\" : \"NACRO\",        \"name\" : \"Roger Smith\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"email\" : \"roger@external.example.com\",        \"username\" : \"CAS2_ASSESSOR_USER\"      },      \"statusUpdateDetails\" : [ {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      }, {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      } ],      \"name\" : \"moreInfoRequested\",      \"description\" : \"More information about the application has been requested from the POM (Prison Offender Manager).\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\",      \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"    }, {      \"updatedBy\" : {        \"origin\" : \"NACRO\",        \"name\" : \"Roger Smith\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"email\" : \"roger@external.example.com\",        \"username\" : \"CAS2_ASSESSOR_USER\"      },      \"statusUpdateDetails\" : [ {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      }, {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      } ],      \"name\" : \"moreInfoRequested\",      \"description\" : \"More information about the application has been requested from the POM (Prison Offender Manager).\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\",      \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"    } ]  },  \"timelineEvents\" : [ {    \"createdByName\" : \"createdByName\",    \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"label\" : \"label\",    \"type\" : \"cas3_person_arrived\",    \"body\" : \"body\"  }, {    \"createdByName\" : \"createdByName\",    \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"label\" : \"label\",    \"type\" : \"cas3_person_arrived\",    \"body\" : \"body\"  } ],  \"applicationOrigin\" : \"homeDetentionCurfew\",  \"createdBy\" : {    \"name\" : \"Roger Smith\",    \"nomisUsername\" : \"SMITHR_GEN\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"email\" : \"Roger.Smith@justice.gov.uk\"  },  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"assignmentDate\" : \"2000-01-23\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"allocatedPomName\" : \"allocatedPomName\",  \"status\" : \"inProgress\"}")
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
