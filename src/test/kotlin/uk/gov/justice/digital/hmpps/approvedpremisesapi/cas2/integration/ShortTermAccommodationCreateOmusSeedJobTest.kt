package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ShortTermAccommodationCreateOmusSeedJobTest : SeedTestBase() {

  @Autowired
  override lateinit var offenderManagementUnitRepository: OffenderManagementUnitRepository

  @Test
  fun `offenderManagementUnitRepository data is wiped before new upload and new data is processed`() {
    val originalOmu = OffenderManagementUnitEntity(UUID.randomUUID(), "LIV", "HMP LIVERPOOL", "omu2@prison.co.uk")
    offenderManagementUnitRepository.save(originalOmu)

    seed(
      SeedFileType.shortTermAccommodationCreateOmus,
      contents = "prisonCode,prisonName,email\n" + "LON,HMP LONDON,omu1@prison.co.uk",
    )

    val omus = offenderManagementUnitRepository.findAll()

    assertThat(omus.size).isEqualTo(1)
    assertThat(omus[0].prisonCode).isEqualTo("LON")
    assertThat(omus[0].prisonName).isEqualTo("HMP LONDON")
    assertThat(omus[0].email).isEqualTo("omu1@prison.co.uk")
  }

  @Test
  fun `Row with empty email cannot be processed`() {
    seed(
      SeedFileType.shortTermAccommodationCreateOmus,
      contents = "prisonCode,prisonName,email\n" + "LON,HMP LONDON,",
    )

    assertThrows<IndexOutOfBoundsException> {
      offenderManagementUnitRepository.findAll()[0]
    }
  }

  @Test
  fun `Row with empty prisonCode cannot be processed`() {
    seed(
      SeedFileType.shortTermAccommodationCreateOmus,
      contents = "prisonCode,prisonName,email\n" + ",HMP LONDON,omu1@prison.co.uk",
    )

    assertThrows<IndexOutOfBoundsException> {
      offenderManagementUnitRepository.findAll()[0]
    }
  }

  @Test
  fun `Row with empty prisonName cannot be processed`() {
    seed(
      SeedFileType.shortTermAccommodationCreateOmus,
      contents = "prisonCode,prisonName,email\n" + "LON,,omu1@prison.co.uk",
    )

    assertThrows<IndexOutOfBoundsException> {
      offenderManagementUnitRepository.findAll()[0]
    }
  }

  @Test
  fun `Row with bad prisonCode cannot be processed`() {
    seed(
      SeedFileType.shortTermAccommodationCreateOmus,
      contents = "prisonCode,prisonName,email\n" + "LON1234,HMP LONDON,omu1@prison.co.uk",
    )

    assertThrows<IndexOutOfBoundsException> {
      offenderManagementUnitRepository.findAll()[0]
    }
  }

  @Test
  fun `Row with bad email cannot be processed`() {
    seed(
      SeedFileType.shortTermAccommodationCreateOmus,
      contents = "prisonCode,prisonName,email\n" + "LON,HMP LONDON,omu1@prison.co.uk;",
    )

    assertThrows<IndexOutOfBoundsException> {
      offenderManagementUnitRepository.findAll()[0]
    }
  }
}
