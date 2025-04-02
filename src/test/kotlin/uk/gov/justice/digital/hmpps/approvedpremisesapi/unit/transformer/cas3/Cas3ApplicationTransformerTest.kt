package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3ApplicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class Cas3ApplicationTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockRisksTransformer = mockk<RisksTransformer>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val cas3ApplicationsTransformer = Cas3ApplicationTransformer(objectMapper, mockPersonTransformer, mockRisksTransformer)

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
    every { mockRisksTransformer.transformDomainToApi(any<PersonRisks>(), any<String>()) } returns mockk()
  }

  @Test
  fun `transformJpaToApiSummary transforms an in progress Temporary Accommodation application correctly`() {
    val application = object : TemporaryAccommodationApplicationSummary {
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Instant.now().randomDateTimeBefore(10)
      override fun getSubmittedAt() = null
      override fun getLatestAssessmentSubmittedAt() = null
      override fun getLatestAssessmentDecision() = null
      override fun getLatestAssessmentHasClarificationNotesWithoutResponse() = false
      override fun getHasBooking() = false
    }

    val result = cas3ApplicationsTransformer.transformDomainToCas3ApplicationSummary(
      application,
      mockk(),
    )

    assertThat(result.id).isEqualTo(application.getId())
    assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
    assertThat(result.status).isEqualTo(ApplicationStatus.inProgress)
    assertThat(result.risks).isNotNull
  }

  @Test
  fun `transformJpaToApiSummary transforms a submitted Temporary Accommodation application correctly`() {
    val application = object : TemporaryAccommodationApplicationSummary {
      override fun getRiskRatings() = objectMapper.writeValueAsString(PersonRisksFactory().produce())
      override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
      override fun getCrn() = randomStringMultiCaseWithNumbers(6)
      override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
      override fun getCreatedAt() = Instant.now().randomDateTimeBefore(30)
      override fun getSubmittedAt() = Instant.now().randomDateTimeBefore(3)
      override fun getLatestAssessmentSubmittedAt() = null
      override fun getLatestAssessmentDecision() = null
      override fun getLatestAssessmentHasClarificationNotesWithoutResponse() = false
      override fun getHasBooking() = false
    }

    val result = cas3ApplicationsTransformer.transformDomainToCas3ApplicationSummary(
      application,
      mockk(),
    )

    assertThat(result.id).isEqualTo(application.getId())
    assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
    assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
    assertThat(result.risks).isNotNull
  }
}
