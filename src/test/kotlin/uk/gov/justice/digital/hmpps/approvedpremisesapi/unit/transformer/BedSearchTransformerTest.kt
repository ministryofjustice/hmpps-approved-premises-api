package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedSearchTransformer
import java.math.BigDecimal
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResult as ApiBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedSearchResult as DomainBedSearchResult

class BedSearchTransformerTest {
  private val bedSearchTransformer = BedSearchTransformer()

  @Test
  fun `transformDomainToApi transforms correctly`() {
    val domain = listOf(
      DomainBedSearchResult(
        premisesId = UUID.fromString("323706f4-5054-452a-aa4a-ecc075ebc9ac"),
        premisesName = "Premises Name",
        premisesCharacteristicPropertyNames = mutableListOf("propertyCharacteristic1", "propertyCharacteristic2"),
        roomId = UUID.fromString("59a2ce43-eda8-4564-807a-72d6c043c0f7"),
        roomName = "Room Name",
        bedId = UUID.fromString("dd8c3e05-c372-4c06-af20-77c6347a403f"),
        bedName = "Bed Name",
        roomCharacteristicPropertyNames = mutableListOf("roomCharacteristic1", "roomCharacteristic2"),
        distance = 32.12
      )
    )

    val result = bedSearchTransformer.domainToApi(domain)

    assertThat(result[0]).isEqualTo(
      ApiBedSearchResult(
        premisesId = UUID.fromString("323706f4-5054-452a-aa4a-ecc075ebc9ac"),
        premisesName = "Premises Name",
        premisesCharacteristicPropertyNames = mutableListOf("propertyCharacteristic1", "propertyCharacteristic2"),
        bedId = UUID.fromString("dd8c3e05-c372-4c06-af20-77c6347a403f"),
        bedName = "Bed Name",
        roomCharacteristicPropertyNames = mutableListOf("roomCharacteristic1", "roomCharacteristic2"),
        distanceMiles = BigDecimal("32.12")
      )
    )
  }
}
