package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RoomTransformer

class RoomTransformerTest {
  private val bedTransformer = mockk<BedTransformer>()
  private val characteristicTransformer = mockk<CharacteristicTransformer>()
  private val roomTransformer = RoomTransformer(bedTransformer, characteristicTransformer)

  @Test
  fun `transformToApi transforms correctly - includes code`() {
    val approvedPremisesEntityFactory = ApprovedPremisesEntityFactory()
    val premises = approvedPremisesEntityFactory
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val roomEntityFactory = RoomEntityFactory()

    val room = roomEntityFactory
      .withPremises(premises)
      .withCode("NEABC-1")
      .withName("Some name")
      .withNotes("This room is large")
      .produce()

    val result = roomTransformer.transformJpaToApi(room)

    assertThat(result.code).isEqualTo("NEABC-1")
    assertThat(result.name).isEqualTo("Some name")
    assertThat(result.notes).isEqualTo("This room is large")
  }
}
