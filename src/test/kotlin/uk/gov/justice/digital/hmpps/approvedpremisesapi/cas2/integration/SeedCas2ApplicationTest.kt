package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedCas2ApplicationTest : SeedTestBase() {
  @Test
  fun `Attempting to seed a Cas2 Application succeeds`() {
    cas2ApplicationRepository.deleteAll()

    val applicant = cas2UserEntityFactory.produceAndPersist {
      withUsername("ROGER_SMITH_FAKE")
    }

    val applicationId = "6a1551ea-cdb7-4f5e-beac-aee9ad73339c"
    val creationTimestamp = OffsetDateTime.parse("2022-12-13T15:00:00+01:00")

    seed(
      SeedFileType.cas2Applications,
      cas2ApplicationSeedCsvRowsToCsv(
        listOf(
          Cas2ApplicationSeedCsvRowFactory()
            .withId(applicationId)
            .withNomsNumber("NOMS-123")
            .withCrn("CRN-ABC")
            .withCreatedBy("ROGER_SMITH_FAKE")
            .withCreatedAt(creationTimestamp)
            .withSubmittedAt(null)
            .withState("NOT_STARTED")
            .withStatusUpdates("0")
            .withLocation("Sheffield")
            .produce(),
        ),
      ),
    )

    val persistedApplication = cas2ApplicationRepository.getReferenceById(UUID.fromString(applicationId))

    assertThat(persistedApplication).isNotNull
    assertThat(persistedApplication.crn).isEqualTo("CRN-ABC")
    assertThat(persistedApplication.nomsNumber).isEqualTo("NOMS-123")
    assertThat(persistedApplication.createdByUser!!.id).isEqualTo(applicant.id)
    assertThat(persistedApplication.createdAt).isEqualTo(creationTimestamp)
    assertThat(persistedApplication.statusUpdates).isEmpty()
  }

  @Test
  fun `An IN_PROGRESS application has _data_ but no _document_ or Assessment`() {
    cas2ApplicationRepository.deleteAll()

    cas2UserEntityFactory.produceAndPersist {
      withUsername("ROGER_SMITH_FAKE")
    }

    val applicationId = "6a1551ea-cdb7-4f5e-beac-aee9ad73339c"
    val creationTimestamp = OffsetDateTime.parse("2022-12-13T15:00:00+01:00")

    seed(
      SeedFileType.cas2Applications,
      cas2ApplicationSeedCsvRowsToCsv(
        listOf(
          Cas2ApplicationSeedCsvRowFactory()
            .withId(applicationId)
            .withNomsNumber("A1234AI")
            .withCrn("CRN-ABC")
            .withCreatedBy("ROGER_SMITH_FAKE")
            .withCreatedAt(creationTimestamp)
            .withSubmittedAt(null)
            .withState("IN_PROGRESS")
            .withStatusUpdates("0")
            .withLocation(null)
            .produce(),
        ),
      ),
    )

    val persistedApplication = cas2ApplicationRepository.getReferenceById(UUID.fromString(applicationId))

    assertThat(persistedApplication).isNotNull

    assertThat(serializableToJsonNode(persistedApplication.data)).isNotEmpty()
    assertThat(
      serializableToJsonNode(persistedApplication.data)
        .get("hdc-licence-dates")
        .get("hdc-licence-dates"),
    ).isNotEmpty()
    assertThat(serializableToJsonNode(persistedApplication.document)).isEmpty()
    assertThat(persistedApplication.assessment).isNull()
  }

  @Test
  fun `A SUBMITTED application has _data_ AND _document_ AND an Assessment AND status updates AND first class fields`() {
    cas2ApplicationRepository.deleteAll()

    val referringPrisonCode = "EXAMPLE_CODE"

    cas2UserEntityFactory.produceAndPersist {
      withUsername("ROGER_SMITH_FAKE")
      withActiveNomisCaseloadId(referringPrisonCode)
    }

    cas2UserEntityFactory.produceAndPersist {
      withUserType(Cas2UserType.EXTERNAL)
    }

    val applicationId = "6a1551ea-cdb7-4f5e-beac-aee9ad73339c"
    val creationTimestamp = OffsetDateTime.parse("2022-12-13T15:00:00+01:00")
    val submissionTimestamp = OffsetDateTime.parse("2022-12-15T12:00:00+01:00")

    seed(
      SeedFileType.cas2Applications,
      cas2ApplicationSeedCsvRowsToCsv(
        listOf(
          Cas2ApplicationSeedCsvRowFactory()
            .withId(applicationId)
            .withNomsNumber("A1234AI")
            .withCrn("CRN-ABC")
            .withCreatedBy("ROGER_SMITH_FAKE")
            .withCreatedAt(creationTimestamp)
            .withSubmittedAt(submissionTimestamp)
            .withState("SUBMITTED")
            .withStatusUpdates("1")
            .withLocation(null)
            .withReferringPrisonCode(referringPrisonCode)
            .produce(),
        ),
      ),
    )

    val persistedApplication = cas2ApplicationRepository.getReferenceById(UUID.fromString(applicationId))

    assertThat(persistedApplication).isNotNull

    assertThat(serializableToJsonNode(persistedApplication.data)).isNotEmpty()
    assertThat(serializableToJsonNode(persistedApplication.document)).isNotEmpty()
    assertThat(persistedApplication.assessment!!.statusUpdates).isNotEmpty()
    assertThat(persistedApplication.referringPrisonCode).isEqualTo("EXAMPLE_CODE")
    assertThat(persistedApplication.preferredAreas).isEqualTo("Bristol | Newcastle")
    assertThat(persistedApplication.hdcEligibilityDate).isEqualTo(LocalDate.now())
    assertThat(persistedApplication.conditionalReleaseDate).isEqualTo(LocalDate.now().plusMonths(2))
    assertThat(persistedApplication.telephoneNumber).isEqualTo("0800 123 456")
  }

  @Test
  fun `An IN_REVIEW application has _data_, _document_ AND status updates`() {
    cas2ApplicationRepository.deleteAll()

    cas2UserEntityFactory.produceAndPersist {
      withUsername("ROGER_SMITH_FAKE")
    }

    cas2UserEntityFactory.produceAndPersist {
      withUserType(Cas2UserType.EXTERNAL)
      withUsername("CAS2_ASSESSOR")
    }

    val applicationId = "6a1551ea-cdb7-4f5e-beac-aee9ad73339c"
    val creationTimestamp = OffsetDateTime.parse("2022-12-13T15:00:00+01:00")
    val submissionTimestamp = OffsetDateTime.parse("2022-12-15T12:00:00+01:00")

    seed(
      SeedFileType.cas2Applications,
      cas2ApplicationSeedCsvRowsToCsv(
        listOf(
          Cas2ApplicationSeedCsvRowFactory()
            .withId(applicationId)
            .withNomsNumber("A1234AI")
            .withCrn("CRN-ABC")
            .withCreatedBy("ROGER_SMITH_FAKE")
            .withCreatedAt(creationTimestamp)
            .withSubmittedAt(submissionTimestamp)
            .withState("IN_REVIEW")
            .withStatusUpdates("2")
            .withLocation(null)
            .produce(),
        ),
      ),
    )

    val persistedApplication = cas2ApplicationRepository.getReferenceById(UUID.fromString(applicationId))

    assertThat(persistedApplication).isNotNull
    assertThat(persistedApplication.submittedAt).isNotNull

    assertThat(serializableToJsonNode(persistedApplication.data)).isNotEmpty()
    assertThat(serializableToJsonNode(persistedApplication.document)).isNotEmpty()

    assertThat(persistedApplication.statusUpdates).isNotEmpty()
    assertThat(persistedApplication.statusUpdates!!.size).isEqualTo(2)
    persistedApplication.statusUpdates!!.forEach { statusUpdate ->
      assertThat(statusUpdate.assessor?.username).isEqualTo("CAS2_ASSESSOR")
    }
  }

  private fun cas2ApplicationSeedCsvRowsToCsv(rows: List<Cas2ApplicationSeedUntypedEnumsCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "id",
        "nomsNumber",
        "crn",
        "createdBy",
        "createdAt",
        "submittedAt",
        "state",
        "statusUpdates",
        "location",
        "referringPrisonCode",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.id)
        .withQuotedField(it.nomsNumber)
        .withQuotedField(it.crn)
        .withQuotedField(it.createdBy)
        .withQuotedField(it.createdAt)
        .withQuotedField(it.submittedAt ?: "")
        .withQuotedField(it.state)
        .withQuotedField(it.statusUpdates)
        .withQuotedField(it.location ?: "")
        .withQuotedField(it.referringPrisonCode ?: "")
        .newRow()
    }

    return builder.build()
  }

  private fun serializableToJsonNode(serializable: Any?): JsonNode {
    if (serializable == null) return NullNode.instance
    if (serializable is String) return objectMapper.readTree(serializable)

    return objectMapper.readTree(objectMapper.writeValueAsString(serializable))
  }
}

