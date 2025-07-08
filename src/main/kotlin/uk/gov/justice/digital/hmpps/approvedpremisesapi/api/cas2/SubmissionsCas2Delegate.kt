package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.generated.Cas2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2Application
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link SubmissionsCas2Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")interface SubmissionsCas2Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see SubmissionsCas2#submissionsApplicationIdGet
     */
    fun submissionsApplicationIdGet(applicationId: java.util.UUID): ResponseEntity<Cas2SubmittedApplication> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "{  \"submittedBy\" : {    \"name\" : \"Roger Smith\",    \"nomisUsername\" : \"SMITHR_GEN\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"email\" : \"Roger.Smith@justice.gov.uk\"  },  \"currentPrisonName\" : \"currentPrisonName\",  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"telephoneNumber\" : \"telephoneNumber\",  \"allocatedPomEmailAddress\" : \"allocatedPomEmailAddress\",  \"omuEmailAddress\" : \"omuEmailAddress\",  \"isTransferredApplication\" : true,  \"document\" : \"{}\",  \"outdatedSchema\" : true,  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"timelineEvents\" : [ {    \"createdByName\" : \"createdByName\",    \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"label\" : \"label\",    \"type\" : \"cas3_person_arrived\",    \"body\" : \"body\"  }, {    \"createdByName\" : \"createdByName\",    \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"label\" : \"label\",    \"type\" : \"cas3_person_arrived\",    \"body\" : \"body\"  } ],  \"assessment\" : {    \"assessorName\" : \"assessorName\",    \"nacroReferralId\" : \"nacroReferralId\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"statusUpdates\" : [ {      \"updatedBy\" : {        \"origin\" : \"NACRO\",        \"name\" : \"Roger Smith\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"email\" : \"roger@external.example.com\",        \"username\" : \"CAS2_ASSESSOR_USER\"      },      \"statusUpdateDetails\" : [ {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      }, {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      } ],      \"name\" : \"moreInfoRequested\",      \"description\" : \"More information about the application has been requested from the POM (Prison Offender Manager).\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\",      \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"    }, {      \"updatedBy\" : {        \"origin\" : \"NACRO\",        \"name\" : \"Roger Smith\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"email\" : \"roger@external.example.com\",        \"username\" : \"CAS2_ASSESSOR_USER\"      },      \"statusUpdateDetails\" : [ {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      }, {        \"name\" : \"moreInfoRequested\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"label\" : \"More information requested\"      } ],      \"name\" : \"moreInfoRequested\",      \"description\" : \"More information about the application has been requested from the POM (Prison Offender Manager).\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"label\" : \"More information requested\",      \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"    } ]  },  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"assignmentDate\" : \"2000-01-23\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"allocatedPomName\" : \"allocatedPomName\"}")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }


    /**
     * @see SubmissionsCas2#submissionsGet
     */
    fun submissionsGet(page: kotlin.Int?): ResponseEntity<List<Cas2SubmittedApplicationSummary>> {
                        getRequest().ifPresent { request ->
                    for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                            ApiUtil.setExampleResponse(request, "application/json", "[ {  \"personName\" : \"personName\",  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"nomsNumber\" : \"nomsNumber\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"crn\" : \"crn\"}, {  \"personName\" : \"personName\",  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"nomsNumber\" : \"nomsNumber\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"crn\" : \"crn\"} ]")
                            break
                        }
                    }
                }
                return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                                            }


    /**
     * @see SubmissionsCas2#submissionsPost
     */
    fun submissionsPost(submitCas2Application: SubmitCas2Application): ResponseEntity<Unit> {
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

}
