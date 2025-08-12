package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService

class Cas1BackfillKeyWorkerUserAssignmentsJobTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Test
  fun `backfill applications correctly`() {
    val booking1NoKeyWorker = givenACas1SpaceBooking()

    val (keyWorker2User, _) = givenAUser(staffDetail = StaffDetailFactory.staffDetail(code = "staffCode1"))
    val booking2KeyWorkerStaffCodeSet = givenACas1SpaceBooking(
      keyWorkerStaffCode = keyWorker2User.deliusStaffCode.uppercase(),
      keyWorkerUser = null,
    )

    val (keyWorker3User, _) = givenAUser(staffDetail = StaffDetailFactory.staffDetail(code = "STAFFCODE2"))
    val booking3KeyWorkerStaffCodeSet = givenACas1SpaceBooking(
      keyWorkerStaffCode = keyWorker3User.deliusStaffCode.uppercase(),
      keyWorkerUser = null,
    )

    val (keyWorker4User, _) = givenAUser(staffDetail = StaffDetailFactory.staffDetail(code = "staffcode3"))
    val booking4KeyWorkerUserAlreadySet = givenACas1SpaceBooking(
      keyWorkerStaffCode = keyWorker4User.deliusStaffCode,
      keyWorkerUser = keyWorker4User,
    )

    migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillKeyWorkerUserAssignments)

    assertThat(
      cas1SpaceBookingRepository.findByIdOrNull(booking1NoKeyWorker.id)!!.keyWorkerUser,
    ).isNull()

    assertThat(
      cas1SpaceBookingRepository.findByIdOrNull(booking2KeyWorkerStaffCodeSet.id)!!.keyWorkerUser!!.id,
    ).isEqualTo(keyWorker2User.id)

    assertThat(
      cas1SpaceBookingRepository.findByIdOrNull(booking3KeyWorkerStaffCodeSet.id)!!.keyWorkerUser!!.id,
    ).isEqualTo(keyWorker3User.id)

    assertThat(
      cas1SpaceBookingRepository.findByIdOrNull(booking4KeyWorkerUserAlreadySet.id)!!.keyWorkerUser!!.id,
    ).isEqualTo(keyWorker4User.id)
  }
}
