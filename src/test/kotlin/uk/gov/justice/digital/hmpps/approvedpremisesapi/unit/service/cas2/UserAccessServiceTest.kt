package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.UserAccessService

class UserAccessServiceTest {

  private val user = NomisUserEntityFactory().produce()
  private val anotherUser = NomisUserEntityFactory().produce()
  private val userAccessService = UserAccessService()

  @Nested
  inner class UserCanViewApplication {

    @Test
    fun `returns true if the user created the application`() {
      val newestJsonSchema = Cas2ApplicationJsonSchemaEntityFactory()
        .withSchema("{}")
        .produce()

      val application = Cas2ApplicationEntityFactory()
        .withCreatedByUser(user)
        .withApplicationSchema(newestJsonSchema)
        .produce()

      assertThat(userAccessService.userCanViewApplication(user, application)).isTrue
    }

    @Test
    fun `returns false when user NOT creator`() {
      val newestJsonSchema = Cas2ApplicationJsonSchemaEntityFactory()
        .withSchema("{}")
        .produce()

      val application = Cas2ApplicationEntityFactory()
        .withCreatedByUser(anotherUser)
        .withApplicationSchema(newestJsonSchema)
        .produce()

      assertThat(userAccessService.userCanViewApplication(user, application)).isFalse
    }
  }
}
