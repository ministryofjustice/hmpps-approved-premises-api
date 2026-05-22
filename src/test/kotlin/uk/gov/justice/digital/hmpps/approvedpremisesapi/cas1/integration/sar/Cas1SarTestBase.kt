package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.sar

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TransferReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.SubjectAccessRequestServiceTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JpaApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

open class Cas1SarTestBase : SubjectAccessRequestServiceTestBase() {

  lateinit var premises: ApprovedPremisesEntity

  companion object {
    const val RELEASE_TYPE_CONDITIONAL = "CONDITIONAL"
    const val WITHDRAWAL_REASON_NOT_WITHDRAWN = "NOT WITHDRAWN"
    const val OTHER_WITHDRAWAL_REASON_NOT_APPLICABLE = "NOT APPLICABLE"
    const val SENTENCE_TYPE_CUSTODIAL = "CUSTODIAL"
    const val LICENCE_EXPIRY_DATE = "2021-07-17"
    const val EXPIRED_REASON = "Expired reason"
    const val REASON_COMMENTS = "agree with short notice reason comments"
    const val LATE_APPLICATION_REASON = "late application reason"
    const val REQUESTED_DURATION = 8
    const val AUTHORISED_DURATION = 9
    const val ADDITIONAL_INFORMATION = "some additional information"
    const val CAS1_DATA_PATH = "db/seed/dev+test/cas1_application_data"

    val TRANSFER_TYPE = TransferType.PLANNED
    val TRANSFER_REASON = TransferReason.movingPersonCloserToResettlementArea

    val CAS1_APPLICATION_DATA by lazy { readResource("$CAS1_DATA_PATH/application_data.json") }
    val CAS1_APPLICATION_DOCUMENT by lazy { readResource("$CAS1_DATA_PATH/application_document.json") }
    val CAS1_ASSESSMENT_DATA by lazy { readResource("$CAS1_DATA_PATH/assessment_data.json") }
    val CAS1_ASSESSMENT_DOCUMENT by lazy { readResource("$CAS1_DATA_PATH/assessment_document.json") }
    val CAS1_PLACEMENT_APPLICATION_DOCUMENT by lazy { readResource("$CAS1_DATA_PATH/placement_application_document.json") }
  }

  @SuppressWarnings("CyclomaticComplexMethod")
  protected fun spaceBookingsJson(booking: Cas1SpaceBookingEntity): String =
    """
      {
         "crn": "${booking.crn}",
         "noms_number":  ${if (booking.application != null) "\"${booking.application!!.nomsNumber}\"" else "null"},
         "canonical_arrival_date": ${if (booking.canonicalArrivalDate != null) "\"${booking.canonicalArrivalDate}\"" else null},
         "canonical_departure_date": ${if (booking.canonicalDepartureDate != null) "\"${booking.canonicalDepartureDate}\"" else null},
         "expected_arrival_date": ${if (booking.expectedArrivalDate != null) "\"${booking.expectedArrivalDate}\"" else null},
         "expected_departure_date": ${if (booking.expectedDepartureDate != null) "\"${booking.expectedDepartureDate}\"" else null},
         "actual_arrival_date": ${if (booking.actualArrivalDate != null) "\"${booking.actualArrivalDate}\"" else null},
         "actual_arrival_time": ${if (booking.actualArrivalTime != null) "\"${booking.actualArrivalTime}:00\"" else null},
         "actual_departure_date": ${if (booking.actualDepartureDate != null) "\"${booking.actualDepartureDate}\"" else null},
         "actual_departure_time": ${if (booking.actualDepartureTime != null) "\"${booking.actualDepartureTime}:00\"" else null},
         "non_arrival_confirmed_at": ${if (booking.nonArrivalConfirmedAt != null) "\"$CREATED_AT\"" else null},
         "non_arrival_notes": ${if (booking.nonArrivalNotes != null) "\"${booking.nonArrivalNotes}\"" else null},
         "non_arrival_reason_id": ${if (booking.nonArrivalReason != null) "\"${booking.nonArrivalReason!!.id}\"" else null},
         "tier": ${if (booking.application?.riskRatings?.tier?.value?.level != null) "\"${booking.application?.riskRatings?.tier?.value?.level}\"" else null},
         "created_at": "$CREATED_AT",
         "key_worker_staff_code": "${booking.keyWorkerStaffCode}",
         "key_worker_assigned_at": "$CREATED_AT",
         "key_worker_name": "${booking.keyWorkerName}",
         "premises_name": "${booking.premises.name}",
         "person_name": ${if (booking.application != null) "\"${booking.application!!.name}\"" else "\"${booking.offlineApplication!!.name}\""},
         "delius_event_number": "${booking.deliusEventNumber}",
         "created_by_user_name":  ${booking.createdBy?.let { "\"${it.name}\"" }},
         "departure_reason": ${booking.departureReason?.let { "\"${it.name}\"" }},
         "departure_notes": ${if (booking.departureNotes != null) "\"${booking.departureNotes}\"" else null},
         "move_on_category": ${booking.departureMoveOnCategory?.let { "\"${it.name}\"" }},
         "cancellation_reason_notes": ${if (booking.cancellationReasonNotes != null) "\"${booking.cancellationReasonNotes}\"" else null},
         "cancellation_reason": ${booking.cancellationReason?.let { "\"${it.name}\"" }},
         "cancellation_occurred_at": ${if (booking.cancellationOccurredAt != null) "\"${booking.cancellationOccurredAt}\"" else null},
         "cancellation_recorded_at": "$CANCELLATION_DATE",
         "characteristics_property_names": "${booking.criteria?.let{ it.map { criteria -> criteria.propertyName}.sortedBy{ propertyName -> propertyName }.joinToString(",")}}",
         "transfer_type": ${booking.transferType?.let { "\"${booking.transferType}\"" }},
         "additional_information": ${booking.additionalInformation?.let { "\"${it}\"" }},
         "transfer_reason": ${booking.transferReason?.let { "\"${it.name}\"" }}
      }
    """.trimIndent()

