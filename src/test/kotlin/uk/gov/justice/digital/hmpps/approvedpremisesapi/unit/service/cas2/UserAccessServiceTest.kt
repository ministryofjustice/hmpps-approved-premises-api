package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.UserAccessService
import java.time.OffsetDateTime

class UserAccessServiceTest {

  @Nested
  inner class UserCanViewApplication {

    private val userAccessService = UserAccessService()
    val newestJsonSchema = Cas2ApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    @Nested
    inner class WhenApplicationCreatedByUser {
      private val user = NomisUserEntityFactory()
        .produce()

      @Test
      fun `returns true`() {
        val application = Cas2ApplicationEntityFactory()
          .withApplicationSchema(newestJsonSchema)
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
            .withApplicationSchema(newestJsonSchema)
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
            .withApplicationSchema(newestJsonSchema)
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
              .withApplicationSchema(newestJsonSchema)
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
            .withApplicationSchema(newestJsonSchema)
            .withCreatedByUser(user)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("my-prison")
            .produce()

          assertThat(userAccessService.userCanViewApplication(user, application)).isTrue
        }

        @Test
        fun `returns true when user NOT creator`() {
          val newestJsonSchema = Cas2ApplicationJsonSchemaEntityFactory()
            .withSchema("{}")
            .produce()

          val application = Cas2ApplicationEntityFactory()
            .withApplicationSchema(newestJsonSchema)
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
