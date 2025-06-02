package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity.cas2

import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2.Cas2UserType
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
  }

  @Test
  fun `nomis user returns correct nomisUsername with getCreatedByUsername`() {
    val nomisUser = NomisUserEntityFactory()
      .withNomisUsername("nomis_username")
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(nomisUser)
      .withCreatedByCas2User(null)
      .produce()

    assertThat(application.createdByUser.nomisUsername).isEqualTo(application.getCreatedByUsername())
  }

  @Test
  fun `cas2user returns correct username with getCreatedByUsername`() {
    val cas2User = Cas2UserEntityFactory()
      .withUsername("cas2user_name")
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByCas2User(cas2User)
      .produce()

    assertThat(application.createdByCas2User?.username).isEqualTo(application.getCreatedByUsername())
  }

  @Test
  fun `nomis user returns correct name with getCreatedByCanonicalName`() {
    val nomisUser = NomisUserEntityFactory()
      .withName("nomis_name")
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(nomisUser)
      .withCreatedByCas2User(null)
      .produce()

    assertThat(application.createdByUser.name).isEqualTo(application.getCreatedByCanonicalName())
  }

  @Test
  fun `cas2user returns correct name with getCreatedByCanonicalName`() {
    val cas2User = Cas2UserEntityFactory()
      .withName("cas2user_name")
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByCas2User(cas2User)
      .produce()

    assertThat(application.createdByCas2User?.name).isEqualTo(application.getCreatedByCanonicalName())
  }

  @Test
  fun `nomis user returns correct id with getCreatedById`() {
    val nomisUser = NomisUserEntityFactory()
      .withName("nomis_name")
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(nomisUser)
      .withCreatedByCas2User(null)
      .produce()

    assertThat(application.createdByUser.id).isEqualTo(application.getCreatedById())
  }

  @Test
  fun `cas2user returns correct id with getCreatedById`() {
    val cas2User = Cas2UserEntityFactory()
      .withName("cas2user_name")
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByCas2User(cas2User)
      .produce()

    assertThat(application.createdByCas2User?.id).isEqualTo(application.getCreatedById())
  }

  @Test
  fun `nomis user returns correct user identifier with getCreatedByUserIdentifier`() {
    val nomisUser = NomisUserEntityFactory()
      .withName("nomis_name")
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(nomisUser)
      .withCreatedByCas2User(null)
      .produce()

    assertThat(application.createdByUser.nomisStaffId.toString()).isEqualTo(application.getCreatedByUserIdentifier())
  }

  @Test
  fun `cas2user returns correct user identifier with getCreatedByUserIdentifier`() {
    val cas2User = Cas2UserEntityFactory()
      .withName("cas2user_name")
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByCas2User(cas2User)
      .produce()

    assertThat(application.createdByCas2User?.staffIdentifier()).isEqualTo(application.getCreatedByUserIdentifier())
  }

  @Test
  fun `nomis user returns correct email with getCreatedByUserEmail`() {
    val nomisUser = NomisUserEntityFactory()
      .withName("nomis_name")
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(nomisUser)
      .withCreatedByCas2User(null)
      .produce()

    assertThat(application.createdByUser.email).isEqualTo(application.getCreatedByUserEmail())
  }

  @Test
  fun `cas2user returns correct email with getCreatedByUserEmail`() {
    val cas2User = Cas2UserEntityFactory()
      .withName("cas2user_name")
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByCas2User(cas2User)
      .produce()

    assertThat(application.createdByCas2User?.email).isEqualTo(application.getCreatedByUserEmail())
  }

  @Test
  fun `nomis user returns correct name with getCreatedByUserType`() {
    val nomisUser = NomisUserEntityFactory()
      .withName("nomis_name")
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(nomisUser)
      .withCreatedByCas2User(null)
      .produce()

    assertThat(Cas2StaffMember.Usertype.nomis).isEqualTo(application.getCreatedByUserType())
  }

  @Test
  fun `returns correct user type with cas2user of type delius`() {
    val cas2User = Cas2UserEntityFactory()
      .withName("cas2user_name")
      .withUserType(Cas2UserType.DELIUS)
      .produce()
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByCas2User(cas2User)
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
      .withCreatedByCas2User(cas2User)
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
      .withCreatedByCas2User(cas2User)
      .produce()

    assertThrows<ForbiddenProblem> { application.getCreatedByUserType() }
  }
}
