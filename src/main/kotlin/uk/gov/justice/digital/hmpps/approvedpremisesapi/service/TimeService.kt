package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TimeService {

  fun nowAsLocalDate(): LocalDate = LocalDate.now()
}
