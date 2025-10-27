package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.seed

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.seed.Cas2NomisUserEmailSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.seed.NomisUsernameEmailRow

@ExtendWith(MockKExtension::class)
class Cas2NomisUserEmailSeedJobTest {

  @MockK
  private lateinit var nomisUserRepository: NomisUserRepository

  @InjectMockKs
  private lateinit var seedJob: Cas2NomisUserEmailSeedJob

  @Test
  fun `fails if no user found`() {
    every { nomisUserRepository.findByNomisUsername("testuser") } returns null

      assertThrows<RuntimeException> {
          seedJob.processRow(NomisUsernameEmailRow("testuser", "asd@asd.com"))
      }
  }

  @Test
  fun `updates email address`() {
    val user = NomisUserEntityFactory().withEmail(null).produce()
    every { nomisUserRepository.findByNomisUsername("testuser") } returns user

    val slot = slot<NomisUserEntity>()
    every { nomisUserRepository.save(capture(slot)) } answers { firstArg() }

    seedJob.processRow(NomisUsernameEmailRow("testuser", "asd@asd.com"))

    Assertions.assertThat(slot.captured.email).isEqualTo("asd@asd.com")
  }
}