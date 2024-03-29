package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas3

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class PersonReferenceFactory : Factory<PersonReference> {
  private var crn: Yielded<String> = { randomStringUpperCase(6) }
  private var noms: Yielded<String?> = { randomStringUpperCase(10) }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withNoms(noms: String?) = apply {
    this.noms = { noms }
  }

  override fun produce() = PersonReference(
    crn = this.crn(),
    noms = this.noms(),
  )
}
