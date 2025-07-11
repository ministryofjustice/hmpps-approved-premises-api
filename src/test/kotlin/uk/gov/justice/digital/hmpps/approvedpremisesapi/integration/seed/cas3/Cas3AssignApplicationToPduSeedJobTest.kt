package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3.Cas3AssignApplicationToPduSeedCsvRow
import java.time.OffsetDateTime

class Cas3AssignApplicationToPduSeedJobTest : SeedTestBase() {
  @Test
  fun `When assign application to pdu then application is assigned to the pdu`() {
    val user = givenAUser().first

    val pduOne = probationDeliveryUnitFactory.produceAndPersist {
      withName("PduOne")
      withProbationRegion(user.probationRegion)
    }

    val pduTwo = probationDeliveryUnitFactory.produceAndPersist {
      withName("PduTwo")
      withProbationRegion(user.probationRegion)
    }

    val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(user)
      withProbationRegion(user.probationRegion)
      withProbationDeliveryUnit(pduOne)
      withSubmittedAt(OffsetDateTime.now().minusDays(10))
    }

    val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
    }

    seed(
      SeedFileType.temporaryAccommodationAssignApplicationToPdu,
      rowsToCsv(listOf(Cas3AssignApplicationToPduSeedCsvRow(assessment.id, pduTwo.name))),
    )

    val persistedApplication = temporaryAccommodationApplicationRepository.findByIdOrNull(application.id)!!
    assertThat(persistedApplication).isNotNull
    assertThat(persistedApplication.probationDeliveryUnit).isEqualTo(pduTwo)
  }

  private fun rowsToCsv(rows: List<Cas3AssignApplicationToPduSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "assessment_id",
        "pdu_name",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedFields(it.assessmentId, it.pduName)
        .newRow()
    }

    return builder.build()
  }
}
