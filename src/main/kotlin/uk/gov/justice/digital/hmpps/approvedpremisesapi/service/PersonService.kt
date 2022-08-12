package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Person

@Service
class PersonService {
  fun getPerson(crn: String): Person? {
    // TODO: Get from Community API instead
    return Person(crn = crn, name = "Mock Person", isActive = true)
  }
}
