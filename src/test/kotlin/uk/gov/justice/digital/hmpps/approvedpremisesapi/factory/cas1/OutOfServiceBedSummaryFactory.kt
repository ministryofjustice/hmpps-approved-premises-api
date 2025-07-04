package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class OutOfServiceBedSummaryFactory : Factory<Cas1OutOfServiceBedRepository.OutOfServiceBedSummary> {
  private var id = { UUID.randomUUID() }
  private var bedId = { UUID.randomUUID() }
  private var premisesId = { UUID.randomUUID() }
  private var startDate = { LocalDate.now() }
  private var endDate = { LocalDate.now() }
  private var reasonName = { randomStringMultiCaseWithNumbers(4) }

  fun withBedId(bedId: UUID) = apply {
    this.bedId = { bedId }
  }

  fun withPremisesId(premisesId: UUID) = apply {
    this.premisesId = { premisesId }
  }

  fun withStartDate(startDate: LocalDate) = apply {
    this.startDate = { startDate }
  }

  fun withEndDate(endDate: LocalDate) = apply {
    this.endDate = { endDate }
  }

  fun withReasonName(reasonName: String) = apply {
    this.reasonName = { reasonName }
  }

  override fun produce(): Cas1OutOfServiceBedRepository.OutOfServiceBedSummary = OutOfServiceBedSummaryImpl(
    id(),
    bedId(),
    premisesId(),
    startDate(),
    endDate(),
    reasonName(),
  )
}

data class OutOfServiceBedSummaryImpl(
  private val id: UUID,
  private val bedId: UUID,
  private val premisesId: UUID,
  private val startDate: LocalDate,
  private val endDate: LocalDate,
  private val reasonName: String,
) : Cas1OutOfServiceBedRepository.OutOfServiceBedSummary {
  override fun getId() = id
  override fun getBedId() = bedId
  override fun getPremisesId() = premisesId
  override fun getStartDate() = startDate
  override fun getEndDate() = endDate
  override fun getReasonName() = reasonName
}
