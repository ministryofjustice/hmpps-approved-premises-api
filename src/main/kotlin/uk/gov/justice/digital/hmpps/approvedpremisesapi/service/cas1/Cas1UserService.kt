package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository

@Service
class Cas1UserService(
  val userRepository: UserRepository,
) {
  fun findFutureManagersByStaffCode(staffCodes: List<String>) = userRepository.findFutureManagersByStaffCode(staffCodes.map { it.uppercase() })
}