  @SuppressWarnings("LongParameterList")
  protected fun spaceBookingEntity(
    offenderDetails: OffenderDetailSummary,
    application: ApprovedPremisesApplicationEntity? = null,
    nonArrivalReason: NonArrivalReasonEntity? = null,
    departureReason: DepartureReasonEntity? = null,
    moveOnCategory: MoveOnCategoryEntity? = null,
    cancellationReason: CancellationReasonEntity? = null,
    offlineApplication: OfflineApplicationEntity? = null,
    transferType: TransferType? = null,
    additionalInformation: String? = null,
    transferReason: TransferReason? = null,
    createdByUser: UserEntity? = null,
    premisesName: String = "a premises ${randomStringMultiCaseWithNumbers(5)}",
    characteristicName: String = randomStringMultiCaseWithNumbers(10),
    characteristicPropertyName: String = randomStringMultiCaseWithNumbers(6),
  ): Cas1SpaceBookingEntity {
    val user = createdByUser ?: givenAUser().first
    val (placementRequest) = givenAPlacementRequest(
      assessmentAllocatedTo = user,
      createdByUser = user,
    )
    val bed = bedEntity(premisesName = premisesName)
    return cas1SpaceBookingEntityFactory.produceAndPersist {
      withPlacementRequest(placementRequest)
      withCrn(offenderDetails.otherIds.crn)
      withPremises(premises)
      withApplication(application)
      withCanonicalArrivalDate(LocalDate.parse(arrivedAtDateOnly))
      withCanonicalDepartureDate(LocalDate.parse(departedAtDateOnly))
      withExpectedArrivalDate(LocalDate.parse(arrivedAtDateOnly))
      withExpectedDepartureDate(LocalDate.parse(departedAtDateOnly))
      withActualArrivalDate(LocalDate.parse(arrivedAtDateOnly))
      withActualArrivalTime(LocalTime.parse(arrivedAtTime))
      withActualDepartureDate(LocalDate.parse(departedAtDateOnly))
      withActualDepartureTime(LocalTime.parse(departedAtTime))
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withKeyworkerStaffCode("KEYWORKERSTAFFCODE")
      withKeyworkerName("KEYWORKERNAME")
      withKeyworkerAssignedAt(OffsetDateTime.parse(CREATED_AT).toInstant())
      withCreatedBy(user)
      withDeliusEventNumber("DELIUSEVENTNUMBER")
      withNonArrivalConfirmedAt(OffsetDateTime.parse(CREATED_AT).toInstant())
      withNonArrivalNotes("NONARRIVALNOTES")
      withNonArrivalReason(nonArrivalReason)
      withDepartureNotes("DEPARTURENOTES")
      withDepartureReason(departureReason)
      withMoveOnCategory(moveOnCategory)
      withCancellationOccurredAt(LocalDate.parse(cancellationDateOnly))
      withCancellationRecordedAt(OffsetDateTime.parse(CANCELLATION_DATE).toInstant())
      withCancellationReason(cancellationReason)
      withCancellationReasonNotes("CANCELLATIONREASONNOTES")
      withCriteria(
        mutableListOf(
          characteristicEntity(name = characteristicName, propertyName = characteristicPropertyName),
        ),
      )
      withOfflineApplication(offlineApplication)
      withTransferType(transferType)
      withAdditionalInformation(additionalInformation)
      withTransferReason(transferReason)
    }
  }

