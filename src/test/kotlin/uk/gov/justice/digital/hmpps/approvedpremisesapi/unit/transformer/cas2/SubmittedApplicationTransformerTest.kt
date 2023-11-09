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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NomisUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.SubmittedApplicationTransformer
import java.time.OffsetDateTime

class SubmittedApplicationTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockNomisUserTransformer = mockk<NomisUserTransformer>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val applicationTransformer = SubmittedApplicationTransformer(
    objectMapper,
    mockPersonTransformer,
    mockNomisUserTransformer,
  )

  private val user = NomisUserEntityFactory().produce()

  private val cas2ApplicationFactory = Cas2ApplicationEntityFactory().withCreatedByUser(user)

  private val submittedCas2ApplicationFactory = cas2ApplicationFactory
    .withSubmittedAt(OffsetDateTime.now())

  private val mockNomisUser = mockk<NomisUser>()

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
    every { mockNomisUserTransformer.transformJpaToApi(any()) } returns mockNomisUser
  }

  @Nested
  inner class TransformJpaToSubmittedApplication {
    @Test
    fun `transforms submitted application to API representation with NomisUser and no data`() {
      val jpaEntity = submittedCas2ApplicationFactory.produce()

      val transformation = applicationTransformer.transformJpaToSubmittedApplication(jpaEntity, mockk())

      assertThat(transformation.submittedBy).isEqualTo(mockNomisUser)
      assertThat(transformation).hasOnlyFields(
        "createdAt",
        "document",
        "id",
        "outdatedSchema",
        "person",
        "schemaVersion",
        "status",
        "statusUpdates",
        "submittedAt",
        "submittedBy",
      )
    }
  }
  // transformJpaSummaryToCas2SubmittedSummary
}
