package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.AdditionalCondition
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.ApConditions
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.BespokeCondition
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.ConditionTypes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.ElectronicMonitoringAdditionalConditionWithRestriction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.ElectronicMonitoringType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.GenericAdditionalCondition
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.Licence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.LicenceConditions
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.LicenceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.LicenceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.MultipleExclusionZoneAdditionalCondition
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.PssConditions
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.SingleUploadAdditionalCondition
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.StandardCondition
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomLong
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.LocalDateTime

class LicenceFactory : Factory<Licence> {
  var id: Yielded<Long> = { randomLong() }
  var kind: Yielded<String?> = { null }
  var licenceType: Yielded<LicenceType> = { LicenceType.AP }
  var policyVersion: Yielded<String?> = { null }
  var version: Yielded<String?> = { null }
  var statusCode: Yielded<LicenceStatus> = { LicenceStatus.ACTIVE }
  var prisonNumber: Yielded<String?> = { randomStringUpperCase(6) }
  var bookingId: Yielded<Long?> = { randomLong() }
  var crn: Yielded<String?> = { "X" + randomStringUpperCase(5) }
  var approvedByUsername: Yielded<String?> = { null }
  var approvedDateTime: Yielded<LocalDateTime?> = { null }
  var createdByUsername: Yielded<String?> = { randomStringLowerCase(8) }
  var createdDateTime: Yielded<LocalDateTime?> = { LocalDateTime.now() }
  var updatedByUsername: Yielded<String?> = { null }
  var updatedDateTime: Yielded<LocalDateTime?> = { null }
  var licenceStartDate: Yielded<LocalDate?> = { LocalDate.now() }
  var isInPssPeriod: Yielded<Boolean?> = { null }
  var conditions: Yielded<LicenceConditions> = { LicenceConditionsFactory().produce() }

  fun withId(id: Long) = apply { this.id = { id } }
  fun withStatus(status: LicenceStatus) = apply { this.statusCode = { status } }
  fun withCrn(crn: String?) = apply { this.crn = { crn } }
  fun withLicenceType(type: LicenceType) = apply { this.licenceType = { type } }
  fun withConditions(conditions: LicenceConditions) = apply { this.conditions = { conditions } }

  override fun produce(): Licence = Licence(
    id = this.id(),
    kind = this.kind(),
    licenceType = this.licenceType(),
    policyVersion = this.policyVersion(),
    version = this.version(),
    statusCode = this.statusCode(),
    prisonNumber = this.prisonNumber(),
    bookingId = this.bookingId(),
    crn = this.crn(),
    approvedByUsername = this.approvedByUsername(),
    approvedDateTime = this.approvedDateTime(),
    createdByUsername = this.createdByUsername(),
    createdDateTime = this.createdDateTime(),
    updatedByUsername = this.updatedByUsername(),
    updatedDateTime = this.updatedDateTime(),
    licenceStartDate = this.licenceStartDate(),
    isInPssPeriod = this.isInPssPeriod(),
    conditions = this.conditions(),
  )
}

class LicenceConditionsFactory : Factory<LicenceConditions> {
  var apConditions: Yielded<ApConditions> = { ApConditionsFactory().produce() }
  var pssConditions: Yielded<PssConditions> = { PssConditionsFactory().produce() }

  fun withApConditions(ap: ApConditions) = apply { this.apConditions = { ap } }
  fun withPssConditions(pss: PssConditions) = apply { this.pssConditions = { pss } }

  override fun produce(): LicenceConditions = LicenceConditions(
    apConditions = this.apConditions(),
    pssConditions = this.pssConditions(),
  )
}

class ApConditionsFactory : Factory<ApConditions> {
  var standard: Yielded<List<StandardCondition>> = { listOf(StandardConditionFactory().produce()) }
  var additional: Yielded<List<AdditionalCondition>> = { listOf(AdditionalConditionFactory().produce()) }
  var bespoke: Yielded<List<BespokeCondition>> = { listOf(BespokeConditionFactory().produce()) }

  fun withStandard(standard: List<StandardCondition>) = apply { this.standard = { standard } }
  fun withAdditional(additional: List<AdditionalCondition>) = apply { this.additional = { additional } }
  fun withBespoke(bespoke: List<BespokeCondition>) = apply { this.bespoke = { bespoke } }

  override fun produce(): ApConditions = ApConditions(
    standard = this.standard(),
    additional = this.additional(),
    bespoke = this.bespoke(),
  )
}

class PssConditionsFactory : Factory<PssConditions> {
  var standard: Yielded<List<StandardCondition>> = { listOf(StandardConditionFactory().produce()) }
  var additional: Yielded<List<AdditionalCondition>> = { listOf(AdditionalConditionFactory().produce()) }

  fun withStandard(standard: List<StandardCondition>) = apply { this.standard = { standard } }
  fun withAdditional(additional: List<AdditionalCondition>) = apply { this.additional = { additional } }

  override fun produce(): PssConditions = PssConditions(
    standard = this.standard(),
    additional = this.additional(),
  )
}

class StandardConditionFactory : Factory<StandardCondition> {
  var code: Yielded<String?> = { randomStringUpperCase(4) }
  var text: Yielded<String?> = { randomStringLowerCase(12) }

  fun withCode(code: String?) = apply { this.code = { code } }
  fun withText(text: String?) = apply { this.text = { text } }

  override fun produce(): StandardCondition = StandardCondition(
    code = this.code(),
    text = this.text(),
  )
}

class AdditionalConditionFactory : Factory<AdditionalCondition> {
  var id: Yielded<Long> = { randomLong() }
  var type: Yielded<String> = { ConditionTypes.STANDARD }
  var text: Yielded<String> = { randomStringLowerCase(12) }
  var code: Yielded<String> = { randomStringUpperCase(4) }
  var category: Yielded<String> = { "CATEGORY" }

  fun withId(id: Long) = apply { this.id = { id } }
  fun withType(type: String) = apply { this.type = { type } }
  fun withText(text: String) = apply { this.text = { text } }
  fun withCode(code: String) = apply { this.code = { code } }
  fun withCategory(category: String) = apply { this.category = { category } }

  override fun produce(): AdditionalCondition = when (this.type()) {
    ConditionTypes.STANDARD -> GenericAdditionalCondition(
      id = this.id(),
      text = this.text(),
      code = this.code(),
      category = this.category(),
    )
    ConditionTypes.ELECTRONIC_MONITORING -> ElectronicMonitoringAdditionalConditionWithRestriction(
      id = this.id(),
      text = this.text(),
      code = this.code(),
      category = this.category(),
      restrictions = emptyList<ElectronicMonitoringType>(),
    )
    ConditionTypes.MULTIPLE_EXCLUSION_ZONE -> MultipleExclusionZoneAdditionalCondition(
      id = this.id(),
      text = this.text(),
      code = this.code(),
      category = this.category(),
      hasImageUpload = true,
    )
    ConditionTypes.SINGLE_UPLOAD -> SingleUploadAdditionalCondition(
      id = this.id(),
      text = this.text(),
      code = this.code(),
      category = this.category(),
      hasImageUpload = true,
    )
    else -> GenericAdditionalCondition(
      id = this.id(),
      text = this.text(),
      code = this.code(),
      category = this.category(),
    )
  }
}

class BespokeConditionFactory : Factory<BespokeCondition> {
  var text: Yielded<String?> = { randomStringLowerCase(12) }

  fun withText(text: String?) = apply { this.text = { text } }

  override fun produce(): BespokeCondition = BespokeCondition(
    text = this.text(),
  )
}
