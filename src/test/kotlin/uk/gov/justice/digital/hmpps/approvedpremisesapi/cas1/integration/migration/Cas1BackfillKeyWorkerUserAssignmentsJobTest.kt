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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1KeyWorkerStaffCodeLookupEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1KeyWorkerStaffCodeLookupRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService

class Cas1BackfillKeyWorkerUserAssignmentsJobTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Autowired
  lateinit var cas1KeyWorkerStaffCodeLookupRepository: Cas1KeyWorkerStaffCodeLookupRepository

  @Test
  fun `backfill applications correctly`() {
    val booking1NoKeyWorker = givenACas1SpaceBooking()

    // not future manager, will be ignored
    givenAUser(
      staffDetail = StaffDetailFactory.staffDetail(code = "staffCode1"),
      roles = listOf(),
    )
    val (keyWorker2UserFutureManager, _) = givenAUser(
      staffDetail = StaffDetailFactory.staffDetail(code = "staffCode1"),
      roles = listOf(UserRole.CAS1_FUTURE_MANAGER),
    )
    val booking2KeyWorkerStaffCodeSet = givenACas1SpaceBooking(
      keyWorkerStaffCode = "STAFFCODE1",
      keyWorkerUser = null,
    )

    val (keyWorker3User, _) = givenAUser(
      staffDetail = StaffDetailFactory.staffDetail(code = "STAFFCODE2"),
      roles = listOf(UserRole.CAS1_FUTURE_MANAGER),
    )
    val booking3KeyWorkerStaffCodeSet = givenACas1SpaceBooking(
      keyWorkerStaffCode = "STAFFCODE2",
      keyWorkerUser = null,
    )

    val (keyWorker4User, _) = givenAUser(
      staffDetail = StaffDetailFactory.staffDetail(code = "staffcode3"),

      roles = listOf(UserRole.CAS1_FUTURE_MANAGER),
    )
    val booking4KeyWorkerUserAlreadySet = givenACas1SpaceBooking(
      keyWorkerStaffCode = "staffcode3",
      keyWorkerUser = keyWorker4User,
    )

    // not future manager, will be ignored
    givenAUser(
      staffDetail = StaffDetailFactory.staffDetail(code = "NEWCODE4"),
      roles = listOf(),
    )
    val (keyWorker5UserFutureManager, _) = givenAUser(
      staffDetail = StaffDetailFactory.staffDetail(code = "NEWCODE4"),
      roles = listOf(UserRole.CAS1_FUTURE_MANAGER),
    )
    cas1KeyWorkerStaffCodeLookupRepository.save(
      Cas1KeyWorkerStaffCodeLookupEntity("OldCode4", "NeWCODE4"),
    )
    val booking5KeyWorkerUsingOldCode = givenACas1SpaceBooking(
      keyWorkerStaffCode = "oldcode4",
      keyWorkerUser = keyWorker5UserFutureManager,
    )

    migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillKeyWorkerUserAssignments)

    assertThat(
      cas1SpaceBookingRepository.findByIdOrNull(booking1NoKeyWorker.id)!!.keyWorkerUser,
    ).isNull()

    assertThat(
      cas1SpaceBookingRepository.findByIdOrNull(booking2KeyWorkerStaffCodeSet.id)!!.keyWorkerUser!!.id,
    ).isEqualTo(keyWorker2UserFutureManager.id)

    assertThat(
      cas1SpaceBookingRepository.findByIdOrNull(booking3KeyWorkerStaffCodeSet.id)!!.keyWorkerUser!!.id,
    ).isEqualTo(keyWorker3User.id)

    assertThat(
      cas1SpaceBookingRepository.findByIdOrNull(booking4KeyWorkerUserAlreadySet.id)!!.keyWorkerUser!!.id,
    ).isEqualTo(keyWorker4User.id)

    assertThat(
      cas1SpaceBookingRepository.findByIdOrNull(booking5KeyWorkerUsingOldCode.id)!!.keyWorkerUser!!.id,
    ).isEqualTo(keyWorker5UserFutureManager.id)
  }
}
