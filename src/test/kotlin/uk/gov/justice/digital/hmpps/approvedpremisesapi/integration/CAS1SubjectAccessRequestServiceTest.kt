package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedMoveEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.assertJsonEquals
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime

@SuppressWarnings("LargeClass")
class CAS1SubjectAccessRequestServiceTest : SubjectAccessRequestServiceTestBase() {

  @Test
  fun `Get CAS1 Information - No Results`() {
    val (offenderDetails, _) = givenAnOffender()
    val result =
      sarService.getCAS1Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)
    assertJsonEquals(
      """ 
     {
          "Applications": [ ],
          "ApplicationTimeline": [ ],
          "Assessments": [ ],
          "AssessmentClarificationNotes": [ ],
          "Bookings": [ ],
          "SpaceBookings": [ ],
          "OfflineApplications":  [ ],
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ],
          "BookingNotMades" : [ ],
          "DomainEvents": [ ],
          "DomainEventsMetadata": [ ]
      }
      """.trimIndent(),
      result,
    )
  }

  @Test
  fun `Get CAS1 Information - Test Null Dates`() {
    val (offenderDetails, _) = givenAnOffender()
    val result =
      sarService.getCAS1Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, null, null)
    assertJsonEquals(
      """ 
     {
          "Applications": [ ],
          "ApplicationTimeline": [ ],
          "Assessments": [ ],
          "AssessmentClarificationNotes": [ ],
          "Bookings": [ ],
          "SpaceBookings": [ ],
          "OfflineApplications":  [ ],
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ],
          "BookingNotMades" : [ ],
          "DomainEvents": [ ],
          "DomainEventsMetadata": [ ]
      }
      """.trimIndent(),
      result,
    )
  }

  @Test
  fun `Get CAS1 Information - Have Application`() {
    val (offenderDetails, _) = givenAnOffender()

    val application = approvedPremisesApplicationEntity(offenderDetails)

    val result =
      sarService.getCAS1Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
        "Applications": [${approvedPremisesApplicationsJson(application, offenderDetails)}],
        "ApplicationTimeline" :[ ],
        "Assessments": [ ],
        "AssessmentClarificationNotes": [ ],
        "Bookings": [ ],
        "SpaceBookings": [ ],
        "OfflineApplications":  [ ],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "BedMoves": [ ],
        "Appeals": [ ],
        "PlacementApplications": [ ],
        "PlacementRequests": [ ],
        "PlacementRequirements": [ ],
        "PlacementRequirementCriteria" : [ ],
        "BookingNotMades" : [ ],
        "DomainEvents": [ ],
        "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(
      expectedJson,
      result,
    )
  }

  @Test
  fun `Get CAS1 information - have application note`() {
    val (offender, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offender)

    val timelineNotes = applicationTimelineNoteEntity(application)

    val result = sarService.getCAS1Result(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
        "Applications": [${approvedPremisesApplicationsJson(application, offender)}],
        "ApplicationTimeline": [${approvedPremisesApplicationTimelineNotesJson(application, timelineNotes, offender)}],
        "Assessments": [ ],
        "AssessmentClarificationNotes": [ ],
        "Bookings": [ ],
        "SpaceBookings": [ ],
        "OfflineApplications":  [ ],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "BedMoves": [ ],
        "Appeals": [ ],
        "PlacementApplications": [ ],
        "PlacementRequests": [ ],
        "PlacementRequirements": [ ],
        "PlacementRequirementCriteria" : [ ],
        "BookingNotMades" : [ ],
        "DomainEvents": [ ],
        "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(
      expectedJson,
      result,
    )
  }

  @Test
  fun `Get CAS1 information - have assessment`() {
    val (offenderDetails, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offenderDetails)

    val assessment = approvedPremisesAssessmentEntity(application)

    val result =
      sarService.getCAS1Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
        "Applications": [${approvedPremisesApplicationsJson(application, offenderDetails)}],
        "ApplicationTimeline" :[ ],
        "Assessments": [${approvedPremisesAssessmentJson(application, offenderDetails, assessment)}],
        "AssessmentClarificationNotes": [ ],
        "Bookings": [ ],
        "SpaceBookings": [ ],
        "OfflineApplications":  [ ],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "BedMoves": [ ],
        "Appeals": [ ],
        "PlacementApplications": [ ],
        "PlacementRequests": [ ],
        "PlacementRequirements": [ ],
        "PlacementRequirementCriteria" : [ ],
        "BookingNotMades" : [ ],
        "DomainEvents": [ ],
        "DomainEventsMetadata": [ ]
    }
    """
    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have assessment with clarification notes`() {
    val (offenderDetails, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offenderDetails)
    val assessment = approvedPremisesAssessmentEntity(application)
    val clarificationNote = approvedPremisesAssessmentClarificationNoteEntity(assessment)

    val result =
      sarService.getCAS1Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
       "Applications": [${approvedPremisesApplicationsJson(application, offenderDetails)}],
       "ApplicationTimeline" :[ ],
       "Assessments": [${approvedPremisesAssessmentJson(application, offenderDetails, assessment)}],
       "AssessmentClarificationNotes": [${approvedPremisesAssessmentClarificationNoteJson(
      assessment,
      offenderDetails,
      clarificationNote,
    )}],
       "Bookings": [ ],
       "SpaceBookings": [ ],
       "OfflineApplications":  [ ],
       "BookingExtensions": [ ],
       "Cancellations": [ ],
       "BedMoves": [ ],
       "Appeals": [ ],
       "PlacementApplications": [ ],
       "PlacementRequests": [ ],
       "PlacementRequirements": [ ],
       "PlacementRequirementCriteria" : [ ],
       "BookingNotMades" : [ ],
       "DomainEvents": [ ],
       "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have a space booking`() {
    val (offenderDetails, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offenderDetails)
    val nonArrivalReason = nonArrivalReasonEntityFactory.produceAndPersist()
    val departureReason = departureReasonEntityFactory.produceAndPersist()
    val moveOnCategory = moveOnCategoryEntityFactory.produceAndPersist()
    val cancellationReason = cancellationReasonEntityFactory.produceAndPersist()
    val booking = spaceBookingEntity(
      offenderDetails = offenderDetails,
      application = application,
      nonArrivalReason = nonArrivalReason,
      departureReason = departureReason,
      moveOnCategory = moveOnCategory,
      cancellationReason = cancellationReason,
    )

    val result =
      sarService.getCAS1Result(
        offenderDetails.otherIds.crn,
        offenderDetails.otherIds.nomsNumber,
        START_DATE,
        END_DATE,
      )

    val expectedJson = """
    {        
      "Applications":[${approvedPremisesApplicationsJson(application, offenderDetails)}],
      "ApplicationTimeline" :[ ],
      "Assessments": [ ],
      "AssessmentClarificationNotes": [ ],
      "Bookings": [],
      "SpaceBookings":  [ ${spaceBookingsJson(booking)} ],
      "OfflineApplications":  [ ],
      "BookingExtensions": [ ],
      "Cancellations": [ ],
      "BedMoves": [ ],
      "Appeals": [ ],
      "PlacementApplications": [ ],
      "PlacementRequests": [ ],
      "PlacementRequirements": [ ],
      "PlacementRequirementCriteria" : [ ],
      "BookingNotMades" : [ ],
      "DomainEvents": [ ],
      "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have a space booking with offline application`() {
    val (offenderDetails, _) = givenAnOffender()
    val offlineApplication = offlineApplicationEntity(offenderDetails)
    val booking = spaceBookingEntity(
      offenderDetails = offenderDetails,
      offlineApplication = offlineApplication,
    )

    val result =
      sarService.getCAS1Result(
        offenderDetails.otherIds.crn,
        offenderDetails.otherIds.nomsNumber,
        START_DATE,
        END_DATE,
      )

    val expectedJson = """
    {        
      "Applications":[ ],
      "ApplicationTimeline" :[ ],
      "Assessments": [ ],
      "AssessmentClarificationNotes": [ ],
      "Bookings": [],
      "SpaceBookings":  [ ${spaceBookingsJson(booking)} ],
      "OfflineApplications": [${offlineApplicationForSpaceBookingJson(booking)}],
      "BookingExtensions": [ ],
      "Cancellations": [ ],
      "BedMoves": [ ],
      "Appeals": [ ],
      "PlacementApplications": [ ],
      "PlacementRequests": [ ],
      "PlacementRequirements": [ ],
      "PlacementRequirementCriteria" : [ ],
      "BookingNotMades" : [ ],
      "DomainEvents": [ ],
      "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have a booking`() {
    val (offenderDetails, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offenderDetails)

    val booking = bookingEntity(offenderDetails, application)

    val result =
      sarService.getCAS1Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {        
      "Applications":[${approvedPremisesApplicationsJson(application, offenderDetails)}],
      "ApplicationTimeline" :[ ],
      "Assessments": [ ],
      "AssessmentClarificationNotes": [ ],
      "Bookings": [${bookingsJson(booking)}],
      "SpaceBookings":  [ ],
      "OfflineApplications":  [ ],
      "BookingExtensions": [ ],
      "Cancellations": [ ],
      "BedMoves": [ ],
      "Appeals": [ ],
      "PlacementApplications": [ ],
      "PlacementRequests": [ ],
      "PlacementRequirements": [ ],
      "PlacementRequirementCriteria" : [ ],
      "BookingNotMades" : [ ],
      "DomainEvents": [ ],
      "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have a booking with an offline application`() {
    val (offenderDetails, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offenderDetails)
    val offlineApplication = offlineApplicationEntity(offenderDetails)
    val booking = bookingEntity(offenderDetails, application, offlineApplication)

    val result =
      sarService.getCAS1Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {       
        "Applications": [${approvedPremisesApplicationsJson(application, offenderDetails)}],
        "ApplicationTimeline" :[ ],
        "Assessments": [ ],
        "AssessmentClarificationNotes": [ ],
        "Bookings":[${bookingsJson(booking)}],
        "SpaceBookings": [ ],
        "OfflineApplications": [${offlineApplicationJson(booking)}],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "BedMoves": [ ],
        "Appeals": [ ],
        "PlacementApplications": [ ],
        "PlacementRequests": [ ],
        "PlacementRequirements": [ ],
        "PlacementRequirementCriteria" : [ ],
        "BookingNotMades" : [ ],
        "DomainEvents": [ ],
        "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have a booking with extension`() {
    val (offenderDetails, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offenderDetails)

    val booking = bookingEntity(offenderDetails, application)
    val bookingExtension = bookingExtensionEntity(booking)

    val result =
      sarService.getCAS1Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
        "Applications": [${approvedPremisesApplicationsJson(application, offenderDetails)}],
        "ApplicationTimeline" :[ ],
        "Assessments": [ ],
        "AssessmentClarificationNotes": [ ],
        "Bookings": [${bookingsJson(booking)}],
        "SpaceBookings":  [ ],
        "OfflineApplications":  [ ],
        "BookingExtensions":[${bookingExtensionJson(bookingExtension)}],
        "Cancellations": [ ],
        "BedMoves": [ ],
        "Appeals": [ ],
        "PlacementApplications": [ ],
        "PlacementRequests": [ ],
        "PlacementRequirements": [ ],
        "PlacementRequirementCriteria" : [ ],
        "BookingNotMades" : [ ],
        "DomainEvents": [ ],
        "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have a booking cancellation`() {
    val (offenderDetails, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offenderDetails)

    val booking = bookingEntity(offenderDetails, application)
    val cancellation = cancellationEntity(booking)

    val result =
      sarService.getCAS1Result(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {      
        "Applications": [${approvedPremisesApplicationsJson(application, offenderDetails)}],
        "ApplicationTimeline" :[ ],
        "Assessments": [ ],
        "AssessmentClarificationNotes": [ ],
        "Bookings": [${bookingsJson(booking)}],
        "SpaceBookings":  [ ],
        "OfflineApplications":  [ ],
        "BookingExtensions": [ ],
        "Cancellations": [${cancellationJson(cancellation)}],
        "BedMoves": [ ],
        "Appeals": [ ],
        "PlacementApplications": [ ],
        "PlacementRequests": [ ],
        "PlacementRequirements": [ ],
        "PlacementRequirementCriteria" : [ ],
        "BookingNotMades" : [ ],
        "DomainEvents": [ ],
        "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has a bed move`() {
    val (offender, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offender)
    val booking = bookingEntity(offender, application)

    val newBed = bedEntity()
    val bedMove = bedMoveEntity(booking, booking.bed!!, newBed)

    val result =
      sarService.getCAS1Result(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)
    val expectedJson = """
    {
        "Applications": [${approvedPremisesApplicationsJson(application, offender)}],
        "ApplicationTimeline" :[ ],
        "Assessments": [ ],
        "AssessmentClarificationNotes": [ ],
        "Bookings": [${bookingsJson(booking)}],
        "SpaceBookings":  [ ],
        "OfflineApplications":  [ ],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "BedMoves": [${bedMovesJson(bedMove)}],
        "Appeals": [ ],
        "PlacementApplications": [ ],
        "PlacementRequests": [ ],
        "PlacementRequirements": [ ],
        "PlacementRequirementCriteria" : [ ],
        "BookingNotMades" : [ ],
        "DomainEvents": [ ],
        "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has an appeal`() {
    val (offender, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offender)
    val assessment = approvedPremisesAssessmentEntity(application)
    val appeal = appealEntity(application, assessment)

    val result =
      sarService.getCAS1Result(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)
    val expectedJson = """
    {        
        "Applications": [${approvedPremisesApplicationsJson(application, offender)}],
        "ApplicationTimeline" :[ ],
        "Assessments": [${approvedPremisesAssessmentJson(application, offender, assessment)}],
        "AssessmentClarificationNotes": [ ],
        "Bookings": [ ],
        "SpaceBookings": [ ],
        "OfflineApplications":  [ ],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "BedMoves": [ ],
        "Appeals":[ ${appealsJson(appeal)}],
        "PlacementApplications": [ ],
        "PlacementRequests": [ ],
        "PlacementRequirements": [ ],
        "PlacementRequirementCriteria" : [ ],
        "BookingNotMades" : [ ],
        "DomainEvents": [ ],
        "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has a placement application`() {
    val (offender, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offender)

    val placementApplication = placementApplicationEntity(application)
    val result =
      sarService.getCAS1Result(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)
    val expectedJson = """
    {
      "Applications": [${approvedPremisesApplicationsJson(application, offender)}],
      "ApplicationTimeline" :[ ],
      "Assessments": [ ],
      "AssessmentClarificationNotes": [ ],
      "Bookings": [ ],
      "SpaceBookings": [ ],
      "OfflineApplications":  [ ],
      "BookingExtensions": [ ],
      "Cancellations": [ ],
      "BedMoves": [ ],
      "Appeals": [ ],
      "PlacementApplications": [${approvedPremisesPlacementApplicationsJson(placementApplication)}],
      "PlacementRequests": [ ],
      "PlacementRequirements": [ ],
      "PlacementRequirementCriteria" : [ ],
      "BookingNotMades" : [ ],
      "DomainEvents": [ ],
      "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has a placement request with requirements`() {
    val (offender, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offender)
    val assessment = approvedPremisesAssessmentEntity(application)
    val booking = bookingEntity(offender, application)
    val placementApplication = placementApplicationEntity(application)
    val allocatedUser = userEntity()

    val placementRequest = placementRequestEntity(booking, assessment, application, allocatedUser, placementApplication)
    val result =
      sarService.getCAS1Result(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)
    val expectedJson = """
    {   
        "Applications": [${approvedPremisesApplicationsJson(application, offender)}],
        "ApplicationTimeline" :[ ],
        "Assessments": [${approvedPremisesAssessmentJson(application, offender, assessment)}],
        "AssessmentClarificationNotes": [ ],
        "Bookings": [${bookingsJson(booking)}],
        "OfflineApplications":  [ ],    
        "SpaceBookings":  [ ],    
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "BedMoves": [ ],
        "Appeals": [ ],
        "PlacementApplications": [${approvedPremisesPlacementApplicationsJson(placementApplication)}],
        "PlacementRequests": [${approvedPremisesPlacementRequestsJson(placementRequest)}],
        "PlacementRequirements": [${placementRequirementJson(placementRequest.placementRequirements)}],
        "PlacementRequirementCriteria" : [${placementRequirementCriteriaJson(placementRequest.placementRequirements)}],
        "BookingNotMades" : [ ],
        "DomainEvents": [ ],
        "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has bookings not made`() {
    val (offender, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offender)
    val assessment = approvedPremisesAssessmentEntity(application)
    val booking = bookingEntity(offender, application)
    val placementApplication = placementApplicationEntity(application)
    val allocatedUser = userEntity()
    val placementRequest = placementRequestEntity(booking, assessment, application, allocatedUser, placementApplication)
    val bookingNotMade = bookingNotMadeEntity(placementRequest)

    val result =
      sarService.getCAS1Result(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)
    val expectedJson = """
    {
      "Applications": [${approvedPremisesApplicationsJson(application, offender)}],
      "ApplicationTimeline" :[ ],
      "Assessments": [${approvedPremisesAssessmentJson(application, offender, assessment)}],
      "AssessmentClarificationNotes": [ ],
      "Bookings":[${bookingsJson(booking)}],
      "SpaceBookings":  [ ],    
      "OfflineApplications":  [ ],    
      "BookingExtensions": [ ],
      "Cancellations": [ ],
      "BedMoves": [ ],
      "Appeals": [ ],
      "PlacementApplications": [${approvedPremisesPlacementApplicationsJson(placementApplication)}],
      "PlacementRequests": [${approvedPremisesPlacementRequestsJson(placementRequest)}],
      "PlacementRequirements": [${placementRequirementJson(placementRequest.placementRequirements)}],
      "PlacementRequirementCriteria" : [${placementRequirementCriteriaJson(placementRequest.placementRequirements)}],
      "BookingNotMades": [${bookingsNotMadeJson(bookingNotMade)}],
      "DomainEvents": [ ],
      "DomainEventsMetadata": [ ]
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - Domain Events`() {
    val (offender, _) = givenAnOffender()

    val application = approvedPremisesApplicationEntity(offender)
    val assessment = approvedPremisesAssessmentEntity(application)
    val user = userEntity()
    val domainEvent = domainEventEntity(offender, application.id, assessment.id, user.id)
    val result = sarService.getCAS1Result(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
      {
        "Applications": [${approvedPremisesApplicationsJson(application, offender)}],
        "ApplicationTimeline" :[ ],
        "Assessments": [${approvedPremisesAssessmentJson(application, offender, assessment)}],
        "AssessmentClarificationNotes": [ ],
        "Bookings": [ ],
        "SpaceBookings": [ ],
        "OfflineApplications":  [ ],    
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "BedMoves": [ ],
        "Appeals": [ ],
        "PlacementApplications": [ ],
        "PlacementRequests": [ ],
        "PlacementRequirements": [ ],
        "PlacementRequirementCriteria" : [ ],
        "BookingNotMades": [],
        "DomainEvents": [${domainEventJson(domainEvent,user)}],
        "DomainEventsMetadata": [${domainEventsMetadataJson(domainEvent)}]
      }
    """.trimIndent()
    assertJsonEquals(expectedJson, result)
  }

  private fun placementRequirementCriteriaJson(placementRequirements: PlacementRequirementsEntity): String =
    """
      {
          "crn": "${placementRequirements.application.crn}",
          "noms_number": "${placementRequirements.application.nomsNumber}",
          "placement_requirement_id": "${placementRequirements.id}",
          "criteria_name": "${placementRequirements.desirableCriteria[0].name}",
          "service_scope": "${placementRequirements.desirableCriteria[0].serviceScope}",
          "model_scope": "${placementRequirements.desirableCriteria[0].modelScope}",
          "property_name": "${placementRequirements.desirableCriteria[0].propertyName}",
          "is_active": ${placementRequirements.desirableCriteria[0].isActive},
          "criteria_type": "DESIRABLE"
      },
      {
          "crn": "${placementRequirements.application.crn}",
          "noms_number": "${placementRequirements.application.nomsNumber}",
          "placement_requirement_id": "${placementRequirements.id}",
          "criteria_name": "${placementRequirements.essentialCriteria[0].name}",
          "service_scope": "${placementRequirements.essentialCriteria[0].serviceScope}",
          "model_scope": "${placementRequirements.essentialCriteria[0].modelScope}",
          "property_name": "${placementRequirements.essentialCriteria[0].propertyName}",
          "is_active": ${placementRequirements.essentialCriteria[0].isActive},
          "criteria_type": "ESSENTIAL"
      }
    """.trimIndent()

  private fun placementRequirementJson(placementRequirement: PlacementRequirementsEntity): String =
    """
      {
        "crn": "${placementRequirement.application.crn}",
        "noms_number": "${placementRequirement.application.nomsNumber}",
        "application_id": "${placementRequirement.application.id}",
        "assessment_id": "${placementRequirement.assessment.id}",
        "placement_requirements_id": "${placementRequirement.id}",
        "gender": "${placementRequirement.gender.value.uppercase()}",
        "ap_type": "${placementRequirement.apType.value.uppercase()}",
        "outcode": "${placementRequirement.postcodeDistrict.outcode}",
        "radius": ${placementRequirement.radius},
        "created_at": "$CREATED_AT"
      }
    """.trimIndent()

  private fun approvedPremisesPlacementRequestsJson(placementRequest: PlacementRequestEntity): String =
    """
      {
        "crn": "${placementRequest.application.crn}",
        "noms_number": "${placementRequest.application.nomsNumber}",
        "expected_arrival": "$arrivedAtDateOnly",
        "duration": ${placementRequest.duration},
        "created_at": "$CREATED_AT",
        "placement_application_id": "${placementRequest.placementApplication?.id}",
        "booking_id": "${placementRequest.booking?.id}",
        "application_id": "${placementRequest.application.id}",
        "assessment_id": "${placementRequest.assessment.id}",
        "notes": "${placementRequest.notes}",
        "is_parole": ${placementRequest.isParole},
        "is_withdrawn": ${placementRequest.isWithdrawn},
        "withdrawal_reason": "${placementRequest.withdrawalReason}",
        "due_at": "$DUE_AT"
      }
    """.trimIndent()

  private fun approvedPremisesPlacementApplicationsJson(placementApplication: PlacementApplicationEntity): String =
    """
      {
        "crn": "${placementApplication.application.crn}",
        "noms_number": "${placementApplication.application.nomsNumber}",
        "application_id": "${placementApplication.application.id}",
        "data": $DATA_JSON_SIMPLE,
        "document": $DOCUMENT_JSON_SIMPLE,
        "created_at": "$CREATED_AT_NO_TZ",
        "submitted_at": "$SUBMITTED_AT_NO_TZ" ,
        "allocated_at": null,
        "reallocated_at": null,
        "due_at": null,
        "decision": "${placementApplication.decision}",
        "decision_made_at": "$DECISION_MADE_AT_NO_TZ" ,
        "placement_type": "${PlacementType.ADDITIONAL_PLACEMENT}",
        "is_withdrawn": ${placementApplication.isWithdrawn},
        "withdrawal_reason": "${placementApplication.withdrawalReason}",
        "created_by_user": "${placementApplication.createdByUser.name}",
        "allocated_user": null
      }
    """.trimIndent()

  private fun appealsJson(appeal: AppealEntity): String =
    """
      {
          "crn": "${appeal.application.crn}" ,
          "noms_number": "${appeal.application.nomsNumber}", 
          "appeal_id": "${appeal.id}",
          "application_id": "${appeal.application.id}",
          "assessment_id": "${appeal.assessment.id}",
          "appeal_date": "$APPEAL_DATE_ONLY",
          "appeal_detail": "${appeal.appealDetail}",
          "decision" : "${appeal.decision}",
          "decision_detail": "${appeal.decisionDetail}" ,
          "appeal_created_at": "$CREATED_AT" ,
          "created_by_user" :  "${appeal.createdBy.name}"
      }
    """.trimIndent()

  private fun approvedPremisesApplicationsJson(
    application: ApprovedPremisesApplicationEntity,
    offenderDetails: OffenderDetailSummary,
  ): String = """
        {
           "id": "${application.id}",
           "name": "$NAME",
           "crn": "${offenderDetails.otherIds.crn}",
           "noms_number": "${offenderDetails.otherIds.nomsNumber}",
           "data": $DATA_JSON_SIMPLE, 
           "document": $DOCUMENT_JSON_SIMPLE,
           "created_at": "$CREATED_AT",
           "submitted_at": "$SUBMITTED_AT",
           "created_by_user": "${application.createdByUser.name}",
           "application_user_name": "${application.applicantUserDetails?.name}",
           "event_number": "$EVENT_NUMBER",
           "is_womens_application": false,
           "offence_id": "$OFFENCE_ID",
           "conviction_id": $CONVICTION_ID,
           "risk_ratings": ${risksJson()}, 
           "release_type": "$RELEASE_TYPE_CONDITIONAL",
           "arrival_date": "$ARRIVED_AT",
           "is_withdrawn": false,
           "withdrawal_reason": "$WITHDRAWAL_REASON_NOT_WITHDRAWN",
           "other_withdrawal_reason": "$OTHER_WITHDRAWAL_REASON_NOT_APPLICABLE",
           "is_emergency_application": true,
           "target_location": null,
           "status": "${ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT}",
           "inmate_in_out_status_on_submission": null,
           "sentence_type": "$SENTENCE_TYPE_CUSTODIAL",
           "notice_type":  "${Cas1ApplicationTimelinessCategory.emergency}",
           "ap_type": "${ApprovedPremisesType.NORMAL}",
           "case_manager_name": "${application.caseManagerUserDetails?.name}",
           "case_manager_is_not_applicant" : true
        }
  """.trimIndent()

  private fun approvedPremisesApplicationTimelineNotesJson(
    application: ApprovedPremisesApplicationEntity,
    timelineNote: ApplicationTimelineNoteEntity,
    offender: OffenderDetailSummary,
    serviceName: ServiceName = ServiceName.approvedPremises,
  ): String =
    """
      {
          "application_id":"${application.id}",
          "service":"${serviceName.value}",
          "crn":"${offender.otherIds.crn}",
          "noms_number":"${offender.otherIds.nomsNumber}",
          "body":"${timelineNote.body}",
          "created_at":"$CREATED_AT_NO_TZ",
          "user_name":"${timelineNote.createdBy?.name}"
      }
    """.trimIndent()

  private fun approvedPremisesAssessmentJson(
    application: ApprovedPremisesApplicationEntity,
    offenderDetails: OffenderDetailSummary,
    assessment: ApprovedPremisesAssessmentEntity,
  ): String =
    """
      {
         "application_id":"${application.id}",
         "assessment_id":"${assessment.id}",
         "crn":"${offenderDetails.otherIds.crn}",
         "noms_number":"${offenderDetails.otherIds.nomsNumber}",
         "assessor_name":"${assessment.allocatedToUser?.name}",
         "data":$DATA_JSON_SIMPLE,
         "document":$DOCUMENT_JSON_SIMPLE,
         "created_at":"$CREATED_AT",
         "allocated_at":"$ALLOCATED_AT",
         "submitted_at":"$SUBMITTED_AT",
         "reallocated_at":null,
         "due_at":"$DUE_AT",
         "decision":"${AssessmentDecision.REJECTED}",
         "rejection_rationale":"rejected as no good",
         "service":"approved-premises",
         "is_withdrawn":false,
         "created_from_appeal":false
      }
    """.trimIndent()

  private fun approvedPremisesAssessmentClarificationNoteJson(
    assessment: ApprovedPremisesAssessmentEntity,
    offenderDetails: OffenderDetailSummary,
    clarificationNote: AssessmentClarificationNoteEntity,
  ): String =
    """
      {
        "application_id": "${assessment.application.id}",
        "assessment_id": "${assessment.id}",
        "crn": "${offenderDetails.otherIds.crn}",
        "noms_number": "${offenderDetails.otherIds.nomsNumber}",
        "created_at": "$CREATED_AT",
        "query": "${clarificationNote.query}",
        "response": "${clarificationNote.response}",
        "created_by_user": "${clarificationNote.createdByUser.name}",   
      }
    """.trimIndent()

  private fun bookingsNotMadeJson(bookingNotMade: BookingNotMadeEntity): String =
    """
      {
        "crn": "${bookingNotMade.placementRequest.application.crn}",
        "noms_number": "${bookingNotMade.placementRequest.application.nomsNumber}",
        "booking_not_made_id": "${bookingNotMade.id}",
        "application_id": "${bookingNotMade.placementRequest.application.id}",
        "placement_request_id": "${bookingNotMade.placementRequest.id}",
        "created_at": "$CREATED_AT_NO_TZ",
        "notes": "${bookingNotMade.notes}"
      }
    """.trimIndent()

  private fun offlineApplicationForSpaceBookingJson(booking: Cas1SpaceBookingEntity) =
    """
      {
        "crn": "${booking.crn}",
        "noms_number": ${if (booking.application?.nomsNumber != null) "\"${booking.application?.nomsNumber}\"" else null},
        "offline_application_id":"${booking.offlineApplication!!.id}",
        "booking_id":"${booking.id}",
        "created_at":"$CREATED_AT"
      }
    """.trimIndent()

  private fun offlineApplicationJson(booking: BookingEntity) =
    """
      {
        "crn": "${booking.crn}",
        "noms_number": "${booking.nomsNumber}",
        "offline_application_id":"${booking.offlineApplication!!.id}",
        "booking_id":"${booking.id}",
        "created_at":"$CREATED_AT"
      }
    """.trimIndent()

  private fun bedMovesJson(bedMove: BedMoveEntity): String =
    """
      {
        "crn": "${bedMove.booking.crn}" ,
        "noms_number": "${bedMove.booking.nomsNumber}",
        "notes": "${bedMove.notes}",
        "previous_bed_name": "${bedMove.previousBed!!.name}",
        "previous_bed_code":"${bedMove.previousBed!!.code}",
        "new_bed_name":"${bedMove.newBed.name}",
        "new_bed_code":"${bedMove.newBed.code}",
        "created_at": "$CREATED_AT_NO_TZ"
      }
    """.trimIndent()

  private fun offlineApplicationEntity(offenderDetails: OffenderDetailSummary) =
    offlineApplicationEntityFactory.produceAndPersist {
      withService(ServiceName.approvedPremises.value)
      withCrn(offenderDetails.otherIds.crn)
      withEventNumber("1")
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    }

  private fun placementRequestEntity(
    booking: BookingEntity,
    assessment: ApprovedPremisesAssessmentEntity,
    application: ApprovedPremisesApplicationEntity,
    allocatedUser: UserEntity,
    placementApplication: PlacementApplicationEntity,
  ) = placementRequestFactory.produceAndPersist {
    withBooking(booking)
    withAssessment(assessment)
    withApplication(application)
    withPlacementApplication(placementApplication)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withAllocatedToUser(allocatedUser)
    withDueAt(OffsetDateTime.parse(DUE_AT))
    withDuration(5)
    withExpectedArrival(LocalDate.parse(arrivedAtDateOnly))
    withIsParole(false)
    withIsWithdrawn(true)
    withWithdrawalReason(PlacementRequestWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST)
    withNotes("some notes")
    withPlacementRequirements(placementRequirementEntity(application, assessment))
  }

  private fun placementRequirementEntity(
    application: ApprovedPremisesApplicationEntity,
    assessment: ApprovedPremisesAssessmentEntity,
  ) = placementRequirementsFactory.produceAndPersist {
    withApplication(application)
    withAssessment(assessment)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withApType(ApType.normal)
    withDesirableCriteria(listOf(characteristicEntity()))
    withEssentialCriteria(listOf(characteristicEntity()))
    withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
  }

  private fun characteristicEntity() =
    characteristicEntityFactory.produceAndPersist {
      withName(randomStringMultiCaseWithNumbers(10))
      withServiceScope(Characteristic.ServiceScope.star.value)
      withModelScope(Characteristic.ModelScope.room.value)
      withPropertyName(randomStringMultiCaseWithNumbers(6))
    }

  private fun placementApplicationEntity(application: ApprovedPremisesApplicationEntity) =
    placementApplicationFactory.produceAndPersist {
      withApplication(application)
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withSubmittedAt(OffsetDateTime.parse(SUBMITTED_AT))
      withDueAt(null)
      withData(DATA_JSON_SIMPLE)
      withDocument(DOCUMENT_JSON_SIMPLE)
      withAllocatedToUser(null)
      withCreatedByUser(application.createdByUser)
      withDecision(PlacementApplicationDecision.ACCEPTED)
      withDecisionMadeAt(OffsetDateTime.parse(DECISION_MADE_AT))
      withIsWithdrawn(true)
      withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
      withReallocatedAt(null)
      withSchemaVersion(approvedPremisesPlacementApplicationJsonSchemaEntity())
      withWithdrawalReason(PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
    }

  private fun appealEntity(
    application: ApprovedPremisesApplicationEntity,
    assessment: ApprovedPremisesAssessmentEntity,
  ) = appealEntityFactory.produceAndPersist {
    withApplication(application)
    withAssessment(assessment)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withCreatedBy(application.createdByUser)
    withAppealDate(LocalDate.parse(APPEAL_DATE_ONLY))
    withAppealDetail("I want to appeal this decision")
    withDecision(AppealDecision.rejected)
    withDecisionDetail("rejected as no good")
  }

  private fun approvedPremisesApplicationJsonSchemaEntity(): ApprovedPremisesApplicationJsonSchemaEntity =
    approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

  private fun approvedPremisesAssessmentJsonSchemaEntity(): ApprovedPremisesAssessmentJsonSchemaEntity =
    approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

  private fun approvedPremisesPlacementApplicationJsonSchemaEntity() =
    approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

  private fun cas1ApplicationUserDetailsEntity(): Cas1ApplicationUserDetailsEntity =
    cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
      withEmailAddress("noname_applicant_user@noname.net")
    }

  private fun cas1CaseManagerUserDetailsEntity(): Cas1ApplicationUserDetailsEntity =
    cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
      withEmailAddress("noname@noname.net")
    }

  private fun applicationTimelineNoteEntity(application: ApprovedPremisesApplicationEntity) =
    applicationTimelineNoteEntityFactory.produceAndPersist {
      withApplicationId(application.id)
      withBody("Some random note about this application")
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withCreatedBy(application.createdByUser)
    }

  private fun bedMoveEntity(booking: BookingEntity, previousBed: BedEntity, newBed: BedEntity): BedMoveEntity =
    this.bedMoveEntityFactory.produceAndPersist {
      withBooking(booking)
      withPreviousBed(previousBed)
      withNewBed(newBed)
      withNotes("Some Notes about a bed move")
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    }
  private fun approvedPremisesApplicationEntity(offenderDetails: OffenderDetailSummary): ApprovedPremisesApplicationEntity {
    val user = userEntity()
    val risk1 = personRisks()
    val applicantUserDetails = cas1ApplicationUserDetailsEntity()
    val caseManagerUserDetails = cas1CaseManagerUserDetailsEntity()
    val applicationSchema = approvedPremisesApplicationJsonSchemaEntity()
    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withNomsNumber(offenderDetails.otherIds.nomsNumber)
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withSubmittedAt(OffsetDateTime.parse(SUBMITTED_AT))
      withName(NAME)
      withCreatedByUser(user)
      withEventNumber(EVENT_NUMBER)
      withIsWomensApplication(false)
      withOffenceId(OFFENCE_ID)
      withConvictionId(CONVICTION_ID)
      withRiskRatings(risk1)
      withReleaseType(RELEASE_TYPE_CONDITIONAL)
      withArrivalDate(OffsetDateTime.parse(ARRIVED_AT))
      withIsWithdrawn(false)
      withWithdrawalReason(WITHDRAWAL_REASON_NOT_WITHDRAWN)
      withOtherWithdrawalReason(OTHER_WITHDRAWAL_REASON_NOT_APPLICABLE)
      withIsEmergencyApplication(true)
      withTargetLocation(null)
      withStatus(ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT)
      withInmateInOutStatusOnSubmission(null)
      withSentenceType(SENTENCE_TYPE_CUSTODIAL)
      withNoticeType(Cas1ApplicationTimelinessCategory.emergency)
      withApType(ApprovedPremisesType.NORMAL)
      withApplicantUserDetails(applicantUserDetails)
      withCaseManagerUserDetails(caseManagerUserDetails)
      withCaseManagerIsNotApplicant(true)
      withApplicationSchema(applicationSchema)
      withData(DATA_JSON_SIMPLE)
      withDocument(DOCUMENT_JSON_SIMPLE)
    }
    return application
  }
  private fun approvedPremisesAssessmentEntity(
    application: ApprovedPremisesApplicationEntity,
  ): ApprovedPremisesAssessmentEntity = approvedPremisesAssessmentEntityFactory.produceAndPersist {
    withData(DATA_JSON_SIMPLE)
    withDocument(DOCUMENT_JSON_SIMPLE)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withAllocatedAt(OffsetDateTime.parse(ALLOCATED_AT))
    withIsWithdrawn(false)
    withAllocatedToUser(userEntity())
    withApplication(application)
    withAssessmentSchema(approvedPremisesAssessmentJsonSchemaEntity())
    withCreatedFromAppeal(false)
    withDecision(AssessmentDecision.REJECTED)
    withReallocatedAt(null)
    withRejectionRationale("rejected as no good")
    withSubmittedAt(OffsetDateTime.parse(SUBMITTED_AT))
    withDueAt(OffsetDateTime.parse(DUE_AT))
  }

  private fun bookingNotMadeEntity(placementRequest: PlacementRequestEntity) =
    bookingNotMadeFactory.produceAndPersist {
      withPlacementRequest(placementRequest)
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withNotes("Some notes on booking not made")
    }
}
