package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Borough
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate

object TeamFactory2 {
  @Suppress("LongParameterList")
  fun team(
    code: String = randomStringMultiCaseWithNumbers(6),
    name: String = randomStringMultiCaseWithNumbers(10),
    ldu: Ldu = Ldu(code = randomStringMultiCaseWithNumbers(6), name = randomStringMultiCaseWithNumbers(6)),
    borough: Borough = Borough(
      code = randomStringMultiCaseWithNumbers(6),
      description = randomStringMultiCaseWithNumbers(6),
    ),
    startDate: LocalDate = LocalDate.now().randomDateBefore(5),
    endDate: LocalDate? = null,
  ) = Team(
    code = code,
    name = name,
    ldu = ldu,
    borough = borough,
    startDate = startDate,
    endDate = endDate,
  )
}
