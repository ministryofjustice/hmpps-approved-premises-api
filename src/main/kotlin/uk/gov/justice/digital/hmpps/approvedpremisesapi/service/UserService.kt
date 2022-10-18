package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import java.util.UUID

@Service
class UserService(
  private val httpAuthService: HttpAuthService,
  private val userRepository: UserRepository
) {
  fun getUserForRequest(): UserEntity {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    // TODO: Make call to Community API for details

    return userRepository.findByDeliusUsername(username)
      ?: userRepository.save(
        UserEntity(
          id = UUID.randomUUID(),
          name = "forenames surname",
          deliusUsername = username,
          deliusStaffIdentifier = 123,
          applications = mutableListOf()
        )
      )
  }
}
