package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Borough
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.PersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Team
import java.time.LocalDate

object StaffDetailFactory {
  fun staffDetail() =
    StaffDetail(
      email = "foo@example.com",
      telephoneNumber = "0123456789",
      staffIdentifier = 5678L,
      teams = listOf(team()),
      probationArea = probationArea(),
      username = "deliususername",
      name = PersonName("New", "Name", "C"),
      code = "STAFF1",
      active = true,
    )

  fun team() =
    Team(
      code = "TEAMCODE1",
      name = "TEAMNAME",
      ldu = ldo(),
      borough = Borough("B1", "B1 Borough"),
      startDate = LocalDate.now().minusYears(1),
      endDate = null,
    )

  fun ldo() = Ldu("LDUCODE", "LDUNAME")

  fun probationArea() = ProbationArea("PACODE", "Probation Area Description")
}
