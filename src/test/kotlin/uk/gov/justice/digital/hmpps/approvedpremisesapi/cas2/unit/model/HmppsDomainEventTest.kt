package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.model

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.AdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.PersonIdentifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.PersonReference
import java.time.ZonedDateTime

class HmppsDomainEventTest {

  private fun buildDomainEvent(
    occurredAt: ZonedDateTime = ZonedDateTime.now(),
    additionalInfo: AdditionalInformation = AdditionalInformation(mutableMapOf("staffCode" to "staffCode1234")),
    personReference: PersonReference = PersonReference(
      listOf(
        PersonIdentifier(
          type = "NOMS",
          value = "nomsNumber",
        ),
      ),
    ),
  ) = HmppsDomainEvent(
    eventType = "eventType",
    version = 1,
    detailUrl = "detailUrl",
    occurredAt = occurredAt,
    description = "description",
    additionalInformation = additionalInfo,
    personReference = personReference,
  )

  @Test
  fun `findNomsNumber gets nomsNumber from HmppsDomainEvent`() {
    val message = buildDomainEvent()
    Assertions.assertThat(message.personReference.findNomsNumber()).isEqualTo("nomsNumber")
  }

  @Test
  fun `findNomsNumber gets null from HmppsDomainEvent if no NOMS number in personReference`() {
    val message = buildDomainEvent(personReference = PersonReference(emptyList()))
    Assertions.assertThat(message.personReference.findNomsNumber()).isNull()
  }
}
