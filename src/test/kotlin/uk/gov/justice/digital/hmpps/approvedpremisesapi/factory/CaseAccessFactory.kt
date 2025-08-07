package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class CaseAccessFactory : Factory<CaseAccess> {
  var crn: Yielded<String> = { randomStringUpperCase(10) }
  var userExcluded: Yielded<Boolean> = { false }
  var userRestricted: Yielded<Boolean> = { false }
  var exclusionMessage: Yielded<String?> = { null }
  var restrictionMessage: Yielded<String?> = { null }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }
  fun withAccess() = apply {
    withUserExcluded(false)
    withUserRestricted(false)
  }
  fun withUserExcluded(userExcluded: Boolean) = apply {
    this.userExcluded = { userExcluded }
  }
  fun withUserRestricted(userRestricted: Boolean) = apply {
    this.userRestricted = { userRestricted }
  }
  fun withExclusionMessage(exclusionMessage: String?) = apply {
    this.exclusionMessage = { exclusionMessage }
  }
  fun withRestrictionMessage(restrictionMessage: String?) = apply {
    this.restrictionMessage = { restrictionMessage }
  }

  override fun produce(): CaseAccess = CaseAccess(
    crn = this.crn(),
    userExcluded = this.userExcluded(),
    userRestricted = this.userRestricted(),
    exclusionMessage = this.exclusionMessage(),
    restrictionMessage = this.restrictionMessage(),
  )
}
