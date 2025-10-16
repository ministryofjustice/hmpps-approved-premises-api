package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.seed.Cas2AssessmentUpdateStatusSeedRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import java.util.UUID

class Cas2UpdateAssessmentStatusSeedJobTest : SeedTestBase() {
  @Test
  fun `Update an assessment status with reasons`() {
    // given
    val application =
      cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(
          cas2UserEntityFactory.produceAndPersist {
            withUsername("nomis_username")
            withNomisStaffCode(100L)
            withNomisStaffIdentifier(101L)
            withIsActive(true)
            withName("nomis user")
            withEmail("nomis@justice.gov.uk")
          },
        )
        withCrn("A123456")
        withData("{}")
        withNomsNumber("A23456V")
      }

    val assessment =
      cas2AssessmentEntityFactory.produceAndPersist {
        withApplication(application)
        withAssessorName("Billy Bunter")
      }

    val assessor =
      cas2UserEntityFactory.produceAndPersist {
        withUserType(Cas2UserType.EXTERNAL)
        withUsername("billy.bunter")
        withName("Billy Bunter")
        withEmail("billy.bunter@example.com")
      }

    val seedRow =
      Cas2AssessmentUpdateStatusSeedRow(
        assessmentId = assessment.id,
        applicationId = application.id,
        assessorUsername = assessor.username,
        newStatus = "cancelled",
        newStatusDetails = listOf("incompleteReferral"),
      )

    // when
    seed(SeedFileType.cas2UpdateAssessmentStatus, rowsToCsv(listOf(seedRow)))

    // Then
    val persistedAssessment = cas2AssessmentRepository.findByIdAndServiceOrigin(assessment.id, Cas2ServiceOrigin.HDC)!!
    assertThat(persistedAssessment.statusUpdates).hasSize(1)

    val statusUpdate =
      persistedAssessment.statusUpdates?.firstOrNull()
        ?: fail("No status update found for assessment ${assessment.id}")
    assertThat(statusUpdate.application.id).isEqualTo(application.id)
    assertThat(statusUpdate.assessment!!.id).isEqualTo(assessment.id)
    assertThat(statusUpdate.assessor.id).isEqualTo(assessor.id)
    assertThat(statusUpdate.statusId).isNotNull()
    assertThat(statusUpdate.description)
      .isEqualTo(
        "The application has been cancelled.",
      )
    assertThat(statusUpdate.label).isEqualTo("Referral cancelled")

    // Verify status update details
    val statusUpdateDetails =
      cas2StatusUpdateDetailRepository.findFirstByStatusUpdateIdOrderByCreatedAtDesc(
        statusUpdate.id,
      )
    assertThat(statusUpdateDetails).isNotNull()
    assertThat(statusUpdateDetails!!.label).isIn("Incomplete referral")

    val domainResults = domainEventRepository.findByApplicationId(application.id)
    assertThat(domainResults).hasSize(1)

    val result = domainResults.first()

    assertThat(result.applicationId).isEqualTo(application.id)
    assertThat(result.type).isEqualTo(DomainEventType.CAS2_APPLICATION_STATUS_UPDATED)
    assertThat(result.service).isEqualTo("CAS2")
    assertThat(result.data).contains("Incomplete referral")
    assertThat(result.data).contains("Referral cancelled")

