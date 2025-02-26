package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.util.UUID
import kotlin.random.Random

@Component
class Cas2v2UsersSeedJob(
  private val repository: Cas2v2UserRepository,
) : SeedJob<Cas2v2UserSeedCsvRow>(
  requiredHeaders = setOf("nomisUsername", "name", "email"),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas2v2UserSeedCsvRow(
    nomisUsername = columns["nomisUsername"]!!.trim().uppercase(),
    name = columns["name"]!!.trim(),
    email = columns["email"]!!.trim(),
  )

  override fun processRow(row: Cas2v2UserSeedCsvRow) {
    log.info("Setting up ${row.nomisUsername}")

    if (repository.findByUsername(row.nomisUsername) !== null) {
      return log.info("Skipping ${row.nomisUsername}: already seeded")
    }

    try {
      createNomisUser(row)
    } catch (exception: Exception) {
      throw RuntimeException("Could not create user ${row.nomisUsername}", exception)
    }
  }

  private fun createNomisUser(row: Cas2v2UserSeedCsvRow) {
    repository.save(
      Cas2v2UserEntity(
        id = UUID.randomUUID(),
        username = TODO(),
        email = TODO(),
        name = TODO(),
        userType = TODO(),
        nomisStaffId = TODO(),
        activeNomisCaseloadId = TODO(),
        deliusTeamCodes = TODO(),
        deliusStaffCode = TODO(),
        isEnabled = TODO(),
        isActive = TODO(),
        createdAt = TODO(),
        applications = TODO(),
      ),
    )
  }

  private fun randomId(): Long = Random.nextInt(100000, 900000).toLong()
}

data class Cas2v2UserSeedCsvRow(
  val nomisUsername: String,
  val name: String,
  val email: String,
)
