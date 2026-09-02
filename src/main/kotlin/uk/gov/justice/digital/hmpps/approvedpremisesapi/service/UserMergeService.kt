package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository

@Service
class UserMergeService(
  private val userService: UserService,
  private val userRepository: UserRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  /**
   * Any roles/qualifications allocated to an existing entry with the
   * new username will not be transferred to the entry with the old username
   *
   * This function will fail if there are any foreign keys linked to the
   * new UserEntity ID. In this case those foreign keys would need 're-pointing'
   * to the 'old' user record to which the new username is being added   */
  @Transactional
  fun mergeUser(
    oldDeliusUsername: String,
    newDeliusUsername: String,
  ) {
    val oldUsernameNormalised = oldDeliusUsername.uppercase().trim()
    val newUsernameNormalised = newDeliusUsername.uppercase().trim()

    if (oldUsernameNormalised == newUsernameNormalised) {
      error("Usernames provided to merge ($oldUsernameNormalised and $newUsernameNormalised) should not be the same")
    }

    val oldUser = requireNotNull(userRepository.findByDeliusUsername(oldUsernameNormalised)) {
      "Cannot find a user with username $oldDeliusUsername"
    }

    val newUser = userRepository.findByDeliusUsername(newUsernameNormalised)

    if (newUser != null) {
      log.info("Removing user $newUsernameNormalised (${newUser.id}) as will be merged into existing record $oldUsernameNormalised (${oldUser.id})")
      userRepository.delete(newUser)
      // flush required to avoid unique constraints when reassigning the username
      userRepository.flush()
    } else {
      log.info("No record exists to remove for new username $newUsernameNormalised")
    }

    oldUser.deliusUsername = newUsernameNormalised
    log.info("Updating username for user ${oldUser.id} from $oldUsernameNormalised to $newUsernameNormalised")
    userRepository.saveAndFlush(oldUser)

    log.info("Refreshing user info for user ${oldUser.id}")
    userService.updateUserFromDelius(oldUser.id)
  }
}
