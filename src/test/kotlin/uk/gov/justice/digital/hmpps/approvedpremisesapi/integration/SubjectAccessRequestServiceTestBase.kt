package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.subjectaccessrequests.SubjectAccessRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

open class SubjectAccessRequestServiceTestBase : IntegrationTestBase() {
  @Autowired
  lateinit var sarService: SubjectAccessRequestService
  lateinit var premises: ApprovedPremisesEntity
  companion object {

    const val CREATED_AT = "2021-09-18T16:00:00+00:00"
    const val SUBMITTED_AT = "2021-10-19T16:00:00+00:00"
    const val SUBMITTED_AT_NO_TZ = "2021-10-19T16:00:00"
    const val ARRIVED_AT = "2021-09-20T16:00:00+00:00"
    const val ALLOCATED_AT = "2021-09-21T16:00:00+00:00"
    const val CREATED_AT_NO_TZ = "2021-09-18T16:00:00"
    const val DUE_AT = "2021-09-22T16:00:00+00:00"
    const val DEPARTED_AT = "2021-09-23T16:00:00+00:00"
    const val NEW_DEPARTED_AT = "2021-09-24T16:00:00+00:00"
    const val CANCELLATION_DATE = "2021-09-25T16:00:00+00:00"
    const val RESPONSE_RECEIVED_AT = "2021-10-23"
    const val APPEAL_DATE_ONLY = "2021-10-24"
    const val DECISION_MADE_AT = "2021-10-25T16:01:00+00:00"
    const val DECISION_MADE_AT_NO_TZ = "2021-10-25T16:01:00"
    const val DATA_JSON_SIMPLE = """{ "key": "value" }"""
    const val DOCUMENT_JSON_SIMPLE = """{ "key2": "value2" }"""
    const val EVENT_NUMBER = "1"
    const val OFFENCE_ID = "BEING_BAD"
    const val CONVICTION_ID = 2L
    const val RELEASE_TYPE_CONDITIONAL = "CONDITIONAL"
    const val WITHDRAWAL_REASON_NOT_WITHDRAWN = "NOT WITHDRAWN"
    const val OTHER_WITHDRAWAL_REASON_NOT_APPLICABLE = "NOT APPLICABLE"
    const val SENTENCE_TYPE_CUSTODIAL = "CUSTODIAL"
    const val NAME = "Jeffity Jeff"

    val START_DATE: LocalDateTime = LocalDateTime.of(2018, 9, 30, 0, 0, 0)
    val END_DATE: LocalDateTime = LocalDateTime.of(2024, 9, 30, 0, 0, 0)
    var arrivedAtDateOnly = ARRIVED_AT.substring(0..9)
    var submittedAtDateOnly = SUBMITTED_AT.substring(0..9)
    var departedAtDateOnly = DEPARTED_AT.substring(0..9)
    var previousDepartureDateOnly = DEPARTED_AT.substring(0..9)
    var newDepartureDateOnly = NEW_DEPARTED_AT.substring(0..9)
    var cancellationDateOnly = CANCELLATION_DATE.substring(0..9)
    var arrivedAtTime = ARRIVED_AT.substring(11..18)
    var departedAtTime = DEPARTED_AT.substring(11..18)
  }

  protected fun OffsetDateTime.toStandardisedFormat(): String = this.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

  protected fun cancellationJson(cancellation: CancellationEntity): String =
    """
      {   
          "crn": "${cancellation.booking.crn}",
          "noms_number": "${cancellation.booking.nomsNumber}",
          "notes": "${cancellation.notes}",
          "cancellation_date": "$cancellationDateOnly",
          "cancellation_reason": "${cancellation.reason.name}",
          "other_reason": "${cancellation.otherReason}",
          "created_at": "$CREATED_AT"
      }
    """.trimIndent()

