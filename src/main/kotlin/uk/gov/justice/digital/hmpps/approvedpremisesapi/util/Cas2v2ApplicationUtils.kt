package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Constants.CAS2_COURT_BAIL_APPLICATION_TYPE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Constants.CAS2_PRISON_BAIL_APPLICATION_TYPE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Constants.HDC_APPLICATION_TYPE

class Cas2v2ApplicationUtils {

  fun getApplicationTypeFromApplicationOrigin(applicationOrigin: ApplicationOrigin): String {
    val applicationType = when (applicationOrigin) {
      ApplicationOrigin.courtBail -> CAS2_COURT_BAIL_APPLICATION_TYPE
      ApplicationOrigin.prisonBail -> CAS2_PRISON_BAIL_APPLICATION_TYPE
      ApplicationOrigin.homeDetentionCurfew -> HDC_APPLICATION_TYPE
    }
    return applicationType
  }
}
