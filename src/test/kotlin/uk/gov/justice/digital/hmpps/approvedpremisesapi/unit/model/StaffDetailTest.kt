package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.PersonName

class StaffDetailTest {
  private val staffCode = "ABC123"
  private val staffIdentifier = "123".toLong()
  private val forename = "Bruce"
  private val middleName = "Middle"
  private val surname = "Lee"
  private val username = "SOME_USERNAME"

  @Test
  fun `toStaffMember converts to a StaffMember`() {
    val staffDetail = StaffDetailFactory.staffDetail().copy(
      code = staffCode,
      staffIdentifier = staffIdentifier,
      name = PersonName(forename = forename, surname = surname),
      username = username,
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
    val staffDetail = StaffDetailFactory.staffDetail().copy(
      name = PersonName(forename = forename, middleName = middleName, surname = surname),
    )
    assertThat(staffDetail.name.forenames()).isEqualTo("$forename $middleName")
  }

  @Test
  fun `staff detail produces correct Delius name`() {
    val staffDetail = StaffDetailFactory.staffDetail().copy(
      name = PersonName(forename = forename, middleName = middleName, surname = surname),
    )
    assertThat(staffDetail.name.deliusName()).isEqualTo("$forename $surname")
  }
}
