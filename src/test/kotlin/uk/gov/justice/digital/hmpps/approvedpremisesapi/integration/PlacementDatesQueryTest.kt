package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateRepository

class PlacementDatesQueryTest : IntegrationTestBase() {
  @Autowired
  lateinit var realPlacementDatesRepository: PlacementDateRepository

  @Test
  fun `findAllByPlacementApplication only returns dates for placementApplication provided`() {
    `Given a User` { user, _ ->
      val jsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      val application1 = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn("CRN123")
        withCreatedByUser(user)
        withApplicationSchema(jsonSchema)
      }

      val application2 = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn("CRN456")
        withCreatedByUser(user)
        withApplicationSchema(jsonSchema)
      }

      val placementApplication1 = placementApplicationFactory.produceAndPersist {
        withCreatedByUser(user)
        withSchemaVersion(jsonSchema)
        withApplication(application1)
      }

      val placementApplication2 = placementApplicationFactory.produceAndPersist {
        withCreatedByUser(user)
        withSchemaVersion(jsonSchema)
        withApplication(application1)
      }

      val placementApplication3 = placementApplicationFactory.produceAndPersist {
        withCreatedByUser(user)
        withSchemaVersion(jsonSchema)
        withApplication(application2)
      }

      val placementDates1 = placementDateFactory.produceAndPersist {
        withPlacementApplication(placementApplication1)
      }

      placementDateFactory.produceAndPersist {
        withPlacementApplication(placementApplication2)
      }

      placementDateFactory.produceAndPersist {
        withPlacementApplication(placementApplication3)
      }

      val result = realPlacementDatesRepository.findAllByPlacementApplication(placementApplication1)

      assertThat(result.size).isEqualTo(1)
      assertThat(result.first().id).isEqualTo(placementDates1.id)
    }
  }
}
