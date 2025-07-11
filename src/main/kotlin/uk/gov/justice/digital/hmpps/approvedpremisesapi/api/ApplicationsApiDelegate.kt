package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Appeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawables
import java.util.Optional

/**
 * A delegate to be called by the {@link ApplicationsApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")
interface ApplicationsApiDelegate {

  fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

  /**
   * @see ApplicationsApi#applicationsApplicationIdAppealsAppealIdGet
   */
  fun applicationsApplicationIdAppealsAppealIdGet(
    applicationId: java.util.UUID,
    appealId: java.util.UUID,
  ): ResponseEntity<Appeal> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"appealDetail\" : \"appealDetail\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"decision\" : \"accepted\",  \"createdByUser\" : {    \"telephoneNumber\" : \"telephoneNumber\",    \"service\" : \"service\",    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\"  },  \"appealDate\" : \"2000-01-23\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"decisionDetail\" : \"decisionDetail\"}")
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
   * @see ApplicationsApi#applicationsApplicationIdAppealsPost
   */
  fun applicationsApplicationIdAppealsPost(
    applicationId: java.util.UUID,
    body: NewAppeal,
  ): ResponseEntity<Appeal> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"appealDetail\" : \"appealDetail\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"decision\" : \"accepted\",  \"createdByUser\" : {    \"telephoneNumber\" : \"telephoneNumber\",    \"service\" : \"service\",    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\"  },  \"appealDate\" : \"2000-01-23\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"decisionDetail\" : \"decisionDetail\"}")
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
   * @see ApplicationsApi#applicationsApplicationIdDocumentsGet
   */
  fun applicationsApplicationIdDocumentsGet(applicationId: java.util.UUID): ResponseEntity<List<Document>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"fileName\" : \"fileName\",  \"level\" : \"Offender\",  \"description\" : \"description\",  \"typeDescription\" : \"typeDescription\",  \"id\" : \"id\",  \"typeCode\" : \"typeCode\"}, {  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"fileName\" : \"fileName\",  \"level\" : \"Offender\",  \"description\" : \"description\",  \"typeDescription\" : \"typeDescription\",  \"id\" : \"id\",  \"typeCode\" : \"typeCode\"} ]")
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
   * @see ApplicationsApi#applicationsApplicationIdGet
   */
  fun applicationsApplicationIdGet(applicationId: java.util.UUID): ResponseEntity<Application> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"type\" : \"type\"}")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see ApplicationsApi#applicationsApplicationIdNotesPost
   */
  fun applicationsApplicationIdNotesPost(
    applicationId: java.util.UUID,
    body: NewApplicationTimelineNote,
  ): ResponseEntity<ApplicationTimelineNote> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"note\" : \"note\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"createdByUser\" : {    \"telephoneNumber\" : \"telephoneNumber\",    \"service\" : \"service\",    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
   * @see ApplicationsApi#applicationsApplicationIdPut
   */
  fun applicationsApplicationIdPut(
    applicationId: java.util.UUID,
    body: UpdateApplication,
  ): ResponseEntity<Application> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"type\" : \"type\"}")
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
   * @see ApplicationsApi#applicationsApplicationIdRequestsForPlacementGet
   */
  fun applicationsApplicationIdRequestsForPlacementGet(applicationId: java.util.UUID): ResponseEntity<List<RequestForPlacement>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"requestReviewedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"document\" : \"{}\",  \"dates\" : {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  },  \"type\" : \"manual\",  \"canBeDirectlyWithdrawn\" : true,  \"placementDates\" : [ {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  }, {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  } ],  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"isWithdrawn\" : true,  \"withdrawalReason\" : \"DuplicatePlacementRequest\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"status\" : \"request_unsubmitted\"}, {  \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"requestReviewedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"document\" : \"{}\",  \"dates\" : {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  },  \"type\" : \"manual\",  \"canBeDirectlyWithdrawn\" : true,  \"placementDates\" : [ {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  }, {    \"duration\" : 0,    \"expectedArrival\" : \"2000-01-23\"  } ],  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"isWithdrawn\" : true,  \"withdrawalReason\" : \"DuplicatePlacementRequest\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"status\" : \"request_unsubmitted\"} ]")
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
   * @see ApplicationsApi#applicationsApplicationIdSubmissionPost
   */
  fun applicationsApplicationIdSubmissionPost(
    applicationId: java.util.UUID,
    submitApplication: SubmitApplication,
  ): ResponseEntity<Unit> {
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
   * @see ApplicationsApi#applicationsApplicationIdWithdrawablesWithNotesGet
   */
  fun applicationsApplicationIdWithdrawablesWithNotesGet(
    applicationId: java.util.UUID,
    xServiceName: ServiceName,
  ): ResponseEntity<Withdrawables> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"withdrawables\" : [ {    \"dates\" : [ {      \"endDate\" : \"2000-01-23\",      \"startDate\" : \"2000-01-23\"    }, {      \"endDate\" : \"2000-01-23\",      \"startDate\" : \"2000-01-23\"    } ],    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"type\" : \"application\"  }, {    \"dates\" : [ {      \"endDate\" : \"2000-01-23\",      \"startDate\" : \"2000-01-23\"    }, {      \"endDate\" : \"2000-01-23\",      \"startDate\" : \"2000-01-23\"    } ],    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"type\" : \"application\"  } ],  \"notes\" : [ \"notes\", \"notes\" ]}")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see ApplicationsApi#applicationsApplicationIdWithdrawalPost
   */
  fun applicationsApplicationIdWithdrawalPost(
    applicationId: java.util.UUID,
    body: NewWithdrawal,
  ): ResponseEntity<Unit> {
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
   * @see ApplicationsApi#applicationsGet
   */
  fun applicationsGet(xServiceName: ServiceName?): ResponseEntity<List<ApplicationSummary>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"type\" : \"type\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\"}, {  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"type\" : \"type\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\"} ]")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see ApplicationsApi#applicationsPost
   */
  fun applicationsPost(
    body: NewApplication,
    xServiceName: ServiceName?,
    createWithRisks: kotlin.Boolean?,
  ): ResponseEntity<Application> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"type\" : \"type\"}")
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
