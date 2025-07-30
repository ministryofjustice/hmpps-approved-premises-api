package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class PremisesFactory : Factory<Premises> {
  private var addressLine1: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var addressLine2: Yielded<String?> = { null }
  private var postcode: Yielded<String> = { randomPostCode() }
  private var town: Yielded<String?> = { null }
  private var region: Yielded<String> = { randomStringUpperCase(16) }

  fun withAddressLine1(addressLine1: String) = apply {
    this.addressLine1 = { addressLine1 }
  }

  fun withAddressLine2(addressLine2: String?) = apply {
    this.addressLine2 = { addressLine2 }
  }

  fun withPostcode(postcode: String) = apply {
    this.postcode = { postcode }
  }

  fun withTown(town: String?) = apply {
    this.town = { town }
  }

  fun withRegion(region: String) = apply {
    this.region = { region }
  }

  override fun produce() = Premises(
    addressLine1 = this.addressLine1(),
    addressLine2 = this.addressLine2(),
    postcode = this.postcode(),
    town = this.town(),
    region = this.region(),
  )
}
