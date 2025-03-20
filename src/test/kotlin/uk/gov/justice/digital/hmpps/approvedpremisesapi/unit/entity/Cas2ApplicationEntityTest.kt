package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import java.time.OffsetDateTime
import java.util.UUID

class Cas2ApplicationEntityTest {

  private val user = NomisUserEntityFactory().produce()
  private val nomsNumber = "NOMSABC"

  @Test
  fun `should access current prison code`() {
    val prisonCode = "A1234AB"

    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withCreatedByUser(user).produce()
    val oldAssignment = Cas2ApplicationAssignmentEntity(
      id = UUID.randomUUID(),
      application = application,
      prisonCode = prisonCode,
      allocatedPomUserId = user.id,
      createdAt = OffsetDateTime.now(),
    )

    application.applicationAssignments.add(oldAssignment)

    assertThat(application.currentPrisonCode).isEqualTo(prisonCode)
  }

  @Test
  fun `should throw error when no assignments exists`() {
    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withCreatedByUser(user).produce()
    val exception = assertThrows<NoSuchElementException> { application.currentPrisonCode }
    assertThat(exception.message).isEqualTo("List is empty.")
  }
}
