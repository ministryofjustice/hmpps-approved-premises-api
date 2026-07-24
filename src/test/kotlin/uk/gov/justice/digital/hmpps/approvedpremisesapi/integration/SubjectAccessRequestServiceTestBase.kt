package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenCas3PremisesAndBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ReleaseType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.subjectaccessrequests.SubjectAccessRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

open class SubjectAccessRequestServiceTestBase : IntegrationTestBase() {

  @Autowired
  lateinit var sarService: SubjectAccessRequestService

  @Autowired
  lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

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
    val RELEASE_TYPE_CONDITIONAL = Cas1ReleaseType.reReleasedPostRecall
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

    @JvmStatic
    protected fun readResource(path: String) = SubjectAccessRequestServiceTestBase::class.java.classLoader.getResource(path)!!.readText()
  }

  protected fun OffsetDateTime.toStandardisedFormat(): String = this.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

  protected fun cancellationJson(cancellation: Cas3CancellationEntity): String =
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

  protected fun bookingExtensionJson(bookingExtension: Cas3ExtensionEntity): String =
    """
      {
        "crn": "${bookingExtension.booking.crn}",
        "noms_number": "${bookingExtension.booking.nomsNumber}",
        "previous_departure_date": "$previousDepartureDateOnly",
        "new_departure_date": "$newDepartureDateOnly",
        "notes": "${bookingExtension.notes}",
        "created_at": "$CREATED_AT"
      }
    """.trimIndent()

  protected fun bookingsJson(booking: Cas3BookingEntity): String =
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
         "offender_name": ${booking.offenderName?.let { "\"${it}\"" }},
      }
    """.trimIndent()

  protected fun cancellationEntity(booking: Cas3BookingEntity): Cas3CancellationEntity = cas3CancellationEntityFactory.produceAndPersist {
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

  protected fun bookingExtensionEntity(booking: Cas3BookingEntity): Cas3ExtensionEntity = cas3ExtensionEntityFactory.produceAndPersist {
    withBooking(booking)
    withNotes("some notes")
    withCreatedAt(OffsetDateTime.parse(CREATED_AT))
    withPreviousDepartureDate(LocalDate.parse(previousDepartureDateOnly))
    withNewDepartureDate(LocalDate.parse(newDepartureDateOnly))
  }

  protected fun bookingEntity(
    offenderDetails: OffenderDetailSummary,
    application: ApplicationEntity,
  ): Cas3BookingEntity {
    val user = userEntity()
    val (cas3Premises, cas3Bedspace) = givenCas3PremisesAndBedspace(
      user = user,
      premises = givenACas3Premises(
        name = "SAR-TEST-PREMISES",
        probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        },
        status = Cas3PremisesStatus.online,
      ),

    )

    return cas3BookingEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withNomsNumber(offenderDetails.otherIds.nomsNumber)
      withCreatedAt(OffsetDateTime.parse(CREATED_AT))
      withDepartureDate(LocalDate.parse(departedAtDateOnly))
      withApplication(application as? TemporaryAccommodationApplicationEntity)
      withArrivalDate(LocalDate.parse(arrivedAtDateOnly))
      withOriginalArrivalDate(LocalDate.parse(arrivedAtDateOnly))
      withOriginalDepartureDate(LocalDate.parse(departedAtDateOnly))
      withPremises(cas3Premises)
      withBedspace(cas3Bedspace)
      withStatus(Cas3BookingStatus.arrived)
      withOffenderName("${offenderDetails.firstName} ${offenderDetails.surname}")
    }
  }

  fun risksJson(): String =
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
              "lastUpdated" : [ 2023, 6, 26 ],
              "version" : "V2"
            }
          },
          "flags" : {
            "status" : "NotFound",
            "value" : null
          }
      }
    """.trimIndent()

  fun personRisks() = PersonRisksFactory()
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

  fun domainEventJson(domainEvent: DomainEventEntity, user: UserEntity?): String =
    """
      {
        "crn": "${domainEvent.crn}",
        "type": "${domainEvent.type}",
        "occurred_at": "$ALLOCATED_AT",
        "created_at": "$CREATED_AT",
        "data": ${domainEvent.data},
        "triggered_by_user": ${user?.let {"\"${it.name}\""} ?: "null"},
        "noms_number": "${domainEvent.nomsNumber}",
      }
    """.trimIndent()

  fun domainEventsMetadataJson(domainEvent: DomainEventEntity): String =
    """
      {
        "crn": "${domainEvent.crn}",
        "noms_number": "${domainEvent.nomsNumber}",
        "created_at": "$CREATED_AT",
        "name": "${MetaDataName.CAS1_REQUESTED_AP_TYPE}",
        "value": "${ApprovedPremisesType.NORMAL}"
      }
    """.trimIndent()

  fun domainEventEntity(
    offender: OffenderDetailSummary,
    applicationId: UUID,
    assessmentId: UUID,
    userId: UUID?,
    type: DomainEventType,
    serviceName: ServiceName = ServiceName.approvedPremises,
  ): DomainEventEntity = domainEventFactory.produceAndPersist {
    withId(UUID.randomUUID())
    withService(serviceName)
    withCrn(offender.otherIds.crn)
    withNomsNumber(offender.otherIds.nomsNumber)
    withApplicationId(applicationId)
    withAssessmentId(assessmentId)
    withType(type)
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

  fun probationRegionEntity(
    name: String = "Probation Region ${randomStringMultiCaseWithNumbers(5)}",
  ) = probationRegionEntityFactory.produceAndPersist {
    withName(name)
    withApArea(givenAnApArea(name = "Probation Area ${randomStringMultiCaseWithNumbers(5)}"))
  }

  fun userEntity(): UserEntity = userEntityFactory.produceAndPersist {
    withProbationRegion(givenAProbationRegion())
  }

  fun probationDeliveryUnitEntity(
    user: UserEntity,
    name: String = randomStringMultiCaseWithNumbers(8),
  ): ProbationDeliveryUnitEntity = probationDeliveryUnitFactory.produceAndPersist {
    withProbationRegion(user.probationRegion)
    withName(name)
  }
}
