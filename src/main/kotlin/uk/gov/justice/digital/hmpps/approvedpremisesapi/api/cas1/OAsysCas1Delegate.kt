package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import java.util.Optional
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysGroup
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysGroupName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysMetadata

/**
 * A delegate to be called by the {@link OAsysCas1Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")
interface OAsysCas1Delegate {

  fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

  /**
   * @see OAsysCas1#answers
   */
  fun answers(
    crn: kotlin.String,
    group: Cas1OASysGroupName,
    includeOptionalSections: kotlin.collections.List<kotlin.Int>?,
  ): ResponseEntity<Cas1OASysGroup> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"assessmentMetadata\" : {    \"hasApplicableAssessment\" : true,    \"dateStarted\" : \"2000-01-23T04:56:07.000+00:00\",    \"dateCompleted\" : \"2000-01-23T04:56:07.000+00:00\"  },  \"answers\" : [ {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  }, {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  } ],  \"group\" : \"riskManagementPlan\"}")
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
   * @see OAsysCas1#metadata
   */
  fun metadata(crn: kotlin.String): ResponseEntity<Cas1OASysMetadata> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"assessmentMetadata\" : {    \"hasApplicableAssessment\" : true,    \"dateStarted\" : \"2000-01-23T04:56:07.000+00:00\",    \"dateCompleted\" : \"2000-01-23T04:56:07.000+00:00\"  },  \"supportingInformation\" : [ {    \"oasysAnswerLinkedToHarm\" : true,    \"oasysAnswerLinkedToReOffending\" : true,    \"inclusionOptional\" : true,    \"section\" : 10,    \"sectionLabel\" : \"sectionLabel\"  }, {    \"oasysAnswerLinkedToHarm\" : true,    \"oasysAnswerLinkedToReOffending\" : true,    \"inclusionOptional\" : true,    \"section\" : 10,    \"sectionLabel\" : \"sectionLabel\"  } ]}")
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
