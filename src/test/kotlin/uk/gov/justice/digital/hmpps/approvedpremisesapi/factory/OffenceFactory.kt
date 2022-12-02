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
  private var mainOffence: Yielded<Boolean> = { true }
  private var offenceDate: Yielded<LocalDateTime> = { LocalDateTime.now().minusDays(10) }
  private var offenceCount: Yielded<Long> = { randomInt(1, 10).toLong() }
  private var tics: Yielded<Long> = { randomInt(1, 10).toLong() }
  private var verdict: Yielded<String> = { "Guilty" }
  private var offenderId: Yielded<Long> = { randomInt(0, 1000).toLong() }
  private var createdDatetime: Yielded<LocalDateTime> = { LocalDateTime.now().minusDays(10) }
  private var lastUpdatedDatetime: Yielded<LocalDateTime> = { LocalDateTime.now().minusDays(10) }

  private var code: Yielded<String> = { randomStringMultiCaseWithNumbers(4) }
  private var description: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var abbreviation: Yielded<String?> = { null }
  private var mainCategoryCode: Yielded<String> = { randomStringMultiCaseWithNumbers(4) }
  private var mainCategoryDescription: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var mainCategoryAbbreviation: Yielded<String?> = { null }
  private var ogrsOffenceCategory: Yielded<String> = { randomStringMultiCaseWithNumbers(4) }
  private var subCategoryCode: Yielded<String> = { randomStringMultiCaseWithNumbers(4) }
  private var subCategoryDescription: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var form20Code: Yielded<String?> = { null }
  private var subCategoryAbbreviation: Yielded<String?> = { null }
  private var cjitCode: Yielded<String?> = { null }

  fun withOffenceId(offenceId: String) = apply {
    this.offenceId = { offenceId }
  }

  fun withMainOffence(mainOffence: Boolean) = apply {
    this.mainOffence = { mainOffence }
  }

  fun withOffenceDate(offenceDate: LocalDateTime) = apply {
    this.offenceDate = { offenceDate }
  }

  fun withOffenceCount(offenceCount: Long) = apply {
    this.offenceCount = { offenceCount }
  }

  fun withTics(tics: Long) = apply {
    this.tics = { tics }
  }

  fun withVerdict(verdict: String) = apply {
    this.verdict = { verdict }
  }

  fun withOffenderId(offenderId: Long) = apply {
    this.offenderId = { offenderId }
  }

  fun withCreatedDateTime(createdDateTime: LocalDateTime) = apply {
    this.createdDatetime = { createdDateTime }
  }

  fun withLastUpdatedDateTime(lastUpdatedDateTime: LocalDateTime) = apply {
    this.lastUpdatedDatetime = { lastUpdatedDateTime }
  }

  fun withCode(code: String) = apply {
    this.code = { code }
  }

  fun withDescription(description: String) = apply {
    this.description = { description }
  }

  fun withAbbreviation(abbreviation: String?) = apply {
    this.abbreviation = { abbreviation }
  }

  fun withMainCategoryCode(mainCategoryCode: String) = apply {
    this.mainCategoryCode = { mainCategoryCode }
  }

  fun withMainCategoryDescription(mainCategoryDescription: String) = apply {
    this.mainCategoryDescription = { mainCategoryDescription }
  }

  fun withMainCategoryAbbreviation(mainCategoryAbbreviation: String?) = apply {
    this.mainCategoryAbbreviation = { mainCategoryAbbreviation }
  }

  fun withOgrsOffenceCategory(ogrsOffenceCategory: String) = apply {
    this.ogrsOffenceCategory = { ogrsOffenceCategory }
  }

  fun withSubCategoryCode(subCategoryCode: String) = apply {
    this.subCategoryCode = { subCategoryCode }
  }

  fun withSubCategoryDescription(subCategoryDescription: String) = apply {
    this.subCategoryDescription = { subCategoryDescription }
  }

  fun withForm20Code(form20Code: String?) = apply {
    this.form20Code = { form20Code }
  }

  fun withSubCategoryAbbreviation(subCategoryAbbreviation: String?) = apply {
    this.subCategoryAbbreviation = { subCategoryAbbreviation }
  }

  fun withCjitCode(cjitCode: String?) = apply {
    this.cjitCode = { cjitCode }
  }

  override fun produce(): Offence = Offence(
    offenceId = this.offenceId(),
    mainOffence = this.mainOffence(),
    detail = OffenceDetail(
      code = this.code(),
      description = this.description(),
      abbreviation = this.abbreviation(),
      mainCategoryCode = this.mainCategoryCode(),
      mainCategoryDescription = this.mainCategoryDescription(),
      mainCategoryAbbreviation = this.mainCategoryAbbreviation(),
      ogrsOffenceCategory = this.ogrsOffenceCategory(),
      subCategoryCode = this.subCategoryCode(),
      subCategoryDescription = this.subCategoryDescription(),
      form20Code = this.form20Code(),
      subCategoryAbbreviation = this.subCategoryAbbreviation(),
      cjitCode = this.cjitCode()
    ),
    offenceDate = this.offenceDate(),
    offenceCount = this.offenceCount(),
    tics = this.tics(),
    verdict = this.verdict(),
    offenderId = this.offenderId(),
    createdDatetime = this.createdDatetime(),
    lastUpdatedDatetime = this.lastUpdatedDatetime()
  )
}
