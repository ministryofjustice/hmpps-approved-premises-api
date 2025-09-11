package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CancellationReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummaryDiscriminator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApprovedPremisesUserFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingAtPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.ActionsResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingActionsService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SpaceBookingAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ChangeRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asOffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1SpaceBookingTransformerTest {
  @MockK
  private lateinit var personTransformer: PersonTransformer

  @MockK
  private lateinit var cancellationReasonTransformer: CancellationReasonTransformer

  @MockK
  private lateinit var userTransformer: UserTransformer

  @MockK
  private lateinit var cas1ChangeRequestRepository: Cas1ChangeRequestRepository

  @MockK
  private lateinit var cas1SpaceBookingActionsService: Cas1SpaceBookingActionsService

  @MockK
  private lateinit var cas1ChangeRequestTransformer: Cas1ChangeRequestTransformer

  @InjectMockKs
  private lateinit var transformer: Cas1SpaceBookingTransformer

  @Nested
  inner class TransformJpaToApi {

    @Test
    fun `Space booking is transformed correctly`() {
      val personInfo = PersonInfoResult.Success.Full(
        "SOMECRN",
        CaseSummaryFactory().produce().asOffenderDetailSummary(),
        null,
      )

      val expectedPerson = RestrictedPerson(
        "SOMECRN",
        PersonType.restrictedPerson,
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withRiskRatings(
          PersonRisksFactory()
            .withTier(RiskWithStatus(RiskStatus.Retrieved, RiskTier("A", LocalDate.now())))
            .produce(),
        )
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .produce()

      val cancellationReason = CancellationReasonEntityFactory().produce()

      val nonArrivalReason = NonArrivalReasonEntityFactory().produce()

      val criteria = listOf(
        CharacteristicEntityFactory().withPropertyName("hasEnSuite").produce(),
        CharacteristicEntityFactory().withPropertyName("isCatered").produce(),
      )

      val departureReason = DepartureReasonEntity(
        id = UUID.randomUUID(),
        name = "departure reason",
        isActive = true,
        serviceScope = "approved-premises",
        legacyDeliusReasonCode = null,
        parentReasonId = null,
      )

      val departureNotes = "These are departure notes"

      val moveOnCategory = MoveOnCategoryEntity(
        id = UUID.randomUUID(),
        name = "moveon category",
        isActive = true,
        serviceScope = "approved-premises",
        legacyDeliusCategoryCode = "delius",
      )

      val keyWorkerUser = UserEntityFactory()
        .withName("Mr Key Worker User")
        .withEmail("keyworker@test.com")
        .withDefaults()
        .produce()

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .withPlacementRequest(placementRequest)
        .withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
        .withKeyworkerName("Mr Key Worker")
        .withKeyworkerStaffCode("K123")
        .withKeyworkerAssignedAt(LocalDateTime.parse("2007-12-03T10:15:30").toInstant(ZoneOffset.UTC))
        .withKeyWorkerUser(keyWorkerUser)
        .withActualArrivalDate(LocalDate.of(2009, 2, 5))
        .withActualArrivalTime(LocalTime.of(11, 25, 10, 0))
        .withActualDepartureDate(LocalDate.of(2012, 12, 25))
        .withActualDepartureTime(LocalTime.of(23, 0, 0, 0))
        .withCancellationOccurredAt(LocalDate.parse("2039-12-28"))
        .withCancellationRecordedAt(Instant.parse("2023-12-29T11:25:10.00Z"))
        .withCancellationReason(cancellationReason)
        .withCancellationReasonNotes("some extra info on cancellation")
        .withCriteria(criteria.toMutableList())
        .withNonArrivalConfirmedAt(Instant.parse("2024-01-20T10:10:10.00Z"))
        .withNonArrivalReason(nonArrivalReason)
        .withNonArrivalNotes("some information on the non arrival")
        .withDepartureReason(departureReason)
        .withDepartureNotes(departureNotes)
        .withMoveOnCategory(moveOnCategory)
        .withDeliusEventNumber("97")
        .produce()

      val expectedCancellationReason = CancellationReason(
        id = UUID.randomUUID(),
        name = "some reason",
        isActive = true,
        serviceScope = "the service",
      )

      val expectedUser = ApprovedPremisesUserFactory().produce()

      val otherBookings = listOf(
        Cas1SpaceBookingAtPremisesImpl(
          id = UUID.randomUUID(),
          canonicalArrivalDate = LocalDate.parse("2025-04-06"),
          canonicalDepartureDate = LocalDate.parse("2025-05-07"),
        ),
      )

      val changeRequests = listOf(
        Cas1ChangeRequestSummary(
          id = UUID.randomUUID(),
          person = FullPersonSummary(
            crn = "CRN1",
            name = "NAME",
            isRestricted = false,
            personType = PersonSummaryDiscriminator.fullPersonSummary,
          ),
          type = Cas1ChangeRequestType.PLACEMENT_APPEAL,
          createdAt = Instant.now(),
          tier = "TierA",
          expectedArrivalDate = LocalDate.parse("2023-01-01"),
          actualArrivalDate = LocalDate.parse("2023-01-01"),
          placementRequestId = UUID.randomUUID(),
        ),
      )

      every { personTransformer.transformModelToPersonApi(personInfo) } returns expectedPerson
      every {
        userTransformer.transformJpaToApi(
          spaceBooking.createdBy!!,
          ServiceName.approvedPremises,
        )
      } returns expectedUser
      every { cancellationReasonTransformer.transformJpaToApi(cancellationReason) } returns expectedCancellationReason
      every { cas1SpaceBookingActionsService.determineActions(spaceBooking) } returns ActionsResult.forAllowedAction(
        SpaceBookingAction.APPEAL_CREATE,
      )

      every { cas1ChangeRequestTransformer.transformToChangeRequestSummaries(any(), any()) } returns changeRequests

      val result = transformer.transformJpaToApi(personInfo, spaceBooking, otherBookings, emptyList())

      assertThat(result.id).isEqualTo(spaceBooking.id)
      assertThat(result.applicationId).isEqualTo(application.id)
      assertThat(result.assessmentId).isEqualTo(placementRequest.assessment.id)
      assertThat(result.person).isEqualTo(expectedPerson)
      assertThat(result.premises.id).isEqualTo(spaceBooking.premises.id)
      assertThat(result.premises.name).isEqualTo(spaceBooking.premises.name)
      assertThat(result.apArea.id).isEqualTo(spaceBooking.premises.probationRegion.apArea!!.id)
      assertThat(result.apArea.name).isEqualTo(spaceBooking.premises.probationRegion.apArea!!.name)
      assertThat(result.bookedBy).isEqualTo(expectedUser)
      assertThat(result.requestForPlacementId).isEqualTo(placementRequest.id)
      assertThat(result.placementRequestId).isEqualTo(placementRequest.id)
      assertThat(result.expectedArrivalDate).isEqualTo(spaceBooking.expectedArrivalDate)
      assertThat(result.expectedDepartureDate).isEqualTo(spaceBooking.expectedDepartureDate)
      assertThat(result.actualArrivalDate).isEqualTo(LocalDate.parse("2009-02-05"))
      assertThat(result.actualArrivalDateOnly).isEqualTo(LocalDate.parse("2009-02-05"))
      assertThat(result.actualArrivalTime).isEqualTo("11:25")
      assertThat(result.actualDepartureDate).isEqualTo(LocalDate.parse("2012-12-25"))
      assertThat(result.actualDepartureDateOnly).isEqualTo(LocalDate.parse("2012-12-25"))
      assertThat(result.actualDepartureTime).isEqualTo("23:00")
      assertThat(result.canonicalArrivalDate).isEqualTo(spaceBooking.expectedArrivalDate)
      assertThat(result.canonicalDepartureDate).isEqualTo(spaceBooking.expectedDepartureDate)
      assertThat(result.createdAt).isEqualTo(spaceBooking.createdAt.toInstant())
      assertThat(result.tier).isEqualTo("A")

      val keyWorkerAllocation = result.keyWorkerAllocation!!
      assertThat(keyWorkerAllocation.allocatedAt).isEqualTo(LocalDate.parse("2007-12-03"))
      assertThat(keyWorkerAllocation.userId).isEqualTo(keyWorkerUser.id)
      assertThat(keyWorkerAllocation.name).isEqualTo("Mr Key Worker")
      assertThat(keyWorkerAllocation.emailAddress).isEqualTo("keyworker@test.com")

      assertThat(result.cancellation!!.occurredAt).isEqualTo(LocalDate.parse("2039-12-28"))
      assertThat(result.cancellation!!.recordedAt).isEqualTo(Instant.parse("2023-12-29T11:25:10.00Z"))
      assertThat(result.cancellation!!.reason).isEqualTo(expectedCancellationReason)
      assertThat(result.cancellation!!.reasonNotes).isEqualTo("some extra info on cancellation")

      assertThat(result.nonArrival!!.confirmedAt).isEqualTo(Instant.parse("2024-01-20T10:10:10Z"))
      assertThat(result.nonArrival!!.reason!!.id).isEqualTo(spaceBooking.nonArrivalReason!!.id)
      assertThat(result.nonArrival!!.notes).isEqualTo("some information on the non arrival")

      assertThat(result.departure!!.reason.id).isEqualTo(departureReason.id)
      assertThat(result.departure!!.reason.name).isEqualTo(departureReason.name)
      assertThat(result.departure!!.moveOnCategory!!.id).isEqualTo(moveOnCategory.id)
      assertThat(result.departure!!.moveOnCategory!!.name).isEqualTo(moveOnCategory.name)
      assertThat(result.departure!!.notes).isEqualTo(departureNotes)
      assertThat(result.departure!!.parentReason).isNull()

      assertThat(result.deliusEventNumber).isEqualTo("97")

      assertThat(result.otherBookingsInPremisesForCrn).hasSize(1)
      assertThat(result.otherBookingsInPremisesForCrn[0].canonicalArrivalDate).isEqualTo(LocalDate.parse("2025-04-06"))
      assertThat(result.otherBookingsInPremisesForCrn[0].canonicalDepartureDate).isEqualTo(LocalDate.parse("2025-05-07"))
      assertThat(result.requestForPlacementId).isEqualTo(placementRequest.id)

      assertThat(result.characteristics).containsExactlyInAnyOrder(
        Cas1SpaceCharacteristic.isCatered,
        Cas1SpaceCharacteristic.hasEnSuite,
      )

      assertThat(result.allowedActions).containsExactly(Cas1SpaceBookingAction.APPEAL_CREATE)
      assertThat(result.openChangeRequests.size).isEqualTo(1)
      assertThat(result.openChangeRequests[0]).isEqualTo(changeRequests[0])
    }

    @Test
    fun `Space booking is transformed correctly with departure reason when parent reason id is valid`() {
      val personInfo = PersonInfoResult.Success.Full(
        "SOMECRN",
        CaseSummaryFactory().produce().asOffenderDetailSummary(),
        null,
      )

      val expectedPerson = RestrictedPerson(
        "SOMECRN",
        PersonType.restrictedPerson,
      )

      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .produce()

      val parentDepartureReason = DepartureReasonEntity(
        id = UUID.randomUUID(),
        name = "Parent Departure Reason",
        isActive = true,
        serviceScope = "approved-premises",
        legacyDeliusReasonCode = null,
        parentReasonId = null,
      )

      val departureReason = DepartureReasonEntity(
        id = UUID.randomUUID(),
        name = "Child Departure Reason",
        isActive = true,
        serviceScope = "approved-premises",
        legacyDeliusReasonCode = null,
        parentReasonId = parentDepartureReason,
      )

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .withPlacementRequest(placementRequest)
        .withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
        .withKeyworkerName("Mr Key Worker")
        .withKeyworkerStaffCode("K123")
        .withKeyworkerAssignedAt(LocalDateTime.parse("2007-12-03T10:15:30").toInstant(ZoneOffset.UTC))
        .withActualArrivalDate(LocalDate.parse("2009-02-05"))
        .withActualArrivalTime(LocalTime.parse("11:25:10"))
        .withActualDepartureDate(LocalDate.parse("2012-12-25"))
        .withActualDepartureTime(LocalTime.parse("00:00:00"))
        .withDeliusEventNumber("97")
        .withDepartureReason(departureReason)
        .produce()

      val expectedUser = ApprovedPremisesUserFactory().produce()

      every { personTransformer.transformModelToPersonApi(personInfo) } returns expectedPerson
      every {
        userTransformer.transformJpaToApi(
          spaceBooking.createdBy!!,
          ServiceName.approvedPremises,
        )
      } returns expectedUser
      every { cas1SpaceBookingActionsService.determineActions(spaceBooking) } returns ActionsResult.forAllowedAction(
        SpaceBookingAction.APPEAL_CREATE,
      )

      every { cas1ChangeRequestTransformer.transformToChangeRequestSummaries(any(), any()) } returns emptyList()

      val result = transformer.transformJpaToApi(personInfo, spaceBooking, emptyList(), emptyList())

      assertThat(result.id).isEqualTo(spaceBooking.id)
      val departure = result.departure!!
      assertThat(departure.reason.id).isEqualTo(departureReason.id)
      assertThat(departure.reason.name).isEqualTo(departureReason.name)
      assertThat(departure.parentReason!!.id).isEqualTo(parentDepartureReason.id)
      assertThat(departure.parentReason!!.name).isEqualTo(parentDepartureReason.name)
      assertThat(departure.notes).isNull()
      assertThat(departure.moveOnCategory).isNull()
    }
  }

  @Nested
  inner class TransformJpaToSummary {

    @ParameterizedTest
    @CsvSource("true,false")
    fun success(cancelled: Boolean) {
      val id = UUID.randomUUID()

      val personSummaryInfo = PersonSummaryInfoResult.Success.Restricted("the crn", "the noms")

      val expectedPersonSummary = RestrictedPersonSummary(
        "the crn",
        PersonSummaryDiscriminator.restrictedPersonSummary,
      )

      val cas1ChangeRequests = listOf(Cas1ChangeRequestEntityFactory().withType(ChangeRequestType.PLACEMENT_APPEAL).produce())
      every { personTransformer.personSummaryInfoToPersonSummary(personSummaryInfo) } returns expectedPersonSummary
      every { cas1ChangeRequestRepository.findAllBySpaceBookingAndResolvedIsFalse(any()) } returns cas1ChangeRequests

      val premises = ApprovedPremisesEntityFactory().withDefaults().withName("The booking's premise").produce()

      val result = transformer.transformToSummary(
        Cas1SpaceBookingEntityFactory()
          .withId(id)
          .withCrn("the crn")
          .withPremises(premises)
          .withCanonicalArrivalDate(LocalDate.parse("2023-12-13"))
          .withCanonicalDepartureDate(LocalDate.parse("2023-01-02"))
          .withExpectedArrivalDate(LocalDate.parse("2022-12-13"))
          .withExpectedDepartureDate(LocalDate.parse("2022-01-02"))
          .withActualArrivalDate(LocalDate.parse("2024-12-13"))
          .withActualDepartureDate(LocalDate.parse("2024-01-02"))
          .withCriteria(
            CharacteristicEntityFactory().withPropertyName("hasTurningSpace").produce(),
            CharacteristicEntityFactory().withPropertyName("isCatered").produce(),
          )
          .withNonArrivalConfirmedAt(Instant.now())
          .withApplication(
            ApprovedPremisesApplicationEntityFactory()
              .withDefaults()
              .withRiskRatings(
                PersonRisksFactory().withTier(
                  RiskWithStatus(
                    RiskTier(
                      level = "M1",
                      lastUpdated = LocalDate.parse("2023-06-26"),
                    ),
                  ),
                ).produce(),
              )
              .produce(),
          )
          .withKeyworkerStaffCode("the staff code")
          .withKeyworkerAssignedAt(LocalDateTime.of(2023, 12, 12, 0, 0, 0).toInstant(ZoneOffset.UTC))
          .withKeyworkerName("the keyworker name")
          .withDeliusEventNumber("event8")
          .withCancellationOccurredAt(
            if (cancelled) {
              LocalDate.now()
            } else {
              null
            },
          )
          .produce(),
        personSummaryInfo,
      )

      assertThat(result.id).isEqualTo(id)
      assertThat(result.person).isEqualTo(expectedPersonSummary)
      assertThat(result.premises.id).isEqualTo(premises.id)
      assertThat(result.premises.name).isEqualTo(premises.name)
      assertThat(result.canonicalArrivalDate).isEqualTo(LocalDate.parse("2023-12-13"))
      assertThat(result.canonicalDepartureDate).isEqualTo(LocalDate.parse("2023-01-02"))
      assertThat(result.expectedArrivalDate).isEqualTo(LocalDate.parse("2022-12-13"))
      assertThat(result.expectedDepartureDate).isEqualTo(LocalDate.parse("2022-01-02"))
      assertThat(result.characteristics).containsExactlyInAnyOrder(
        Cas1SpaceCharacteristic.hasTurningSpace,
        Cas1SpaceCharacteristic.isCatered,
      )
      assertThat(result.actualArrivalDate).isEqualTo(LocalDate.parse("2024-12-13"))
      assertThat(result.actualDepartureDate).isEqualTo(LocalDate.parse("2024-01-02"))
      assertThat(result.isNonArrival).isEqualTo(true)
      assertThat(result.tier).isEqualTo("M1")
      assertThat(result.keyWorkerAllocation!!.allocatedAt).isEqualTo(LocalDate.parse("2023-12-12"))
      assertThat(result.deliusEventNumber).isEqualTo("event8")
      assertThat(result.isCancelled).isEqualTo(cancelled)
      assertThat(result.plannedTransferRequested).isFalse()
      assertThat(result.appealRequested).isFalse()
      assertThat(result.openChangeRequestTypes).containsExactly(Cas1ChangeRequestType.PLACEMENT_APPEAL)
    }
  }

  @Nested
  inner class TransformSearchResultToSummary {

    @Test
    fun `Space booking is transformed correctly with case worker`() {
      val id = UUID.randomUUID()

      val personSummaryInfo = PersonSummaryInfoResult.Success.Restricted("the crn", "the noms")
      val expectedPersonSummary = RestrictedPersonSummary(
        "the crn",
        PersonSummaryDiscriminator.restrictedPersonSummary,
      )

      val premises = ApprovedPremisesEntityFactory().withDefaults().withName("the premise").produce()

      every { personTransformer.personSummaryInfoToPersonSummary(personSummaryInfo) } returns expectedPersonSummary

      val keyWorkerId = UUID.randomUUID()
      val result = transformer.transformSearchResultToSummary(
        Cas1SpaceBookingSearchResultImpl(
          id = id,
          crn = "the crn",
          canonicalArrivalDate = LocalDate.parse("2023-12-13"),
          canonicalDepartureDate = LocalDate.parse("2023-01-02"),
          expectedArrivalDate = LocalDate.parse("2022-12-13"),
          expectedDepartureDate = LocalDate.parse("2022-01-02"),
          actualArrivalDate = LocalDate.parse("2024-12-13"),
          actualArrivalTime = null,
          actualDepartureDate = LocalDate.parse("2024-01-02"),
          actualDepartureTime = null,
          nonArrivalConfirmedAtDateTime = null,
          tier = "A",
          keyWorkerStaffCode = "the staff code",
          keyWorkerAssignedAt = LocalDateTime.of(2023, 12, 12, 0, 0, 0).toInstant(ZoneOffset.UTC),
          keyWorkerName = "the keyworker name",
          keyWorkerUserId = keyWorkerId,
          keyWorkerEmail = "the keyworker email",
          characteristicsPropertyNamesCsv = "hasTurningSpace,isCatered",
          deliusEventNumber = "event8",
          cancelled = true,
          openChangeRequestTypeNamesCsv = "PLANNED_TRANSFER,PLACEMENT_EXTENSION",
        ),
        premises,
        personSummaryInfo,
      )

      assertThat(result.id).isEqualTo(id)
      assertThat(result.person).isEqualTo(expectedPersonSummary)
      assertThat(result.premises.id).isEqualTo(premises.id)
      assertThat(result.premises.name).isEqualTo(premises.name)
      assertThat(result.canonicalArrivalDate).isEqualTo(LocalDate.parse("2023-12-13"))
      assertThat(result.canonicalDepartureDate).isEqualTo(LocalDate.parse("2023-01-02"))
      assertThat(result.expectedArrivalDate).isEqualTo(LocalDate.parse("2022-12-13"))
      assertThat(result.expectedDepartureDate).isEqualTo(LocalDate.parse("2022-01-02"))
      assertThat(result.characteristics).containsExactlyInAnyOrder(
        Cas1SpaceCharacteristic.hasTurningSpace,
        Cas1SpaceCharacteristic.isCatered,
      )
      assertThat(result.actualArrivalDate).isEqualTo(LocalDate.parse("2024-12-13"))
      assertThat(result.actualDepartureDate).isEqualTo(LocalDate.parse("2024-01-02"))
      assertThat(result.isNonArrival).isEqualTo(false)
      assertThat(result.tier).isEqualTo("A")
      assertThat(result.keyWorkerAllocation!!.allocatedAt).isEqualTo(LocalDate.parse("2023-12-12"))
      assertThat(result.keyWorkerAllocation!!.userId).isEqualTo(keyWorkerId)
      assertThat(result.keyWorkerAllocation!!.name).isEqualTo("the keyworker name")
      assertThat(result.keyWorkerAllocation!!.emailAddress).isEqualTo("the keyworker email")
      assertThat(result.deliusEventNumber).isEqualTo("event8")
      assertThat(result.isCancelled).isTrue()
      assertThat(result.plannedTransferRequested).isFalse()
      assertThat(result.appealRequested).isFalse()
      assertThat(result.openChangeRequestTypes).containsExactly(
        Cas1ChangeRequestType.PLANNED_TRANSFER,
        Cas1ChangeRequestType.PLACEMENT_EXTENSION,
      )
    }

    @Test
    fun `Space booking is transformed correctly without case worker`() {
      val id = UUID.randomUUID()

      val personSummaryInfo = PersonSummaryInfoResult.Success.Restricted("the crn", "the noms")
      val expectedPersonSummary = RestrictedPersonSummary(
        "the crn",
        PersonSummaryDiscriminator.restrictedPersonSummary,
      )

      every { personTransformer.personSummaryInfoToPersonSummary(personSummaryInfo) } returns expectedPersonSummary

      val result = transformer.transformSearchResultToSummary(
        Cas1SpaceBookingSearchResultImpl(
          id = id,
          crn = "the crn",
          canonicalArrivalDate = LocalDate.parse("2023-12-13"),
          canonicalDepartureDate = LocalDate.parse("2023-01-02"),
          expectedArrivalDate = LocalDate.parse("2023-12-13"),
          expectedDepartureDate = LocalDate.parse("2023-01-02"),
          actualArrivalDate = null,
          actualArrivalTime = null,
          actualDepartureDate = null,
          actualDepartureTime = null,
          nonArrivalConfirmedAtDateTime = null,
          tier = "A",
          keyWorkerStaffCode = null,
          keyWorkerAssignedAt = null,
          keyWorkerName = null,
          keyWorkerUserId = null,
          keyWorkerEmail = null,
          characteristicsPropertyNamesCsv = null,
          deliusEventNumber = "event8",
          cancelled = false,
          openChangeRequestTypeNamesCsv = "",
        ),
        premises = ApprovedPremisesEntityFactory().withDefaults().produce(),
        personSummaryInfo,
      )

      assertThat(result.id).isEqualTo(id)
      assertThat(result.keyWorkerAllocation).isNull()
      assertThat(result.isCancelled).isFalse()
      assertThat(result.plannedTransferRequested).isFalse()
      assertThat(result.appealRequested).isFalse()
    }
  }
}

data class Cas1SpaceBookingSearchResultImpl(
  override val id: UUID,
  override val crn: String,
  override val canonicalArrivalDate: LocalDate,
  override val canonicalDepartureDate: LocalDate,
  override val expectedArrivalDate: LocalDate,
  override val expectedDepartureDate: LocalDate,
  override val actualArrivalDate: LocalDate?,
  override val actualArrivalTime: LocalTime?,
  override val actualDepartureDate: LocalDate?,
  override val actualDepartureTime: LocalTime?,
  override val nonArrivalConfirmedAtDateTime: LocalDateTime?,
  override val tier: String?,
  override val keyWorkerStaffCode: String?,
  override val keyWorkerAssignedAt: Instant?,
  override val keyWorkerName: String?,
  override val keyWorkerUserId: UUID?,
  override val keyWorkerEmail: String?,
  override val characteristicsPropertyNamesCsv: String?,
  override val deliusEventNumber: String?,
  override val cancelled: Boolean,
  override val openChangeRequestTypeNamesCsv: String,
) : Cas1SpaceBookingSearchResult

data class Cas1SpaceBookingAtPremisesImpl(
  override val id: UUID,
  override val canonicalArrivalDate: LocalDate,
  override val canonicalDepartureDate: LocalDate,
) : Cas1SpaceBookingAtPremises
