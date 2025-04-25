package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Suppress("LongParameterList")
class Cas2ApplicationSummaryEntityFactory : Factory<Cas2ApplicationSummaryEntity> {

  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var nomsNumber: Yielded<String> = { randomStringUpperCase(6) }
  private var userId: Yielded<UUID> = { UUID.randomUUID() }
  private var userName: Yielded<String> = { "${randomStringUpperCase(6)} ${randomStringUpperCase(6)}" }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().minusHours(4) }
  private var submittedAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().minusHours(2) }
  private var abandonedAt: Yielded<OffsetDateTime?> = { null }
  private var hdcEligibilityDate: Yielded<LocalDate?> = { null }
  private var latestStatusUpdateLabel: Yielded<String?> = { null }
  private var latestStatusUpdateStatusId: Yielded<String?> = { null }
  private var prisonCode: Yielded<String> = { "LON" }
  private var currentPrisonCode: Yielded<String?> = { "LON" }
  private var assignmentDate: Yielded<OffsetDateTime?> = { OffsetDateTime.now() }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withNomsNumber(nomsNumber: String) = apply {
    this.nomsNumber = { nomsNumber }
  }

  fun withUserId(userId: UUID) = apply {
    this.userId = { userId }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withSubmittedAt(submittedAt: OffsetDateTime) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withAbandonedAt(abandonedAt: OffsetDateTime?) = apply {
    this.abandonedAt = { abandonedAt }
  }

  fun withHdcEligibilityDate(hdcEligibilityDate: LocalDate) = apply {
    this.hdcEligibilityDate = { hdcEligibilityDate }
  }

  fun withLatestStatusUpdateLabel(latestStatusUpdateLabel: String) = apply {
    this.latestStatusUpdateLabel = { latestStatusUpdateLabel }
  }

  fun withLatestStatusUpdateStatusId(latestStatusUpdateStatusId: String) = apply {
    this.latestStatusUpdateStatusId = { latestStatusUpdateStatusId }
  }

  fun withPrisonCode(prisonCode: String) = apply {
    this.prisonCode = { prisonCode }
  }

  fun withCurrentPrisonCode(currentPrisonCode: String) = apply {
    this.currentPrisonCode = { currentPrisonCode }
  }

  fun withAssignmentDate(assignmentDate: OffsetDateTime) = apply {
    this.assignmentDate = { assignmentDate }
  }

  override fun produce(
  ) = Cas2ApplicationSummaryEntity(
    id = this.id(),
    crn = this.crn(),
    nomsNumber = this.nomsNumber(),
    userId = this.userId().toString(),
    userName = this.userName(),
    allocatedPomUserId = this.userId(),
    allocatedPomName = this.userName(),
    createdAt = this.createdAt(),
    submittedAt = this.submittedAt(),
    abandonedAt = this.abandonedAt(),
    hdcEligibilityDate = this.hdcEligibilityDate(),
    latestStatusUpdateLabel = this.latestStatusUpdateLabel(),
    latestStatusUpdateStatusId = this.latestStatusUpdateStatusId(),
    prisonCode = this.prisonCode(),
    currentPrisonCode = this.currentPrisonCode(),
    assignmentDate = this.assignmentDate(),
  )
}
