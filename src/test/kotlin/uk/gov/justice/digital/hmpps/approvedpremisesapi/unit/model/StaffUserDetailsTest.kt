package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.PersonName

class StaffUserDetailsTest {
  @Test
  fun `toStaffMember converts to a StaffMember`() {
    val staffCode = "ABC123"
    val staffIdentifier = "123".toLong()
    val forenames = "Bruce"
    val surname = "Lee"
    val username = "SOME_USERNAME"

    val staffDetail = StaffDetailFactory.staffDetail().copy(code = staffCode, staffIdentifier = staffIdentifier, name = PersonName(forename = forenames, surname = surname), username = username)

    val staffUserDetails = StaffUserDetailsFactory()
      .withStaffCode(staffCode)
      .withStaffIdentifier(staffIdentifier)
      .withForenames(forenames)
      .withSurname(surname)
      .withUsername(username)
      .produce()

    assertThat(staffUserDetails.toStaffMember()).isEqualTo(staffDetail.toStaffMember())

    assertThat(staffUserDetails.toStaffMember()).isEqualTo(
      StaffMember(
        staffCode = staffCode,
        staffIdentifier = staffIdentifier,
        forenames = forenames,
        surname = surname,
        username = username,
      ),
    )
  }
}
