package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Offence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenceDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDateTime

class OffenceFactory : Factory<Offence> {
  private var offenceId: Yielded<String> = { randomInt(0, 1000).toString() }
  private var offenceDate: Yielded<LocalDateTime?> = { LocalDateTime.now().minusDays(10) }

  private var code: Yielded<String> = { randomStringMultiCaseWithNumbers(4) }
  private var description: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var mainCategoryDescription: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var subCategoryDescription: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }

  fun withOffenceId(offenceId: String) = apply {
    this.offenceId = { offenceId }
  }

  fun withOffenceDate(offenceDate: LocalDateTime?) = apply {
    this.offenceDate = { offenceDate }
  }

  fun withCode(code: String) = apply {
    this.code = { code }
  }

  fun withDescription(description: String) = apply {
    this.description = { description }
  }

  fun withMainCategoryDescription(mainCategoryDescription: String) = apply {
    this.mainCategoryDescription = { mainCategoryDescription }
  }

  fun withSubCategoryDescription(subCategoryDescription: String) = apply {
    this.subCategoryDescription = { subCategoryDescription }
  }

  override fun produce(): Offence = Offence(
    offenceId = this.offenceId(),
    detail = OffenceDetail(
      mainCategoryDescription = this.mainCategoryDescription(),
      subCategoryDescription = this.subCategoryDescription(),
    ),
    offenceDate = this.offenceDate(),
  )
}
