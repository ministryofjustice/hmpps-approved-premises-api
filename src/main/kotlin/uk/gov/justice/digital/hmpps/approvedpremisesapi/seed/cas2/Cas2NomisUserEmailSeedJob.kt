package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob

@Component
class Cas2NomisUserEmailSeedJob(
  private val nomisUsersRepository: NomisUserRepository,
) : SeedJob<NomisUsernameEmailRow>() {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = NomisUsernameEmailRow(
    nomisUsername = columns["nomis_username"]!!.trim(),
    emailAddress = columns["email_address"]!!.trim(),
  )

  override fun processRow(row: NomisUsernameEmailRow) {
    log.info("Cas2UsersEmailFixSeedJob: updating user: ${row.nomisUsername}")

    val user = nomisUsersRepository.findByNomisUsername(row.nomisUsername)
      ?: error("Nomis User does not exist: ${row.nomisUsername}")

    user.email = row.emailAddress
    nomisUsersRepository.save(user)

    log.info("Cas2UsersEmailFixSeedJob: updated user: ${row.nomisUsername}")
  }
}

data class NomisUsernameEmailRow(
  val nomisUsername: String,
  val emailAddress: String,
)
