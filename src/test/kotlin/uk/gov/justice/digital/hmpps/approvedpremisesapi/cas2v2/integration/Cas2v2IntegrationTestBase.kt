package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.factory.Cas2v2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.factory.Cas2v2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.factory.Cas2v2NoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.factory.Cas2v2StatusUpdateDetailEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.factory.Cas2v2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.factory.Cas2v2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateDetailRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.repository.Cas2v2StatusUpdateTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersistedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.config.IntegrationTestDbManager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.config.TestPropertiesInitializer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas2v2ApplicationTestRepository
import java.util.UUID

@ExtendWith(IntegrationTestDbManager.IntegrationTestListener::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(initializers = [TestPropertiesInitializer::class])
@ActiveProfiles("test")
@Tag("integration")
abstract class Cas2v2IntegrationTestBase : IntegrationTestBase() {

  @Autowired
  lateinit var cas2v2ApplicationRepository: Cas2v2ApplicationTestRepository

  @Autowired
  lateinit var cas2v2AssessmentRepository: Cas2v2AssessmentRepository

  @Autowired
  lateinit var cas2v2StatusUpdateRepository: Cas2v2StatusUpdateTestRepository

  @Autowired
  lateinit var cas2v2UserRepository: Cas2v2UserRepository

  @Autowired
  lateinit var cas2v2NoteRepository: Cas2v2ApplicationNoteRepository

  @Autowired
  lateinit var cas2v2StatusUpdateDetailEntityRepository: Cas2v2StatusUpdateDetailRepository

  lateinit var cas2v2ApplicationEntityFactory: PersistedFactory<Cas2v2ApplicationEntity, UUID, Cas2v2ApplicationEntityFactory>
  lateinit var cas2v2AssessmentEntityFactory: PersistedFactory<Cas2v2AssessmentEntity, UUID, Cas2v2AssessmentEntityFactory>
  lateinit var cas2v2StatusUpdateEntityFactory: PersistedFactory<Cas2v2StatusUpdateEntity, UUID, Cas2v2StatusUpdateEntityFactory>
  lateinit var cas2v2UserEntityFactory: PersistedFactory<Cas2v2UserEntity, UUID, Cas2v2UserEntityFactory>
  lateinit var cas2v2NoteEntityFactory: PersistedFactory<Cas2v2ApplicationNoteEntity, UUID, Cas2v2NoteEntityFactory>
  lateinit var cas2v2StatusUpdateDetailEntityFactory: PersistedFactory<Cas2v2StatusUpdateDetailEntity, UUID, Cas2v2StatusUpdateDetailEntityFactory>

  override fun setupFactories() {
    super.setupFactories()
    cas2v2ApplicationEntityFactory = PersistedFactory({ Cas2v2ApplicationEntityFactory() }, cas2v2ApplicationRepository)
    cas2v2AssessmentEntityFactory = PersistedFactory({ Cas2v2AssessmentEntityFactory() }, cas2v2AssessmentRepository)
    cas2v2StatusUpdateEntityFactory =
      PersistedFactory({ Cas2v2StatusUpdateEntityFactory() }, cas2v2StatusUpdateRepository)
    cas2v2UserEntityFactory = PersistedFactory({ Cas2v2UserEntityFactory() }, cas2v2UserRepository)
    cas2v2NoteEntityFactory = PersistedFactory({ Cas2v2NoteEntityFactory() }, cas2v2NoteRepository)
    cas2v2StatusUpdateDetailEntityFactory = PersistedFactory({ Cas2v2StatusUpdateDetailEntityFactory() }, cas2v2StatusUpdateDetailEntityRepository)
  }
}