  protected fun bedEntity(
    premisesEntity: ApprovedPremisesEntity? = null,
    premisesName: String = "a premises ${randomStringMultiCaseWithNumbers(5)}",
  ) = bedEntityFactory.produceAndPersist {
    withName("a bed ${randomStringMultiCaseWithNumbers(5)}")
    withCode("a code ${randomStringMultiCaseWithNumbers(5)}")

    premises = givenAnApprovedPremises(
      name = premisesName,
      apCode = "AP Code ${randomStringMultiCaseWithNumbers(5)}",
      localAuthorityArea = localAuthorityEntityFactory.produceAndPersist {
        withName("An LAA ${randomStringMultiCaseWithNumbers(5)}")
        withIdentifier("LAA ID ${randomStringMultiCaseWithNumbers(5)}")
      },
      region = probationRegionEntity(),
    )

    withRoom(
      roomEntityFactory.produceAndPersist {
        withCode("room code ${randomStringMultiCaseWithNumbers(5)}")
        withName("room name ${randomStringMultiCaseWithNumbers(5)}")
        withPremises(premises)
      },
    )
  }

  protected fun approvedPremisesAssessmentClarificationNoteEntity(assessment: ApprovedPremisesAssessmentEntity): AssessmentClarificationNoteEntity = assessmentClarificationNoteEntityFactory.produceAndPersist {
    withAssessment(assessment)
    withCreatedBy(assessment.allocatedToUser!!)
    withQuery("some query")
    withResponse("a useful response")
    withResponseReceivedOn(LocalDate.parse(RESPONSE_RECEIVED_AT))
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
  }

  protected fun approvedPremisesApplicationEntity(
    offenderDetails: OffenderDetailSummary,
    caseManagerName: String? = null,
    createdByUser: UserEntity? = null,
    applicantUserName: String? = null,
    data: String = DATA_JSON_SIMPLE,
    document: String = DOCUMENT_JSON_SIMPLE,
  ): ApprovedPremisesApplicationEntity {
    val user = createdByUser ?: userEntity()
    val applicantUserDetails = cas1ApplicationUserDetailsEntity(applicantUserName)
    val caseManagerUserDetails = cas1CaseManagerUserDetailsEntity(caseManagerName)
    return approvedPremisesApplicationEntityFactory.produceAndPersist {
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
      withRiskRatings(personRisks())
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
      withData(data)
      withDocument(document)
      withSituation(SituationOption.bailSentence.toString())
      withIsInapplicable(false)
      withLicenseExpiredDate(LocalDate.parse(LICENCE_EXPIRY_DATE))
      withExpiredReason(EXPIRED_REASON)
    }
  }

  protected fun approvedPremisesAssessmentEntity(
    application: ApprovedPremisesApplicationEntity,
    assessor: UserEntity = userEntity(),
    data: String = DATA_JSON_SIMPLE,
    document: String = DOCUMENT_JSON_SIMPLE,
  ): ApprovedPremisesAssessmentEntity = approvedPremisesAssessmentEntityFactory.produceAndPersist {
    withData(data)
    withDocument(document)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withAllocatedAt(OffsetDateTime.parse(ALLOCATED_AT))
    withIsWithdrawn(false)
    withAllocatedToUser(assessor)
    withApplication(application)
    withCreatedFromAppeal(false)
    withDecision(AssessmentDecision.REJECTED)
    withReallocatedAt(null)
    withRejectionRationale("rejected as no good")
    withSubmittedAt(OffsetDateTime.parse(SUBMITTED_AT))
    withDueAt(OffsetDateTime.parse(DUE_AT))
    withAgreeWithShortNoticeReason(false)
    withAgreeWithShortNoticeReasonComments(REASON_COMMENTS)
    withReasonForLateApplication(LATE_APPLICATION_REASON)
  }

  protected fun applicationTimelineNoteEntity(application: ApprovedPremisesApplicationEntity): ApplicationTimelineNoteEntity = applicationTimelineNoteEntityFactory.produceAndPersist {
    withApplicationId(application.id)
    withBody("Some random note about this application")
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withCreatedBy(application.createdByUser)
  }

  protected fun offlineApplicationEntity(offenderDetails: OffenderDetailSummary): OfflineApplicationEntity = offlineApplicationEntityFactory.produceAndPersist {
    withService(ServiceName.approvedPremises.value)
    withCrn(offenderDetails.otherIds.crn)
    withEventNumber("1")
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
  }

