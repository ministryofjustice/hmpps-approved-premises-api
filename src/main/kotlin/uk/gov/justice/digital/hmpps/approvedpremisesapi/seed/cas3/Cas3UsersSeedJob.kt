package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.AbstractUsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

class Cas3UsersSeedJob(userService: UserService) : AbstractUsersSeedJob(listOf(ServiceName.temporaryAccommodation), userService)
