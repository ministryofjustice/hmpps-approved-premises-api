package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelinessEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

data class MockApplicationTimelinessEntity(
  val mockId: String,
  val mockTier: String?,
  val mockApplicationSubmittedAt: Instant?,
  val mockBookingMadeAt: Instant?,
  val mockOverallTimeliness: Int?,
  val mockPlacementMatchingTimeliness: Int?,
) : ApplicationTimelinessEntity {
  override fun getId() = this.mockId
  override fun getTier() = this.mockTier
  override fun getApplicationSubmittedAt() = this.mockApplicationSubmittedAt
  override fun getBookingMadeAt() = this.mockBookingMadeAt
  override fun getOverallTimeliness() = this.mockOverallTimeliness
  override fun getPlacementMatchingTimeliness() = this.mockPlacementMatchingTimeliness
}

class ApplicationTimelinessEntityFactory : Factory<MockApplicationTimelinessEntity> {
  private var id: Yielded<String> = { UUID.randomUUID().toString() }
  private var tier: Yielded<String?> = { randomStringUpperCase(2) }
  private var applicationSubmittedAt: Yielded<Instant?> = { LocalDateTime.now().atZone(ZoneOffset.UTC).toInstant() }
  private var bookingMadeAt: Yielded<Instant?> = { LocalDateTime.now().atZone(ZoneOffset.UTC).toInstant() }
  private var overallTimeliness: Yielded<Int?> = { randomInt(0, 10) }
  private var placementMatchingTimeliness: Yielded<Int?> = { randomInt(0, 10) }
  private var overallTimelinessInWorkingDays: Yielded<Int?> = { randomInt(0, 10) }

  fun withId(id: String) = apply {
    this.id = { id }
  }
  fun withTier(tier: String?) = apply {
    this.tier = { tier }
  }
  fun withApplicationSubmittedAt(applicationSubmittedAt: Instant?) = apply {
    this.applicationSubmittedAt = { applicationSubmittedAt }
  }
  fun withBookingMadeAt(bookingMadeAt: Instant?) = apply {
    this.bookingMadeAt = { bookingMadeAt }
  }
  fun withOverallTimeliness(overallTimeliness: Int?) = apply {
    this.overallTimeliness = { overallTimeliness }
  }
  fun withPlacementMatchingTimeliness(placementMatchingTimeliness: Int?) = apply {
    this.placementMatchingTimeliness = { placementMatchingTimeliness }
  }

  override fun produce() = MockApplicationTimelinessEntity(
    mockId = this.id(),
    mockTier = this.tier(),
    mockApplicationSubmittedAt = this.applicationSubmittedAt(),
    mockBookingMadeAt = this.bookingMadeAt(),
    mockOverallTimeliness = this.overallTimeliness(),
    mockPlacementMatchingTimeliness = this.placementMatchingTimeliness(),
  )
}
