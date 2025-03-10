package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2v2

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.random.Random

@Component
class Cas2v2UsersSeedJob(
  private val repository: Cas2v2UserRepository,
) : SeedJob<Cas2v2UserSeedCsvRow>(
  requiredHeaders = setOf(
    "username",
    "email",
    "name",
    "userType",
    "nomisStaffId",
    "activeCaseloadId",
    "deliusTeamCodes",
    "deliusStaffCode",
    "isEnabled",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas2v2UserSeedCsvRow(
    username = columns["username"]!!.trim().uppercase(),
    email = columns["email"]!!.trim(),
    name = columns["name"]!!.trim(),
    userType = userType(columns["userType"]!!.trim()),
    nomisStaffId = columns["nomisStaffId"]?.trim()?.takeIf { it.isNotEmpty() }?.toLongOrNull(),
    activeNomisCaseloadId = columns["activeNomisCaseloadId"]?.trim()?.takeIf { it.isNotEmpty() },
    deliusTeamCodes = columns["deliusTeamCodes"]?.trim()?.takeIf { it.isNotEmpty() }?.split("|"),
    deliusStaffCode = columns["deliusStaffCode"]?.trim()?.takeIf { it.isNotEmpty() },
    isEnabled = columns["isEnabled"]!!.trim().uppercase() == "TRUE",
    isActive = columns["isActive"]!!.trim().uppercase() == "TRUE",
  )

  @SuppressWarnings("TooGenericExceptionThrown", "TooGenericExceptionCaught")
  override fun processRow(row: Cas2v2UserSeedCsvRow) {
    log.info("Setting up ${row.username}")

    val users = repository.findAll()
    users.map { user ->
    }

    if (repository.findByUsername(row.username) !== null) {
      return log.info("Skipping ${row.username}: already seeded")
    }

    try {
      createCas2v2User(row)
    } catch (exception: Exception) {
      throw RuntimeException("Could not create user ${row.username}", exception)
    }
  }

  @SuppressWarnings("MagicNumber")
  private fun createCas2v2User(row: Cas2v2UserSeedCsvRow) {
    repository.save(
      Cas2v2UserEntity(
        id = UUID.randomUUID(),
        username = row.username,
        email = row.email,
        name = row.name,
        userType = row.userType,
        nomisStaffId = row.nomisStaffId,
        activeNomisCaseloadId = row.activeNomisCaseloadId,
        deliusTeamCodes = row.deliusTeamCodes,
        deliusStaffCode = row.deliusStaffCode,
        isEnabled = row.isEnabled,
        isActive = row.isActive,
        createdAt = OffsetDateTime.now(ZoneOffset.UTC).minusDays(Random.nextLong(1, 365)),
        applications = mutableListOf(),
      ),
    )
  }

  private fun userType(type: String) = when (type) {
    Cas2v2UserType.NOMIS.toString() -> Cas2v2UserType.NOMIS
    Cas2v2UserType.DELIUS.toString() -> Cas2v2UserType.DELIUS
    else -> Cas2v2UserType.EXTERNAL
  }
}

data class Cas2v2UserSeedCsvRow(
  val username: String,
  val email: String,
  val name: String,
  val userType: Cas2v2UserType,
  val nomisStaffId: Long?,
  val activeNomisCaseloadId: String?,
  val deliusTeamCodes: List<String>?,
  val deliusStaffCode: String?,
  val isEnabled: Boolean,
  val isActive: Boolean,
)
