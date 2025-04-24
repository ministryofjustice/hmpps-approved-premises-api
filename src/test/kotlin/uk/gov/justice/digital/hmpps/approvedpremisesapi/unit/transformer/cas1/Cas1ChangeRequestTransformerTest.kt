package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummaryDiscriminator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository.FindOpenChangeRequestResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ChangeRequestTransformer
import java.time.LocalDate
import java.util.UUID

class Cas1ChangeRequestTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  val offenderDetailSummary = OffenderDetailsSummaryFactory().produce()
  val personInfoResult = PersonSummaryInfoResult.Success.Full(
    crn = offenderDetailSummary.otherIds.crn,
    summary = CaseSummaryFactory()
      .withName(Name("max", "power", emptyList()))
      .produce(),
  )

  val personSummary = object : PersonSummary {
    override val crn = offenderDetailSummary.otherIds.crn
    override val personType = PersonSummaryDiscriminator.fullPersonSummary
  }

  val entity = Cas1ChangeRequestEntityFactory()
    .withDecision(ChangeRequestDecision.APPROVED)
    .produce()

  private val cas1ChangeRequestTransformer = Cas1ChangeRequestTransformer(mockPersonTransformer, objectMapper)

  @Test
  fun `findOpenResultsToChangeRequestSummary transforms correctly`() {
    every { mockPersonTransformer.personSummaryInfoToPersonSummary(personInfoResult) } returns personSummary

    val findOpenChangeRequestResult = object : FindOpenChangeRequestResult {
      override val id = UUID.randomUUID()
      override val crn = offenderDetailSummary.otherIds.crn
      override val type = ChangeRequestType.PLACEMENT_APPEAL.name
      override val createdAt = java.time.Instant.now()
      override val lengthOfStayDays = 30
      override val tier = "TierA"
      override val expectedArrivalDate = LocalDate.of(2025, 1, 1)
      override val actualArrivalDate: LocalDate? = null
    }

    val expected = Cas1ChangeRequestSummary(
      id = findOpenChangeRequestResult.id,
      person = personSummary,
      type = Cas1ChangeRequestType.PLACEMENT_APPEAL,
      createdAt = findOpenChangeRequestResult.createdAt,
      lengthOfStayDays = findOpenChangeRequestResult.lengthOfStayDays,
      tier = findOpenChangeRequestResult.tier,
      expectedArrivalDate = findOpenChangeRequestResult.expectedArrivalDate,
      actualArrivalDate = findOpenChangeRequestResult.actualArrivalDate,
    )

    val result = cas1ChangeRequestTransformer.findOpenResultsToChangeRequestSummary(findOpenChangeRequestResult, personInfoResult)

    assertThat(result).isInstanceOf(Cas1ChangeRequestSummary::class.java)
    assertThat(result).isEqualTo(expected)
  }

  @Test
  fun `transformEntityToCas1ChangeRequest transforms correctly`() {
    val expected = Cas1ChangeRequest(
      id = entity.id,
      type = Cas1ChangeRequestType.valueOf(entity.type.name),
      createdAt = entity.createdAt.toInstant(),
      requestReason = NamedId(id = entity.requestReason.id, name = entity.requestReason.code),
      updatedAt = entity.updatedAt.toInstant(),
      decision = Cas1ChangeRequestDecision.APPROVED,
      decisionJson = null,
      rejectionReason = null,
    )

    val result = cas1ChangeRequestTransformer.transformEntityToCas1ChangeRequest(entity)

    assertThat(result).isInstanceOf(Cas1ChangeRequest::class.java)
    assertThat(result).isEqualTo(expected)
  }
}
