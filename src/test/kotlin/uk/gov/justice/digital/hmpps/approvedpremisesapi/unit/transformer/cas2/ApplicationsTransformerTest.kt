package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.sql.Timestamp
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationsTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val applicationsTransformer = uk.gov.justice.digital.hmpps
    .approvedpremisesapi.transformer.cas2.ApplicationsTransformer(
      objectMapper,
      mockPersonTransformer,
    )

  private val user = NomisUserEntityFactory().produce()

  private val cas2ApplicationFactory = Cas2ApplicationEntityFactory().withCreatedByUser(user)

  private val submittedCas2ApplicationFactory = cas2ApplicationFactory
    .withSubmittedAt(OffsetDateTime.now())

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
  }

  @Nested
  inner class TransformJpaToApi {

    @Test
    fun `transformJpaToApi transforms an in progress CAS-2 application correctly`() {
      val application = cas2ApplicationFactory
        .withSubmittedAt(null)
        .produce()

      val result = applicationsTransformer.transformJpaToApi(application, mockk())

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.createdByUserId).isEqualTo(user.id)
      assertThat(result.createdByUserId).isEqualTo(user.id)
      assertThat(result.status).isEqualTo(ApplicationStatus.inProgress)
    }

    @Test
    fun `transformJpaToApi transforms a submitted CAS2 application correctly`() {
      val application = submittedCas2ApplicationFactory.produce()

      val result = applicationsTransformer.transformJpaToApi(application, mockk())

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
    }
  }

  @Nested
  inner class TransformJpaSummaryToSummary {

    @Test
    fun `transforms an in progress CAS2 application correctly`
    () {
      val application = object : Cas2ApplicationSummary {
        override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
        override fun getCrn() = randomStringMultiCaseWithNumbers(6)
        override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
        override fun getCreatedAt() = Timestamp(Instant.parse("2023-04-19T13:25:00+01:00").toEpochMilli())
        override fun getSubmittedAt() = null
      }

      val result = applicationsTransformer.transformJpaSummaryToSummary(
        application,
        mockk(),
      )

      assertThat(result.id).isEqualTo(application.getId())
      assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
      assertThat(result.risks).isNull()
    }

    @Test
    fun `transforms a submitted CAS2 application correctly`() {
      val application = object : Cas2ApplicationSummary {
        override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
        override fun getCrn() = randomStringMultiCaseWithNumbers(6)
        override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
        override fun getCreatedAt() = Timestamp(Instant.parse("2023-04-19T13:25:00+01:00").toEpochMilli())
        override fun getSubmittedAt() = Timestamp(Instant.parse("2023-04-19T13:25:30+01:00").toEpochMilli())
      }

      val result = applicationsTransformer.transformJpaSummaryToSummary(
        application,
        mockk(),
      )

      assertThat(result.id).isEqualTo(application.getId())
      assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
    }
  }
}
