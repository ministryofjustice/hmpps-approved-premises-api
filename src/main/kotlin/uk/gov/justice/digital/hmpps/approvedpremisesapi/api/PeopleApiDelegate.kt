package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ActiveOffence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSections
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonAcctAlert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PrisonCaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")
interface PeopleApiDelegate {

  fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

  fun peopleCrnAcctAlertsGet(crn: kotlin.String): ResponseEntity<List<PersonAcctAlert>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"dateCreated\" : \"2000-01-23\",  \"dateExpires\" : \"2000-01-23\",  \"description\" : \"description\",  \"comment\" : \"comment\",  \"alertId\" : 0,  \"alertTypeDescription\" : \"alertTypeDescription\"}, {  \"dateCreated\" : \"2000-01-23\",  \"dateExpires\" : \"2000-01-23\",  \"description\" : \"description\",  \"comment\" : \"comment\",  \"alertId\" : 0,  \"alertTypeDescription\" : \"alertTypeDescription\"} ]")
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

  fun peopleCrnAdjudicationsGet(
    crn: kotlin.String,
    xServiceName: ServiceName,
  ): ResponseEntity<List<Adjudication>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"reportedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"establishment\" : \"establishment\",  \"id\" : 0,  \"finding\" : \"finding\",  \"offenceDescription\" : \"Wounding or inflicting grievous bodily harm (inflicting bodily injury with or without weapon) (S20) - 00801\",  \"hearingHeld\" : true}, {  \"reportedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"establishment\" : \"establishment\",  \"id\" : 0,  \"finding\" : \"finding\",  \"offenceDescription\" : \"Wounding or inflicting grievous bodily harm (inflicting bodily injury with or without weapon) (S20) - 00801\",  \"hearingHeld\" : true} ]")
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

  fun peopleCrnOasysSectionsGet(
    crn: kotlin.String,
    selectedSections: kotlin.collections.List<kotlin.Int>?,
  ): ResponseEntity<OASysSections> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"roshSummary\" : [ {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  }, {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  } ],  \"riskToSelf\" : [ {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  }, {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  } ],  \"dateStarted\" : \"2000-01-23T04:56:07.000+00:00\",  \"dateCompleted\" : \"2000-01-23T04:56:07.000+00:00\",  \"offenceDetails\" : [ {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  }, {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  } ],  \"supportingInformation\" : [ {    \"answer\" : \"answer\",    \"sectionNumber\" : 0,    \"linkedToReOffending\" : true,    \"label\" : \"label\",    \"linkedToHarm\" : true,    \"questionNumber\" : \"questionNumber\"  }, {    \"answer\" : \"answer\",    \"sectionNumber\" : 0,    \"linkedToReOffending\" : true,    \"label\" : \"label\",    \"linkedToHarm\" : true,    \"questionNumber\" : \"questionNumber\"  } ],  \"assessmentId\" : 138985987,  \"assessmentState\" : \"Completed\",  \"riskManagementPlan\" : [ {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  }, {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  } ]}")
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

  fun peopleCrnOffencesGet(crn: kotlin.String): ResponseEntity<List<ActiveOffence>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"offenceId\" : \"M1502750438\",  \"convictionId\" : 1502724704,  \"deliusEventNumber\" : \"7\",  \"offenceDate\" : \"2000-01-23\",  \"offenceDescription\" : \"offenceDescription\"}, {  \"offenceId\" : \"M1502750438\",  \"convictionId\" : 1502724704,  \"deliusEventNumber\" : \"7\",  \"offenceDate\" : \"2000-01-23\",  \"offenceDescription\" : \"offenceDescription\"} ]")
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

  fun peopleCrnPrisonCaseNotesGet(
    crn: kotlin.String,
    xServiceName: ServiceName,
  ): ResponseEntity<List<PrisonCaseNote>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"note\" : \"note\",  \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"authorName\" : \"authorName\",  \"subType\" : \"subType\",  \"id\" : \"id\",  \"sensitive\" : true,  \"type\" : \"type\"}, {  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"note\" : \"note\",  \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"authorName\" : \"authorName\",  \"subType\" : \"subType\",  \"id\" : \"id\",  \"sensitive\" : true,  \"type\" : \"type\"} ]")
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

  fun peopleSearchGet(crn: kotlin.String): ResponseEntity<Person> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"type\" : \"FullPerson\",  \"crn\" : \"crn\"}")
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