  protected fun bookingExtensionJson(bookingExtension: ExtensionEntity): String =
    """
      {
        "application_id": "${bookingExtension.booking.application?.id}",
        "offline_application_id": ${bookingExtension.booking.offlineApplication?.let { "\"${bookingExtension.booking.offlineApplication!!.id}\"" }},
        "crn": "${bookingExtension.booking.crn}",
        "noms_number": "${bookingExtension.booking.nomsNumber}",
        "previous_departure_date": "$previousDepartureDateOnly",
        "new_departure_date": "$newDepartureDateOnly",
        "notes": "${bookingExtension.notes}",
        "created_at": "$CREATED_AT"
      }
    """.trimIndent()

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
         "approved_premises_application_id": ${if (booking.application != null) "\"${booking.application!!.id}\"" else null},
         "offline_application_id": ${if (booking.offlineApplication != null) "\"${booking.offlineApplication!!.id}\"" else null},
         "premises_name": "${booking.premises.name}",
         "person_name": ${if (booking.application != null) "\"${booking.application!!.name}\"" else "\"${booking.offlineApplication!!.name}\""},
         "delius_event_number": "${booking.deliusEventNumber}",
         "placement_request_id":  "${booking.placementRequest!!.id}",
         "created_by_user_id":  "${booking.createdBy!!.id}",
         "departure_reason_id": ${if (booking.departureReason != null) "\"${booking.departureReason!!.id}\"" else null},
         "departure_notes": ${if (booking.departureNotes != null) "\"${booking.departureNotes}\"" else null},
         "departure_move_on_category_id": ${if (booking.departureMoveOnCategory != null) "\"${booking.departureMoveOnCategory!!.id}\"" else null},
         "cancellation_reason_notes": ${if (booking.cancellationReasonNotes != null) "\"${booking.cancellationReasonNotes}\"" else null},
         "cancellation_reason_id": ${if (booking.cancellationReason != null) "\"${booking.cancellationReason!!.id}\"" else null},
         "cancellation_occurred_at": ${if (booking.cancellationOccurredAt != null) "\"${booking.cancellationOccurredAt}\"" else null},
         "cancellation_recorded_at": "$CANCELLATION_DATE",
         "characteristics_property_names": "${booking.criteria?.let{ it.map { criteria -> criteria.propertyName}.sortedBy{ propertyName -> propertyName }.joinToString(",")}}",
         "migrated_management_info_from":  ${if (booking.migratedManagementInfoFrom != null) "\"${booking.migratedManagementInfoFrom}\"" else null},
         "version": ${booking.version}
      }
    """.trimIndent()

  protected fun bookingsJson(booking: BookingEntity): String =
    """
      {
         "crn": "${booking.crn}",
         "noms_number": "${booking.nomsNumber}",
         "arrival_date": "${booking.arrivalDate}",
         "departure_date": "${booking.departureDate}",
         "original_arrival_date": "${booking.originalArrivalDate}",
         "original_departure_date": "${booking.originalDepartureDate}",
         "created_at": "$CREATED_AT",
         "status": "${booking.status}",
         "premises_name": "${booking.premises.name}",
         "adhoc": ${booking.adhoc},
         "key_worker_staff_code": "${booking.keyWorkerStaffCode}",
         "service": "${booking.service}",
         "application_id": "${booking.application?.id}",
         "offline_application_id": ${if (booking.offlineApplication != null) "\"${booking.offlineApplication!!.id}\"" else "null"},
         "version": ${booking.version}
      }
    """.trimIndent()

  protected fun risksJson(): String =
    """
      {
          "roshRisks" : {
            "status" : "NotFound",
            "value" : null
          },
          "mappa" : {
            "status" : "NotFound",
            "value" : null
          },
          "tier" : {
            "status" : "Retrieved",
            "value" : {
              "level" : "M1",
              "lastUpdated" : [ 2023, 6, 26 ]
            }
          },
          "flags" : {
            "status" : "NotFound",
            "value" : null
          }
      }
    """.trimIndent()

  protected fun domainEventJson(domainEvent: DomainEventEntity, user: UserEntity?): String =
    """ 
      {
        "id": "${domainEvent.id}",
        "application_id": "${domainEvent.applicationId}",
        "crn": "${domainEvent.crn}",
        "type": "${domainEvent.type}",
        "occurred_at": "$ALLOCATED_AT",
        "created_at": "$CREATED_AT",
        "data": ${domainEvent.data},
        "booking_id": null,
        "service": "${domainEvent.service}",
        "assessment_id": "${domainEvent.assessmentId}",
        "triggered_by_user": ${user?.let {"\"${it.name}\""} ?: "null"},
        "noms_number": "${domainEvent.nomsNumber}",
        "trigger_source": null
      }
    """.trimIndent()

  protected fun domainEventsMetadataJson(domainEvent: DomainEventEntity): String =
    """
      {
        "crn": "${domainEvent.crn}",
        "noms_number": "${domainEvent.nomsNumber}",
        "created_at": "$CREATED_AT",
        "domain_event_id": "${domainEvent.id}",
        "name": "${MetaDataName.CAS1_REQUESTED_AP_TYPE}",
        "value": "${ApprovedPremisesType.NORMAL}"
      }
    """.trimIndent()

  protected fun cancellationEntity(booking: BookingEntity): CancellationEntity =
    cancellationEntityFactory.produceAndPersist {
      withReason(
        cancellationReasonEntityFactory.produceAndPersist {
          withName("some reason")
          withServiceScope("approved-premises")
          withIsActive(true)
        },
      )
      withBooking(booking)
      withNotes("some notes")
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withDate(LocalDate.parse(cancellationDateOnly))
      withOtherReason("some other reason")
    }

  protected fun bookingExtensionEntity(booking: BookingEntity): ExtensionEntity {
    return extensionEntityFactory.produceAndPersist {
      withBooking(booking)
      withNotes("some notes")
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withPreviousDepartureDate(LocalDate.parse(previousDepartureDateOnly))
      withNewDepartureDate(LocalDate.parse(newDepartureDateOnly))
    }
  }

  @SuppressWarnings("LongParameterList")
  protected fun spaceBookingEntity(
    offenderDetails: OffenderDetailSummary,
    application: ApprovedPremisesApplicationEntity? = null,
    nonArrivalReason: NonArrivalReasonEntity? = null,
    departureReason: DepartureReasonEntity? = null,
    moveOnCategory: MoveOnCategoryEntity? = null,
    cancellationReason: CancellationReasonEntity? = null,
    offlineApplication: OfflineApplicationEntity? = null,
  ): Cas1SpaceBookingEntity {
    val (user, _) = givenAUser()
    val (placementRequest) = givenAPlacementRequest(
      placementRequestAllocatedTo = user,
      assessmentAllocatedTo = user,
      createdByUser = user,
    )
    val bed = bedEntity()
    val spaceBooking =
      cas1SpaceBookingEntityFactory.produceAndPersist {
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
            characteristicEntityFactory.produceAndPersist(),
          ),
        )
        withOfflineApplication(offlineApplication)
      }
    return spaceBooking
  }

  protected fun bookingEntity(
    offenderDetails: OffenderDetailSummary,
    application: ApplicationEntity,
    offlineApplication: OfflineApplicationEntity? = null,
    serviceName: ServiceName = ServiceName.approvedPremises,
  ): BookingEntity {
    val bed = bedEntity()

    val booking = bookingEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withNomsNumber(offenderDetails.otherIds.nomsNumber)
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withAdhoc(true)
      withOfflineApplication(offlineApplication)
      withDepartureDate(LocalDate.parse(departedAtDateOnly))
      withApplication(application)
      withArrivalDate(LocalDate.parse(arrivedAtDateOnly))
      withOriginalArrivalDate(LocalDate.parse(arrivedAtDateOnly))
      withOriginalDepartureDate(LocalDate.parse(departedAtDateOnly))
      withPremises(bed.room.premises)
      withStaffKeyWorkerCode("KEYWORKERSTAFFCODE")
      withStatus(BookingStatus.arrived)
      withBed(bed)
      withServiceName(serviceName)
    }
    return booking
  }

  protected fun bedEntity(premisesEntity: ApprovedPremisesEntity? = null) = bedEntityFactory.produceAndPersist {
    withName("a bed ${randomStringMultiCaseWithNumbers(5)}")
    withCode("a code ${randomStringMultiCaseWithNumbers(5)}")
    premises = approvedPremisesEntityFactory.produceAndPersist {
      withName("a premises ${randomStringMultiCaseWithNumbers(5)}")
      withApCode("AP Code ${randomStringMultiCaseWithNumbers(5)}")
      withLocalAuthorityArea(
        localAuthorityEntityFactory.produceAndPersist {
          withName("An LAA ${randomStringMultiCaseWithNumbers(5)}")
          withIdentifier("LAA ID ${randomStringMultiCaseWithNumbers(5)}")
        },
      )
      withProbationRegion(
        probationRegionEntity(),
      )
    }
    withRoom(
      roomEntityFactory.produceAndPersist {
        withCode("room code ${randomStringMultiCaseWithNumbers(5)}")
        withName("room name ${randomStringMultiCaseWithNumbers(5)}")
        withPremises(premises)
      },
    )
  }

  protected fun probationRegionEntity() = probationRegionEntityFactory.produceAndPersist {
    withName("Probation Region ${randomStringMultiCaseWithNumbers(5)}")
    withApArea(givenAnApArea(name = "Probation Area ${randomStringMultiCaseWithNumbers(5)}"))
  }

  protected fun userEntity(): UserEntity =
    userEntityFactory.produceAndPersist {
      withProbationRegion(givenAProbationRegion())
    }

  protected fun personRisks() =
    PersonRisksFactory()
      .withTier(
        RiskWithStatus(
          status = RiskStatus.Retrieved,
          RiskTier(
            level = "M1",
            lastUpdated = LocalDate.parse("2023-06-26"),
          ),
        ),
      ).withRoshRisks(
        RiskWithStatus(status = RiskStatus.NotFound),
      ).withMappa(
        RiskWithStatus(status = RiskStatus.NotFound),
      ).withFlags(
        RiskWithStatus(status = RiskStatus.NotFound),
      ).produce()

  protected fun approvedPremisesAssessmentClarificationNoteEntity(assessment: ApprovedPremisesAssessmentEntity): AssessmentClarificationNoteEntity =
    assessmentClarificationNoteEntityFactory.produceAndPersist {
      withAssessment(assessment)
      withCreatedBy(assessment.allocatedToUser!!)
      withQuery("some query")
      withResponse("a useful response")
      withResponseReceivedOn(LocalDate.parse(RESPONSE_RECEIVED_AT))
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    }

  protected fun domainEventEntity(
    offender: OffenderDetailSummary,
    applicationId: UUID,
    assessmentId: UUID,
    userId: UUID?,
    serviceName: ServiceName = ServiceName.approvedPremises,
  ): DomainEventEntity {
    val domainEvent = domainEventFactory.produceAndPersist {
      withId(UUID.randomUUID())
      withService(serviceName)
      withCrn(offender.otherIds.crn)
      withNomsNumber(offender.otherIds.nomsNumber)
      withApplicationId(applicationId)
      withAssessmentId(assessmentId)
      withType(DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED)
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withOccurredAt(OffsetDateTime.parse(ALLOCATED_AT))
      withData("{ }")
      withTriggeredByUserId(userId)
      withMetadata(
        mapOf(
          MetaDataName.CAS1_REQUESTED_AP_TYPE to ApprovedPremisesType.NORMAL.toString(),
        ),
      )
    }
    return domainEvent
  }
}
