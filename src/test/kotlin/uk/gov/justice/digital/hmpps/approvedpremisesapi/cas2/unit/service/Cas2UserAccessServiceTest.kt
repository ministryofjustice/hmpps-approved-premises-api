package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.transformCas2UserEntityToNomisUserEntity
import java.time.OffsetDateTime

class Cas2UserAccessServiceTest {

  @Nested
  inner class UserCanViewApplication {

    private val userAccessService = Cas2UserAccessService()

    @Nested
    inner class WhenTransferredApplication {
      val referringPrisonCode = "other"
      val transferredToPrisonCode = "prison"

      private val user = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).withActiveNomisCaseloadId(transferredToPrisonCode).produce()
      private val samePrisonUser = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).withActiveNomisCaseloadId(transferredToPrisonCode).produce()
      private val otherUser = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).withActiveNomisCaseloadId(referringPrisonCode).produce()
      private val otherPrisonUser = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).withActiveNomisCaseloadId(referringPrisonCode).produce()

      @Test
      fun `returns true if currently allocated POM or if in same prison`() {
        val application = Cas2ApplicationEntityFactory()
          .withCreatedByUser(transformCas2UserEntityToNomisUserEntity(otherUser))
          .withCreatedByCas2User(otherUser)
          .withSubmittedAt(OffsetDateTime.now())
          .withReferringPrisonCode(referringPrisonCode)
          .produce()

        // created by user is the first assigned POM
        application.createApplicationAssignment(referringPrisonCode, transformCas2UserEntityToNomisUserEntity(otherUser))
        assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(user), application)).isFalse
        assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(samePrisonUser), application)).isFalse
        assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(otherUser), application)).isTrue
        assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(otherPrisonUser), application)).isTrue

        // transfer to new prison, POM in new prison can view
        application.createApplicationAssignment(transferredToPrisonCode, null)
        assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(user), application)).isTrue
        assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(samePrisonUser), application)).isTrue
        assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(otherUser), application)).isFalse
        assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(otherPrisonUser), application)).isFalse

        // allocate to new POM
        application.createApplicationAssignment(transferredToPrisonCode, transformCas2UserEntityToNomisUserEntity(user))
        assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(user), application)).isTrue
        assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(samePrisonUser), application)).isTrue
        assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(otherUser), application)).isFalse
        assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(otherPrisonUser), application)).isFalse
      }
    }

    @Nested
    inner class WhenApplicationCreatedByUser {
      private val user = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
        .produce()

      @Test
      fun `returns true`() {
        val application = Cas2ApplicationEntityFactory()
          .withCreatedByUser(transformCas2UserEntityToNomisUserEntity(user))
          .withCreatedByCas2User(user)
          .produce()

        assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(user), application)).isTrue
      }
    }

    @Nested
    inner class WhenApplicationNotCreatedByUser {

      @Nested
      inner class WhenApplicationNotSubmitted {
        private val user = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
          .produce()
        private val anotherUser = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
          .produce()

        @Test
        fun `returns false`() {
          val application = Cas2ApplicationEntityFactory()
            .withCreatedByUser(transformCas2UserEntityToNomisUserEntity(anotherUser))
            .withCreatedByCas2User(anotherUser)
            .produce()

          assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(user), application)).isFalse
        }
      }

      @Nested
      inner class WhenApplicationMadeForDifferentPrison {
        private val user = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
          .withActiveNomisCaseloadId("my-prison")
          .produce()
        private val anotherUser = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
          .withActiveNomisCaseloadId("different-prison").produce()

        @Test
        fun `returns false`() {
          val application = Cas2ApplicationEntityFactory()
            .withCreatedByUser(transformCas2UserEntityToNomisUserEntity(anotherUser))
            .withCreatedByCas2User(anotherUser)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("different-prison")
            .produce()

          assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(user), application)).isFalse
        }

        @Nested
        inner class WhenNoPrisonData {
          private val userWithNoPrison = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
            .withActiveNomisCaseloadId("my-prison")
            .produce()
          private val anotherUserWithNoPrison = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
            .withActiveNomisCaseloadId("different-prison").produce()

          @Test
          fun `returns false`() {
            val application = Cas2ApplicationEntityFactory()
              .withCreatedByUser(transformCas2UserEntityToNomisUserEntity(anotherUserWithNoPrison))
              .withCreatedByCas2User(anotherUserWithNoPrison)
              .withSubmittedAt(OffsetDateTime.now())
              .produce()

            assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(userWithNoPrison), application)).isFalse
          }
        }
      }

      @Nested
      inner class WhenApplicationMadeForSamePrison {
        private val user = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
          .withActiveNomisCaseloadId("my-prison")
          .produce()
        private val anotherUser = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
          .withActiveNomisCaseloadId("my-prison")
          .produce()

        @Test
        fun `returns true if the user created the application`() {
          val application = Cas2ApplicationEntityFactory()
            .withCreatedByUser(transformCas2UserEntityToNomisUserEntity(user))
            .withCreatedByCas2User(user)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("my-prison")
            .produce()

          assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(user), application)).isTrue
        }

        @Test
        fun `returns true when user NOT creator`() {
          val application = Cas2ApplicationEntityFactory()
            .withCreatedByUser(transformCas2UserEntityToNomisUserEntity(anotherUser))
            .withCreatedByCas2User(anotherUser)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("my-prison")
            .produce()

          assertThat(userAccessService.userCanViewApplication(transformCas2UserEntityToNomisUserEntity(user), application)).isTrue
        }
      }
    }
  }
}
