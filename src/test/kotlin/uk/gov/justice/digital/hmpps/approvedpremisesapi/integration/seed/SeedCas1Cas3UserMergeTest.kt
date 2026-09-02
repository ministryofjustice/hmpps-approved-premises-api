package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole

class SeedCas1Cas3UserMergeTest : SeedTestBase() {

  @Test
  fun `Merge user, removing record for the new username`() {
    val (oldUser) = givenAUser(
      staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "OLD_USERNAME"),
    )

    val (newUser) = givenAUser(
      staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "NEW_USERNAME"),
      roles = listOf(
        UserRole.CAS1_ASSESSOR,
      ),
      qualifications = listOf(
        UserQualification.EMERGENCY,
      ),
    )

    seed(
      SeedFileType.cas1cas3UserMerge,
      """old_delius_username,new_delius_username
        |old_username,new_username
      """.trimMargin(),
    )

    val updatedOldUser = userRepository.findByIdOrNull(oldUser.id)!!
    assertThat(updatedOldUser.deliusUsername).isEqualTo("NEW_USERNAME")

    assertThat(userRepository.findByIdOrNull(newUser.id)).isNull()
  }
}
