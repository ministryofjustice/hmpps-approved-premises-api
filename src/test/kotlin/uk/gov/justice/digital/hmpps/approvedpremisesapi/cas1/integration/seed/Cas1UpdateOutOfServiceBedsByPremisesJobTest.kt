package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremisesBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import java.time.LocalDate
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class Cas1UpdateOutOfServiceBedsByPremisesJobTest : SeedTestBase() {

  @Test
  fun `Updates all active out-of-service beds for a valid premises`() {
    val premises = givenAnApprovedPremises()
    val bed1 = givenAnApprovedPremisesBed(premises = premises)
    val bed2 = givenAnApprovedPremisesBed(premises = premises)
    val otherPremises = givenAnApprovedPremises()
    val otherBed = givenAnApprovedPremisesBed(premises = otherPremises)

    val oosb1 = givenAnOutOfServiceBed(bed = bed1, reason = "Reason 1")
    val oosb2 = givenAnOutOfServiceBed(bed = bed2, reason = "Reason 2")
    val otherOosb = givenAnOutOfServiceBed(bed = otherBed, reason = "Reason 3")

    val originalReason3Id = otherOosb.reason.id

    val newEndDate = LocalDate.now().plusMonths(1)
    val newNotes = "Updated via seed job"

    val newReason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist {
      withName("New Reason")
    }

    seed(
      SeedFileType.approvedPremisesUpdateOutOfServiceBeds,
      rowsToCsv(
        listOf(
          UpdateOutOfServiceBedsByPremisesCsvRow(
            premisesId = premises.id,
            endDate = newEndDate,
            notes = newNotes,
            reasonId = newReason.id,
          ),
        ),
      ),
    )

    val oosb1After = cas1OutOfServiceBedTestRepository.findById(oosb1.id).get()
    assertThat(oosb1After.endDate).isEqualTo(newEndDate)
    assertThat(oosb1After.notes).isEqualTo(newNotes)
    assertThat(oosb1After.reason.id).isEqualTo(newReason.id)

    val oosb2After = cas1OutOfServiceBedTestRepository.findById(oosb2.id).get()
    assertThat(oosb2After.endDate).isEqualTo(newEndDate)
    assertThat(oosb2After.notes).isEqualTo(newNotes)
    assertThat(oosb2After.reason.id).isEqualTo(newReason.id)

    val otherOosbAfter = cas1OutOfServiceBedTestRepository.findById(otherOosb.id).get()
    assertThat(otherOosbAfter.endDate).isNotEqualTo(newEndDate)
    assertThat(otherOosbAfter.reason.id).isEqualTo(originalReason3Id)
  }

  @Test
  fun `Returns error if premises does not exist`() {
    val randomId = UUID.randomUUID()
    seed(
      SeedFileType.approvedPremisesUpdateOutOfServiceBeds,
      rowsToCsv(
        listOf(
          UpdateOutOfServiceBedsByPremisesCsvRow(
            premisesId = randomId,
            endDate = LocalDate.now(),
            notes = "Notes",
            reasonId = UUID.randomUUID(),
          ),
        ),
      ),
    )

    assertError(1, "No Premises with ID $randomId exists.")
  }

  @Test
  fun `Returns error if reason does not exist`() {
    val premises = givenAnApprovedPremises()
    val randomReasonId = UUID.randomUUID()
    seed(
      SeedFileType.approvedPremisesUpdateOutOfServiceBeds,
      rowsToCsv(
        listOf(
          UpdateOutOfServiceBedsByPremisesCsvRow(
            premisesId = premises.id,
            endDate = LocalDate.now(),
            notes = "Notes",
            reasonId = randomReasonId,
          ),
        ),
      ),
    )

    assertError(1, "No Out of Service Bed Reason with ID $randomReasonId exists.")
  }

  private fun rowsToCsv(rows: List<UpdateOutOfServiceBedsByPremisesCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "premises_id",
        "end_date",
        "notes",
        "reason_id",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.premisesId.toString())
        .withQuotedField(it.endDate.toString())
        .withQuotedField(it.notes ?: "")
        .withQuotedField(it.reasonId.toString())
        .newRow()
    }

    return builder.build()
  }

  data class UpdateOutOfServiceBedsByPremisesCsvRow(
    val premisesId: UUID,
    val endDate: LocalDate,
    val notes: String?,
    val reasonId: UUID,
  )
}
