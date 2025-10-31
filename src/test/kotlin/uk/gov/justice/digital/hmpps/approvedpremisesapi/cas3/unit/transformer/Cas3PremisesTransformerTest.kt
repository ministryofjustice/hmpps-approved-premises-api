package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesCharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesArchiveAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LocalAuthorityAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationDeliveryUnitTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationRegionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomEmailAddress
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3PremisesTransformerTest {
  private val probationRegionTransformer = mockk<ProbationRegionTransformer>()
  private val localAuthorityAreaTransformer = mockk<LocalAuthorityAreaTransformer>()
  private val probationDeliveryUnitTransformer = mockk<ProbationDeliveryUnitTransformer>()
  private val cas3PremisesTransformer = Cas3PremisesTransformer(
    probationRegionTransformer,
    localAuthorityAreaTransformer,
    probationDeliveryUnitTransformer,
  )

  @Nested
  inner class V2 {
    private val probationRegionEntity = ProbationRegionEntityFactory()
      .withDefaults()
      .produce()
    private val localAuthorityAreaEntity = LocalAuthorityAreaEntityFactory().produce()
    private val probationDeliveryUnitEntity = ProbationDeliveryUnitEntityFactory()
      .withProbationRegion(probationRegionEntity)
      .produce()

    private val characteristics = mutableListOf(Cas3PremisesCharacteristicEntityFactory().produce())

    private val startDate = LocalDate.now().minusDays(1)
    private val endDate = LocalDate.now().plusMonths(3)
    private val lastUpdatedAt = OffsetDateTime.now()

    private val premises = Cas3PremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityAreaEntity)
      .withProbationDeliveryUnit(probationDeliveryUnitEntity)
      .withCharacteristics(characteristics)
      .withStatus(Cas3PremisesStatus.online)
      .withStartDate(startDate)
      .withEndDate(endDate)
      .withLastUpdatedAt(lastUpdatedAt)
      .produce()

    private val archiveHistory = listOf(
      Cas3PremisesArchiveAction(
        status = Cas3PremisesStatus.online,
        date = premises.startDate,
      ),
      Cas3PremisesArchiveAction(
        status = Cas3PremisesStatus.archived,
        date = endDate,
      ),
    )

    private val probationRegion = ProbationRegion(UUID.randomUUID(), "probationRegion", randomEmailAddress())
    private val localAuthorityArea = LocalAuthorityArea(UUID.randomUUID(), "identifier", "name")
    private val probationDeliveryUnit = ProbationDeliveryUnit(UUID.randomUUID(), "pduName")

    @BeforeEach
    fun setup() {
      every { probationRegionTransformer.transformJpaToApi(probationRegionEntity) } returns probationRegion
      every { localAuthorityAreaTransformer.transformJpaToApi(localAuthorityAreaEntity) } returns localAuthorityArea
      every { probationDeliveryUnitTransformer.transformJpaToApi(probationDeliveryUnitEntity) } returns probationDeliveryUnit
    }

    @Test
    fun `toCas3Premises transforms Cas3PremisesEntity to Cas3Premises correctly`() {
      premises.bedspaces = mutableListOf(
        Cas3BedspaceEntityFactory().archivedBedspace(premises),
        Cas3BedspaceEntityFactory().onlineBedspace(premises),
        Cas3BedspaceEntityFactory().onlineBedspace(premises),
        Cas3BedspaceEntityFactory().onlineBedspace(premises),
        Cas3BedspaceEntityFactory().upcomingBedspace(premises),
        Cas3BedspaceEntityFactory().upcomingBedspace(premises),
      )

      val result = cas3PremisesTransformer.toCas3Premises(
        premises = premises,
        archiveHistory = archiveHistory,
      )

      assertAll({
        assertThat(result.id).isEqualTo(premises.id)
        assertThat(result.reference).isEqualTo(premises.name)
        assertThat(result.addressLine1).isEqualTo(premises.addressLine1)
        assertThat(result.addressLine2).isEqualTo(premises.addressLine2)
        assertThat(result.town).isEqualTo(premises.town)
        assertThat(result.postcode).isEqualTo(premises.postcode)
        assertThat(result.localAuthorityArea).isEqualTo(localAuthorityArea)
        assertThat(result.probationRegion).isEqualTo(probationRegion)
        assertThat(result.probationDeliveryUnit).isEqualTo(probationDeliveryUnit)
        assertThat(result.characteristics).isNull()
        assertThat(result.premisesCharacteristics)
          .containsAll(characteristics.map { it.toCas3PremisesCharacteristic() })
        assertThat(result.startDate).isEqualTo(startDate)
        assertThat(result.endDate).isEqualTo(endDate)
        assertThat(result.status).isEqualTo(Cas3PremisesStatus.online)
        assertThat(result.notes).isEqualTo(premises.notes)
        assertThat(result.turnaroundWorkingDays).isEqualTo(premises.turnaroundWorkingDays)
        assertThat(result.totalOnlineBedspaces).isEqualTo(3)
        assertThat(result.totalUpcomingBedspaces).isEqualTo(2)
        assertThat(result.totalArchivedBedspaces).isEqualTo(1)
        assertThat(result.archiveHistory).isEqualTo(archiveHistory)
        assertThat(result.scheduleUnarchiveDate).isNull() // will not be populated if premises start date is < today
      })
    }

    @Test
    fun `scheduleUnarchiveDate if populated with start date if start date is after today`() {
      val startDate = LocalDate.now().plusDays(1)
      val result = cas3PremisesTransformer.toCas3Premises(
        premises = premises.copy(startDate = startDate, status = Cas3PremisesStatus.archived),
        archiveHistory = archiveHistory,
      )

      assertThat(result.status).isEqualTo(Cas3PremisesStatus.archived)
      assertThat(result.scheduleUnarchiveDate).isEqualTo(startDate)
    }

    @Test
    fun `toCas3Premises transforms Cas3PremisesEntity to Cas3Premises correctly without optional elements`() {
      val result = cas3PremisesTransformer.toCas3Premises(
        premises = premises.copy(
          addressLine2 = null,
          town = null,
          localAuthorityArea = null,
          characteristics = mutableListOf(),
          endDate = null,

        ),
        archiveHistory = archiveHistory,
      )

      assertAll({
        assertThat(result.addressLine2).isNull()
        assertThat(result.town).isNull()
        assertThat(result.localAuthorityArea).isNull()
        assertThat(result.characteristics).isNull()
        assertThat(result.premisesCharacteristics).isEmpty()
        assertThat(result.endDate).isNull()
        assertThat(result.scheduleUnarchiveDate).isNull() // will not be populated if premises start date is < today
      })
    }
  }
}
