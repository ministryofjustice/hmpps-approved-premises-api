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
  fun `mostRecentLocationAssignment returns the most recent assignment that has null allocatedPomUser`() {
    val oldPrisonCode = "OLD"
    val newPrisonCode = "NEW"
    val nomisUser = NomisUserEntityFactory().produce()
    val nomsNumber = "123ABC"

    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withReferringPrisonCode(oldPrisonCode).withCreatedByUser(nomisUser).produce()

    application.createApplicationAssignment(prisonCode = oldPrisonCode, allocatedPomUser = nomisUser)
    application.createApplicationAssignment(prisonCode = newPrisonCode, allocatedPomUser = null)
    application.applicationAssignments.sortByDescending { it.createdAt }

    assertThat(application.mostRecentLocationAssignment!!.prisonCode).isEqualTo(newPrisonCode)
  }

  @Test
  fun `mostRecentLocationAssignment returns null if there aren't anny assignments with allocatedPomUser as null`() {
    val oldPrisonCode = "OLD"
    val newPrisonCode = "NEW"
    val nomisUser = NomisUserEntityFactory().produce()
    val nomsNumber = "123ABC"

    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withReferringPrisonCode(oldPrisonCode).withCreatedByUser(nomisUser).produce()

    application.createApplicationAssignment(prisonCode = oldPrisonCode, allocatedPomUser = nomisUser)
    application.createApplicationAssignment(prisonCode = newPrisonCode, allocatedPomUser = nomisUser)
    application.applicationAssignments.sortByDescending { it.createdAt }

    assertThat(application.mostRecentLocationAssignment).isNull()
  }

  @Test
  fun `hasLocationChangedAssignment returns true when the most recent assignment that has null allocatedPomUser`() {
    val oldPrisonCode = "OLD"
    val newPrisonCode = "NEW"
    val nomisUser = NomisUserEntityFactory().produce()
    val nomsNumber = "123ABC"

    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withReferringPrisonCode(oldPrisonCode).withCreatedByUser(nomisUser).produce()

    application.createApplicationAssignment(prisonCode = oldPrisonCode, allocatedPomUser = nomisUser)
    application.createApplicationAssignment(prisonCode = newPrisonCode, allocatedPomUser = null)
    application.applicationAssignments.sortByDescending { it.createdAt }

    assertThat(application.hasLocationChangedAssignment).isTrue()
  }

  @Test
  fun `hasLocationChangedAssignment returns false if there aren't anny assignments with allocatedPomUser as null`() {
    val oldPrisonCode = "OLD"
    val newPrisonCode = "NEW"
    val nomisUser = NomisUserEntityFactory().produce()
    val nomsNumber = "123ABC"

    val application = Cas2ApplicationEntityFactory().withNomsNumber(nomsNumber).withReferringPrisonCode(oldPrisonCode).withCreatedByUser(nomisUser).produce()

    application.createApplicationAssignment(prisonCode = oldPrisonCode, allocatedPomUser = nomisUser)
    application.createApplicationAssignment(prisonCode = newPrisonCode, allocatedPomUser = nomisUser)
    application.applicationAssignments.sortByDescending { it.createdAt }

    assertThat(application.hasLocationChangedAssignment).isFalse()
  }

  @Test
  fun `currentPomUserId returns null when not allocated`() {
    val application = Cas2ApplicationEntityFactory().produce()
    application.createApplicationAssignment("ONE", application.createdByUser)
    application.createApplicationAssignment("TWO", null)
    assertThat(application.currentPomUserId).isEqualTo(null)
  }
}
