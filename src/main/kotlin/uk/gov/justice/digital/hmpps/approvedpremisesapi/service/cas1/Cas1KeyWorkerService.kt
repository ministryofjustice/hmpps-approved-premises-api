package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1KeyWorkerStaffCodeLookupRepository

@Service
class Cas1KeyWorkerService(
  val cas1KeyWorkerStaffCodeLookupRepository: Cas1KeyWorkerStaffCodeLookupRepository,
  val userRepository: UserRepository,
) {

  fun findByDeliusStaffCode(staffCode: String): UserEntity? {
    val staffCodes = listOfNotNull(
      staffCode,
      cas1KeyWorkerStaffCodeLookupRepository.findByStaffCode1(staffCode)?.staffCode2,
    )

    return userRepository.findByDeliusStaffCodeAndRole(
      staffCodes = staffCodes.map { it.uppercase() },
      role = UserRole.CAS1_FUTURE_MANAGER,
    ).firstOrNull()
  }
}
