package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.AbstractUsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.UsersBasicSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.UsersSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

/**
 * Seeds users, along with their roles and qualifications.
 *
 * NB: this clears any existing CAS1 roles and qualifications
 *
 * If you want to seed users without touching the pre-existing roles/qualifications
 * of pre-existing users, then consider using [UsersBasicSeedJob].
 */
@Component
class Cas1UsersSeedJob(
  userService: UserService,
  private val cas1CruManagementAreaRepository: Cas1CruManagementAreaRepository,
) : AbstractUsersSeedJob(listOf(ServiceName.approvedPremises), userService) {

  override fun processRowForCas(row: UsersSeedCsvRow, user: UserEntity) {
    if (row.cruManagementAreaOverride != null) {
      val override = cas1CruManagementAreaRepository.findByName(row.cruManagementAreaOverride)
        ?: error("Could not find cru management area with name '${row.cruManagementAreaOverride}'")
      userService.updateCruManagementOverride(user, override)
    }
  }
}
