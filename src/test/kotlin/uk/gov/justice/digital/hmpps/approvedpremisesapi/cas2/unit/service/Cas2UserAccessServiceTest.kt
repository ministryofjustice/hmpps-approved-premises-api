package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserAccessService
import java.time.OffsetDateTime

class Cas2UserAccessServiceTest {

  @Nested
  inner class UserCanViewApplication {

    private val userAccessService = Cas2UserAccessService()

    @Nested
    inner class WhenTransferredApplication {
      val referringPrisonCode = "other"
      val transferredToPrisonCode = "prison"

      private val user = NomisUserEntityFactory().withActiveCaseloadId(transferredToPrisonCode).produce()
      private val samePrisonUser = NomisUserEntityFactory().withActiveCaseloadId(transferredToPrisonCode).produce()
      private val otherUser = NomisUserEntityFactory().withActiveCaseloadId(referringPrisonCode).produce()
      private val otherPrisonUser = NomisUserEntityFactory().withActiveCaseloadId(referringPrisonCode).produce()

      @Test
      fun `returns true if currently allocated POM or if in same prison`() {
        val application = Cas2ApplicationEntityFactory()
          .withCreatedByUser(otherUser)
          .withSubmittedAt(OffsetDateTime.now())
          .withReferringPrisonCode(referringPrisonCode)
          .produce()

        // created by user is the first assigned POM
        application.createApplicationAssignment(referringPrisonCode, otherUser)
        assertThat(userAccessService.userCanViewApplication(user, application)).isFalse
        assertThat(userAccessService.userCanViewApplication(samePrisonUser, application)).isFalse
        assertThat(userAccessService.userCanViewApplication(otherUser, application)).isTrue
        assertThat(userAccessService.userCanViewApplication(otherPrisonUser, application)).isTrue

        // transfer to new prison, POM in new prison can view
        application.createApplicationAssignment(transferredToPrisonCode, null)
        assertThat(userAccessService.userCanViewApplication(user, application)).isTrue
        assertThat(userAccessService.userCanViewApplication(samePrisonUser, application)).isTrue
        assertThat(userAccessService.userCanViewApplication(otherUser, application)).isFalse
        assertThat(userAccessService.userCanViewApplication(otherPrisonUser, application)).isFalse

        // allocate to new POM
        application.createApplicationAssignment(transferredToPrisonCode, user)
        assertThat(userAccessService.userCanViewApplication(user, application)).isTrue
        assertThat(userAccessService.userCanViewApplication(samePrisonUser, application)).isTrue
        assertThat(userAccessService.userCanViewApplication(otherUser, application)).isFalse
        assertThat(userAccessService.userCanViewApplication(otherPrisonUser, application)).isFalse
      }
    }

    @Nested
    inner class WhenApplicationCreatedByUser {
      private val user = NomisUserEntityFactory()
        .produce()

      @Test
      fun `returns true`() {
        val application = Cas2ApplicationEntityFactory()
          .withCreatedByUser(user)
          .produce()

        assertThat(userAccessService.userCanViewApplication(user, application)).isTrue
      }
    }

    @Nested
    inner class WhenApplicationNotCreatedByUser {

      @Nested
      inner class WhenApplicationNotSubmitted {
        private val user = NomisUserEntityFactory()
          .produce()
        private val anotherUser = NomisUserEntityFactory()
          .produce()

        @Test
        fun `returns false`() {
          val application = Cas2ApplicationEntityFactory()
            .withCreatedByUser(anotherUser)
            .produce()

          assertThat(userAccessService.userCanViewApplication(user, application)).isFalse
        }
      }

      @Nested
      inner class WhenApplicationMadeForDifferentPrison {
        private val user = NomisUserEntityFactory()
          .withActiveCaseloadId("my-prison")
          .produce()
        private val anotherUser = NomisUserEntityFactory()
          .withActiveCaseloadId("different-prison").produce()

        @Test
        fun `returns false`() {
          val application = Cas2ApplicationEntityFactory()
            .withCreatedByUser(anotherUser)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("different-prison")
            .produce()

          assertThat(userAccessService.userCanViewApplication(user, application)).isFalse
        }

        @Nested
        inner class WhenNoPrisonData {
          private val userWithNoPrison = NomisUserEntityFactory()
            .withActiveCaseloadId("my-prison")
            .produce()
          private val anotherUserWithNoPrison = NomisUserEntityFactory()
            .withActiveCaseloadId("different-prison").produce()

          @Test
          fun `returns false`() {
            val application = Cas2ApplicationEntityFactory()
              .withCreatedByUser(anotherUserWithNoPrison)
              .withSubmittedAt(OffsetDateTime.now())
              .produce()

            assertThat(userAccessService.userCanViewApplication(userWithNoPrison, application)).isFalse
          }
        }
      }

      @Nested
      inner class WhenApplicationMadeForSamePrison {
        private val user = NomisUserEntityFactory()
          .withActiveCaseloadId("my-prison")
          .produce()
        private val anotherUser = NomisUserEntityFactory()
          .withActiveCaseloadId("my-prison")
          .produce()

        @Test
        fun `returns true if the user created the application`() {
          val application = Cas2ApplicationEntityFactory()
            .withCreatedByUser(user)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("my-prison")
            .produce()

          assertThat(userAccessService.userCanViewApplication(user, application)).isTrue
        }

        @Test
        fun `returns true when user NOT creator`() {
          val application = Cas2ApplicationEntityFactory()
            .withCreatedByUser(anotherUser)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("my-prison")
            .produce()

          assertThat(userAccessService.userCanViewApplication(user, application)).isTrue
        }
      }
    }
  }
}
