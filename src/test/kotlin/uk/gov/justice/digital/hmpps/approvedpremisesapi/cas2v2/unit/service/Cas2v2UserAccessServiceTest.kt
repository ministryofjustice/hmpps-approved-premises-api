package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import java.time.OffsetDateTime

class Cas2v2UserAccessServiceTest {

  val mockkCas2UserService = mockk<Cas2UserService>()

  @Nested
  inner class UserCanViewApplication {

    private val cas2UserAccessService = Cas2UserAccessService(mockkCas2UserService)

    @Nested
    inner class WhenApplicationCreatedByUser {
      private val user = Cas2UserEntityFactory()
        .withServiceOrigin(Cas2ServiceOrigin.BAIL)
        .produce()

      @Test
      fun `returns true`() {
        val application = Cas2ApplicationEntityFactory()
          .withCreatedByUser(user)
          .withServiceOrigin(Cas2ServiceOrigin.BAIL)
          .produce()

        assertThat(cas2UserAccessService.userCanViewCas2v2Application(user, application)).isTrue
      }
    }

    @Nested
    inner class WhenApplicationNotCreatedByUser {

      @Nested
      inner class WhenApplicationNotSubmitted {
        private val user = Cas2UserEntityFactory()
          .withServiceOrigin(Cas2ServiceOrigin.BAIL)
          .produce()
        private val anotherUser = Cas2UserEntityFactory()
          .withServiceOrigin(Cas2ServiceOrigin.BAIL)
          .produce()

        @Test
        fun `returns false`() {
          val cas2v2Application = Cas2ApplicationEntityFactory()
            .withServiceOrigin(Cas2ServiceOrigin.BAIL)
            .withCreatedByUser(anotherUser)
            .produce()

          assertThat(cas2UserAccessService.userCanViewCas2v2Application(user, cas2v2Application)).isFalse
        }
      }

      @Nested
      inner class WhenApplicationMadeForDifferentPrison {
        private val user = Cas2UserEntityFactory()
          .withActiveNomisCaseloadId("my-prison")
          .withServiceOrigin(Cas2ServiceOrigin.BAIL)
          .produce()
        private val anotherUser = Cas2UserEntityFactory()
          .withActiveNomisCaseloadId("different-prison")
          .withServiceOrigin(Cas2ServiceOrigin.BAIL)
          .produce()

        @Test
        fun `returns false`() {
          val cas2v2Application = Cas2ApplicationEntityFactory()
            .withCreatedByUser(anotherUser)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("different-prison")
            .withServiceOrigin(Cas2ServiceOrigin.BAIL)
            .produce()

          assertThat(cas2UserAccessService.userCanViewCas2v2Application(user, cas2v2Application)).isFalse
        }

        @Nested
        inner class WhenNoPrisonData {
          private val userWithNoPrison = Cas2UserEntityFactory()
            .withActiveNomisCaseloadId("my-prison")
            .withServiceOrigin(Cas2ServiceOrigin.BAIL)
            .produce()
          private val anotherUserWithNoPrison = Cas2UserEntityFactory()
            .withActiveNomisCaseloadId("different-prison")
            .withServiceOrigin(Cas2ServiceOrigin.BAIL)
            .produce()

          @Test
          fun `returns false`() {
            val cas2v2Application = Cas2ApplicationEntityFactory()
              .withCreatedByUser(anotherUserWithNoPrison)
              .withServiceOrigin(Cas2ServiceOrigin.BAIL)
              .withSubmittedAt(OffsetDateTime.now())
              .produce()

            assertThat(
              cas2UserAccessService.userCanViewCas2v2Application(
                userWithNoPrison,
                cas2v2Application,
              ),
            ).isFalse
          }
        }
      }

      @Nested
      inner class WhenCas2v2ApplicationMadeForSamePrison {
        private val user = Cas2UserEntityFactory()
          .withActiveNomisCaseloadId("my-prison")
          .withServiceOrigin(Cas2ServiceOrigin.BAIL)
          .produce()
        private val anotherUser = Cas2UserEntityFactory()
          .withActiveNomisCaseloadId("my-prison")
          .withServiceOrigin(Cas2ServiceOrigin.BAIL)
          .produce()

        @Test
        fun `returns true if the user created the application`() {
          val cas2v2Application = Cas2ApplicationEntityFactory()
            .withCreatedByUser(user)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("my-prison")
            .withServiceOrigin(Cas2ServiceOrigin.BAIL)
            .produce()

          assertThat(cas2UserAccessService.userCanViewCas2v2Application(user, cas2v2Application)).isTrue
        }

        @Test
        fun `returns true when user NOT creator`() {
          val cas2v2Application = Cas2ApplicationEntityFactory()
            .withCreatedByUser(anotherUser)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("my-prison")
            .withServiceOrigin(Cas2ServiceOrigin.BAIL)
            .produce()

          assertThat(cas2UserAccessService.userCanViewCas2v2Application(user, cas2v2Application)).isTrue
        }
      }

      @Nested
      inner class PrisonBailApplications {
        private val referrerOne = Cas2UserEntityFactory()
          .withServiceOrigin(Cas2ServiceOrigin.BAIL)
          .produce()
        private val referrerTwo = Cas2UserEntityFactory()
          .withServiceOrigin(Cas2ServiceOrigin.BAIL)
          .produce()

        private val submittedPrisonApplication = Cas2ApplicationEntityFactory()
          .withApplicationOrigin(ApplicationOrigin.prisonBail)
          .withCreatedByUser(referrerOne)
          .withSubmittedAt(OffsetDateTime.now())
          .withServiceOrigin(Cas2ServiceOrigin.BAIL)
          .produce()

        private val unsubmittedPrisonApplication = Cas2ApplicationEntityFactory()
          .withApplicationOrigin(ApplicationOrigin.prisonBail)
          .withCreatedByUser(referrerOne)
          .withServiceOrigin(Cas2ServiceOrigin.BAIL)
          .produce()

        @Nested
        inner class WhenApplicationIsPrisonBail {

          @Nested
          inner class WhenLoggedInAsPrisonReferrer {
            @BeforeEach
            fun setup() {
              every {
                mockkCas2UserService.userForRequestHasRole(
                  listOf(SimpleGrantedAuthority("ROLE_CAS2_PRISON_BAIL_REFERRER")),
                )
              } returns true
            }

            @Test
            fun `access submitted prison bail applications`() {
              assertThat(
                cas2UserAccessService.userCanViewCas2v2Application(
                  referrerTwo,
                  submittedPrisonApplication,
                ),
              ).isTrue
            }

            @Test
            fun `cannot access unsubmitted prison bail applications`() {
              assertThat(
                cas2UserAccessService.userCanViewCas2v2Application(
                  referrerTwo,
                  unsubmittedPrisonApplication,
                ),
              ).isFalse
            }

            @Test
            fun `access a note to any application that is of origin prisonBail`() {
              assertThat(cas2UserAccessService.userCanAddNote(referrerTwo, submittedPrisonApplication)).isTrue
              assertThat(cas2UserAccessService.userCanAddNote(referrerTwo, unsubmittedPrisonApplication)).isTrue
            }
          }
        }
      }
    }
  }
}
