package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationBedSearchResultFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CharacteristicNames
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedSearchResultTransformer
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchResult as ApiTemporaryAccommodationBedSearchResult

class BedSearchResultTransformerTest {
  private val bedSearchTransformer = BedSearchResultTransformer()

  @Test
  fun `transformDomainToApi transforms correctly for Temporary Accommodation results`() {
    val premisesOne = UUID.fromString("e17d7c19-dabb-4c6d-83b9-ada34d290b56")
    val premisesTwo = UUID.fromString("d9a81794-7000-4438-95e8-9f4aa7de5028")

    val roomOne = UUID.fromString("3d76ea31-e04d-4152-9c56-4e26b15306e6")
    val roomTwo = UUID.fromString("d856ecdd-89b3-4ddb-83a6-34914d39d3f6")
    val roomThree = UUID.fromString("3b805d1f-5632-4e77-87e3-34938cf9ade2")

    val domainResults = listOf(
      TemporaryAccommodationBedSearchResultFactory()
        .withPremisesId(premisesOne)
        .withRoomId(roomOne)
        .withPremisesBedCount(6)
        .produce(),
      TemporaryAccommodationBedSearchResultFactory()
        .withPremisesId(premisesOne)
        .withRoomId(roomTwo)
        .withPremisesBedCount(6)
        .produce(),
      TemporaryAccommodationBedSearchResultFactory()
        .withPremisesId(premisesTwo)
        .withRoomId(roomThree)
        .withPremisesBedCount(6)
        .produce(),
      TemporaryAccommodationBedSearchResultFactory()
        .withPremisesId(premisesTwo)
        .withRoomId(roomThree)
        .withPremisesBedCount(6)
        .produce(),
    )

    val result = bedSearchTransformer.transformDomainToApi(domainResults)

    assertThat(result.resultsBedCount).isEqualTo(4)
    assertThat(result.resultsPremisesCount).isEqualTo(2)
    assertThat(result.resultsRoomCount).isEqualTo(3)

    result.results.forEachIndexed { index, it ->
      val domainResult = domainResults[index]

      assertThat(domainResult.premisesId).isEqualTo(it.premises.id)
      assertThat(domainResult.premisesName).isEqualTo(it.premises.name)
      assertThat(domainResult.premisesAddressLine1).isEqualTo(it.premises.addressLine1)
      assertThat(domainResult.premisesAddressLine2).isEqualTo(it.premises.addressLine2)
      assertThat(domainResult.premisesTown).isEqualTo(it.premises.town)
      assertThat(domainResult.premisesPostcode).isEqualTo(it.premises.postcode)
      assertThat(domainResult.premisesCharacteristics).isEqualTo(it.premises.characteristics.map { c -> CharacteristicNames(c.propertyName, c.name) })
      assertThat(domainResult.premisesBedCount).isEqualTo(it.premises.bedCount)
      assertThat(domainResult.roomId).isEqualTo(it.room.id)
      assertThat(domainResult.roomName).isEqualTo(it.room.name)
      assertThat(domainResult.bedId).isEqualTo(it.bed.id)
      assertThat(domainResult.bedName).isEqualTo(it.bed.name)
      assertThat(domainResult.roomCharacteristics).isEqualTo(it.room.characteristics.map { c -> CharacteristicNames(c.propertyName, c.name) })
      assertThat(domainResult.overlaps).isEqualTo((it as ApiTemporaryAccommodationBedSearchResult).overlaps)
    }
  }
}
