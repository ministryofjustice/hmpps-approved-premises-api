package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

class AllCasUsersSeedJob(userService: UserService) : AbstractUsersSeedJob(ServiceName.entries, userService)
