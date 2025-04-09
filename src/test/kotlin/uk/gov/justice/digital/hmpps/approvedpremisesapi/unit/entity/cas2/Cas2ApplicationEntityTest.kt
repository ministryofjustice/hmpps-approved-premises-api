package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity.cas2

import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2.Cas2ApplicationEntityFactory

class Cas2ApplicationEntityTest {

  @Test
  fun `createApplicationAssigment adds assignment to list`() {
    val application = Cas2ApplicationEntityFactory().produce()
    application.createApplicationAssignment("PRI", application.createdByUser)
    assertThat(application.applicationAssignments).hasSize(1)
  }

  @Test
  fun `entity returns values from the newest assignment`() {
    val application = Cas2ApplicationEntityFactory().produce()
    application.createApplicationAssignment("ONE", application.createdByUser)
    val user2 = NomisUserEntityFactory().produce()
    application.createApplicationAssignment("TWO", user2)
    assertThat(application.currentPrisonCode).isEqualTo("TWO")
    assertThat(application.currentPomUserId).isEqualTo(user2.id)
  }

  @Test
  fun `currentPomUserId returns null when not allocated`() {
    val application = Cas2ApplicationEntityFactory().produce()
    application.createApplicationAssignment("ONE", application.createdByUser)
    application.createApplicationAssignment("TWO", null)
    assertThat(application.currentPomUserId).isEqualTo(null)
    assertThat(application.mostRecentPomUserId).isEqualTo(application.createdByUser.id)
  }
}
