package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.unit.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Constants.CAS2_COURT_BAIL_APPLICATION_TYPE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Constants.CAS2_PRISON_BAIL_APPLICATION_TYPE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Constants.HDC_APPLICATION_TYPE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.util.Cas2v2ApplicationUtils

class Cas2v2ApplicationUtilsTest {

  @Test
  fun `in Cas2v2ApplicationUtils getApplicationTypeFromApplicationOrigin returns correct applicationType`() {
    val applicationOrigin1 = ApplicationOrigin.courtBail
    val applicationType1 = Cas2v2ApplicationUtils().getApplicationTypeFromApplicationOrigin(applicationOrigin1)

    val applicationOrigin2 = ApplicationOrigin.prisonBail
    val applicationType2 = Cas2v2ApplicationUtils().getApplicationTypeFromApplicationOrigin(applicationOrigin2)

    val applicationOrigin3 = ApplicationOrigin.homeDetentionCurfew
    val applicationType3 = Cas2v2ApplicationUtils().getApplicationTypeFromApplicationOrigin(applicationOrigin3)

    assertThat(applicationType1).isEqualTo(CAS2_COURT_BAIL_APPLICATION_TYPE)
    assertThat(applicationType2).isEqualTo(CAS2_PRISON_BAIL_APPLICATION_TYPE)
    assertThat(applicationType3).isEqualTo(HDC_APPLICATION_TYPE)
  }
}
