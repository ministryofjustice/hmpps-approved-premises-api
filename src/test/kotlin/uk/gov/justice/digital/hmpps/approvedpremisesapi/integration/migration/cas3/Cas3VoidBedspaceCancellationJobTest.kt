package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas3

import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate

class Cas3VoidBedspaceCancellationJobTest : MigrationJobTestBase() {

  @SpykBean
  private lateinit var cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository

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
      val voidBedspaces = createVoidBedspaces(premises, 25)

      // this job needs to have ran first.
      migrationJobService.runMigrationJob(MigrationJobType.updateCas3BedspaceModelData)
      val bedspaces = cas3BedspacesRepository.findAll().filter { it.premises.id == premises.id }
      assertThat(bedspaces).hasSize(voidBedspaces.size)

      val bedspacesToCancel = voidBedspaces.take(20)

      // cancel some voids
      bedspacesToCancel.forEach {
        cas3VoidBedspaceCancellationEntityFactory.produceAndPersist {
          withVoidBedspace(it)
          withNotes(randomStringMultiCaseWithNumbers(50))
        }
      }

      migrationJobService.runMigrationJob(MigrationJobType.updateCas3VoidBedspaceCancellationData, 10)

      val cancelledVoidBedspaces = cas3VoidBedspacesRepository.findAll().filter { it.cancellation != null }
      assertThat(cancelledVoidBedspaces).hasSize(20)
      cancelledVoidBedspaces.forEach {
        assertThat(it.cancellationDate).isEqualTo(it.cancellation!!.createdAt)
        assertThat(it.cancellationNotes).isEqualTo(it.cancellation!!.notes)
        assertThat(it.bedspace!!.id).isEqualTo(it.bed.id)
      }

      // should be called twice - 20 cancelled bedspaces with a page size of 10
      verify(exactly = 2) { cas3VoidBedspacesRepository.saveAllAndFlush<Cas3VoidBedspaceEntity>(any()) }
    }
  }
}
