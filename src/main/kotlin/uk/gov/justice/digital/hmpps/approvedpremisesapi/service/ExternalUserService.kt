package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserRepository
import java.util.UUID

@Service
class ExternalUserService(
  private val httpAuthService: HttpAuthService,
  private val userRepository: ExternalUserRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getUserForRequest(): ExternalUserEntity {
    val authenticatedPrincipal = httpAuthService.getCas2AuthenticatedPrincipalOrThrow()
    val username = authenticatedPrincipal.name

    return getUserForUsername(username)
  }

  fun getUserForUsername(username: String): ExternalUserEntity {
    val normalisedUsername = username.uppercase()

    val existingUser = userRepository.findByUsername(normalisedUsername)
    if (existingUser != null) return existingUser

    // We will obtain some further user details from the Manage-Users API
    // e.g. name, email, phone number

    return userRepository.save(
      ExternalUserEntity(
        id = UUID.randomUUID(),
        username = username,
        isEnabled = true,
        origin = "NACRO",
      ),
    )
  }
}
