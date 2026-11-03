package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementDtoV2
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementWithdrawalDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.PlacementApplicationDecisionDto
import java.time.LocalDate
import java.time.LocalDateTime

class Cas1ExternalRequestForPlacementDtoV2Factory : Factory<Cas1ExternalRequestForPlacementDtoV2> {
  private var status: Yielded<RequestForPlacementStatus> = { RequestForPlacementStatus.awaitingMatch }
  private var statusLastUpdated: Yielded<LocalDateTime> = { LocalDateTime.now() }
  private var submittedBy: Yielded<Cas1StaffDto?> = { null }
  private var submittedAt: Yielded<LocalDate?> = { null }
  private var expectedArrivalDate: Yielded<LocalDate?> = { null }
  private var durationDays: Yielded<Int?> = { null }
  private var decision: Yielded<PlacementApplicationDecisionDto?> = { null }
  private var rejectionReason: Yielded<String?> = { null }
  private var withdrawal: Yielded<Cas1ExternalRequestForPlacementWithdrawalDto?> = { null }

  fun withStatus(status: RequestForPlacementStatus) = apply {
    this.status = { status }
  }

  fun withSubmittedBy(submittedBy: Cas1StaffDto?) = apply {
    this.submittedBy = { submittedBy }
  }

  fun withSubmittedAt(submittedAt: LocalDate?) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withExpectedArrivalDate(expectedArrivalDate: LocalDate?) = apply {
    this.expectedArrivalDate = { expectedArrivalDate }
  }

  fun withDurationDays(durationDays: Int?) = apply {
    this.durationDays = { durationDays }
  }

  fun withDecision(decision: PlacementApplicationDecisionDto?) = apply {
    this.decision = { decision }
  }

  fun withRejectionReason(rejectionReason: String?) = apply {
    this.rejectionReason = { rejectionReason }
  }

  fun withWithdrawal(withdrawal: Cas1ExternalRequestForPlacementWithdrawalDto?) = apply {
    this.withdrawal = { withdrawal }
  }

  override fun produce() = Cas1ExternalRequestForPlacementDtoV2(
    status = this.status(),
    statusLastUpdated = this.statusLastUpdated(),
    submittedBy = this.submittedBy(),
    submittedAt = this.submittedAt(),
    expectedArrivalDate = this.expectedArrivalDate(),
    durationDays = this.durationDays(),
    decision = this.decision(),
    rejectionReason = this.rejectionReason(),
    withdrawal = this.withdrawal(),
  )
}
