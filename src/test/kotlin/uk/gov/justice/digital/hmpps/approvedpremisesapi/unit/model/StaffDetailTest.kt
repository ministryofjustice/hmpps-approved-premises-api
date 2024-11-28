package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.PersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomLong
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class StaffDetailTest {
  private val staffCode = randomStringMultiCaseWithNumbers(10).uppercase()
  private val staffIdentifier = randomLong()
  private val forename = randomStringLowerCase(10)
  private val middleName = randomStringLowerCase(10)
  private val surname = randomStringUpperCase(10)
  private val username = randomStringUpperCase(10)

  @Test
  fun `toStaffMember converts to a StaffMember`() {
    val staffDetail = StaffDetailFactory.staffDetail(
      code = staffCode,
      staffIdentifier = staffIdentifier,
      name = PersonName(forename = forename, surname = surname),
      deliusUsername = username,
    ).toStaffMember()

    val staffMember = StaffMember(
      staffCode = staffCode,
      staffIdentifier = staffIdentifier,
      forenames = forename,
      surname = surname,
      username = username,
    )
    assertThat(staffDetail.staffCode).isEqualTo(staffMember.staffCode)
    assertThat(staffDetail.staffIdentifier).isEqualTo(staffMember.staffIdentifier)
    assertThat(staffDetail.forenames).isEqualTo(staffMember.forenames)
    assertThat(staffDetail.surname).isEqualTo(staffMember.surname)
    assertThat(staffDetail.username).isEqualTo(staffMember.username)
  }

  @Test
  fun `staff detail produces correct forenames`() {
    val staffDetail = StaffDetailFactory.staffDetail(
      name = PersonName(forename = forename, middleName = middleName, surname = surname),
    )
    assertThat(staffDetail.name.forenames()).isEqualTo("$forename $middleName")
  }

  @Test
  fun `staff detail produces correct name with middle names`() {
    val staffDetail = StaffDetailFactory.staffDetail(
      name = PersonName(forename = forename, middleName = middleName, surname = surname),
    )
    assertThat(staffDetail.name.deliusName()).isEqualTo("$forename $middleName $surname")
  }

  @Test
  fun `staff detail produces correct name without middle names`() {
    val staffDetail = StaffDetailFactory.staffDetail(
      name = PersonName(forename = forename, surname = surname),
    )
    assertThat(staffDetail.name.deliusName()).isEqualTo("$forename $surname")
  }
}
