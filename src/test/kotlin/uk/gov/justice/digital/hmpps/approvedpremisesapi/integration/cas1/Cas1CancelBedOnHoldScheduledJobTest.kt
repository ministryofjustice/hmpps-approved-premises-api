package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.scheduled.Cas1CancelBedOnHoldScheduledJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1CancelBedOnHoldScheduledJobTest : IntegrationTestBase() {

  @Autowired
  lateinit var cas1CancelBedOnHoldScheduledJob: Cas1CancelBedOnHoldScheduledJob

  @Test
  fun `autoCancelBedOnHoldsPastStartDate works across multiple premises`() {
    givenAUser { user, _ ->
      val reason = cas1OutOfServiceBedReasonTestRepository.getReferenceById(Cas1OutOfServiceBedReasonRepository.BED_ON_HOLD_REASON_ID)

      val premises1 = givenAnApprovedPremises(supportsSpaceBookings = true)
      val bed1 = bedEntityFactory.produceAndPersist {
        withRoom(roomEntityFactory.produceAndPersist { withPremises(premises1) })
      }
      val boh1 = createBedOnHold(bed1, user, LocalDate.now().minusDays(2), LocalDate.now().plusDays(10), reason)

      val premises2 = givenAnApprovedPremises(supportsSpaceBookings = true)
      val bed2 = bedEntityFactory.produceAndPersist {
        withRoom(roomEntityFactory.produceAndPersist { withPremises(premises2) })
      }
      val boh2 = createBedOnHold(bed2, user, LocalDate.now().minusDays(3), LocalDate.now().plusDays(10), reason)

      // non-conflicting dates for the same bed
      createBedOnHold(bed2, user, LocalDate.now().plusMonths(3), LocalDate.now().plusMonths(5), reason)

      val alreadyCancelled = createBedOnHold(bed1, user, LocalDate.now().minusDays(1), LocalDate.now().plusDays(10), reason)
      cas1OutOfServiceBedCancellationEntityFactory.produceAndPersist {
        withOutOfServiceBed(alreadyCancelled)
        withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
      }

      cas1CancelBedOnHoldScheduledJob.autoCancelBedOnHoldsPastStartDate()

      val cancelledBeds = cas1OutOfServiceBedCancellationTestRepository.findAll().toList()

      assertThat(cancelledBeds)
        .extracting<UUID> { it.outOfServiceBed.id }
        .containsExactlyInAnyOrder(
          boh1.id,
          boh2.id,
          alreadyCancelled.id,
        )
    }
  }

  private fun createBedOnHold(
    bed: BedEntity,
    user: UserEntity,
    startDate: LocalDate,
    endDate: LocalDate,
    reason: Cas1OutOfServiceBedReasonEntity,
  ) = cas1OutOfServiceBedEntityFactory.produceAndPersist {
    withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
    withBed(bed)
  }.apply {
    cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
      withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
      withCreatedBy(user)
      withOutOfServiceBed(this@apply)
      withStartDate(startDate)
      withEndDate(endDate)
      withReason(reason)
    }
  }
}
