package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Suppress("LongParameterList")
object Cas2ApplicationSummaryFactory {
  fun produce(
    id: UUID = UUID.randomUUID(),
    crn: String = randomStringMultiCaseWithNumbers(8),
    nomsNumber: String? = randomStringMultiCaseWithNumbers(8),
    userId: UUID = UUID.randomUUID(),
    userName: String = "${randomStringUpperCase(6)} ${randomStringUpperCase(6)}",
    allocatedPomUserId: UUID? = userId,
    allocatedPomName: String? = userName,
    createdAt: Instant = Instant.now().minus(4, ChronoUnit.HOURS),
    submittedAt: Instant? = Instant.now().minus(2, ChronoUnit.HOURS),
    hdcEligibilityDate: LocalDate? = null,
    latestStatusUpdateLabel: String? = null,
    latestStatusUpdateStatusId: UUID? = null,
    prisonCode: String? = "LON",
    currentPrisonCode: String? = prisonCode,
    assignmentDate: Instant? = Instant.now(),
    bailHearingDate: LocalDate? = null,
    applicationOrigin: ApplicationOrigin = ApplicationOrigin.homeDetentionCurfew,
    cohort: Cas2Cohort? = null,
  ): Cas2ApplicationSummary = TestCas2ApplicationSummary(
    id = id,
    crn = crn,
    nomsNumber = nomsNumber,
    userId = userId,
    userName = userName,
    allocatedPomUserId = allocatedPomUserId,
    allocatedPomName = allocatedPomName,
    createdAt = createdAt,
    submittedAt = submittedAt,
    hdcEligibilityDate = hdcEligibilityDate,
    latestStatusUpdateLabel = latestStatusUpdateLabel,
    latestStatusUpdateStatusId = latestStatusUpdateStatusId,
    prisonCode = prisonCode,
    currentPrisonCode = currentPrisonCode,
    assignmentDate = assignmentDate,
    bailHearingDate = bailHearingDate,
    applicationOrigin = applicationOrigin.toString(),
    cohort = cohort,
  )

  private data class TestCas2ApplicationSummary(
    override val id: UUID,
    override val crn: String,
    override val nomsNumber: String?,
    override val userId: UUID,
    override val userName: String,
    override val allocatedPomUserId: UUID?,
    override val allocatedPomName: String?,
    override val createdAt: Instant,
    override val submittedAt: Instant?,
    override val hdcEligibilityDate: LocalDate?,
    override val latestStatusUpdateLabel: String?,
    override val latestStatusUpdateStatusId: UUID?,
    override val prisonCode: String?,
    override val currentPrisonCode: String?,
    override val assignmentDate: Instant?,
    override val bailHearingDate: LocalDate?,
    override val applicationOrigin: String?,
    override val cohort: Cas2Cohort?,
  ) : Cas2ApplicationSummary
}
