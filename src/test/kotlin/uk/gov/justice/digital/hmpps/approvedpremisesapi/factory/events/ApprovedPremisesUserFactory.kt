package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class ApprovedPremisesUserFactory : Factory<ApprovedPremisesUser> {
  private var qualifications: Yielded<List<UserQualification>> = { listOf() }
  private var roles: Yielded<List<ApprovedPremisesUserRole>> = { listOf() }
  private var apArea: Yielded<ApArea> = {
    ApArea(
      UUID.randomUUID(),
      randomStringUpperCase(6),
      randomStringMultiCaseWithNumbers(20),
    )
  }
  private var cruManagementArea: Yielded<NamedId> = {
    NamedId(UUID.randomUUID(), randomStringUpperCase(6))
  }
  private var cruManagementAreaDefault: Yielded<NamedId> = {
    NamedId(UUID.randomUUID(), randomStringUpperCase(6))
  }
  private var cruManagementAreaOverride: Yielded<NamedId?> = { null }
  private var service: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var deliusUsername: Yielded<String> = { randomStringUpperCase(6) }
  private var region: Yielded<ProbationRegion> = {
    ProbationRegion(
      UUID.randomUUID(),
      randomStringMultiCaseWithNumbers(20),
    )
  }
  private var email: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var telephoneNumber: Yielded<String?> = { null }
  private var isActive: Yielded<Boolean?> = { true }

  fun withQualifications(qualifications: List<UserQualification>) = apply {
    this.qualifications = { qualifications }
  }

  fun withRoles(roles: List<ApprovedPremisesUserRole>) = apply {
    this.roles = { roles }
  }

  fun withApArea(apArea: ApArea) = apply {
    this.apArea = { apArea }
  }

  fun withService(service: String) = apply {
    this.service = { service }
  }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withDeliusUsername(deliusUsername: String) = apply {
    this.deliusUsername = { deliusUsername }
  }

  fun withRegion(region: ProbationRegion) = apply {
    this.region = { region }
  }

  fun withEmail(email: String?) = apply {
    this.email = { email }
  }

  fun withTelephoneNumber(telephoneNumber: String?) = apply {
    this.telephoneNumber = { telephoneNumber }
  }

  fun withIsActive(isActive: Boolean?) = apply {
    this.isActive = { isActive }
  }

  override fun produce() = ApprovedPremisesUser(
    qualifications = this.qualifications(),
    roles = this.roles(),
    apArea = this.apArea(),
    cruManagementArea = this.cruManagementArea(),
    cruManagementAreaDefault = this.cruManagementAreaDefault(),
    cruManagementAreaOverride = this.cruManagementAreaOverride(),
    service = this.service(),
    id = this.id(),
    name = this.name(),
    deliusUsername = this.deliusUsername(),
    region = this.region(),
    email = this.email(),
    telephoneNumber = this.telephoneNumber(),
    isActive = this.isActive(),
  )
}
