package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.unit.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.factory.Cas2v2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.factory.Cas2v2ApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.factory.Cas2v2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.service.Cas2v2UserAccessService
import java.time.OffsetDateTime

class Cas2v2UserAccessServiceTest {

  @Nested
  inner class UserCanViewApplication {

    private val cas2v2UserAccessService = Cas2v2UserAccessService()
    val newestJsonSchema = Cas2v2ApplicationJsonSchemaEntityFactory()
      .withSchema("{}")
      .produce()

    @Nested
    inner class WhenApplicationCreatedByUser {
      private val user = Cas2v2UserEntityFactory()
        .produce()

      @Test
      fun `returns true`() {
        val application = Cas2v2ApplicationEntityFactory()
          .withApplicationSchema(newestJsonSchema)
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
            .withApplicationSchema(newestJsonSchema)
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
            .withApplicationSchema(newestJsonSchema)
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
              .withApplicationSchema(newestJsonSchema)
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
            .withApplicationSchema(newestJsonSchema)
            .withCreatedByUser(user)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("my-prison")
            .produce()

          assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(user, cas2v2Application)).isTrue
        }

        @Test
        fun `returns true when user NOT creator`() {
          val newestJsonSchema = Cas2ApplicationJsonSchemaEntityFactory()
            .withSchema("{}")
            .produce()

          val cas2v2Application = Cas2v2ApplicationEntityFactory()
            .withApplicationSchema(newestJsonSchema)
            .withCreatedByUser(anotherUser)
            .withSubmittedAt(OffsetDateTime.now())
            .withReferringPrisonCode("my-prison")
            .produce()

          assertThat(cas2v2UserAccessService.userCanViewCas2v2Application(user, cas2v2Application)).isTrue
        }
      }
    }
  }
}
