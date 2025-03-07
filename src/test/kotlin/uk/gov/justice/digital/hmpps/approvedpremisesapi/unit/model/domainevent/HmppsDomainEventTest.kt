package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.model.domainevent

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.PersonReference
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
    assertThat(message.personReference.findNomsNumber()).isEqualTo("nomsNumber")
  }

  @Test
  fun `findNomsNumber gets null from HmppsDomainEvent if no NOMS number in personReference`() {
    val message = buildDomainEvent(personReference = PersonReference(emptyList()))
    assertThat(message.personReference.findNomsNumber()).isNull()
  }
}
