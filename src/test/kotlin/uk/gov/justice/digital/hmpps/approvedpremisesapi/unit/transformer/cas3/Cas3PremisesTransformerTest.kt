package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LocalAuthorityAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationDeliveryUnitTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationRegionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class Cas3PremisesTransformerTest {
  private val probationRegionTransformer = mockk<ProbationRegionTransformer>()
  private val localAuthorityAreaTransformer = mockk<LocalAuthorityAreaTransformer>()
  private val characteristicTransformer = mockk<CharacteristicTransformer>()
  private val probationDeliveryUnitTransformer = mockk<ProbationDeliveryUnitTransformer>()
  private val cas3PremisesTransformer = Cas3PremisesTransformer(
    probationRegionTransformer,
    localAuthorityAreaTransformer,
    probationDeliveryUnitTransformer,
    characteristicTransformer,
  )

  @Test
  fun `transformDomainToApi transforms TemporaryAccommodationPremisesEntity to Cas3Premises correctly`() {
    val probationRegionEntity = ProbationRegionEntityFactory()
      .withDefaults()
      .produce()
    val localAuthorityAreaEntity = LocalAuthorityAreaEntityFactory().produce()
    val probationDeliveryUnitEntity = ProbationDeliveryUnitEntityFactory()
      .withProbationRegion(probationRegionEntity)
      .produce()
    val characteristics = mutableListOf(CharacteristicEntityFactory().produce())
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withProbationRegion(probationRegionEntity)
      .withLocalAuthorityArea(localAuthorityAreaEntity)
      .withProbationDeliveryUnit(probationDeliveryUnitEntity)
      .withCharacteristics(characteristics)
      .withStatus(PropertyStatus.active)
      .produce()

    // online bedspaces
    createRoomWithOneBedspace(premises, LocalDate.now().minusDays(5), null)
    createRoomWithOneBedspace(premises, LocalDate.now().minusDays(210), LocalDate.now().plusDays(10))

    // upcoming bedspaces
    createRoomWithOneBedspace(premises, LocalDate.now().plusDays(10), null)
    createRoomWithOneBedspace(premises, LocalDate.now().plusDays(12), LocalDate.now().plusDays(192))

    // archived bedspaces
    createRoomWithOneBedspace(premises, LocalDate.now().minusDays(360), LocalDate.now().minusDays(31))

    val probationRegion = ProbationRegion(UUID.randomUUID(), "probationRegion")
    every { probationRegionTransformer.transformJpaToApi(probationRegionEntity) } returns probationRegion

    val localAuthorityArea = LocalAuthorityArea(UUID.randomUUID(), "identifier", "name")
    every { localAuthorityAreaTransformer.transformJpaToApi(localAuthorityAreaEntity) } returns localAuthorityArea

    val probationDeliveryUnit = ProbationDeliveryUnit(UUID.randomUUID(), "pduName")
    every { probationDeliveryUnitTransformer.transformJpaToApi(probationDeliveryUnitEntity) } returns probationDeliveryUnit

    val characteristic = Characteristic(
      UUID.randomUUID(),
      "name",
      Characteristic.ServiceScope.temporaryMinusAccommodation,
      Characteristic.ModelScope.premises,
      randomStringMultiCaseWithNumbers(10),
    )
    every { characteristicTransformer.transformJpaToApi(characteristics[0]) } returns characteristic

    val result = cas3PremisesTransformer.transformDomainToApi(premises)

    assertThat(result).isEqualTo(
      Cas3Premises(
        id = premises.id,
        reference = premises.name,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        town = premises.town,
        postcode = premises.postcode,
        localAuthorityArea = localAuthorityArea,
        probationRegion = probationRegion,
        probationDeliveryUnit = probationDeliveryUnit,
        characteristics = listOf(characteristic),
        startDate = premises.startDate,
        status = Cas3PremisesStatus.online,
        notes = premises.notes,
        turnaroundWorkingDays = premises.turnaroundWorkingDays,
        totalOnlineBedspaces = 2,
        totalUpcomingBedspaces = 2,
        totalArchivedBedspaces = 1,
      ),
    )
  }

  @Test
  fun `transformDomainToApi transforms TemporaryAccommodationPremisesEntity to Cas3Premises correctly without optional elements`() {
    val probationRegionEntity = ProbationRegionEntityFactory()
      .withDefaults()
      .produce()
    val probationDeliveryUnitEntity = ProbationDeliveryUnitEntityFactory()
      .withProbationRegion(probationRegionEntity)
      .produce()
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withProbationRegion(probationRegionEntity)
      .withProbationDeliveryUnit(probationDeliveryUnitEntity)
      .withStatus(PropertyStatus.active)
      .produce()

    // online bedspaces
    createRoomWithOneBedspace(premises, LocalDate.now().minusDays(7), null)
    createRoomWithOneBedspace(premises, LocalDate.now().minusDays(130), LocalDate.now().plusDays(19))

    // upcoming bedspaces
    createRoomWithOneBedspace(premises, LocalDate.now().plusDays(1), null)

    val probationRegion = ProbationRegion(UUID.randomUUID(), "probationRegion")
    every { probationRegionTransformer.transformJpaToApi(probationRegionEntity) } returns probationRegion

    val probationDeliveryUnit = ProbationDeliveryUnit(UUID.randomUUID(), "pduName")
    every { probationDeliveryUnitTransformer.transformJpaToApi(probationDeliveryUnitEntity) } returns probationDeliveryUnit

    val result = cas3PremisesTransformer.transformDomainToApi(premises)

    assertThat(result).isEqualTo(
      Cas3Premises(
        id = premises.id,
        reference = premises.name,
        addressLine1 = premises.addressLine1,
        addressLine2 = null,
        town = null,
        postcode = premises.postcode,
        localAuthorityArea = null,
        probationRegion = probationRegion,
        probationDeliveryUnit = probationDeliveryUnit,
        characteristics = emptyList(),
        startDate = premises.startDate,
        status = Cas3PremisesStatus.online,
        notes = premises.notes,
        turnaroundWorkingDays = 2,
        totalOnlineBedspaces = 2,
        totalUpcomingBedspaces = 1,
        totalArchivedBedspaces = 0,
      ),
    )
  }

  private fun createRoomWithOneBedspace(premises: PremisesEntity, startDate: LocalDate, endDate: LocalDate?): RoomEntity {
    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()
      .also {
        premises.rooms.add(it)
      }

    BedEntityFactory()
      .withRoom(room)
      .withStartDate(startDate)
      .withEndDate(endDate)
      .produce()
      .also {
        room.beds.add(it)
      }

    return room
  }
}
