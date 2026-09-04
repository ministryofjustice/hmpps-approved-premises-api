package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1AuthorisedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingShortSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class RequestForPlacementFactory : Factory<RequestForPlacement> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var document: Yielded<String?> = { "{}" }
  private var createdAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(30) }
  private var submittedAt: Yielded<Instant?> = { null }
  private var withdrawalReason: Yielded<WithdrawPlacementRequestReason?> = { null }
  private var isWithdrawn: Yielded<Boolean> = { false }
  private var sentenceType: Yielded<SentenceTypeOption?> = { null }
  private var releaseType: Yielded<ReleaseTypeOption?> = { null }
  private var createdByUserId: Yielded<UUID> = { UUID.randomUUID() }
  private var canBeDirectlyWithdrawn: Yielded<Boolean> = { false }
  private var requestedPlacementPeriod: Yielded<Cas1RequestedPlacementPeriod> = {
    Cas1RequestedPlacementPeriod(
      arrival = LocalDate.now(),
      arrivalFlexible = null,
      duration = 14,
    )
  }
  private var canonicalPlacementPeriod: Yielded<Cas1RequestedPlacementPeriod> = {
    Cas1RequestedPlacementPeriod(
      arrival = LocalDate.now(),
      arrivalFlexible = null,
      duration = 14,
    )
  }
  private var type: Yielded<RequestForPlacementType> = { RequestForPlacementType.automatic }

  private var authorisedPlacementPeriod: Yielded<Cas1AuthorisedPlacementPeriod?> = { null }
  private var status: Yielded<RequestForPlacementStatus> = { RequestForPlacementStatus.placementBooked }
  private var statusSetDate: Yielded<LocalDate> = { LocalDate.now() }
  private var submittedBy: Yielded<Cas1StaffDto?> = { null }
  private var decision: Yielded<PlacementApplicationDecision?> = { null }
  private var requestReviewedAt: Yielded<Instant?> = { null }
  private var placements: Yielded<List<Cas1SpaceBookingShortSummary>> = { emptyList() }
  private var situation: Yielded<SituationOption?> = { null }

  fun withPlacements(placements: List<Cas1SpaceBookingShortSummary>) = apply {
    this.placements = { placements }
  }

  fun withStatus(status: RequestForPlacementStatus) = apply {
    this.status = { status }
  }

  fun withSubmittedBy(submittedBy: Cas1StaffDto?) = apply {
    this.submittedBy = { submittedBy }
  }

  fun withStatusSetDate(statusSetDate: LocalDate) = apply {
    this.statusSetDate = { statusSetDate }
  }

  fun withCanonicalPlacementPeriod(canonicalPlacementPeriod: Cas1RequestedPlacementPeriod) = apply {
    this.canonicalPlacementPeriod = { canonicalPlacementPeriod }
  }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withDocument(document: String?) = apply {
    this.document = { document }
  }

  fun withDecision(decision: PlacementApplicationDecision?) = apply {
    this.decision = { decision }
  }

  fun withCreatedAt(createdAt: Instant) = apply {
    this.createdAt = { createdAt }
  }

  fun withSubmittedAt(submittedAt: Instant?) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withWithdrawalReason(withdrawalReason: WithdrawPlacementRequestReason?) = apply {
    this.withdrawalReason = { withdrawalReason }
  }

  fun withIsWithdrawn(isWithdrawn: Boolean) = apply {
    this.isWithdrawn = { isWithdrawn }
  }

  fun withSentenceType(sentenceType: SentenceTypeOption?) = apply {
    this.sentenceType = { sentenceType }
  }

  fun withReleaseType(releaseType: ReleaseTypeOption?) = apply {
    this.releaseType = { releaseType }
  }

  fun withSituation(situation: SituationOption?) = apply {
    this.situation = { situation }
  }

  override fun produce(): RequestForPlacement = RequestForPlacement(
    id = this.id(),
    document = this.document(),
    createdAt = this.createdAt(),
    submittedAt = this.submittedAt(),
    withdrawalReason = this.withdrawalReason(),
    isWithdrawn = this.isWithdrawn(),
    sentenceType = this.sentenceType(),
    releaseType = this.releaseType(),
    createdByUserId = this.createdByUserId(),
    canBeDirectlyWithdrawn = this.canBeDirectlyWithdrawn(),
    type = this.type(),
    requestedPlacementPeriod = this.requestedPlacementPeriod(),
    authorisedPlacementPeriod = this.authorisedPlacementPeriod(),
    status = this.status(),
    statusSetDate = this.statusSetDate(),
    requestReviewedAt = this.requestReviewedAt(),
    situation = this.situation(),
    placements = this.placements(),
    submittedBy = this.submittedBy(),
    decision = this.decision(),
    canonicalPlacementPeriod = this.canonicalPlacementPeriod(),
  )
}
