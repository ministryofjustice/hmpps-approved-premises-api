package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.migration

import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.Cas3VoidBedspaceMigrationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas3VoidBedspaceJobTest : MigrationJobTestBase() {

  @SpykBean
  private lateinit var cas3VoidBedspacesRepository: Cas3VoidBedspaceMigrationRepository

  @Autowired
  private lateinit var cas3BedspacesRepository: Cas3BedspacesRepository

  private fun createPremises(user: UserEntity): TemporaryAccommodationPremisesEntity {
    val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(user.probationRegion)
    }

    return temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { user.probationRegion }
      withProbationDeliveryUnit(probationDeliveryUnit)
    }
  }

  private fun createVoidBedspaces(
    premises: TemporaryAccommodationPremisesEntity,
    amount: Int,
  ): List<Cas3VoidBedspaceEntity> {
    val voidBedspaces = mutableListOf<Cas3VoidBedspaceEntity>()
    repeat(amount) {
      voidBedspaces += cas3VoidBedspaceEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().plusDays(2))
        withEndDate(LocalDate.now().plusDays(4))
        withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        withBed(
          bedEntityFactory.produceAndPersist {
            withRoom(
              roomEntityFactory.produceAndPersist {
                withPremises(premises)
              },
            )
          },
        )
        withPremises(premises)
      }
    }
    return voidBedspaces
  }

  @Test
  fun `all cancellation data is added to void bedspaces`() {
    givenAUser { user, _ ->

      val premises = createPremises(user)
      val voidBedspaces = createVoidBedspaces(premises, 50)

      // this job needs to have ran first.
      migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceModelData)
      val bedspaces = cas3BedspacesRepository.findAll().filter { it.premises.id == premises.id }
      assertThat(bedspaces).hasSize(voidBedspaces.size)

      val bedspacesToCancel = voidBedspaces.asSequence().shuffled().take(20).toList()

      // cancel some voids
      bedspacesToCancel.forEach {
        cas3VoidBedspaceCancellationEntityFactory.produceAndPersist {
          withVoidBedspace(it)
          withNotes(randomStringMultiCaseWithNumbers(50))
          withCreatedAt(OffsetDateTime.now())
        }
      }

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3VoidBedspaceData, 10)

      val migratedBedspaces = cas3VoidBedspacesRepository.findAll()

      assertThat(migratedBedspaces.filter { it.cancellation == null }).hasSize(30)

      val cancelledVoidBedspaces = migratedBedspaces.filter { it.cancellation != null }.map { it.id }
      assertThat(cancelledVoidBedspaces).hasSize(20)
      assertThat(cancelledVoidBedspaces).containsAll(bedspacesToCancel.map { it.id }.toList().sorted())

      migratedBedspaces.forEach {
        assertThat(it.bedspace!!.id).isEqualTo(it.bed!!.id)
        if (it.cancellation == null) {
          assertThat(it.cancellationDate).isNull()
          assertThat(it.cancellationNotes).isNull()
        } else {
          assertThat(it.cancellationDate).isNotNull
          assertThat(it.notes).isNotNull
        }
      }

      // should be called 5 times - 50 records
      verify(exactly = 5) { cas3VoidBedspacesRepository.saveAllAndFlush<Cas3VoidBedspaceEntity>(any()) }
    }
  }
}
