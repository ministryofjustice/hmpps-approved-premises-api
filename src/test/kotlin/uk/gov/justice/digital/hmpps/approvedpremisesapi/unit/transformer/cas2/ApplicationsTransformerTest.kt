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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LatestCas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NomisUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.StatusUpdateTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.TimelineEventsTransformer
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationsTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockNomisTransformer = mockk<NomisUserTransformer>()
  private val mockStatusUpdateTransformer = mockk<StatusUpdateTransformer>()
  private val mockTimelineEventsTransformer = mockk<TimelineEventsTransformer>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val applicationsTransformer = uk.gov.justice.digital.hmpps
    .approvedpremisesapi.transformer.cas2.ApplicationsTransformer(
      objectMapper,
      mockPersonTransformer,
      mockNomisTransformer,
      mockStatusUpdateTransformer,
      mockTimelineEventsTransformer,
    )

  private val user = NomisUserEntityFactory().produce()

  private val cas2ApplicationFactory = Cas2ApplicationEntityFactory().withCreatedByUser(user)

  private val submittedCas2ApplicationFactory = cas2ApplicationFactory
    .withSubmittedAt(OffsetDateTime.now())

  @BeforeEach
  fun setup() {
    every { mockPersonTransformer.transformModelToPersonApi(any()) } returns mockk<Person>()
    every { mockNomisTransformer.transformJpaToApi(any()) } returns NomisUser(
      id = user.id,
      name = user.name,
      nomisUsername = user.nomisUsername,
      isActive = user.isActive,
    )
    every { mockStatusUpdateTransformer.transformJpaToApi(any()) } returns mockk<Cas2StatusUpdate>()
    every { mockStatusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(any()) } returns mockk<LatestCas2StatusUpdate>()
    every { mockTimelineEventsTransformer.transformApplicationToTimelineEvents(any()) } returns listOf()
  }

  @Nested
  inner class TransformJpaToApi {

    @Test
    fun `transformJpaToApi transforms an in progress CAS-2 application correctly`() {
      val application = cas2ApplicationFactory
        .withSubmittedAt(null)
        .produce()

      val result = applicationsTransformer.transformJpaToApi(application, mockk())

      assertThat(result).hasOnlyFields(
        "id",
        "person",
        "createdBy",
        "schemaVersion",
        "outdatedSchema",
        "createdAt",
        "submittedAt",
        "data",
        "document",
        "status",
        "type",
        "telephoneNumber",
        "statusUpdates",
        "timelineEvents",
      )

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.createdBy.id).isEqualTo(user.id)
      assertThat(result.status).isEqualTo(ApplicationStatus.inProgress)
      assertThat(result.timelineEvents).isEqualTo(listOf<Cas2TimelineEvent>())
    }

    @Test
    fun `transformJpaToApi transforms a submitted CAS2 application correctly without status updates`() {
      val application = submittedCas2ApplicationFactory.produce()

      val result = applicationsTransformer.transformJpaToApi(application, mockk())

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
      assertThat(result.telephoneNumber).isEqualTo(application.telephoneNumber)
    }

    @Test
    fun `transformJpaToApi transforms a submitted CAS2 application correctly with status updates`() {
      val statusUpdateEntity = Cas2StatusUpdateEntityFactory().withLabel("status update")
        .withApplication(submittedCas2ApplicationFactory.produce()).produce()
      val statusUpdateForApi = Cas2StatusUpdate(
        id = statusUpdateEntity.id,
        name = "statusUpdate",
        label = "status update",
        description = "status update",
      )
      every { mockStatusUpdateTransformer.transformJpaToApi(statusUpdateEntity) } returns statusUpdateForApi

      val application = submittedCas2ApplicationFactory.withStatusUpdates(
        mutableListOf(
          statusUpdateEntity,
        ),
      )
        .produce()

      val result = applicationsTransformer.transformJpaToApi(application, mockk())

      assertThat(result.id).isEqualTo(application.id)
      assertThat(result.statusUpdates).hasSize(1).containsExactly(statusUpdateForApi)
    }
  }

  @Nested
  inner class TransformJpaSummaryToSummary {

    @Test
    fun `transforms an in progress CAS2 application correctly`
    () {
      val application = object : Cas2ApplicationSummary {
        override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
        override fun getCrn() = "CRNNUM"
        override fun getNomsNumber() = "NOMNUM"
        override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
        override fun getCreatedByUserName() = "first last"
        override fun getCreatedAt() = Timestamp(Instant.parse("2023-04-19T13:25:00+01:00").toEpochMilli())
        override fun getSubmittedAt() = null
        override fun getHdcEligibilityDate() = null
        override fun getLatestStatusUpdateLabel() = null
        override fun getLatestStatusUpdateStatusId() = null
      }

      every { mockStatusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(any()) } returns null

      val result = applicationsTransformer.transformJpaSummaryToSummary(
        application,
        "firstName surname",
      )

      assertThat(result.id).isEqualTo(application.getId())
      assertThat(result.createdByUserId).isEqualTo(application.getCreatedByUserId())
      assertThat(result.risks).isNull()
      assertThat(result.personName).isEqualTo("firstName surname")
      assertThat(result.crn).isEqualTo(application.getCrn())
      assertThat(result.nomsNumber).isEqualTo(application.getNomsNumber())
      assertThat(result.hdcEligibilityDate).isNull()
      assertThat(result.latestStatusUpdate).isNull()
      assertThat(result.createdByUserName).isEqualTo("first last")
    }

    @Test
    fun `transforms a submitted CAS2 application correctly`() {
      val application = object : Cas2ApplicationSummary {
        override fun getId() = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809")
        override fun getCrn() = "CRNNUM"
        override fun getNomsNumber() = "NOMNUM"
        override fun getCreatedByUserId() = UUID.fromString("836a9460-b177-433a-a0d9-262509092c9f")
        override fun getCreatedByUserName() = "first last"
        override fun getCreatedAt() = Timestamp(Instant.parse("2023-04-19T13:25:00+01:00").toEpochMilli())
        override fun getSubmittedAt() = Timestamp(Instant.parse("2023-04-19T13:25:30+01:00").toEpochMilli())
        override fun getHdcEligibilityDate() = LocalDate.parse("2023-04-29")
        override fun getLatestStatusUpdateStatusId() = UUID.fromString("ae544aee-7170-4794-99fb-703090cbc7db")
        override fun getLatestStatusUpdateLabel() = "my latest status update"
      }

      every { mockStatusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(any()) } returns LatestCas2StatusUpdate(
        statusId = application.getLatestStatusUpdateStatusId(),
        label = application.getLatestStatusUpdateLabel(),
      )

      val result = applicationsTransformer.transformJpaSummaryToSummary(
        application,
        "firstName surname",
      )

      assertThat(result.id).isEqualTo(application.getId())
      assertThat(result.status).isEqualTo(ApplicationStatus.submitted)
      assertThat(result.hdcEligibilityDate).isEqualTo("2023-04-29")
      assertThat(result.personName).isEqualTo("firstName surname")
      assertThat(result.crn).isEqualTo(application.getCrn())
      assertThat(result.nomsNumber).isEqualTo(application.getNomsNumber())
      assertThat(result.latestStatusUpdate?.label).isEqualTo("my latest status update")
      assertThat(result.latestStatusUpdate?.statusId).isEqualTo(UUID.fromString("ae544aee-7170-4794-99fb-703090cbc7db"))
      assertThat(result.createdByUserName).isEqualTo("first last")
    }
  }
}
