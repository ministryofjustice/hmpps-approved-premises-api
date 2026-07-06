package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ChangeRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ChangeRequestTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.JsonMapperFactory

class Cas1ChangeRequestTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()

  private val jsonMapper = JsonMapperFactory.createJackson3JsonMapper()

  val offenderDetailSummary = OffenderDetailsSummaryFactory().produce()

  val entity = Cas1ChangeRequestEntityFactory()
    .withDecision(ChangeRequestDecision.APPROVED)
    .produce()

  private val cas1ChangeRequestTransformer = Cas1ChangeRequestTransformer(mockPersonTransformer, jsonMapper)

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
      requestJson = entity.requestJson,
      spaceBookingId = entity.spaceBooking.id,
    )

    val result = cas1ChangeRequestTransformer.transformEntityToCas1ChangeRequest(entity)

    assertThat(result).isInstanceOf(Cas1ChangeRequest::class.java)
    assertThat(result).isEqualTo(expected)
  }
}