data class Cas2ApplicationSeedUntypedEnumsCsvRow(
  val id: UUID,
  val nomsNumber: String,
  val crn: String,
  val createdBy: String,
  val createdAt: OffsetDateTime,
  val submittedAt: OffsetDateTime?,
  // NOT_STARTED | IN-PROGRESS | SUBMITTED | IN_REVIEW
  val state: String,
  val statusUpdates: String,
  val location: String?,
  val referringPrisonCode: String?,
)

class Cas2ApplicationSeedCsvRowFactory : Factory<Cas2ApplicationSeedUntypedEnumsCsvRow> {
  private var id: Yielded<String> = { "6a1551ea-cdb7-4f5e-beac-aee9ad73339c" }
  private var nomsNumber: Yielded<String> = { randomStringUpperCase(6) }
  private var crn: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var createdBy: Yielded<String> = { "ROGER_SMITH_FAKE" }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var submittedAt: Yielded<OffsetDateTime?> = { null }
  private var statusUpdates: Yielded<String> = { "0" }
  private var location: Yielded<String?> = { "Leeds" }
  private var state: Yielded<String> = { listOf("NOT_STARTED", "IN_PROGRESS", "SUBMITTED", "IN_REVIEW").random() }
  private var referringPrisonCode: Yielded<String?> = { null }

  fun withId(id: String) = apply {
    this.id = { id }
  }

  fun withNomsNumber(nomsNumber: String) = apply {
    this.nomsNumber = { nomsNumber }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withCreatedBy(createdBy: String) = apply {
    this.createdBy = { createdBy }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withSubmittedAt(submittedAt: OffsetDateTime?) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withStatusUpdates(statusUpdates: String) = apply {
    this.statusUpdates = { statusUpdates }
  }

  fun withLocation(location: String?) = apply {
    this.location = { location }
  }

  fun withState(state: String) = apply {
    this.state = { state }
  }

  fun withReferringPrisonCode(referringPrisonCode: String) = apply {
    this.referringPrisonCode = { referringPrisonCode }
  }

  override fun produce() = Cas2ApplicationSeedUntypedEnumsCsvRow(
    id = UUID.fromString(this.id()),
    crn = this.crn(),
    createdBy = this.createdBy(),
    nomsNumber = this.nomsNumber(),
    createdAt = this.createdAt(),
    submittedAt = this.submittedAt(),
    statusUpdates = this.statusUpdates(),
    location = this.location(),
    state = this.state(),
    referringPrisonCode = this.referringPrisonCode(),
  )
}
