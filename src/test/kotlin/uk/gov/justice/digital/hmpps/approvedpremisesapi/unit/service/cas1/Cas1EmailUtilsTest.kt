package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1ApplicationUserDetailsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.interestedPartiesEmailAddresses

class Cas1EmailUtilsTest {

  @Nested
  inner class InterestedPartiesEmailAddresses {

    @Test
    fun `interestedPartiesEmailAddresses applicant case manager, has no email`() {
      val result = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(UserEntityFactory().withDefaults().withEmail(null).produce())
        .withCaseManagerIsNotApplicant(false)
        .produce()
        .interestedPartiesEmailAddresses()

      assertThat(result).isEmpty()
    }

    @Test
    fun `interestedPartiesEmailAddresses applicant case manager, has email`() {
      val result = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(UserEntityFactory().withDefaults().withEmail("applicantEmail@here.com").produce())
        .withCaseManagerIsNotApplicant(false)
        .produce()
        .interestedPartiesEmailAddresses()

      assertThat(result).containsOnly("applicantEmail@here.com")
    }

    @Test
    fun `interestedPartiesEmailAddresses applicant is not case manager`() {
      val result = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(UserEntityFactory().withDefaults().withEmail("applicantEmail@here.com").produce())
        .withCaseManagerIsNotApplicant(true)
        .withCaseManagerUserDetails(
          Cas1ApplicationUserDetailsEntityFactory()
            .withEmailAddress("caseManager@here.com")
            .produce(),
        )
        .produce()
        .interestedPartiesEmailAddresses()

      assertThat(result).containsOnly("applicantEmail@here.com", "caseManager@here.com")
    }
  }
}
