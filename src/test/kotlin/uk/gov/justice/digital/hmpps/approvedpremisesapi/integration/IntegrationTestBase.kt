package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DestinationProviderEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ExtensionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersistedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationOfficerEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationOfficerEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.hmppsauth.GetTokenResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApAreaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApplicationSchemaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApplicationTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ArrivalTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CancellationReasonTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CancellationTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.DepartureReasonTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.DepartureTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.DestinationProviderTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ExtensionTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.LocalAuthorityAreaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.LostBedReasonTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.LostBedsTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.MoveOnCategoryTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.NonArrivalReasonTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.NonArrivalTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.PremisesTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ProbationOfficerTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ProbationRegionTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.JwtAuthHelper
import java.time.Duration
import java.util.UUID

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {
  lateinit var wiremockServer: WireMockServer

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  private lateinit var flyway: Flyway

  @Autowired
  private lateinit var cacheManager: CacheManager

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

  @Autowired
  lateinit var cancellationReasonRepository: CancellationReasonTestRepository

  @Autowired
  lateinit var lostBedsRepository: LostBedsTestRepository

  @Autowired
  lateinit var lostBedReasonRepository: LostBedReasonTestRepository

  @Autowired
  lateinit var extensionRepository: ExtensionTestRepository

  @Autowired
  lateinit var nonArrivalReasonRepository: NonArrivalReasonTestRepository

  @Autowired
  lateinit var applicationRepository: ApplicationTestRepository

  @Autowired
  lateinit var applicationSchemaRepository: ApplicationSchemaTestRepository

  @Autowired
  lateinit var probationOfficerRepository: ProbationOfficerTestRepository

  lateinit var probationRegionEntityFactory: PersistedFactory<ProbationRegionEntity, UUID, ProbationRegionEntityFactory>
  lateinit var apAreaEntityFactory: PersistedFactory<ApAreaEntity, UUID, ApAreaEntityFactory>
  lateinit var localAuthorityEntityFactory: PersistedFactory<LocalAuthorityAreaEntity, UUID, LocalAuthorityEntityFactory>
  lateinit var premisesEntityFactory: PersistedFactory<PremisesEntity, UUID, PremisesEntityFactory>
  lateinit var bookingEntityFactory: PersistedFactory<BookingEntity, UUID, BookingEntityFactory>
  lateinit var arrivalEntityFactory: PersistedFactory<ArrivalEntity, UUID, ArrivalEntityFactory>
  lateinit var departureEntityFactory: PersistedFactory<DepartureEntity, UUID, DepartureEntityFactory>
  lateinit var destinationProviderEntityFactory: PersistedFactory<DestinationProviderEntity, UUID, DestinationProviderEntityFactory>
  lateinit var departureReasonEntityFactory: PersistedFactory<DepartureReasonEntity, UUID, DepartureReasonEntityFactory>
  lateinit var moveOnCategoryEntityFactory: PersistedFactory<MoveOnCategoryEntity, UUID, MoveOnCategoryEntityFactory>
  lateinit var nonArrivalEntityFactory: PersistedFactory<NonArrivalEntity, UUID, NonArrivalEntityFactory>
  lateinit var cancellationEntityFactory: PersistedFactory<CancellationEntity, UUID, CancellationEntityFactory>
  lateinit var cancellationReasonEntityFactory: PersistedFactory<CancellationReasonEntity, UUID, CancellationReasonEntityFactory>
  lateinit var lostBedsEntityFactory: PersistedFactory<LostBedsEntity, UUID, LostBedsEntityFactory>
  lateinit var lostBedReasonEntityFactory: PersistedFactory<LostBedReasonEntity, UUID, LostBedReasonEntityFactory>
  lateinit var extensionEntityFactory: PersistedFactory<ExtensionEntity, UUID, ExtensionEntityFactory>
  lateinit var nonArrivalReasonEntityFactory: PersistedFactory<NonArrivalReasonEntity, UUID, NonArrivalReasonEntityFactory>
  lateinit var applicationEntityFactory: PersistedFactory<ApplicationEntity, UUID, ApplicationEntityFactory>
  lateinit var applicationSchemaEntityFactory: PersistedFactory<ApplicationSchemaEntity, UUID, ApplicationSchemaEntityFactory>
  lateinit var probationOfficerEntityFactory: PersistedFactory<ProbationOfficerEntity, UUID, ProbationOfficerEntityFactory>

  @BeforeEach
  fun beforeEach() {
    wiremockServer = WireMockServer(57839)
    wiremockServer.start()

    flyway.clean()
    flyway.migrate()

    cacheManager.cacheNames.forEach {
      cacheManager.getCache(it)!!.clear()
    }
  }

  @AfterEach
  fun stopMockServer() {
    wiremockServer.stop()
  }

  @BeforeEach
  fun setupFactories() {
    probationRegionEntityFactory = PersistedFactory(ProbationRegionEntityFactory(), probationRegionRepository)
    apAreaEntityFactory = PersistedFactory(ApAreaEntityFactory(), apAreaRepository)
    localAuthorityEntityFactory = PersistedFactory(LocalAuthorityEntityFactory(), localAuthorityAreaRepository)
    premisesEntityFactory = PersistedFactory(PremisesEntityFactory(), premisesRepository)
    bookingEntityFactory = PersistedFactory(BookingEntityFactory(), bookingRepository)
    arrivalEntityFactory = PersistedFactory(ArrivalEntityFactory(), arrivalRepository)
    departureEntityFactory = PersistedFactory(DepartureEntityFactory(), departureRepository)
    destinationProviderEntityFactory = PersistedFactory(DestinationProviderEntityFactory(), destinationProviderRepository)
    departureReasonEntityFactory = PersistedFactory(DepartureReasonEntityFactory(), departureReasonRepository)
    moveOnCategoryEntityFactory = PersistedFactory(MoveOnCategoryEntityFactory(), moveOnCategoryRepository)
    nonArrivalEntityFactory = PersistedFactory(NonArrivalEntityFactory(), nonArrivalRepository)
    cancellationEntityFactory = PersistedFactory(CancellationEntityFactory(), cancellationRepository)
    cancellationReasonEntityFactory = PersistedFactory(CancellationReasonEntityFactory(), cancellationReasonRepository)
    lostBedsEntityFactory = PersistedFactory(LostBedsEntityFactory(), lostBedsRepository)
    lostBedReasonEntityFactory = PersistedFactory(LostBedReasonEntityFactory(), lostBedReasonRepository)
    extensionEntityFactory = PersistedFactory(ExtensionEntityFactory(), extensionRepository)
    nonArrivalReasonEntityFactory = PersistedFactory(NonArrivalReasonEntityFactory(), nonArrivalReasonRepository)
    applicationEntityFactory = PersistedFactory(ApplicationEntityFactory(), applicationRepository)
    applicationSchemaEntityFactory = PersistedFactory(ApplicationSchemaEntityFactory(), applicationSchemaRepository)
    probationOfficerEntityFactory = PersistedFactory(ProbationOfficerEntityFactory(), probationOfficerRepository)
  }

  fun mockClientCredentialsJwtRequest(
    username: String? = null,
    roles: List<String> = listOf(),
    authSource: String = "none"
  ) {
    wiremockServer.stubFor(
      post(urlEqualTo("/auth/oauth/token"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              objectMapper.writeValueAsString(
                GetTokenResponse(
                  accessToken = jwtAuthHelper.createClientCredentialsJwt(
                    username = username,
                    roles = roles,
                    authSource = authSource
                  ),
                  tokenType = "bearer",
                  expiresIn = Duration.ofHours(1).toSeconds().toInt(),
                  scope = "read",
                  sub = username?.uppercase() ?: "integration-test-client-id",
                  authSource = authSource,
                  jti = UUID.randomUUID().toString(),
                  iss = "http://localhost:9092/auth/issuer"
                )
              )
            )
        )
    )
  }

  fun mockOffenderDetailsCommunityApiCall(offenderDetails: OffenderDetailSummary) = wiremockServer.stubFor(
    WireMock.get(WireMock.urlEqualTo("/secure/offenders/crn/${offenderDetails.otherIds.crn}"))
      .willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withBody(
            objectMapper.writeValueAsString(offenderDetails)
          )
      )
  )

  fun mockStaffMemberCommunityApiCall(staffMember: StaffMember) = wiremockServer.stubFor(
    WireMock.get(WireMock.urlEqualTo("/secure/staff/staffIdentifier/${staffMember.staffIdentifier}"))
      .willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withBody(
            objectMapper.writeValueAsString(staffMember)
          )
      )
  )

  fun mockInmateDetailPrisonsApiCall(inmateDetail: InmateDetail) = wiremockServer.stubFor(
    WireMock.get(WireMock.urlEqualTo("/api/offenders/${inmateDetail.offenderNo}"))
      .willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withBody(
            objectMapper.writeValueAsString(inmateDetail)
          )
      )
  )
}
