package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository

class CancellationQueryTest : IntegrationTestBase() {
  @Autowired
  lateinit var realCancellationRepository: CancellationRepository

  @Test
  fun `Get cancellations for application returns all the relevant cancellations`() {
    givenAUser { user, _ ->
      givenAnApplication(createdByUser = user) { application ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withProbationRegion(givenAProbationRegion())
          withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
        }

        val cancellationsForApplication = listOf(
          cancellationEntityFactory.produceAndPersist {
            withBooking(
              bookingEntityFactory.produceAndPersist {
                withApplication(application)
                withPremises(premises)
              },
            )
            withReason(cancellationReasonEntityFactory.produceAndPersist())
          },
          cancellationEntityFactory.produceAndPersist {
            withBooking(
              bookingEntityFactory.produceAndPersist {
                withApplication(application)
                withPremises(premises)
              },
            )
            withReason(cancellationReasonEntityFactory.produceAndPersist())
          },
        )

        val otherCancellations = listOf(
          cancellationEntityFactory.produceAndPersist {
            withBooking(
              bookingEntityFactory.produceAndPersist {
                withPremises(premises)
              },
            )
            withReason(cancellationReasonEntityFactory.produceAndPersist())
          },
          cancellationEntityFactory.produceAndPersist {
            withBooking(
              bookingEntityFactory.produceAndPersist {
                withPremises(premises)
              },
            )
            withReason(cancellationReasonEntityFactory.produceAndPersist())
          },
        )

        val result = realCancellationRepository.getCancellationsForApplicationId(application.id)

        assertThat(result).isEqualTo(cancellationsForApplication)
      }
    }
  }
}
