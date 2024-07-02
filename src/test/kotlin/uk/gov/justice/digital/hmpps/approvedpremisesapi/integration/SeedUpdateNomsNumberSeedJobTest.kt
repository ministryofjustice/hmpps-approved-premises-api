package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.UpdateNomsNumberSeedRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedUpdateNomsNumberSeedJobTest : SeedTestBase() {

  @Autowired
  lateinit var domainEventService: DomainEventService

  companion object CONSTANTS {
    const val CRN = "CRN123"
    const val NEW_NOMS = "NOMS456"
    const val OTHER_CRN = "OTHERCRN"
    const val OTHER_NOMS = "OTHERNOMS"
  }

  @Test
  fun `Update Application and Booking NOMS Numbers`() {
    val (applicant, _) = `Given a User`()

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withDefaults()
      withCreatedByUser(applicant)
      withApplicationSchema(
        approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
      )
      withApArea(apAreaEntityFactory.produceAndPersist())
      withCrn(CRN)
      withNomsNumber("OLDVALUE")
    }

    val otherUnaffectedApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withDefaults()
      withCreatedByUser(applicant)
      withApplicationSchema(
        approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
      )
      withApArea(apAreaEntityFactory.produceAndPersist())
      withCrn(OTHER_CRN)
      withNomsNumber(OTHER_NOMS)
    }

    val booking = bookingEntityFactory.produceAndPersist {
      withPremises(
        approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        },
      )
      withCrn(CRN)
      withNomsNumber("OLDVALUE")
    }

    val otherUnaffectedBooking = bookingEntityFactory.produceAndPersist {
      withPremises(
        approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        },
      )
      withCrn(OTHER_CRN)
      withNomsNumber(OTHER_NOMS)
    }

    withCsv(
      "valid-csv",
      rowsToCsv(
        listOf(
          UpdateNomsNumberSeedRow(
            CRN,
            NEW_NOMS,
          ),
        ),
      ),
    )

    seedService.seedData(SeedFileType.updateNomsNumber, "valid-csv")

    val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)
    assertThat(updatedApplication!!.nomsNumber).isEqualTo(NEW_NOMS)

    val updatedOtherApplication = approvedPremisesApplicationRepository.findByIdOrNull(otherUnaffectedApplication.id)
    assertThat(updatedOtherApplication!!.nomsNumber).isEqualTo(OTHER_NOMS)

    val updatedBooking = bookingRepository.findByIdOrNull(booking.id)
    assertThat(updatedBooking!!.nomsNumber).isEqualTo(NEW_NOMS)

    val updatedOtherBooking = bookingRepository.findByIdOrNull(otherUnaffectedBooking.id)
    assertThat(updatedOtherBooking!!.nomsNumber).isEqualTo(OTHER_NOMS)
  }

  private fun rowsToCsv(rows: List<UpdateNomsNumberSeedRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "crn",
        "newNomsNumber",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.crn)
        .withQuotedField(it.newNomsNumber)
        .newRow()
    }

    return builder.build()
  }
}
