package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection.asc
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection.desc
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1NewSpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingDaySummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingDaySummarySortField.CANONICAL_ARRIVAL_DATE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingDaySummarySortField.CANONICAL_DEPARTURE_DATE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingDaySummarySortField.PERSON_NAME
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingDaySummarySortField.PERSON_TIER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingDaySummarySortField.RELEASE_TYPE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingDaySummarySortField.TIER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingResidency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingSummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.controller.PaginationMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult.GeneralValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult.Success
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResultValidatedScope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.ifError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository.Constants.CAS1_RELATED_APP_WITHDRAWN_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository.Constants.CAS1_RELATED_PLACEMENT_APP_WITHDRAWN_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository.Constants.CAS1_RELATED_PLACEMENT_REQ_WITHDRAWN_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LockablePlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.forCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService.Companion.FEATURE_FLAG_USE_TIER_V3
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingCreateService.CreateBookingDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingUpdateService.UpdateBookingDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.orEmpty
import kotlin.collections.toSet

@SuppressWarnings("TooManyFunctions")
@Service
class Cas1SpaceBookingService(
  private val cas1PremisesService: Cas1PremisesService,
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  private val cas1BookingDomainEventService: Cas1SpaceBookingDomainEventService,
  private val cas1BookingEmailService: Cas1BookingEmailService,
  private val cas1ApplicationStatusService: Cas1ApplicationStatusService,
  private val cancellationReasonRepository: CancellationReasonRepository,
  private val lockablePlacementRequestRepository: LockablePlacementRequestRepository,
  private val characteristicService: CharacteristicService,
  private val cas1SpaceBookingActionsService: Cas1SpaceBookingActionsService,
  private val cas1SpaceBookingCreateService: Cas1SpaceBookingCreateService,
  private val cas1SpaceBookingUpdateService: Cas1SpaceBookingUpdateService,
  private val offenderService: OffenderService,
  private val spaceBookingTransformer: Cas1SpaceBookingTransformer,
  private val featureFlagService: FeatureFlagService,
) {

  @Transactional
  fun createNewBooking(
    placementRequestId: UUID,
    newSpaceBooking: Cas1NewSpaceBooking,
    createdBy: UserEntity,
    characteristics: List<Cas1CharacteristicEntity>,
  ): CasResult<Cas1SpaceBookingEntity> = validatedCasResult {
    lockablePlacementRequestRepository.acquirePessimisticLock(placementRequestId)

    val validatedCreateBooking = when (
      val result = cas1SpaceBookingCreateService.validate(
        CreateBookingDetails(
          premisesId = newSpaceBooking.premisesId,
          placementRequestId = placementRequestId,
          expectedArrivalDate = newSpaceBooking.arrivalDate,
          expectedDepartureDate = newSpaceBooking.departureDate,
          createdBy = createdBy,
          characteristics = characteristics,
          transferReason = newSpaceBooking.transferReason,
          additionalInformation = newSpaceBooking.additionalInformation,
        ),
      )
    ) {
      is CasResult.Error -> return result.reviseType()
      is Success -> result.value
    }

    success(cas1SpaceBookingCreateService.create(validatedCreateBooking))
  }

  fun search(
    premisesId: UUID,
    filterCriteria: SpaceBookingFilterCriteria,
    pageCriteria: PageCriteria<Cas1SpaceBookingSummarySortField>,
  ): CasResult<SearchResultContainer> {
    val useTierV3 = featureFlagService.getBooleanFlag(FEATURE_FLAG_USE_TIER_V3)

    val premises = cas1PremisesService.findPremisesById(premisesId)
      ?: return CasResult.NotFound("premises", premisesId.toString())

    val page = cas1SpaceBookingRepository.search(
      filterCriteria.residency?.name,
      filterCriteria.crnOrName,
      filterCriteria.keyWorkerStaffCode,
      filterCriteria.keyWorkerUserId,
      premisesId,
      pageCriteria.toPageableOrAllPages(
        sortBy = when (pageCriteria.sortBy) {
          Cas1SpaceBookingSummarySortField.personName -> "personName"
          Cas1SpaceBookingSummarySortField.canonicalArrivalDate -> "canonicalArrivalDate"
          Cas1SpaceBookingSummarySortField.canonicalDepartureDate -> "canonicalDepartureDate"
          Cas1SpaceBookingSummarySortField.keyWorkerName -> "keyWorkerName"
          Cas1SpaceBookingSummarySortField.tier -> "tierOnApplicationCreation"
          Cas1SpaceBookingSummarySortField.personTier -> if (useTierV3) {
            "personTierV3Score"
          } else {
            "personTierV2Score"
          }
        },
      ),
    )

    return Success(
      SearchResultContainer(
        results = page.toList(),
        paginationMetadata = getMetadata(page, pageCriteria),
        premises = premises,
      ),
    )
  }

  fun getBookingForPremisesAndId(premisesId: UUID, bookingId: UUID): CasResult<Cas1SpaceBookingEntity> {
    if (!cas1PremisesService.premisesExistsById(premisesId)) return CasResult.NotFound("premises", premisesId.toString())

    return getBooking(bookingId)
  }

  fun getBooking(bookingId: UUID): CasResult<Cas1SpaceBookingEntity> {
    val booking = cas1SpaceBookingRepository.findByIdOrNull(bookingId) ?: return CasResult.NotFound("booking", bookingId.toString())

    return Success(booking)
  }

  fun getBookingsForPremisesAndCrn(premisesId: UUID, crn: String) = cas1SpaceBookingRepository.findByPremisesIdAndCrn(
    premisesId = premisesId,
    crn = crn,
  )

  fun getWithdrawableState(spaceBooking: Cas1SpaceBookingEntity, user: UserEntity): WithdrawableState = WithdrawableState(
    withdrawable = !spaceBooking.isCancelled() && !spaceBooking.hasArrival() && !spaceBooking.hasNonArrival(),
    withdrawn = spaceBooking.isCancelled(),
    userMayDirectlyWithdraw = user.hasAtLeastOnePermission(
      UserPermission.CAS1_SPACE_BOOKING_WITHDRAW,
      UserPermission.CAS1_PLACEMENT_APPEAL_ASSESS,
    ),
    blockingReason = if (spaceBooking.hasArrival()) {
      BlockingReason.ArrivalRecordedInCas1
    } else if (spaceBooking.hasNonArrival()) {
      BlockingReason.NonArrivalRecordedInCas1
    } else {
      null
    },
  )

  fun withdraw(
    spaceBooking: Cas1SpaceBookingEntity,
    occurredAt: LocalDate,
    userProvidedReasonId: UUID?,
    userProvidedReasonNotes: String?,
    withdrawalContext: WithdrawalContext,
  ): CasResult<Unit> {
    if (spaceBooking.isCancelled()) {
      return Success(Unit)
    }

    val resolvedReasonId = toCas1CancellationReason(withdrawalContext, userProvidedReasonId)

    val reason = cancellationReasonRepository.findByIdOrNull(resolvedReasonId)
      ?: return CasResult.FieldValidationError(mapOf("$.reason" to "doesNotExist"))

    if (reason.name == "Other" && userProvidedReasonNotes.isNullOrEmpty()) {
      return CasResult.FieldValidationError(mapOf("$.otherReason" to "empty"))
    }

    spaceBooking.cancellationReason = reason
    spaceBooking.cancellationOccurredAt = occurredAt
    spaceBooking.cancellationRecordedAt = Instant.now()
    spaceBooking.cancellationReasonNotes = userProvidedReasonNotes
    cas1SpaceBookingRepository.save(spaceBooking)

    val user = when (withdrawalContext.withdrawalTriggeredBy) {
      is WithdrawalTriggeredByUser -> withdrawalContext.withdrawalTriggeredBy.user
      else -> throw InternalServerErrorProblem("Withdrawal triggered automatically is not supported")
    }

    cas1BookingDomainEventService.spaceBookingCancelled(Cas1BookingCancelledEvent(spaceBooking, user, reason))
    cas1ApplicationStatusService.spaceBookingCancelled(
      spaceBooking,
      isUserRequestedWithdrawal = withdrawalContext.triggeringEntityType == WithdrawableEntityType.SpaceBooking,
    )

    spaceBooking.application?.let {
      cas1BookingEmailService.spaceBookingWithdrawn(
        spaceBooking = spaceBooking,
        application = it,
        withdrawalTriggeredBy = withdrawalContext.withdrawalTriggeredBy,
      )
    }

    return Success(Unit)
  }

  @Transactional
  fun updateBooking(
    updateBookingDetails: UpdateBookingDetails,
  ): CasResult<Cas1SpaceBookingEntity> {
    cas1SpaceBookingUpdateService.validate(updateBookingDetails).ifError { return it.reviseType() }

    return Success(cas1SpaceBookingUpdateService.update(updateBookingDetails))
  }

  @Transactional
  fun shortenBooking(
    shortenedBookingDetails: UpdateBookingDetails,
  ): CasResult<Cas1SpaceBookingEntity> = validatedCasResult {
    val existingBooking = cas1SpaceBookingRepository.findByIdOrNull(shortenedBookingDetails.bookingId)

    validateShortenedSpaceBooking(shortenedBookingDetails, existingBooking)
    if (hasErrors()) return errors()

    cas1SpaceBookingActionsService.determineActions(existingBooking!!)
      .unavailableReason(SpaceBookingAction.SHORTEN)?.let {
        return GeneralValidationError(it)
      }

    val updateBookingDetails = UpdateBookingDetails(
      bookingId = shortenedBookingDetails.bookingId,
      premisesId = shortenedBookingDetails.premisesId,
      departureDate = shortenedBookingDetails.departureDate,
      updatedBy = shortenedBookingDetails.updatedBy,
      updateType = UpdateType.SHORTENING,
    )
    cas1SpaceBookingUpdateService.validate(updateBookingDetails).ifError { return it.reviseType() }

    return Success(cas1SpaceBookingUpdateService.update(updateBookingDetails))
  }

  fun getSpaceBookingsByPremisesIdAndCriteriaForDate(
    premises: ApprovedPremisesEntity,
    date: LocalDate,
    bookingsCriteriaFilter: List<Cas1SpaceBookingCharacteristic>?,
    bookingsSortBy: Cas1SpaceBookingDaySummarySortField,
    bookingsSortDirection: SortDirection,
    excludeSpaceBookingId: UUID? = null,
    user: UserEntity,
  ): List<Cas1SpaceBookingSummary> {
    val useTierV3 = featureFlagService.getBooleanFlag(FEATURE_FLAG_USE_TIER_V3)
    val sort = Sort.by(
      bookingsSortDirection.toSpringDirection(),
      bookingsSortBy.toRepositoryProperty(useTierV3),
    )

    val spaceBookingsForDate = cas1SpaceBookingRepository.findSpaceBookingsByPremisesIdAndCriteriaForDate(
      premisesId = premises.id,
      date = date,
      criteria = getBookingCharacteristicIds(bookingsCriteriaFilter),
      sort = sort,
      excludeSpaceBookingId = excludeSpaceBookingId,
    )

    val offenderSummaries = getOffenderSummariesForBookings(spaceBookingsForDate, user)

    return spaceBookingsForDate.map { bookingSummary ->
      spaceBookingTransformer.transformSearchResultToSummary(
        searchResult = bookingSummary,
        premises = premises,
        personSummaryInfo = offenderSummaries.forCrn(bookingSummary.crn),
      )
    }
  }

  fun SortDirection.toSpringDirection() = when (this) {
    desc -> Sort.Direction.DESC
    asc -> Sort.Direction.ASC
  }

  private fun Cas1SpaceBookingDaySummarySortField.toRepositoryProperty(useTierV3: Boolean) = when (this) {
    PERSON_NAME -> "personName"
    PERSON_TIER -> if (useTierV3) "personTierV3Score" else "personTierV2Score"
    TIER -> "tierOnApplicationCreation"
    CANONICAL_ARRIVAL_DATE -> "canonicalArrivalDate"
    CANONICAL_DEPARTURE_DATE -> "canonicalDepartureDate"
    RELEASE_TYPE -> "releaseType"
  }

  fun findAllBookingsForCrn(crn: String, includeCancelled: Boolean): List<Cas1SpaceBookingEntity> = cas1SpaceBookingRepository.findAllSpaceBookingsForCrn(crn, includeCancelled)

  fun getLatestPlacement(applicationId: UUID): Cas1SpaceBookingEntity? = cas1SpaceBookingRepository.findAllSpaceBookingsForApplicationId(applicationId).maxByOrNull { it.createdAt }

  private fun getBookingCharacteristicIds(bookingsCriteriaFilter: List<Cas1SpaceBookingCharacteristic>?) = bookingsCriteriaFilter?.let { bookingCriteria ->
    val characteristics = bookingCriteria.map { it.value }
    characteristicService.getCas1CharacteristicsByPropertyNames(characteristics)
      .map { characteristic -> characteristic.id }
  }

  private fun getOffenderSummariesForBookings(spaceBookings: List<Cas1SpaceBookingSearchResult>, user: UserEntity): List<PersonSummaryInfoResult> = offenderService.getPersonSummaryInfoResults(
    crns = spaceBookings.map { it.crn }.toSet(),
    laoStrategy = user.cas1LaoStrategy(),
  )

  private fun CasResultValidatedScope<Cas1SpaceBookingEntity>.validateShortenedSpaceBooking(
    shortenedBookingDetails: UpdateBookingDetails,
    existingSpaceBooking: Cas1SpaceBookingEntity?,
  ) {
    val newDepartureDate = shortenedBookingDetails.departureDate

    if (newDepartureDate!!.isBefore(LocalDate.now())) {
      "$.departureDate" hasValidationError "The departure date is in the past."
    }

    existingSpaceBooking?.let {
      if (newDepartureDate.isAfter(existingSpaceBooking.expectedDepartureDate)) {
        "$.departureDate" hasValidationError "The departure date is after the current expected departure date."
      }

      if (newDepartureDate.isEqual(existingSpaceBooking.expectedDepartureDate)) {
        "$.departureDate" hasValidationError "The departure date is the same as the current expected departure date."
      }
    }
  }

  private fun getCharacteristicsEntity(cas1SpaceCharacteristics: List<Cas1SpaceCharacteristic>?): List<Cas1CharacteristicEntity> = characteristicService.getCas1CharacteristicsByPropertyNames(
    cas1SpaceCharacteristics.orEmpty().toSet().map { it.value },
  )

  private fun toCas1CancellationReason(
    withdrawalContext: WithdrawalContext,
    userProvidedCancellationReasonId: UUID?,
  ) = when (withdrawalContext.triggeringEntityType) {
    WithdrawableEntityType.Application -> CAS1_RELATED_APP_WITHDRAWN_ID
    WithdrawableEntityType.PlacementApplication -> CAS1_RELATED_PLACEMENT_APP_WITHDRAWN_ID
    WithdrawableEntityType.PlacementRequest -> CAS1_RELATED_PLACEMENT_REQ_WITHDRAWN_ID
    WithdrawableEntityType.SpaceBooking -> userProvidedCancellationReasonId!!
  }

  data class SpaceBookingFilterCriteria(
    val residency: Cas1SpaceBookingResidency?,
    val crnOrName: String?,
    val keyWorkerStaffCode: String?,
    val keyWorkerUserId: UUID?,
  )

  enum class UpdateType {
    AMENDMENT,
    SHORTENING,
  }

  data class SearchResultContainer(
    val results: List<Cas1SpaceBookingSearchResult>,
    val paginationMetadata: PaginationMetadata?,
    val premises: ApprovedPremisesEntity,
  )
}
