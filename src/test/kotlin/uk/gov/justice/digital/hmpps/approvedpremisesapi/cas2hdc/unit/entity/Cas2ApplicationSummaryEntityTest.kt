package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.toCas2ApplicationSummary
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2ApplicationSummaryEntityTest {

  @Test
  fun `toCas2ApplicationSummary maps all fields`() {
    val entity = Cas2ApplicationSummaryEntity(
      id = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809"),
      crn = "CRN123",
      nomsNumber = "NOMS123",
      userId = "836a9460-b177-433a-a0d9-262509092c9f",
      userName = "first last",
      allocatedPomUserId = UUID.fromString("b3a0f8b4-6d4e-4f0e-9c1a-6a2f6f0b8c11"),
      allocatedPomName = "pom name",
      createdAt = OffsetDateTime.parse("2023-04-19T13:25:00+01:00"),
      submittedAt = OffsetDateTime.parse("2023-04-20T13:25:30+01:00"),
      abandonedAt = OffsetDateTime.parse("2023-04-21T09:00:00+01:00"),
      hdcEligibilityDate = LocalDate.parse("2023-04-29"),
      latestStatusUpdateLabel = "my latest status update",
      latestStatusUpdateStatusId = "ae544aee-7170-4794-99fb-703090cbc7db",
      prisonCode = "BRI",
      currentPrisonCode = "LON",
      assignmentDate = OffsetDateTime.parse("2023-04-22T10:00:00+01:00"),
      bailHearingDate = LocalDate.parse("2023-05-01"),
      applicationOrigin = "prisonBail",
      serviceOrigin = "BAIL",
      cohort = Cas2Cohort.PRISON_BAIL,
    )

    val result = entity.toCas2ApplicationSummary()

    assertThat(result.id).isEqualTo(entity.id)
    assertThat(result.crn).isEqualTo(entity.crn)
    assertThat(result.nomsNumber).isEqualTo(entity.nomsNumber)
    assertThat(result.userId).isEqualTo(entity.userId)
    assertThat(result.userName).isEqualTo(entity.userName)
    assertThat(result.allocatedPomUserId).isEqualTo(entity.allocatedPomUserId)
    assertThat(result.allocatedPomName).isEqualTo(entity.allocatedPomName)
    assertThat(result.createdAt).isEqualTo(entity.createdAt)
    assertThat(result.submittedAt).isEqualTo(entity.submittedAt)
    assertThat(result.abandonedAt).isEqualTo(entity.abandonedAt)
    assertThat(result.hdcEligibilityDate).isEqualTo(entity.hdcEligibilityDate)
    assertThat(result.latestStatusUpdateLabel).isEqualTo(entity.latestStatusUpdateLabel)
    assertThat(result.latestStatusUpdateStatusId).isEqualTo(entity.latestStatusUpdateStatusId)
    assertThat(result.prisonCode).isEqualTo(entity.prisonCode)
    assertThat(result.currentPrisonCode).isEqualTo(entity.currentPrisonCode)
    assertThat(result.assignmentDate).isEqualTo(entity.assignmentDate)
    assertThat(result.bailHearingDate).isEqualTo(entity.bailHearingDate)
    assertThat(result.applicationOrigin).isEqualTo(entity.applicationOrigin)
    assertThat(result.serviceOrigin).isEqualTo(entity.serviceOrigin)
    assertThat(result.cohort).isEqualTo(entity.cohort)
  }

  @Test
  fun `toCas2ApplicationSummary results for the same entity are equal`() {
    val entity = Cas2ApplicationSummaryEntity(
      id = UUID.randomUUID(),
      crn = "CRN123",
      nomsNumber = "NOMS123",
      userId = UUID.randomUUID().toString(),
      userName = "first last",
      allocatedPomUserId = null,
      allocatedPomName = null,
      createdAt = OffsetDateTime.parse("2023-04-19T13:25:00+01:00"),
      submittedAt = null,
      prisonCode = "BRI",
      currentPrisonCode = null,
      assignmentDate = null,
    )

    assertThat(entity.toCas2ApplicationSummary()).isEqualTo(entity.toCas2ApplicationSummary())
  }
}
