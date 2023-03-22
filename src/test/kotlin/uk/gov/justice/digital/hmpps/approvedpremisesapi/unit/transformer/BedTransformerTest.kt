package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedTransformer

class BedTransformerTest {
  private val bedTransformer = BedTransformer()

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

    val bedEntityFactory = BedEntityFactory()

    val bed = bedEntityFactory
      .withCode("NEABC01")
      .withName("Some name")
      .withRoom(room)
      .produce()

    val result = bedTransformer.transformJpaToApi(bed)

    Assertions.assertThat(result.code).isEqualTo("NEABC01")
    Assertions.assertThat(result.name).isEqualTo("Some name")
  }
}
