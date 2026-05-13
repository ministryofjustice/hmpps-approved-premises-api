package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.sar

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.assertJsonEquals

class CAS3SubjectAccessRequestServiceTest : Cas3SarTestBase() {

  @Test
  fun `Get CAS3 Information - No Results`() {
    val (offenderDetails, _) = givenAnOffender()
    val result =
      sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    assertNull(result)
  }

  @Test
  fun `Get CAS3 Information - Null dates check`() {
    val (offenderDetails, _) = givenAnOffender()
    val result =
      sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, null, null)

    assertNull(result)
  }

  @Test
  fun `Get CAS3 Information - Applications`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = userEntity()
    val temporaryAccommodationApplication = temporaryAccommodationApplicationEntity(offenderDetails, user)
    val result = sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)
    assertNotNull(result)

    val expectedJson = """
      {
        "Applications" : [${temporaryAccommodationApplicationJson(temporaryAccommodationApplication)}],
        "Assessments"  : [],
        "AssessmentReferralHistoryNotes" : [],
        "Bookings": [],
        "BookingExtensions": [],
        "Cancellations": [],
        "DomainEvents": [],
        "DomainEventsMetadata": []
      }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS3 Information - Assessments`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = userEntity()
    val temporaryAccommodationApplication = temporaryAccommodationApplicationEntity(offenderDetails, user)
    val temporaryAccomodationAssessment = temporaryAccommodationAssessmentEntity(temporaryAccommodationApplication)
    val result = sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    assertNotNull(result)

    val expectedJson = """
      {
        "Applications" : [${temporaryAccommodationApplicationJson(temporaryAccommodationApplication)}],
        "Assessments"  : [${temporaryAccommodationAssessmentJson(temporaryAccomodationAssessment)}],
        "AssessmentReferralHistoryNotes" : [],
        "Bookings": [],
        "BookingExtensions": [],
        "Cancellations": [],
        "DomainEvents": [],
        "DomainEventsMetadata": []
      }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS3 Information - Assessment Referral History Notes`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = userEntity()
    val temporaryAccommodationApplication = temporaryAccommodationApplicationEntity(offenderDetails, user)
    val temporaryAccomodationAssessment = temporaryAccommodationAssessmentEntity(temporaryAccommodationApplication)
    val assessmentReferralHistoryNoteSystem =
      assessmentReferralHistorySystemNoteEntity(temporaryAccomodationAssessment, user)
    val assessmentReferralHistoryNoteUser = assessmentReferralHistoryUserNoteEntity(temporaryAccomodationAssessment, user)
    val result = sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    assertNotNull(result)

    val expectedJson = """
      {
        "Applications": [${temporaryAccommodationApplicationJson(temporaryAccommodationApplication)}],
        "Assessments": [${temporaryAccommodationAssessmentJson(temporaryAccomodationAssessment)}],
        "AssessmentReferralHistoryNotes": [${assessmentReferralHistoryNotesJson(assessmentReferralHistoryNoteSystem, assessmentReferralHistoryNoteUser)} ],
        "Bookings": [],
        "BookingExtensions": [],
        "Cancellations": [],
        "DomainEvents": [],
        "DomainEventsMetadata": []
      }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS3 information - have a booking`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = userEntity()
    val application = temporaryAccommodationApplicationEntity(offenderDetails, user)

    val booking = bookingEntity(offenderDetails, application, null, ServiceName.temporaryAccommodation)

    val result =
      sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    assertNotNull(result)

    val expectedJson = """
    {        
        "Applications" : [${temporaryAccommodationApplicationJson(application)}],
        "Assessments"  : [],
        "AssessmentReferralHistoryNotes" : [],
        "Bookings": [${bookingsJson(booking)}],
        "BookingExtensions": [],
        "Cancellations": [],
        "DomainEvents": [],
        "DomainEventsMetadata": []
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS3 information - have a booking with extension`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = userEntity()
    val application = temporaryAccommodationApplicationEntity(offenderDetails, user)

    val booking = bookingEntity(offenderDetails, application, null, ServiceName.temporaryAccommodation)
    val bookingExtension = bookingExtensionEntity(booking)

    val result =
      sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    assertNotNull(result)

    val expectedJson = """
    {
        "Applications" : [${temporaryAccommodationApplicationJson(application)}],
        "Assessments"  : [],
        "AssessmentReferralHistoryNotes" : [],
        "Bookings": [${bookingsJson(booking)}],
        "BookingExtensions": [${bookingExtensionJson(bookingExtension)}],
        "Cancellations": [],
        "DomainEvents": [],
        "DomainEventsMetadata": []
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS3 information - have a booking cancellation`() {
    val (offenderDetails, _) = givenAnOffender()
    val user = userEntity()
    val application = temporaryAccommodationApplicationEntity(offenderDetails, user)
    val booking = bookingEntity(offenderDetails, application, null, ServiceName.temporaryAccommodation)
    val cancellation = cancellationEntity(booking)

    val result =
      sarService.getCAS3Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    assertNotNull(result)

    val expectedJson = """
    {      
      "Applications" : [${temporaryAccommodationApplicationJson(application)}],
      "Assessments"  : [],
      "AssessmentReferralHistoryNotes" : [], 
      "Bookings": [${bookingsJson(booking)}],
      "BookingExtensions": [],
      "Cancellations": [${cancellationJson(cancellation)}],
      "DomainEvents": [],
      "DomainEventsMetadata": []
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS3 information - Domain Events`() {
    val (offender, _) = givenAnOffender()
    val user = userEntity()
    val application = temporaryAccommodationApplicationEntity(offender, user)
    val assessment = temporaryAccommodationAssessmentEntity(application)

    val domainEvent = domainEventEntity(offender, application.id, assessment.id, user.id, ServiceName.temporaryAccommodation)
    val result = sarService.getCAS3Result(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)

    assertNotNull(result)

    val expectedJson = """
      {
        "Applications" : [${temporaryAccommodationApplicationJson(application)}],
        "Assessments"  : [${temporaryAccommodationAssessmentJson(assessment)}],
        "AssessmentReferralHistoryNotes" : [],
        "Bookings": [],
        "BookingExtensions": [],
        "Cancellations": [],
        "DomainEvents": [${domainEventJson(domainEvent,user)}],
        "DomainEventsMetadata": [${domainEventsMetadataJson(domainEvent)}]
      }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }
}
