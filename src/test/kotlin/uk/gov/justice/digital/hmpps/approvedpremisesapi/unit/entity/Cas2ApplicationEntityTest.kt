package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory

class Cas2ApplicationEntityTest {

  private val user = NomisUserEntityFactory().produce()
  private val nomsNumber = "NOMSABC"

  @Test
  fun `should access current prison code and current pom Id`() {
    val prisonCode = "A1234AB"

    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withCreatedByUser(user).produce()
    application.createApplicationAssignment(prisonCode = prisonCode, allocatedPomUser = user)
    assertThat(application.currentPrisonCode).isEqualTo(prisonCode)
    assertThat(application.mostRecentPomUserId).isEqualTo(user.id)
  }
}
