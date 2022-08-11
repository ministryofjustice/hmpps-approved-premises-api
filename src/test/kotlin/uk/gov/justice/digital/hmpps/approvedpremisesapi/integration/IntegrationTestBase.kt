package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DestinationProviderEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApAreaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ArrivalTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CancellationTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.DepartureReasonTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.DepartureTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.DestinationProviderTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.LocalAuthorityAreaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.MoveOnCategoryTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.NonArrivalTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.PersonTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.PremisesTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ProbationRegionTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.JwtAuthHelper

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  private lateinit var flyway: Flyway

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var probationRegionRepository: ProbationRegionTestRepository

  @Autowired
  lateinit var apAreaRepository: ApAreaTestRepository

  @Autowired
  lateinit var localAuthorityAreaRepository: LocalAuthorityAreaTestRepository

  @Autowired
  lateinit var premisesRepository: PremisesTestRepository

  @Autowired
  lateinit var bookingRepository: BookingTestRepository

  @Autowired
  lateinit var personRepository: PersonTestRepository

  @Autowired
  lateinit var arrivalRepository: ArrivalTestRepository

  @Autowired
  lateinit var departureRepository: DepartureTestRepository

  @Autowired
  lateinit var destinationProviderRepository: DestinationProviderTestRepository

  @Autowired
  lateinit var nonArrivalRepository: NonArrivalTestRepository

  @Autowired
  lateinit var cancellationRepository: CancellationTestRepository

  @Autowired
  lateinit var departureReasonRepository: DepartureReasonTestRepository

  @Autowired
  lateinit var moveOnCategoryRepository: MoveOnCategoryTestRepository

  lateinit var probationRegionEntityFactory: ProbationRegionEntityFactory
  lateinit var apAreaEntityFactory: ApAreaEntityFactory
  lateinit var localAuthorityEntityFactory: LocalAuthorityEntityFactory
  lateinit var premisesEntityFactory: PremisesEntityFactory
  lateinit var bookingEntityFactory: BookingEntityFactory
  lateinit var personEntityFactory: PersonEntityFactory
  lateinit var arrivalEntityFactory: ArrivalEntityFactory
  lateinit var departureEntityFactory: DepartureEntityFactory
  lateinit var destinationProviderEntityFactory: DestinationProviderEntityFactory
  lateinit var departureReasonEntityFactory: DepartureReasonEntityFactory
  lateinit var moveOnCategoryEntityFactory: MoveOnCategoryEntityFactory
  lateinit var nonArrivalEntityFactory: NonArrivalEntityFactory
  lateinit var cancellationEntityFactory: CancellationEntityFactory

  @BeforeEach
  fun beforeEach() {
    flyway.clean()
    flyway.migrate()
  }

  @BeforeEach
  fun setupFactories() {
    probationRegionEntityFactory = ProbationRegionEntityFactory(probationRegionRepository)
    apAreaEntityFactory = ApAreaEntityFactory(apAreaRepository)
    localAuthorityEntityFactory = LocalAuthorityEntityFactory(localAuthorityAreaRepository)
    premisesEntityFactory = PremisesEntityFactory(premisesRepository)
    bookingEntityFactory = BookingEntityFactory(bookingRepository)
    personEntityFactory = PersonEntityFactory(personRepository)
    arrivalEntityFactory = ArrivalEntityFactory(arrivalRepository)
    departureEntityFactory = DepartureEntityFactory(departureRepository)
    destinationProviderEntityFactory = DestinationProviderEntityFactory(destinationProviderRepository)
    departureReasonEntityFactory = DepartureReasonEntityFactory(departureReasonRepository)
    moveOnCategoryEntityFactory = MoveOnCategoryEntityFactory(moveOnCategoryRepository)
    nonArrivalEntityFactory = NonArrivalEntityFactory(nonArrivalRepository)
    cancellationEntityFactory = CancellationEntityFactory(cancellationRepository)
  }
}
