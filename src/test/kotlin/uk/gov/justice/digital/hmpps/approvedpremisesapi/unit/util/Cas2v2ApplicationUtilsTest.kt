package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Constants.CAS2_COURT_BAIL_APPLICATION_TYPE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Constants.CAS2_PRISON_BAIL_APPLICATION_TYPE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Constants.HDC_APPLICATION_TYPE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.Cas2v2ApplicationUtils

class Cas2v2ApplicationUtilsTest {

  @Test
  fun `in Cas2v2ApplicationUtils getApplicationTypeFromApplicationOrigin returns correct applicationType`() {
    val applicationOrigin1 = ApplicationOrigin.courtBail
    val applicationType1 = Cas2v2ApplicationUtils().getApplicationTypeFromApplicationOrigin(applicationOrigin1)

    val applicationOrigin2 = ApplicationOrigin.prisonBail
    val applicationType2 = Cas2v2ApplicationUtils().getApplicationTypeFromApplicationOrigin(applicationOrigin2)

    val applicationOrigin3 = ApplicationOrigin.homeDetentionCurfew
    val applicationType3 = Cas2v2ApplicationUtils().getApplicationTypeFromApplicationOrigin(applicationOrigin3)

    Assertions.assertThat(applicationType1.equals(CAS2_COURT_BAIL_APPLICATION_TYPE)).isTrue
    Assertions.assertThat(applicationType2.equals(CAS2_PRISON_BAIL_APPLICATION_TYPE)).isTrue
    Assertions.assertThat(applicationType3.equals(HDC_APPLICATION_TYPE)).isTrue
  }
}
