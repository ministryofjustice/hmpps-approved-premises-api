package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import java.util.Optional
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1UpdateAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1UpdatedClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection

/**
 * A delegate to be called by the {@link AssessmentsCas1Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")
interface AssessmentsCas1Delegate {

  fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

  /**
   * @see AssessmentsCas1#acceptAssessment
   */
  fun acceptAssessment(
    assessmentId: java.util.UUID,
    cas1AssessmentAcceptance: Cas1AssessmentAcceptance,
  ): ResponseEntity<Unit> = ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

  /**
   * @see AssessmentsCas1#addClarificationNoteToAssessment
   */
  fun addClarificationNoteToAssessment(
    assessmentId: java.util.UUID,
    cas1NewClarificationNote: Cas1NewClarificationNote,
  ): ResponseEntity<Cas1ClarificationNote> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"response\" : \"response\",  \"query\" : \"query\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"responseReceivedOn\" : \"2000-01-23\",  \"createdByStaffMemberId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see AssessmentsCas1#getAssessment
   */
  fun getAssessment(assessmentId: java.util.UUID): ResponseEntity<Cas1Assessment> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"decision\" : \"accepted\",  \"data\" : \"{}\",  \"document\" : \"{}\",  \"outdatedSchema\" : true,  \"createdFromAppeal\" : true,  \"allocatedToStaffMember\" : {    \"telephoneNumber\" : \"telephoneNumber\",    \"roles\" : [ \"assessor\", \"assessor\" ],    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"cruManagementArea\" : \"\",    \"cruManagementAreaDefault\" : \"\",    \"isActive\" : true,    \"version\" : 1,    \"cruManagementAreaOverride\" : \"\",    \"qualifications\" : [ \"pipe\", \"pipe\" ],    \"apArea\" : {      \"identifier\" : \"LON\",      \"name\" : \"Yorkshire & The Humber\",      \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"    },    \"service\" : \"service\",    \"permissions\" : [ \"cas1_adhoc_booking_create\", \"cas1_adhoc_booking_create\" ],    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\"  },  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"application\" : {    \"assessmentDecisionDate\" : \"2000-01-23\",    \"data\" : \"{}\",    \"document\" : \"{}\",    \"licenceExpiryDate\" : \"2000-01-23\",    \"isPipeApplication\" : true,    \"isEsapApplication\" : true,    \"cruManagementArea\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"apArea\" : {      \"identifier\" : \"LON\",      \"name\" : \"Yorkshire & The Humber\",      \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"personStatusOnSubmission\" : \"InCustody\",    \"isEmergencyApplication\" : true,    \"isWomensApplication\" : true,    \"caseManagerUserDetails\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"name\" : \"name\",      \"email\" : \"email\"    },    \"arrivalDate\" : \"2000-01-23T04:56:07.000+00:00\",    \"risks\" : {      \"mappa\" : {        \"value\" : {          \"lastUpdated\" : \"2000-01-23\",          \"level\" : \"level\"        }      },      \"tier\" : {        \"value\" : {          \"lastUpdated\" : \"2000-01-23\",          \"level\" : \"level\"        }      },      \"roshRisks\" : {        \"value\" : {          \"lastUpdated\" : \"2000-01-23\",          \"overallRisk\" : \"overallRisk\",          \"riskToChildren\" : \"riskToChildren\",          \"riskToPublic\" : \"riskToPublic\",          \"riskToKnownAdult\" : \"riskToKnownAdult\",          \"riskToStaff\" : \"riskToStaff\"        },        \"status\" : \"retrieved\"      },      \"flags\" : {        \"value\" : [ \"value\", \"value\" ]      },      \"crn\" : \"crn\"    },    \"caseManagerIsNotApplicant\" : true,    \"applicantUserDetails\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"name\" : \"name\",      \"email\" : \"email\"    },    \"person\" : {      \"type\" : \"FullPerson\",      \"crn\" : \"crn\"    },    \"status\" : \"started\"  },  \"rejectionRationale\" : \"rejectionRationale\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"allocatedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"clarificationNotes\" : [ {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"response\" : \"response\",    \"query\" : \"query\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"responseReceivedOn\" : \"2000-01-23\",    \"createdByStaffMemberId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"response\" : \"response\",    \"query\" : \"query\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"responseReceivedOn\" : \"2000-01-23\",    \"createdByStaffMemberId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"status\" : \"awaiting_response\"}")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see AssessmentsCas1#getAssessmentsForUser
   */
  fun getAssessmentsForUser(
    sortDirection: SortDirection?,
    sortBy: Cas1AssessmentSortField?,
    statuses: kotlin.collections.List<Cas1AssessmentStatus>?,
    crnOrName: kotlin.String?,
    page: kotlin.Int?,
    perPage: kotlin.Int?,
  ): ResponseEntity<List<Cas1AssessmentSummary>> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "[ {  \"dueAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"decision\" : \"accepted\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"dateOfInfoRequest\" : \"2000-01-23T04:56:07.000+00:00\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"arrivalDate\" : \"2000-01-23T04:56:07.000+00:00\",  \"status\" : \"awaiting_response\"}, {  \"dueAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"risks\" : {    \"mappa\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"tier\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"level\" : \"level\"      }    },    \"roshRisks\" : {      \"value\" : {        \"lastUpdated\" : \"2000-01-23\",        \"overallRisk\" : \"overallRisk\",        \"riskToChildren\" : \"riskToChildren\",        \"riskToPublic\" : \"riskToPublic\",        \"riskToKnownAdult\" : \"riskToKnownAdult\",        \"riskToStaff\" : \"riskToStaff\"      },      \"status\" : \"retrieved\"    },    \"flags\" : {      \"value\" : [ \"value\", \"value\" ]    },    \"crn\" : \"crn\"  },  \"decision\" : \"accepted\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"dateOfInfoRequest\" : \"2000-01-23T04:56:07.000+00:00\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"arrivalDate\" : \"2000-01-23T04:56:07.000+00:00\",  \"status\" : \"awaiting_response\"} ]")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see AssessmentsCas1#rejectAssessment
   */
  fun rejectAssessment(
    assessmentId: java.util.UUID,
    cas1AssessmentRejection: Cas1AssessmentRejection,
  ): ResponseEntity<Unit> = ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

  /**
   * @see AssessmentsCas1#updateAssessment
   */
  fun updateAssessment(
    assessmentId: java.util.UUID,
    cas1UpdateAssessment: Cas1UpdateAssessment,
  ): ResponseEntity<Cas1Assessment> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"schemaVersion\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"decision\" : \"accepted\",  \"data\" : \"{}\",  \"document\" : \"{}\",  \"outdatedSchema\" : true,  \"createdFromAppeal\" : true,  \"allocatedToStaffMember\" : {    \"telephoneNumber\" : \"telephoneNumber\",    \"roles\" : [ \"assessor\", \"assessor\" ],    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"cruManagementArea\" : \"\",    \"cruManagementAreaDefault\" : \"\",    \"isActive\" : true,    \"version\" : 1,    \"cruManagementAreaOverride\" : \"\",    \"qualifications\" : [ \"pipe\", \"pipe\" ],    \"apArea\" : {      \"identifier\" : \"LON\",      \"name\" : \"Yorkshire & The Humber\",      \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"    },    \"service\" : \"service\",    \"permissions\" : [ \"cas1_adhoc_booking_create\", \"cas1_adhoc_booking_create\" ],    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\"  },  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"application\" : {    \"assessmentDecisionDate\" : \"2000-01-23\",    \"data\" : \"{}\",    \"document\" : \"{}\",    \"licenceExpiryDate\" : \"2000-01-23\",    \"isPipeApplication\" : true,    \"isEsapApplication\" : true,    \"cruManagementArea\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"apArea\" : {      \"identifier\" : \"LON\",      \"name\" : \"Yorkshire & The Humber\",      \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"createdByUserId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"personStatusOnSubmission\" : \"InCustody\",    \"isEmergencyApplication\" : true,    \"isWomensApplication\" : true,    \"caseManagerUserDetails\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"name\" : \"name\",      \"email\" : \"email\"    },    \"arrivalDate\" : \"2000-01-23T04:56:07.000+00:00\",    \"risks\" : {      \"mappa\" : {        \"value\" : {          \"lastUpdated\" : \"2000-01-23\",          \"level\" : \"level\"        }      },      \"tier\" : {        \"value\" : {          \"lastUpdated\" : \"2000-01-23\",          \"level\" : \"level\"        }      },      \"roshRisks\" : {        \"value\" : {          \"lastUpdated\" : \"2000-01-23\",          \"overallRisk\" : \"overallRisk\",          \"riskToChildren\" : \"riskToChildren\",          \"riskToPublic\" : \"riskToPublic\",          \"riskToKnownAdult\" : \"riskToKnownAdult\",          \"riskToStaff\" : \"riskToStaff\"        },        \"status\" : \"retrieved\"      },      \"flags\" : {        \"value\" : [ \"value\", \"value\" ]      },      \"crn\" : \"crn\"    },    \"caseManagerIsNotApplicant\" : true,    \"applicantUserDetails\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"name\" : \"name\",      \"email\" : \"email\"    },    \"person\" : {      \"type\" : \"FullPerson\",      \"crn\" : \"crn\"    },    \"status\" : \"started\"  },  \"rejectionRationale\" : \"rejectionRationale\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"allocatedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"clarificationNotes\" : [ {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"response\" : \"response\",    \"query\" : \"query\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"responseReceivedOn\" : \"2000-01-23\",    \"createdByStaffMemberId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"response\" : \"response\",    \"query\" : \"query\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"responseReceivedOn\" : \"2000-01-23\",    \"createdByStaffMemberId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"status\" : \"awaiting_response\"}")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }

  /**
   * @see AssessmentsCas1#updateAssessmentClarificationNote
   */
  fun updateAssessmentClarificationNote(
    assessmentId: java.util.UUID,
    noteId: java.util.UUID,
    cas1UpdatedClarificationNote: Cas1UpdatedClarificationNote,
  ): ResponseEntity<Cas1ClarificationNote> {
    getRequest().ifPresent { request ->
      for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
        if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
          ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"response\" : \"response\",  \"query\" : \"query\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"responseReceivedOn\" : \"2000-01-23\",  \"createdByStaffMemberId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
          break
        }
      }
    }
    return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
  }
}
