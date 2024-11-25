package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.AbstractUsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

class Cas1UsersSeedJob(userService: UserService) : AbstractUsersSeedJob(listOf(ServiceName.approvedPremises), userService)
