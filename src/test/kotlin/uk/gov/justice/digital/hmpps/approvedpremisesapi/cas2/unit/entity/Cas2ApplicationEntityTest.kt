package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.entity

import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem

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
    val user2 = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce()
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
  }

  @Test
  fun `isCreatedBy returns true when cas2 user did create application`() {
    val cas2User = Cas2UserEntityFactory()
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(cas2User)
      .produce()

    assertThat(application.isCreatedBy(cas2User)).isTrue()
  }

  @Test
  fun `isCreatedBy returns false when cas2 user did not create application`() {
    val cas2User = Cas2UserEntityFactory()
      .produce()
    val originalCas2User = Cas2UserEntityFactory()
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(originalCas2User)
      .produce()

    assertThat(application.isCreatedBy(cas2User)).isFalse()
  }

  @Test
  fun `returns correct user type with cas2user of type delius`() {
    val cas2User = Cas2UserEntityFactory()
      .withName("cas2user_name")
      .withUserType(Cas2UserType.DELIUS)
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(cas2User)
      .produce()

    assertThat(Cas2StaffMember.Usertype.delius).isEqualTo(application.getCreatedByUserType())
  }

  @Test
  fun `returns correct user type with cas2user of type nomis`() {
    val cas2User = Cas2UserEntityFactory()
      .withName("cas2user_name")
      .withUserType(Cas2UserType.NOMIS)
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(cas2User)
      .produce()

    assertThat(Cas2StaffMember.Usertype.nomis).isEqualTo(application.getCreatedByUserType())
  }

  @Test
  fun `cas2user of type external throws ForbiddenProblem when user type is external`() {
    val cas2User = Cas2UserEntityFactory()
      .withName("cas2user_name")
      .withUserType(Cas2UserType.EXTERNAL)
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(cas2User)
      .produce()

    assertThrows<ForbiddenProblem> { application.getCreatedByUserType() }
  }
}