  protected fun appealEntity(
    application: ApprovedPremisesApplicationEntity,
    assessment: ApprovedPremisesAssessmentEntity,
  ): AppealEntity = appealEntityFactory.produceAndPersist {
    withApplication(application)
    withAssessment(assessment)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withCreatedBy(application.createdByUser)
    withAppealDate(LocalDate.parse(APPEAL_DATE_ONLY))
    withAppealDetail("I want to appeal this decision")
    withDecision(AppealDecision.rejected)
    withDecisionDetail("rejected as no good")
  }

  protected fun placementApplicationEntity(
    application: ApprovedPremisesApplicationEntity,
    document: String = DOCUMENT_JSON_SIMPLE
  ): PlacementApplicationEntity = placementApplicationFactory.produceAndPersist {
    withApplication(application)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withSubmittedAt(OffsetDateTime.parse(SUBMITTED_AT))
    withDueAt(null)
    withData(DATA_JSON_SIMPLE)
    withDocument(document)
    withAllocatedToUser(null)
    withCreatedByUser(application.createdByUser)
    withDecision(PlacementApplicationDecision.ACCEPTED)
    withDecisionMadeAt(OffsetDateTime.parse(DECISION_MADE_AT))
    withIsWithdrawn(true)
    withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
    withReallocatedAt(null)
    withWithdrawalReason(PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST)
    withSentenceType(SENTENCE_TYPE_CUSTODIAL)
    withReleaseType(RELEASE_TYPE_CONDITIONAL)
    withRequestedDuration(REQUESTED_DURATION)
    withAuthorisedDuration(AUTHORISED_DURATION)
    withExpectedArrival(LocalDate.parse(arrivedAtDateOnly))
    withExpectedArrivalFlexible(true)
    withSituation(SituationOption.awaitingSentence.toString())
  }

  protected fun placementRequestEntity(
    assessment: ApprovedPremisesAssessmentEntity,
    application: ApprovedPremisesApplicationEntity,
    placementApplication: PlacementApplicationEntity,
    placementRequirements: PlacementRequirementsEntity? = null,
  ): PlacementRequestEntity = placementRequestFactory.produceAndPersist {
    withAssessment(assessment)
    withApplication(application)
    withPlacementApplication(placementApplication)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withDueAt(OffsetDateTime.parse(DUE_AT))
    withDuration(5)
    withExpectedArrival(LocalDate.parse(arrivedAtDateOnly))
    withIsParole(false)
    withIsWithdrawn(true)
    withWithdrawalReason(PlacementRequestWithdrawalReason.ERROR_IN_PLACEMENT_REQUEST)
    withNotes("some notes")
    withPlacementRequirements(placementRequirements ?: placementRequirementEntity(application, assessment))
  }

  protected fun placementRequirementEntity(
    application: ApprovedPremisesApplicationEntity,
    assessment: ApprovedPremisesAssessmentEntity,
    desirableCriteria: List<uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity> = listOf(characteristicEntity()),
    essentialCriteria: List<uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity> = listOf(characteristicEntity()),
    postcodeOutcode: String? = null,
  ): PlacementRequirementsEntity = placementRequirementsFactory.produceAndPersist {
    withApplication(application)
    withAssessment(assessment)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withApType(JpaApType.NORMAL)
    withDesirableCriteria(desirableCriteria)
    withEssentialCriteria(essentialCriteria)
    withPostcodeDistrict(
      postCodeDistrictFactory.produceAndPersist {
        if (postcodeOutcode != null) withOutcode(postcodeOutcode)
      },
    )
  }

  protected fun characteristicEntity(
    name: String = randomStringMultiCaseWithNumbers(10),
    propertyName: String = randomStringMultiCaseWithNumbers(6),
  ) = characteristicEntityFactory.produceAndPersist {
    withName(name)
    withServiceScope(Characteristic.ServiceScope.star.value)
    withModelScope(Characteristic.ModelScope.room.value)
    withPropertyName(propertyName)
  }

  protected fun bookingNotMadeEntity(placementRequest: PlacementRequestEntity): BookingNotMadeEntity = bookingNotMadeFactory.produceAndPersist {
    withPlacementRequest(placementRequest)
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withNotes("Some notes on booking not made")
  }

  protected fun cas1ApplicationUserDetailsEntity(
    name: String? = null,
  ): Cas1ApplicationUserDetailsEntity = cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
    if (name != null) withName(name)
    withEmailAddress("noname_applicant_user@noname.net")
  }

  protected fun cas1CaseManagerUserDetailsEntity(name: String? = null): Cas1ApplicationUserDetailsEntity = cas1ApplicationUserDetailsEntityFactory.produceAndPersist {
    if (name != null) withName(name)
    withEmailAddress("noname@noname.net")
  }
}
