package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.seed

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.AbstractUsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.UsersBasicSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

/**
 * Seeds users, along with their roles and qualifications.
 *
 * NB: this clears any existing CAS3 roles and qualifications
 *
 * If you want to seed users without touching the pre-existing roles/qualifications
 * of pre-existing users, then consider using [UsersBasicSeedJob].
 */
@Component
class Cas3UsersSeedJob(userService: UserService) : AbstractUsersSeedJob(listOf(ServiceName.temporaryAccommodation), userService)
