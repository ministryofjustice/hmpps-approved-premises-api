package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.LicenceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.LicenceSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.LicenceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomLong
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDateTime

class LicenceSummaryFactory : Factory<LicenceSummary> {
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
  var isInPssPeriod: Yielded<Boolean?> = { null }

  fun withId(id: Long) = apply { this.id = { id } }
  fun withStatus(status: LicenceStatus) = apply { this.statusCode = { status } }
  fun withCrn(crn: String?) = apply { this.crn = { crn } }
  fun withLicenceType(type: LicenceType) = apply { this.licenceType = { type } }

  override fun produce(): LicenceSummary = LicenceSummary(
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
    isInPssPeriod = this.isInPssPeriod(),
  )
}
