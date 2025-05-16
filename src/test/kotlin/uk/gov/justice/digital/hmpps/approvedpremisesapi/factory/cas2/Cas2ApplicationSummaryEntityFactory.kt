package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Suppress("LongParameterList")
object Cas2ApplicationSummaryEntityFactory {
  fun produce(
    id: UUID = UUID.randomUUID(),
    crn: String = randomStringMultiCaseWithNumbers(8),
    nomsNumber: String = randomStringMultiCaseWithNumbers(8),
    userId: UUID = UUID.randomUUID(),
    userName: String = "${randomStringUpperCase(6)} ${randomStringUpperCase(6)}",
    allocatedPomUserId: UUID = userId,
    allocatedPomName: String = userName,
    createdAt: OffsetDateTime = OffsetDateTime.now().minusHours(4),
    submittedAt: OffsetDateTime = OffsetDateTime.now().minusHours(2),
    abandonedAt: OffsetDateTime? = null,
    hdcEligibilityDate: LocalDate? = null,
    latestStatusUpdateLabel: String? = null,
    latestStatusUpdateStatusId: String? = null,
    referringPrisonCode: String = "LON",
    currentPrisonCode: String = referringPrisonCode,
    assignmentDate: OffsetDateTime = OffsetDateTime.now(),
    applicationOrigin: ApplicationOrigin = ApplicationOrigin.homeDetentionCurfew,
  ) = Cas2ApplicationSummaryEntity(
    id = id,
    crn = crn,
    nomsNumber = nomsNumber,
    userId = userId.toString(),
    userName = userName,
    allocatedPomUserId = allocatedPomUserId,
    allocatedPomName = allocatedPomName,
    createdAt = createdAt,
    submittedAt = submittedAt,
    abandonedAt = abandonedAt,
    hdcEligibilityDate = hdcEligibilityDate,
    latestStatusUpdateLabel = latestStatusUpdateLabel,
    latestStatusUpdateStatusId = latestStatusUpdateStatusId,
    prisonCode = referringPrisonCode,
    currentPrisonCode = currentPrisonCode,
    assignmentDate = assignmentDate,
    applicationOrigin = applicationOrigin.toString(),
  )
}
