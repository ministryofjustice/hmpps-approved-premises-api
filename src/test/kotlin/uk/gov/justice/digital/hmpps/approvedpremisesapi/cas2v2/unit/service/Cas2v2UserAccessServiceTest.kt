package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.factory.Cas2v2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.factory.Cas2v2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2UserService
import java.time.OffsetDateTime

class Cas2v2UserAccessServiceTest {

  val mockkCas2v2UserService = mockk<Cas2v2UserService>()

  @Nested
  inner class UserCanViewApplication {

    private val cas2v2UserAccessService = Cas2v2UserAccessService(mockkCas2v2UserService)

    @Nested
    inner class WhenApplicationCreatedByUser {
      private val user = Cas2v2UserEntityFactory()
        .produce()

      @Test
      fun `returns true`() {
        val application = Cas2v2ApplicationEntityFactory()
          .withCreatedByUser(user)
          .produce()

        assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(user, application)).isTrue
      }
    }

    @Nested
    inner class WhenApplicationNotCreatedByUser {

      @Nested
      inner class WhenApplicationNotSubmitted {
        private val user = Cas2v2UserEntityFactory()
          .produce()
        private val anotherUser = Cas2v2UserEntityFactory()
          .produce()

        @Test
        fun `returns false`() {
          val cas2v2Application = Cas2v2ApplicationEntityFactory()
            .withCreatedByUser(anotherUser)
            .produce()

          assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(user, cas2v2Application)).isFalse
        }
      }

      @Nested
      inner class WhenApplicationMadeForDifferentPrison {
        private val user = Cas2v2UserEntityFactory()
          .withActiveNomisCaseloadId("my-prison")
          .produce()
        private val anotherUser = Cas2v2UserEntityFactory()
          .withActiveNomisCaseloadId("different-prison").produce()

        @Test
        fun `returns false`() {
          val cas2v2Application = Cas2v2ApplicationEntityFactory()
            .withCreatedByUser(anotherUser)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("different-prison")
            .produce()

          assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(user, cas2v2Application)).isFalse
        }

        @Nested
        inner class WhenNoPrisonData {
          private val userWithNoPrison = Cas2v2UserEntityFactory()
            .withActiveNomisCaseloadId("my-prison")
            .produce()
          private val anotherUserWithNoPrison = Cas2v2UserEntityFactory()
            .withActiveNomisCaseloadId("different-prison").produce()

          @Test
          fun `returns false`() {
            val cas2v2Application = Cas2v2ApplicationEntityFactory()
              .withCreatedByUser(anotherUserWithNoPrison)
              .withSubmittedAt(OffsetDateTime.now())
              .produce()

            assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(userWithNoPrison, cas2v2Application)).isFalse
          }
        }
      }

      @Nested
      inner class WhenCas2v2ApplicationMadeForSamePrison {
        private val user = Cas2v2UserEntityFactory()
          .withActiveNomisCaseloadId("my-prison")
          .produce()
        private val anotherUser = Cas2v2UserEntityFactory()
          .withActiveNomisCaseloadId("my-prison")
          .produce()

        @Test
        fun `returns true if the user created the application`() {
          val cas2v2Application = Cas2v2ApplicationEntityFactory()
            .withCreatedByUser(user)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("my-prison")
            .produce()

          assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(user, cas2v2Application)).isTrue
        }

        @Test
        fun `returns true when user NOT creator`() {
          val cas2v2Application = Cas2v2ApplicationEntityFactory()
            .withCreatedByUser(anotherUser)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("my-prison")
            .produce()

          assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(user, cas2v2Application)).isTrue
        }
      }

      @Nested
      inner class PrisonBailApplications {
        private val referrerOne = Cas2v2UserEntityFactory()
          .produce()
        private val referrerTwo = Cas2v2UserEntityFactory()
          .produce()

        private val submittedPrisonApplication = Cas2v2ApplicationEntityFactory()
          .withApplicationOrigin(ApplicationOrigin.prisonBail)
          .withCreatedByUser(referrerOne)
          .withSubmittedAt(OffsetDateTime.now())
          .produce()

        private val unsubmittedPrisonApplication = Cas2v2ApplicationEntityFactory()
          .withApplicationOrigin(ApplicationOrigin.prisonBail)
          .withCreatedByUser(referrerOne)
          .produce()

        private val courtApplication = Cas2v2ApplicationEntityFactory()
          .withApplicationOrigin(ApplicationOrigin.courtBail)
          .withCreatedByUser(referrerOne)
          .produce()

        private val hdcApplication = Cas2v2ApplicationEntityFactory()
          .withApplicationOrigin(ApplicationOrigin.homeDetentionCurfew)
          .withCreatedByUser(referrerOne)
          .produce()

        @Nested
        inner class WhenApplicationIsPrisonBail {

          @Test
          fun `returns true when secondary user is a prison referrer and application is submitted`() {
            every {
              mockkCas2v2UserService.userForRequestHasRole(
                listOf(SimpleGrantedAuthority("ROLE_CAS2_PRISON_BAIL_REFERRER")),
              )
            } returns true

            assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(referrerTwo, submittedPrisonApplication)).isTrue
          }

          @Test
          fun `returns false when secondary user is a prison referrer and application is unsubmitted`() {
            every {
              mockkCas2v2UserService.userForRequestHasRole(
                listOf(SimpleGrantedAuthority("ROLE_CAS2_PRISON_BAIL_REFERRER")),
              )
            } returns false

            assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(referrerTwo, unsubmittedPrisonApplication)).isFalse
          }

          @Test
          fun `returns false when secondary user is a court referrer`() {
            every {
              mockkCas2v2UserService.userForRequestHasRole(
                listOf(SimpleGrantedAuthority("ROLE_CAS2_PRISON_BAIL_REFERRER")),
              )
            } returns false

            assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(referrerTwo, submittedPrisonApplication)).isFalse
          }
        }

        @Nested
        inner class WhenApplicationIsCourtBail {
          @Test
          fun `returns false when secondary user is a prison referrer`() {
            every {
              mockkCas2v2UserService.userForRequestHasRole(
                listOf(SimpleGrantedAuthority("ROLE_CAS2_PRISON_BAIL_REFERRER")),
              )
            } returns true

            assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(referrerTwo, courtApplication)).isFalse
          }

          @Test
          fun `returns false when secondary user is a court referrer`() {
            every {
              mockkCas2v2UserService.userForRequestHasRole(
                listOf(SimpleGrantedAuthority("ROLE_CAS2_PRISON_BAIL_REFERRER")),
              )
            } returns false

            assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(referrerTwo, courtApplication)).isFalse
          }
        }

        @Nested
        inner class WhenApplicationIsHDC {
          @Test
          fun `returns false when secondary user is a prison referrer`() {
            every {
              mockkCas2v2UserService.userForRequestHasRole(
                listOf(SimpleGrantedAuthority("ROLE_CAS2_PRISON_BAIL_REFERRER")),
              )
            } returns true

            assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(referrerTwo, hdcApplication)).isFalse
          }

          @Test
          fun `returns false when secondary user is a court referrer`() {
            every {
              mockkCas2v2UserService.userForRequestHasRole(
                listOf(SimpleGrantedAuthority("ROLE_CAS2_PRISON_BAIL_REFERRER")),
              )
            } returns false

            assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(referrerTwo, hdcApplication)).isFalse
          }
        }
      }
    }
  }
}
