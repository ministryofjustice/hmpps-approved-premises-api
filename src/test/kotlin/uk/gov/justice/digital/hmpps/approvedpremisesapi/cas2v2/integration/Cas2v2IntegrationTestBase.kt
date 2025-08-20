package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2NoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2StatusUpdateDetailEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersistedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.config.IntegrationTestDbManager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.config.TestPropertiesInitializer

@ExtendWith(IntegrationTestDbManager.IntegrationTestListener::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(initializers = [TestPropertiesInitializer::class])
@ActiveProfiles("test")
@Tag("integration")
abstract class Cas2v2IntegrationTestBase : IntegrationTestBase() {

  override fun setupFactories() {
    super.setupFactories()
    cas2ApplicationEntityFactory = PersistedFactory({ Cas2ApplicationEntityFactory() }, cas2ApplicationRepository)
    cas2AssessmentEntityFactory = PersistedFactory({ Cas2AssessmentEntityFactory() }, cas2AssessmentRepository)
    cas2StatusUpdateEntityFactory =
      PersistedFactory({ Cas2StatusUpdateEntityFactory() }, cas2StatusUpdateRepository)
    cas2UserEntityFactory = PersistedFactory({ Cas2UserEntityFactory() }, cas2UserRepository)
    cas2NoteEntityFactory = PersistedFactory({ Cas2NoteEntityFactory() }, cas2NoteRepository)
    cas2StatusUpdateDetailEntityFactory = PersistedFactory({ Cas2StatusUpdateDetailEntityFactory() }, cas2StatusUpdateDetailEntityRepository)
  }
}
