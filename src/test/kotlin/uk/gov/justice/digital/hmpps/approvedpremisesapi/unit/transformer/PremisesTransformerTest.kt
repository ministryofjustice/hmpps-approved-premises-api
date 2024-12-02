package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LocalAuthorityAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationDeliveryUnitTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationRegionTransformer
import java.util.UUID

class PremisesTransformerTest {

  private val probationRegionTransformer = mockk<ProbationRegionTransformer>()
  private val apAreaTransformer = mockk<ApAreaTransformer>()
  private val localAuthorityAreaTransformer = mockk<LocalAuthorityAreaTransformer>()
  private val characteristicTransformer = mockk<CharacteristicTransformer>()
  private val probationDeliveryUnitTransformer = mockk<ProbationDeliveryUnitTransformer>()

  private val service = PremisesTransformer(
    probationRegionTransformer,
    apAreaTransformer,
    localAuthorityAreaTransformer,
    characteristicTransformer,
    probationDeliveryUnitTransformer,
  )

  @Test
  fun `transformJpaToApi CAS1`() {
    val id = UUID.randomUUID()
    val apAreaEntity = ApAreaEntityFactory().produce()
    val probationRegionEntity = ProbationRegionEntityFactory()
      .withDefaults()
      .withApArea(apAreaEntity)
      .produce()
    val localAuthorityAreaEntity = LocalAuthorityAreaEntityFactory().produce()
    val characteristics = mutableListOf(CharacteristicEntityFactory().produce())

    val approvedPremisesEntity = ApprovedPremisesEntityFactory()
      .withDefaults()
      .withId(id)
      .withName("theName")
      .withApCode("theApCode")
      .withAddressLine1("theAddressLine1")
      .withAddressLine2("theAddressLine2")
      .withTown("theTown")
      .withPostcode("thePostcode")
      .withNotes("theNotes")
      .withProbationRegion(probationRegionEntity)
      .withLocalAuthorityArea(localAuthorityAreaEntity)
      .withCharacteristics(characteristics)
      .withStatus(PropertyStatus.ACTIVE)
      .produce()

    val probationRegion = ProbationRegion(UUID.randomUUID(), "probationRegion")
    every { probationRegionTransformer.transformJpaToApi(probationRegionEntity) } returns probationRegion

    val apArea = ApArea(UUID.randomUUID(), "someIdentifier", "someName")
    every { apAreaTransformer.transformJpaToApi(apAreaEntity) } returns apArea

    val localAuthorityArea = LocalAuthorityArea(UUID.randomUUID(), "identifier", "name")
    every { localAuthorityAreaTransformer.transformJpaToApi(localAuthorityAreaEntity) } returns localAuthorityArea

    val characteristic = Characteristic(
      UUID.randomUUID(),
      "name",
      Characteristic.ServiceScope.star,
      Characteristic.ModelScope.premises,
      "propertyName",
    )
    every { characteristicTransformer.transformJpaToApi(characteristics[0]) } returns characteristic

    val result = service.transformJpaToApi(
      jpa = approvedPremisesEntity,
      totalBeds = 2,
      availableBedsForToday = 3,
    ) as ApprovedPremises

    assertThat(result.id).isEqualTo(id)
    assertThat(result.name).isEqualTo("theName")
    assertThat(result.apCode).isEqualTo("theApCode")
    assertThat(result.addressLine1).isEqualTo("theAddressLine1")
    assertThat(result.addressLine2).isEqualTo("theAddressLine2")
    assertThat(result.town).isEqualTo("theTown")
    assertThat(result.postcode).isEqualTo("thePostcode")
    assertThat(result.bedCount).isEqualTo(2)
    assertThat(result.service).isEqualTo("approved-premises")
    assertThat(result.notes).isEqualTo("theNotes")
    assertThat(result.availableBedsForToday).isEqualTo(3)
    assertThat(result.probationRegion).isEqualTo(probationRegion)
    assertThat(result.apArea).isEqualTo(apArea)
    assertThat(result.localAuthorityArea).isEqualTo(localAuthorityArea)
    assertThat(result.characteristics!![0]).isEqualTo(characteristic)
    assertThat(result.status).isEqualTo(PropertyStatus.ACTIVE)
  }
}
