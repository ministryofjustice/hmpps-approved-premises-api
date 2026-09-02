package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserMergeService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

@ExtendWith(MockKExtension::class)
class UserMergeServiceTest {

  @RelaxedMockK
  private lateinit var userRepository: UserRepository

  @RelaxedMockK
  private lateinit var userService: UserService

  @InjectMockKs
  private lateinit var userMergeService: UserMergeService

  companion object {
    const val OLD_USERNAME = "old_user"
    const val NEW_USERNAME = "new_user"
  }

  @Nested
  inner class MergeUser {

    @Test
    fun `error if usernames provided are the same`() {
      assertThatThrownBy {
        userMergeService.mergeUser(
          OLD_USERNAME,
          OLD_USERNAME,
        )
      }.hasMessage("Usernames provided to merge (OLD_USER and OLD_USER) should not be the same")
    }

    @Test
    fun `old user doesn't exist, error`() {
      every {
        userRepository.findByDeliusUsername(OLD_USERNAME.uppercase())
      } returns null

      assertThatThrownBy {
        userMergeService.mergeUser(
          OLD_USERNAME,
          NEW_USERNAME,
        )
      }.hasMessage("Cannot find a user with username old_user")
    }

    @Test
    fun `new user doesn't exist, continue merge`() {
      val oldUser = UserEntityFactory().withDefaults().withDeliusUsername(OLD_USERNAME).produce()

      every {
        userRepository.findByDeliusUsername(OLD_USERNAME.uppercase())
      } returns oldUser

      every {
        userRepository.findByDeliusUsername(NEW_USERNAME.uppercase())
      } returns null

      every {
        userRepository.saveAndFlush(any())
      } returnsArgument 0

      every {
        userService.updateUserFromDelius(oldUser)
      } returns UserService.GetUserResponse.Success(oldUser)

      userMergeService.mergeUser(
        OLD_USERNAME,
        NEW_USERNAME,
      )

      val updatedUserSlot = slot<UserEntity>()
      verify {
        userRepository.saveAndFlush(capture(updatedUserSlot))
      }

      assertThat(updatedUserSlot.captured.id).isEqualTo(oldUser.id)
      assertThat(updatedUserSlot.captured.deliusUsername).isEqualTo(NEW_USERNAME.uppercase())

      verify {
        userService.updateUserFromDelius(oldUser)
      }

      verify(exactly = 0) { userRepository.delete(any<UserEntity>()) }
    }

    @Test
    fun `new user exists, delete as part of merge`() {
      val oldUser = UserEntityFactory().withDefaults().withDeliusUsername(OLD_USERNAME).produce()
      every {
        userRepository.findByDeliusUsername(OLD_USERNAME.uppercase())
      } returns oldUser

      val newUser = UserEntityFactory().withDefaults().withDeliusUsername(NEW_USERNAME).produce()
      every {
        userRepository.findByDeliusUsername(NEW_USERNAME.uppercase())
      } returns newUser

      every {
        userRepository.saveAndFlush(any())
      } returnsArgument 0

      every {
        userService.updateUserFromDelius(oldUser)
      } returns UserService.GetUserResponse.Success(oldUser)

      userMergeService.mergeUser(
        OLD_USERNAME,
        NEW_USERNAME,
      )

      val updatedUserSlot = slot<UserEntity>()
      verify {
        userRepository.saveAndFlush(capture(updatedUserSlot))
      }

      assertThat(updatedUserSlot.captured.id).isEqualTo(oldUser.id)
      assertThat(updatedUserSlot.captured.deliusUsername).isEqualTo(NEW_USERNAME.uppercase())

      verify {
        userService.updateUserFromDelius(oldUser)
      }

      verify {
        userRepository.delete(newUser)
      }
    }
  }

  @Test
  fun `error if non-success response from update`() {
    val oldUser = UserEntityFactory().withDefaults().withDeliusUsername(OLD_USERNAME).produce()

    every {
      userRepository.findByDeliusUsername(OLD_USERNAME.uppercase())
    } returns oldUser

    every {
      userRepository.findByDeliusUsername(NEW_USERNAME.uppercase())
    } returns null

    every {
      userRepository.saveAndFlush(any())
    } returnsArgument 0

    every {
      userService.updateUserFromDelius(oldUser)
    } returns UserService.GetUserResponse.StaffRecordNotFound

    assertThatThrownBy {
      userMergeService.mergeUser(
        OLD_USERNAME,
        NEW_USERNAME,
      )
    }.hasMessage("Error updating user StaffRecordNotFound")
  }
}