    assertThat(result.nomsNumber).isEqualTo("A23456V")
  }

  @Test
  fun `should make no change when assessment not found`() {
    // Given
    val application =
      cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(
          cas2UserEntityFactory.produceAndPersist {
            withUsername("nomis_username")
            withNomisStaffCode(100L)
            withNomisStaffIdentifier(101L)
            withIsActive(true)
            withName("nomis user")
            withEmail("nomis@justice.gov.uk")
          },
        )
        withCrn("C345678")
        withData("{}")
        withNomsNumber("C34567X")
      }

    val assessor =
      cas2UserEntityFactory.produceAndPersist {
        withUserType(Cas2UserType.EXTERNAL)
        withUsername("john.smith")
        withName("John Smith")
        withEmail("john.smith@example.com")
      }

    val nonExistentAssessmentId = UUID.randomUUID()
    val seedRow =
      Cas2AssessmentUpdateStatusSeedRow(
        assessmentId = nonExistentAssessmentId,
        applicationId = application.id,
        assessorUsername = assessor.username,
        newStatus = "awaitingDecision",
        newStatusDetails = emptyList(),
      )

    // When
    seed(SeedFileType.cas2UpdateAssessmentStatus, rowsToCsv(listOf(seedRow)))

    // then
    val domainResults = domainEventRepository.findByApplicationId(application.id)
    assertThat(domainResults).isEmpty()
  }

  @Test
  fun `should make no change when application not found`() {
    // Given
    val application =
      cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(
          cas2UserEntityFactory.produceAndPersist {
            withUsername("nomis_username")
            withNomisStaffCode(100L)
            withNomisStaffIdentifier(101L)
            withIsActive(true)
            withName("nomis user")
            withEmail("nomis@justice.gov.uk")
          },
        )
        withCrn("D901234")
        withData("{}")
        withNomsNumber("D90123Y")
      }

    val assessment =
      cas2AssessmentEntityFactory.produceAndPersist {
        withApplication(application)
        withAssessorName("Bob Wilson")
      }

    val assessor =
      cas2UserEntityFactory.produceAndPersist {
        withUserType(Cas2UserType.EXTERNAL)
        withUsername("bob.wilson")
        withName("Bob Wilson")
        withEmail("bob.wilson@example.com")
      }

    val nonExistentApplicationId = UUID.randomUUID()
    val seedRow =
      Cas2AssessmentUpdateStatusSeedRow(
        assessmentId = assessment.id,
        applicationId = nonExistentApplicationId,
        assessorUsername = assessor.username,
        newStatus = "awaitingDecision",
        newStatusDetails = emptyList(),
      )

    // When
    seed(SeedFileType.cas2UpdateAssessmentStatus, rowsToCsv(listOf(seedRow)))

    // then
    val domainResults = domainEventRepository.findByApplicationId(application.id)
    assertThat(domainResults).isEmpty()
  }

  @Test
  fun `should make no change when assessor not found`() {
    // Given
    val application =
      cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(
          cas2UserEntityFactory.produceAndPersist {
            withUsername("nomis_username")
            withNomisStaffCode(100L)
            withNomisStaffIdentifier(101L)
            withIsActive(true)
            withName("nomis user")
            withEmail("nomis@justice.gov.uk")
          },
        )
        withCrn("E567890")
        withData("{}")
        withNomsNumber("E56789Z")
      }

    val assessment =
      cas2AssessmentEntityFactory.produceAndPersist {
        withApplication(application)
        withAssessorName("Alice Brown")
      }

    val seedRow =
      Cas2AssessmentUpdateStatusSeedRow(
        assessmentId = assessment.id,
        applicationId = application.id,
        assessorUsername = "non.existent.user",
        newStatus = "awaitingDecision",
        newStatusDetails = emptyList(),
      )

    // When
    seed(SeedFileType.cas2UpdateAssessmentStatus, rowsToCsv(listOf(seedRow)))

    // then
    val domainResults = domainEventRepository.findByApplicationId(application.id)
    assertThat(domainResults).isEmpty()
  }

  @Test
  fun `should make no change if application ID does not match assessment application`() {
    // Given
    val user =
      cas2UserEntityFactory.produceAndPersist {
        withUsername("nomis_username")
        withNomisStaffCode(100L)
        withNomisStaffIdentifier(101L)
        withIsActive(true)
        withName("nomis user")
        withEmail("nomis@justice.gov.uk")
      }

    val application1 =
      cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(user)
        withCrn("F123456")
        withData("{}")
        withNomsNumber("F12345A")
      }

    val application2 =
      cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(user)
        withCrn("G789012")
        withData("{}")
        withNomsNumber("G78901B")
      }

    val assessment =
      cas2AssessmentEntityFactory.produceAndPersist {
        withApplication(application1)
        withAssessorName("Charlie Davis")
      }

    val assessor =
      cas2UserEntityFactory.produceAndPersist {
        withUserType(Cas2UserType.EXTERNAL)
        withUsername("charlie.davis")
        withName("Charlie Davis")
        withEmail("charlie.davis@example.com")
      }

    val seedRow =
      Cas2AssessmentUpdateStatusSeedRow(
        assessmentId = assessment.id,
        applicationId = application2.id, // Different application
        assessorUsername = assessor.username,
        newStatus = "awaitingDecision",
        newStatusDetails = emptyList(),
      )

    // When
    seed(SeedFileType.cas2UpdateAssessmentStatus, rowsToCsv(listOf(seedRow)))

    // then
    val domainResults = domainEventRepository.findByApplicationId(application1.id)
    assertThat(domainResults).isEmpty()
  }

  @Test
  fun `should make no change when assessor name does not match assessment assessor name`() {
    // Given
    val application =
      cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(
          cas2UserEntityFactory.produceAndPersist {
            withUsername("nomis_username")
            withNomisStaffCode(100L)
            withNomisStaffIdentifier(101L)
            withIsActive(true)
            withName("nomis user")
            withEmail("nomis@justice.gov.uk")
          },
        )
        withCrn("H345678")
        withData("{}")
        withNomsNumber("H34567C")
      }

    val assessment =
      cas2AssessmentEntityFactory.produceAndPersist {
        withApplication(application)
        withAssessorName("David Evans") // Assessment has this assessor name
      }

    val assessor =
      cas2UserEntityFactory.produceAndPersist {
        withUserType(Cas2UserType.EXTERNAL)
        withUsername("david.evans")
        withName("David Wilson") // But user has different name
        withEmail("david.evans@example.com")
      }

    val seedRow =
      Cas2AssessmentUpdateStatusSeedRow(
        assessmentId = assessment.id,
        applicationId = application.id,
        assessorUsername = assessor.username,
        newStatus = "awaitingDecision",
        newStatusDetails = emptyList(),
      )

    // When
    seed(SeedFileType.cas2UpdateAssessmentStatus, rowsToCsv(listOf(seedRow)))

    // then
    val domainResults = domainEventRepository.findByApplicationId(application.id)
    assertThat(domainResults).isEmpty()
  }

  @Test
  fun `should make no change when status not found`() {
    // Given
    val application =
      cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(
          cas2UserEntityFactory.produceAndPersist {
            withUsername("nomis_username")
            withNomisStaffCode(100L)
            withNomisStaffIdentifier(101L)
            withIsActive(true)
            withName("nomis user")
            withEmail("nomis@justice.gov.uk")
          },
        )
        withCrn("I901234")
        withData("{}")
        withNomsNumber("I90123D")
      }

    val assessment =
      cas2AssessmentEntityFactory.produceAndPersist {
        withApplication(application)
        withAssessorName("Emma Foster")
      }

    val assessor =
      cas2UserEntityFactory.produceAndPersist {
        withUserType(Cas2UserType.EXTERNAL)
        withUsername("emma.foster")
        withName("Emma Foster")
        withEmail("emma.foster@example.com")
      }

    val seedRow =
      Cas2AssessmentUpdateStatusSeedRow(
        assessmentId = assessment.id,
        applicationId = application.id,
        assessorUsername = assessor.username,
        newStatus = "nonExistentStatus", // Non-existent status - this is intentional for
        // testing
        newStatusDetails = emptyList(),
      )

    // When
    seed(SeedFileType.cas2UpdateAssessmentStatus, rowsToCsv(listOf(seedRow)))

    // then
    val domainResults = domainEventRepository.findByApplicationId(application.id)
    assertThat(domainResults).isEmpty()
  }

  @Test
  fun `should handle multiple status updates for same assessment`() {
    // Given
    val application =
      cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(
          cas2UserEntityFactory.produceAndPersist {
            withUsername("nomis_username")
            withNomisStaffCode(100L)
            withNomisStaffIdentifier(101L)
            withIsActive(true)
            withName("nomis user")
            withEmail("nomis@justice.gov.uk")
          },
        )
        withCrn("J567890")
        withData("{}")
        withNomsNumber("J56789E")
      }

    val assessment =
      cas2AssessmentEntityFactory.produceAndPersist {
        withApplication(application)
        withAssessorName("Frank Green")
      }

    val assessor =
      cas2UserEntityFactory.produceAndPersist {
        withUserType(Cas2UserType.EXTERNAL)
        withUsername("frank.green")
        withName("Frank Green")
        withEmail("frank.green@example.com")
      }

    val seedRow1 =
      Cas2AssessmentUpdateStatusSeedRow(
        assessmentId = assessment.id,
        applicationId = application.id,
        assessorUsername = assessor.username,
        newStatus = "moreInfoRequested",
        newStatusDetails = listOf("personalInformation"),
      )

    val seedRow2 =
      Cas2AssessmentUpdateStatusSeedRow(
        assessmentId = assessment.id,
        applicationId = application.id,
        assessorUsername = assessor.username,
        newStatus = "cancelled",
        newStatusDetails = listOf("incompleteReferral"),
      )

    // When
    seed(SeedFileType.cas2UpdateAssessmentStatus, rowsToCsv(listOf(seedRow1, seedRow2)))

    // Then
    val persistedAssessment = cas2AssessmentRepository.findByIdAndServiceOrigin(assessment.id, Cas2ServiceOrigin.HDC)
    assertThat(persistedAssessment?.statusUpdates).hasSize(2)

    // Verify both status updates were created
    val statusUpdates =
      persistedAssessment?.statusUpdates?.sortedBy { it.createdAt }
        ?: fail { "No status updates found" }

    assertThat(statusUpdates[0].description)
      .isEqualTo(
        "The referrer must provide information requested for the application to progress.",
      )
    assertThat(statusUpdates[1].description)
      .isEqualTo(
        "The application has been cancelled.",
      )

    // then
    val domainResults = domainEventRepository.findByApplicationId(application.id)
    assertThat(domainResults).hasSize(2)

    val one = domainResults.minByOrNull { it.createdAt }!!

    assertThat(one.applicationId).isEqualTo(application.id)
    assertThat(one.type).isEqualTo(DomainEventType.CAS2_APPLICATION_STATUS_UPDATED)
    assertThat(one.service).isEqualTo("CAS2")
    assertThat(one.data).contains("More information requested")
    assertThat(one.data).contains("Personal information")

    assertThat(one.nomsNumber).isEqualTo("J56789E")

    val two = domainResults.maxByOrNull { it.createdAt }!!

    assertThat(two.applicationId).isEqualTo(application.id)
    assertThat(two.type).isEqualTo(DomainEventType.CAS2_APPLICATION_STATUS_UPDATED)
    assertThat(two.service).isEqualTo("CAS2")
    assertThat(two.data).contains("Incomplete referral")
    assertThat(two.data).contains("Referral cancelled")

    assertThat(two.nomsNumber).isEqualTo("J56789E")
  }

  private fun rowsToCsv(rows: List<Cas2AssessmentUpdateStatusSeedRow>): String {
    val builder =
      CsvBuilder()
        .withUnquotedFields(
          "assessmentId",
          "applicationId",
          "assessorUsername",
          "newStatus",
          "newStatusDetails",
        )
        .newRow()

    rows.forEach { row ->
      builder.withQuotedFields(
        row.assessmentId.toString(),
        row.applicationId.toString(),
        row.assessorUsername,
        row.newStatus,
        row.newStatusDetails.joinToString("||"),
      )
        .newRow()
    }

    return builder.build()
  }
}
