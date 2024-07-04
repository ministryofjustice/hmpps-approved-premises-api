package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.assertJsonEquals

class CAS1SubjectAccessRequestServiceTest : SubjectAccessRequestServiceTestBase() {

  @Test
  fun `Get CAS1 Information - No Results`() {
    val (offenderDetails, _) = `Given an Offender`()
    val result =
      sarService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)
    assertJsonEquals(
      """ 
     {
        "approvedPremises":
        {
          "Applications": [ ],
          "ApplicationTimeline": [ ],
          "Assessments": [ ],
          "AssessmentClarificationNotes": [ ],
          "Bookings": [ ],
          "OfflineApplications":  [ ],
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
          }
      }
      """.trimIndent(),
      result,
    )
  }

  @Test
  fun `Get CAS1 Information - Have Application`() {
    val (offenderDetails, _) = `Given an Offender`()

    val application = approvedPremisesApplicationEntity(offenderDetails)

    val result =
      sarService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
      "approvedPremises": 
      {    
        "Applications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
        "ApplicationTimeline" :[ ],
        "Assessments": [ ],
        "AssessmentClarificationNotes": [ ],
        "Bookings": [ ],
        "OfflineApplications":  [ ],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "BedMoves": [ ],
        "Appeals": [ ],
        "PlacementApplications": [ ],
        "PlacementRequests": [ ],
        "PlacementRequirements": [ ],
        "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(
      expectedJson,
      result,
    )
  }

  @Test
  fun `Get CAS1 information - have application note`() {
    val (offender, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offender)

    val timelineNotes = applicationTimelineNoteEntity(application)

    val result = sarService.getSarResult(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
      "approvedPremises" : 
      {
        "Applications": ${approvedPremisesApplicationsJson(application, offender)},
        "ApplicationTimeline": ${approvedPremisesApplicationTimelineNotesJson(application, timelineNotes, offender)},
        "Assessments": [ ],
        "AssessmentClarificationNotes": [ ],
        "Bookings": [ ],
        "OfflineApplications":  [ ],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "BedMoves": [ ],
        "Appeals": [ ],
        "PlacementApplications": [ ],
        "PlacementRequests": [ ],
        "PlacementRequirements": [ ],
        "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(
      expectedJson,
      result,
    )
  }

  @Test
  fun `Get CAS1 information - have assessment`() {
    val (offenderDetails, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offenderDetails)

    val assessment = approvedPremisesAssessment(application)

    val result =
      sarService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
      "approvedPremises" : 
      {  
        "Applications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
        "ApplicationTimeline" :[ ],
        "Assessments": ${approvedPremisesAssessmentJson(application, offenderDetails, assessment)},
        "AssessmentClarificationNotes": [ ],
        "Bookings": [ ],
        "OfflineApplications":  [ ],
        "BookingExtensions": [ ],
        "Cancellations": [ ],
        "BedMoves": [ ],
        "Appeals": [ ],
        "PlacementApplications": [ ],
        "PlacementRequests": [ ],
        "PlacementRequirements": [ ],
        "PlacementRequirementCriteria" : [ ]
      }
    }
    """
    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have assessment with clarification notes`() {
    val (offenderDetails, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offenderDetails)
    val assessment = approvedPremisesAssessment(application)
    val clarificationNote = approvedPremisesAssessmentClarificationNote(assessment)

    val result =
      sarService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
          "ApplicationTimeline" :[ ],
          "Assessments": ${approvedPremisesAssessmentJson(application, offenderDetails, assessment)},
          "AssessmentClarificationNotes": ${
    approvedPremisesAssessmentClarificationNoteJson(
      assessment,
      offenderDetails,
      clarificationNote,
    )
    },
          "Bookings": [ ],
          "OfflineApplications":  [ ],
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have a booking`() {
    val (offenderDetails, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offenderDetails)

    val booking = bookingEntity(offenderDetails, application)

    val result =
      sarService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
          "ApplicationTimeline" :[ ],
          "Assessments": [ ],
          "AssessmentClarificationNotes": [ ],
          "Bookings": ${bookingsJson(booking)},
          
          "OfflineApplications":  [ ],
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have a booking with an offline application`() {
    val (offenderDetails, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offenderDetails)
    val offlineApplication = offlineApplicationEntity(offenderDetails)
    val booking = bookingEntity(offenderDetails, application, offlineApplication)

    val result =
      sarService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
          "ApplicationTimeline" :[ ],
          "Assessments": [ ],
          "AssessmentClarificationNotes": [ ],
          "Bookings": ${bookingsJson(booking)},
          "OfflineApplications":  ${offlineApplicationJson(booking)},
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have a booking with extension`() {
    val (offenderDetails, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offenderDetails)

    val booking = bookingEntity(offenderDetails, application)
    val bookingExtension = bookingExtensionEntity(booking)

    val result =
      sarService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
          "ApplicationTimeline" :[ ],
          "Assessments": [ ],
          "AssessmentClarificationNotes": [ ],
          "Bookings": ${bookingsJson(booking)},
          "OfflineApplications":  [ ],
          "BookingExtensions": ${bookingExtensionJson(bookingExtension)},
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `Get CAS1 information - have a booking cancellation`() {
    val (offenderDetails, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offenderDetails)

    val booking = bookingEntity(offenderDetails, application)
    val cancellation = cancellationEntity(booking)

    val result =
      sarService.getSarResult(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber, START_DATE, END_DATE)

    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offenderDetails)},
          "ApplicationTimeline" :[ ],
          "Assessments": [ ],
          "AssessmentClarificationNotes": [ ],
          "Bookings": ${bookingsJson(booking)},
          "OfflineApplications":  [ ],
          "BookingExtensions": [ ],
          "Cancellations": ${cancellationJson(cancellation)},
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has a bed move`() {
    val (offender, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offender)
    val booking = bookingEntity(offender, application)

    val newBed = bedEntity()
    val bedMove = bedMoveEntity(booking, booking.bed!!, newBed)

    val result =
      sarService.getSarResult(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)
    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offender)},
          "ApplicationTimeline" :[ ],
          "Assessments": [ ],
          "AssessmentClarificationNotes": [ ],
          "Bookings": ${bookingsJson(booking)},
          "OfflineApplications":  [ ],
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": ${bedMovesJson(bedMove)},
          "Appeals": [ ],
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has an appeal`() {
    val (offender, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offender)
    val assessment = approvedPremisesAssessment(application)
    val appeal = appealEntity(application, assessment)

    val result =
      sarService.getSarResult(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)
    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offender)},
          "ApplicationTimeline" :[ ],
          "Assessments": ${approvedPremisesAssessmentJson(application, offender, assessment)},
          "AssessmentClarificationNotes": [ ],
          "Bookings": [ ],
          "OfflineApplications":  [ ],
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": ${appealsJson(appeal)},
          "PlacementApplications": [ ],
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has a placement application`() {
    val (offender, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offender)

    val placementApplication = placementApplicationEntity(application)
    val result =
      sarService.getSarResult(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)
    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offender)},
          "ApplicationTimeline" :[ ],
          "Assessments": [ ],
          "AssessmentClarificationNotes": [ ],
          "Bookings": [ ],
          "OfflineApplications":  [ ],
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": ${approvedPremisesPlacementApplicationsJson(placementApplication)},
          "PlacementRequests": [ ],
          "PlacementRequirements": [ ],
          "PlacementRequirementCriteria" : [ ]
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }

  @Test
  fun `get CAS1 information - has a placement request with requirements`() {
    val (offender, _) = `Given an Offender`()
    val application = approvedPremisesApplicationEntity(offender)
    val assessment = approvedPremisesAssessment(application)
    val booking = bookingEntity(offender, application)
    val placementApplication = placementApplicationEntity(application)
    val allocatedUser = userEntity()

    val placementRequest = placementRequestEntity(booking, assessment, application, allocatedUser, placementApplication)
    val result =
      sarService.getSarResult(offender.otherIds.crn, offender.otherIds.nomsNumber, START_DATE, END_DATE)
    val expectedJson = """
    {
      "approvedPremises" : 
      {        
          "Applications": ${approvedPremisesApplicationsJson(application, offender)},
          "ApplicationTimeline" :[ ],
          "Assessments": ${approvedPremisesAssessmentJson(application, offender, assessment)},
          "AssessmentClarificationNotes": [ ],
          "Bookings": ${bookingsJson(booking)},
          "OfflineApplications":  [ ],    
          "BookingExtensions": [ ],
          "Cancellations": [ ],
          "BedMoves": [ ],
          "Appeals": [ ],
          "PlacementApplications": ${approvedPremisesPlacementApplicationsJson(placementApplication)},
          "PlacementRequests": ${approvedPremisesPlacementRequestsJson(placementRequest)},
          "PlacementRequirements": ${placementRequirementJson(placementRequest.placementRequirements)},
          "PlacementRequirementCriteria" : ${placementRequirementCriteriaJson(placementRequest.placementRequirements)}
      }
    }
    """.trimIndent()

    assertJsonEquals(expectedJson, result)
  }
}
