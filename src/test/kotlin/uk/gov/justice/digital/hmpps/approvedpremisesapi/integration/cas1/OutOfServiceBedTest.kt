package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedRevisionType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas1OutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas1OutOfServiceBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Temporality
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas1OutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.withinSeconds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.stream.Stream

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class OutOfServiceBedTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var outOfServiceBedTransformer: Cas1OutOfServiceBedTransformer

  companion object {
    @JvmStatic
    fun temporalityArgs(): Stream<Arguments> {
      return Stream.of(
        Arguments.of(
          emptyList<Temporality>(),
        ),
        Arguments.of(
          listOf(
            Temporality.past,
          ),
        ),
        Arguments.of(
          listOf(
            Temporality.current,
          ),
        ),
        Arguments.of(
          listOf(
            Temporality.future,
          ),
        ),
        Arguments.of(
          listOf(
            Temporality.past,
            Temporality.current,
          ),
        ),
        Arguments.of(
          listOf(
            Temporality.past,
            Temporality.future,
          ),
        ),
        Arguments.of(
          listOf(
            Temporality.current,
            Temporality.future,
          ),
        ),
        Arguments.of(
          listOf(
            Temporality.past,
            Temporality.current,
            Temporality.future,
          ),
        ),
      )
    }
  }

  @Nested
  inner class GetAllOutOfServiceBeds {
    @Test
    fun `Get All Out-Of-Service Beds without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/out-of-service-beds")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_WORKFLOW_MANAGER", "CAS1_FUTURE_MANAGER", "CAS1_CRU_MEMBER" ])
    fun `Get All Out-Of-Service Beds returns OK with correct body when user has the role WORKFLOW_MANAGER`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { user, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withBed(bed)
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().plusDays(2))
            withEndDate(LocalDate.now().plusDays(4))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        val cancelledOutOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withBed(bed)
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().plusDays(3))
            withEndDate(LocalDate.now().plusDays(5))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        val cancellation = cas1OutOfServiceBedCancellationEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withOutOfServiceBed(cancelledOutOfServiceBed)
        }

        cancelledOutOfServiceBed.cancellation = cancellation

        val expectedJson = objectMapper.writeValueAsString(
          listOf(
            outOfServiceBedTransformer.transformJpaToApi(outOfServiceBed),
            outOfServiceBedTransformer.transformJpaToApi(cancelledOutOfServiceBed),
          ),
        )

        webTestClient.get()
          .uri("/cas1/out-of-service-beds")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @Test
    fun `Get All Out-Of-Service Beds filters by premises ID correctly`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val otherPremises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val otherBed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { otherPremises }
            }
          }
        }

        val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withBed(bed)
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().plusDays(2))
            withEndDate(LocalDate.now().plusDays(4))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        // otherPremisesOutOfServiceBed
        cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withBed(otherBed)
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().plusDays(3))
            withEndDate(LocalDate.now().plusDays(5))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        val expectedJson = objectMapper.writeValueAsString(
          listOf(
            outOfServiceBedTransformer.transformJpaToApi(outOfServiceBed),
          ),
        )

        webTestClient.get()
          .uri("/cas1/out-of-service-beds?premisesId=${premises.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @Test
    fun `Get All Out-Of-Service Beds filters by AP area ID correctly`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val otherPremises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val otherBed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { otherPremises }
            }
          }
        }

        val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withBed(bed)
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().plusDays(2))
            withEndDate(LocalDate.now().plusDays(4))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        // otherPremisesOutOfServiceBed
        cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withBed(otherBed)
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().plusDays(3))
            withEndDate(LocalDate.now().plusDays(5))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        val expectedJson = objectMapper.writeValueAsString(
          listOf(
            outOfServiceBedTransformer.transformJpaToApi(outOfServiceBed),
          ),
        )

        webTestClient.get()
          .uri("/cas1/out-of-service-beds?apAreaId=${premises.probationRegion.apArea!!.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.OutOfServiceBedTest#temporalityArgs")
    @ParameterizedTest
    fun `Get All Out-Of-Service Beds filters by temporality correctly`(temporality: List<Temporality>) {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val pastOutOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withBed(bed)
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().minusDays(4))
            withEndDate(LocalDate.now().minusDays(2))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        val currentOutOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withBed(bed)
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().minusDays(1))
            withEndDate(LocalDate.now().plusDays(1))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        val futureOutOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withBed(bed)
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().plusDays(2))
            withEndDate(LocalDate.now().plusDays(4))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        val expectedOutOfServiceBeds = mutableListOf<Cas1OutOfServiceBedEntity>()

        if (temporality.contains(Temporality.past)) {
          expectedOutOfServiceBeds += pastOutOfServiceBed
        }
        if (temporality.contains(Temporality.current)) {
          expectedOutOfServiceBeds += currentOutOfServiceBed
        }
        if (temporality.contains(Temporality.future)) {
          expectedOutOfServiceBeds += futureOutOfServiceBed
        }

        val expectedJson = objectMapper.writeValueAsString(
          expectedOutOfServiceBeds.map(outOfServiceBedTransformer::transformJpaToApi),
        )

        webTestClient.get()
          .uri("/cas1/out-of-service-beds?temporality=${temporality.joinToString(",")}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @CsvSource(
      "'premisesName','asc'",
      "'roomName','asc'",
      "'bedName','asc'",
      "'startDate','asc'",
      "'endDate','asc'",
      "'reason','asc'",
      "'daysLost','asc'",

      "'premisesName','desc'",
      "'roomName','desc'",
      "'bedName','desc'",
      "'startDate','desc'",
      "'endDate','desc'",
      "'reason','desc'",
      "'daysLost','desc'",
    )
    @ParameterizedTest
    @Suppress("detekt:CyclomaticComplexMethod")
    fun `Get All Out-Of-Service Beds sorts correctly`(sortField: Cas1OutOfServiceBedSortField, sortDirection: SortDirection) {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val otherPremises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val otherBed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { otherPremises }
            }
          }
        }

        val expectedOutOfServiceBeds = cas1OutOfServiceBedEntityFactory.produceAndPersistMultipleIndexed(4) {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())

          when {
            it % 2 == 0 -> withBed(bed)
            else -> withBed(otherBed)
          }
        }.mapIndexed { index, entity ->
          entity.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(entity)
            withStartDate(LocalDate.now().plusDays(index.toLong()))
            withEndDate(LocalDate.now().plusDays(index.toLong() * 2))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }

          entity
        }

        val sortedOutOfServiceBeds = when (sortDirection) {
          SortDirection.asc -> when (sortField) {
            Cas1OutOfServiceBedSortField.premisesName -> expectedOutOfServiceBeds.sortedBy { it.premises.name }
            Cas1OutOfServiceBedSortField.roomName -> expectedOutOfServiceBeds.sortedBy { it.bed.room.name }
            Cas1OutOfServiceBedSortField.bedName -> expectedOutOfServiceBeds.sortedBy { it.bed.name }
            Cas1OutOfServiceBedSortField.startDate -> expectedOutOfServiceBeds.sortedBy { it.startDate }
            Cas1OutOfServiceBedSortField.endDate -> expectedOutOfServiceBeds.sortedBy { it.endDate }
            Cas1OutOfServiceBedSortField.reason -> expectedOutOfServiceBeds.sortedBy { it.reason.name }
            Cas1OutOfServiceBedSortField.daysLost -> expectedOutOfServiceBeds.sortedBy { Duration.between(it.startDate.atStartOfDay(), it.endDate.plusDays(1).atStartOfDay()).toDays() }
          }
          SortDirection.desc -> when (sortField) {
            Cas1OutOfServiceBedSortField.premisesName -> expectedOutOfServiceBeds.sortedByDescending { it.premises.name }
            Cas1OutOfServiceBedSortField.roomName -> expectedOutOfServiceBeds.sortedByDescending { it.bed.room.name }
            Cas1OutOfServiceBedSortField.bedName -> expectedOutOfServiceBeds.sortedByDescending { it.bed.name }
            Cas1OutOfServiceBedSortField.startDate -> expectedOutOfServiceBeds.sortedByDescending { it.startDate }
            Cas1OutOfServiceBedSortField.endDate -> expectedOutOfServiceBeds.sortedByDescending { it.endDate }
            Cas1OutOfServiceBedSortField.reason -> expectedOutOfServiceBeds.sortedByDescending { it.reason.name }
            Cas1OutOfServiceBedSortField.daysLost -> expectedOutOfServiceBeds.sortedByDescending { Duration.between(it.startDate.atStartOfDay(), it.endDate.plusDays(1).atStartOfDay()).toDays() }
          }
        }

        val expectedJson = objectMapper.writeValueAsString(
          sortedOutOfServiceBeds.map(outOfServiceBedTransformer::transformJpaToApi),
        )

        webTestClient.get()
          .uri("/cas1/out-of-service-beds?sortBy=$sortField&sortDirection=$sortDirection")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @Test
    fun `Get All Out-Of-Service Beds paginates correctly`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { user, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val outOfServiceBeds = cas1OutOfServiceBedEntityFactory.produceAndPersistMultipleIndexed(15) {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withBed(bed)
        }.mapIndexed { index, entity ->
          entity.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(entity)
            withStartDate(LocalDate.now().plusDays(index.toLong()))
            withEndDate(LocalDate.now().plusDays(index.toLong() + 2))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }

          entity
        }

        val expectedOutOfServiceBeds = outOfServiceBeds.slice(5..9)

        val expectedJson = objectMapper.writeValueAsString(
          expectedOutOfServiceBeds.map(outOfServiceBedTransformer::transformJpaToApi),
        )

        webTestClient.get()
          .uri("/cas1/out-of-service-beds?page=2&perPage=5")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader()
          .valueEquals("X-Pagination-CurrentPage", "2")
          .expectHeader()
          .valueEquals("X-Pagination-TotalPages", "3")
          .expectHeader()
          .valueEquals("X-Pagination-TotalResults", "15")
          .expectHeader()
          .valueEquals("X-Pagination-PageSize", "5")
          .expectBody()
          .json(expectedJson)
      }
    }
  }

  @Nested
  inner class GetAllOutOfServiceBedsOnPremises {
    @Test
    fun `Get All Out-Of-Service Beds On Premises without JWT returns 401`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}/out-of-service-beds")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get All Out-Of-Service Beds on non existent Premises returns 404`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.get()
        .uri("/cas1/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/out-of-service-beds")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Get All Out-Of-Service Beds On Premises returns OK with correct body when user has one of roles FUTURE_MANAGER, MANAGER, MATCHER`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { user, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withBed(bed)
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().plusDays(2))
            withEndDate(LocalDate.now().plusDays(4))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        val cancelledOutOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withBed(bed)
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().plusDays(2))
            withEndDate(LocalDate.now().plusDays(4))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        cas1OutOfServiceBedCancellationEntityFactory.produceAndPersist {
          withOutOfServiceBed(cancelledOutOfServiceBed)
        }

        val expectedJson = objectMapper.writeValueAsString(listOf(outOfServiceBedTransformer.transformJpaToApi(outOfServiceBed)))

        webTestClient.get()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }
  }

  @Nested
  inner class GetOutOfServiceBed {
    @Test
    fun `Get Out-Of-Service Bed without JWT returns 401`() {
      `Given a User` { user, _ ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withBed(
            bedEntityFactory.produceAndPersist {
              withYieldedRoom {
                roomEntityFactory.produceAndPersist {
                  withYieldedPremises { premises }
                }
              }
            },
          )
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().plusDays(2))
            withEndDate(LocalDate.now().plusDays(4))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        webTestClient.get()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}")
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }

    @Test
    fun `Get Out-Of-Service Bed for non-existent premises returns 404`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.get()
        .uri("/cas1/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/out-of-service-beds")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Get Out-Of-Service Bed for non-existent out-of-service bed returns 404`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        webTestClient.get()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Get Out-Of-Service Bed returns OK with correct body when user has one of roles FUTURE_MANAGER, MANAGER, MATCHER`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { user, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withBed(
            bedEntityFactory.produceAndPersist {
              withYieldedRoom {
                roomEntityFactory.produceAndPersist {
                  withYieldedPremises { premises }
                }
              }
            },
          )
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().plusDays(2))
            withEndDate(LocalDate.now().plusDays(4))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        val expectedJson = objectMapper.writeValueAsString(outOfServiceBedTransformer.transformJpaToApi(outOfServiceBed))

        webTestClient.get()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }
  }

  @Nested
  inner class CreateOutOfServiceBed {
    @Test
    fun `Create Out-Of-Service Beds without JWT returns 401`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val bed = bedEntityFactory.produceAndPersist {
        withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
      }

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/out-of-service-beds")
        .bodyValue(
          NewCas1OutOfServiceBed(
            startDate = LocalDate.parse("2022-08-15"),
            endDate = LocalDate.parse("2022-08-18"),
            bedId = bed.id,
            reason = UUID.randomUUID(),
            referenceNumber = "REF-123",
            notes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Create Out-Of-Service Beds returns 400 Bad Request if the bed ID does not reference a bed on the premises`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist {
              withYieldedApArea {
                apAreaEntityFactory.produceAndPersist()
              }
            }
          }
        }

        val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewCas1OutOfServiceBed(
              startDate = LocalDate.parse("2022-08-17"),
              endDate = LocalDate.parse("2022-08-18"),
              reason = reason.id,
              referenceNumber = "REF-123",
              notes = "notes",
              bedId = UUID.randomUUID(),
            ),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("invalid-params[0].propertyName").isEqualTo("$.bedId")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Create Out-Of-Service Beds returns OK with correct body when user has one of roles FUTURE_MANAGER, MANAGER, MATCHER`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { user, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        bedEntityFactory.produceAndPersistMultiple(3) {
          withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
        }

        val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewCas1OutOfServiceBed(
              startDate = LocalDate.parse("2022-08-17"),
              endDate = LocalDate.parse("2022-08-18"),
              bedId = bed.id,
              reason = reason.id,
              referenceNumber = "REF-123",
              notes = "notes",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.startDate").isEqualTo("2022-08-17")
          .jsonPath("$.endDate").isEqualTo("2022-08-18")
          .jsonPath("$.bed.id").isEqualTo(bed.id.toString())
          .jsonPath("$.bed.name").isEqualTo(bed.name)
          .jsonPath("$.room.id").isEqualTo(bed.room.id.toString())
          .jsonPath("$.room.name").isEqualTo(bed.room.name)
          .jsonPath("$.premises.id").isEqualTo(premises.id.toString())
          .jsonPath("$.premises.name").isEqualTo(premises.name)
          .jsonPath("$.apArea.id").isEqualTo(premises.probationRegion.apArea!!.id.toString())
          .jsonPath("$.apArea.name").isEqualTo(premises.probationRegion.apArea!!.name)
          .jsonPath("$.reason.id").isEqualTo(reason.id.toString())
          .jsonPath("$.reason.name").isEqualTo(reason.name)
          .jsonPath("$.reason.isActive").isEqualTo(true)
          .jsonPath("$.daysLostCount").isEqualTo(2)
          .jsonPath("$.temporality").isEqualTo(Temporality.past.value)
          .jsonPath("$.referenceNumber").isEqualTo("REF-123")
          .jsonPath("$.notes").isEqualTo("notes")
          .jsonPath("$.status").isEqualTo("active")
          .jsonPath("$.cancellation").isEqualTo(null)
          .jsonPath("$.revisionHistory[0].updatedBy.id").isEqualTo(user.id.toString())
          .jsonPath("$.revisionHistory[0].updatedBy.name").isEqualTo(user.name)
          .jsonPath("$.revisionHistory[0].updatedBy.deliusUsername").isEqualTo(user.deliusUsername)
          .jsonPath("$.revisionHistory[0].revisionType").value(
            containsInAnyOrder(
              Cas1OutOfServiceBedRevisionType.created.value,
            ),
          )
          .jsonPath("$.revisionHistory[0].startDate").isEqualTo("2022-08-17")
          .jsonPath("$.revisionHistory[0].endDate").isEqualTo("2022-08-18")
          .jsonPath("$.revisionHistory[0].reason.id").isEqualTo(reason.id.toString())
          .jsonPath("$.revisionHistory[0].reason.name").isEqualTo(reason.name)
          .jsonPath("$.revisionHistory[0].reason.isActive").isEqualTo(true)
          .jsonPath("$.revisionHistory[0].referenceNumber").isEqualTo("REF-123")
          .jsonPath("$.revisionHistory[0].notes").isEqualTo("notes")
          .jsonPath("$.revisionHistory[1]").doesNotExist()
      }
    }

    @Test
    fun `Create Out-Of-Service Bed succeeds even if overlapping with Booking`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { user, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
        }

        val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        BookingEntityFactory()
          .withBed(bed)
          .withPremises(premises)
          .withArrivalDate(LocalDate.parse("2022-08-15"))
          .withDepartureDate(LocalDate.parse("2022-08-18"))
          .produce()

        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewCas1OutOfServiceBed(
              startDate = LocalDate.parse("2022-08-17"),
              endDate = LocalDate.parse("2022-08-18"),
              bedId = bed.id,
              reason = reason.id,
              referenceNumber = "REF-123",
              notes = "notes",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.startDate").isEqualTo("2022-08-17")
          .jsonPath("$.endDate").isEqualTo("2022-08-18")
          .jsonPath("$.bed.id").isEqualTo(bed.id.toString())
          .jsonPath("$.bed.name").isEqualTo(bed.name)
          .jsonPath("$.room.id").isEqualTo(bed.room.id.toString())
          .jsonPath("$.room.name").isEqualTo(bed.room.name)
          .jsonPath("$.premises.id").isEqualTo(premises.id.toString())
          .jsonPath("$.premises.name").isEqualTo(premises.name)
          .jsonPath("$.apArea.id").isEqualTo(premises.probationRegion.apArea!!.id.toString())
          .jsonPath("$.apArea.name").isEqualTo(premises.probationRegion.apArea!!.name)
          .jsonPath("$.reason.id").isEqualTo(reason.id.toString())
          .jsonPath("$.reason.name").isEqualTo(reason.name)
          .jsonPath("$.reason.isActive").isEqualTo(true)
          .jsonPath("$.daysLostCount").isEqualTo(2)
          .jsonPath("$.temporality").isEqualTo(Temporality.past.value)
          .jsonPath("$.referenceNumber").isEqualTo("REF-123")
          .jsonPath("$.notes").isEqualTo("notes")
          .jsonPath("$.status").isEqualTo("active")
          .jsonPath("$.cancellation").isEqualTo(null)
          .jsonPath("$.revisionHistory[0].updatedBy.id").isEqualTo(user.id.toString())
          .jsonPath("$.revisionHistory[0].updatedBy.name").isEqualTo(user.name)
          .jsonPath("$.revisionHistory[0].updatedBy.deliusUsername").isEqualTo(user.deliusUsername)
          .jsonPath("$.revisionHistory[0].revisionType").value(
            containsInAnyOrder(
              Cas1OutOfServiceBedRevisionType.created.value,
            ),
          )
          .jsonPath("$.revisionHistory[0].startDate").isEqualTo("2022-08-17")
          .jsonPath("$.revisionHistory[0].endDate").isEqualTo("2022-08-18")
          .jsonPath("$.revisionHistory[0].reason.id").isEqualTo(reason.id.toString())
          .jsonPath("$.revisionHistory[0].reason.name").isEqualTo(reason.name)
          .jsonPath("$.revisionHistory[0].reason.isActive").isEqualTo(true)
          .jsonPath("$.revisionHistory[0].referenceNumber").isEqualTo("REF-123")
          .jsonPath("$.revisionHistory[0].notes").isEqualTo("notes")
          .jsonPath("$.revisionHistory[1]").doesNotExist()
      }
    }

    @Test
    fun `Create Out-Of-Service Beds for current day does not break GET all Premises endpoint`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { user, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
        }

        cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withBed(
            bedEntityFactory.produceAndPersist {
              withYieldedRoom {
                roomEntityFactory.produceAndPersist {
                  withYieldedPremises { premises }
                }
              }
            },
          )
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().minusDays(2))
            withEndDate(LocalDate.now().plusDays(2))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withOriginalArrivalDate(LocalDate.now().minusDays(4))
          withArrivalDate(LocalDate.now().minusDays(4))
          withOriginalDepartureDate(LocalDate.now().plusDays(6))
          withDepartureDate(LocalDate.now().plusDays(6))
        }

        webTestClient.get()
          .uri("/premises")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
      }
    }

    @Test
    fun `Create Out-Of-Service Bed returns 409 Conflict when An out-of-service bed for the same bed overlaps`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { user, jwt ->
        `Given an Offender` { _, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
            }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

          val existingOutOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withBed(bed)
          }.apply {
            this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
              withCreatedBy(user)
              withOutOfServiceBed(this@apply)
              withStartDate(LocalDate.parse("2022-07-15"))
              withEndDate(LocalDate.parse("2022-08-15"))
              withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
            }
          }

          webTestClient.post()
            .uri("/cas1/premises/${premises.id}/out-of-service-beds")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewCas1OutOfServiceBed(
                startDate = LocalDate.parse("2022-08-01"),
                endDate = LocalDate.parse("2022-08-30"),
                reason = reason.id,
                referenceNumber = "REF-123",
                notes = "notes",
                bedId = bed.id,
              ),
            )
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody()
            .jsonPath("title").isEqualTo("Conflict")
            .jsonPath("status").isEqualTo(409)
            .jsonPath("detail").isEqualTo("An out-of-service bed already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingOutOfServiceBed.id}")
        }
      }
    }

    @Test
    fun `Create Out-Of-Service Bed returns OK with correct body when only cancelled out-of-service beds for the same bed overlap`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { user, jwt ->
        `Given an Offender` { _, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
            }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

          val existingOutOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withBed(bed)
          }.apply {
            this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
              withCreatedBy(user)
              withOutOfServiceBed(this@apply)
              withStartDate(LocalDate.parse("2022-07-15"))
              withEndDate(LocalDate.parse("2022-08-15"))
              withReason(
                cas1OutOfServiceBedReasonEntityFactory.produceAndPersist(),
              )
            }
          }

          existingOutOfServiceBed.cancellation = cas1OutOfServiceBedCancellationEntityFactory.produceAndPersist {
            withOutOfServiceBed(existingOutOfServiceBed)
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }

          webTestClient.post()
            .uri("/cas1/premises/${premises.id}/out-of-service-beds")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewCas1OutOfServiceBed(
                startDate = LocalDate.parse("2022-08-01"),
                endDate = LocalDate.parse("2022-08-30"),
                reason = reason.id,
                referenceNumber = "REF-123",
                notes = "notes",
                bedId = bed.id,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.startDate").isEqualTo("2022-08-01")
            .jsonPath("$.endDate").isEqualTo("2022-08-30")
            .jsonPath("$.bed.id").isEqualTo(bed.id.toString())
            .jsonPath("$.bed.name").isEqualTo(bed.name)
            .jsonPath("$.room.id").isEqualTo(bed.room.id.toString())
            .jsonPath("$.room.name").isEqualTo(bed.room.name)
            .jsonPath("$.premises.id").isEqualTo(premises.id.toString())
            .jsonPath("$.premises.name").isEqualTo(premises.name)
            .jsonPath("$.apArea.id").isEqualTo(premises.probationRegion.apArea!!.id.toString())
            .jsonPath("$.apArea.name").isEqualTo(premises.probationRegion.apArea!!.name)
            .jsonPath("$.reason.id").isEqualTo(reason.id.toString())
            .jsonPath("$.reason.name").isEqualTo(reason.name)
            .jsonPath("$.reason.isActive").isEqualTo(true)
            .jsonPath("$.daysLostCount").isEqualTo(30)
            .jsonPath("$.temporality").isEqualTo(Temporality.past.value)
            .jsonPath("$.referenceNumber").isEqualTo("REF-123")
            .jsonPath("$.notes").isEqualTo("notes")
            .jsonPath("$.status").isEqualTo("active")
            .jsonPath("$.cancellation").isEqualTo(null)
            .jsonPath("$.revisionHistory[0].updatedBy.id").isEqualTo(user.id.toString())
            .jsonPath("$.revisionHistory[0].updatedBy.name").isEqualTo(user.name)
            .jsonPath("$.revisionHistory[0].updatedBy.deliusUsername").isEqualTo(user.deliusUsername)
            .jsonPath("$.revisionHistory[0].revisionType").value(
              containsInAnyOrder(
                Cas1OutOfServiceBedRevisionType.created.value,
              ),
            )
            .jsonPath("$.revisionHistory[0].startDate").isEqualTo("2022-08-01")
            .jsonPath("$.revisionHistory[0].endDate").isEqualTo("2022-08-30")
            .jsonPath("$.revisionHistory[0].reason.id").isEqualTo(reason.id.toString())
            .jsonPath("$.revisionHistory[0].reason.name").isEqualTo(reason.name)
            .jsonPath("$.revisionHistory[0].reason.isActive").isEqualTo(true)
            .jsonPath("$.revisionHistory[0].referenceNumber").isEqualTo("REF-123")
            .jsonPath("$.revisionHistory[0].notes").isEqualTo("notes")
            .jsonPath("$.revisionHistory[1]").doesNotExist()
        }
      }
    }
  }

  @Nested
  inner class UpdateOutOfServiceBed {
    @Test
    fun `Update Out-Of-Service Bed without JWT returns 401`() {
      `Given a User` { user, _ ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withBed(
            bedEntityFactory.produceAndPersist {
              withYieldedRoom {
                roomEntityFactory.produceAndPersist {
                  withYieldedPremises { premises }
                }
              }
            },
          )
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().plusDays(2))
            withEndDate(LocalDate.now().plusDays(4))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        webTestClient.put()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}")
          .bodyValue(
            UpdateCas1OutOfServiceBed(
              startDate = LocalDate.parse("2022-08-15"),
              endDate = LocalDate.parse("2022-08-18"),
              reason = UUID.randomUUID(),
              referenceNumber = "REF-123",
              notes = null,
            ),
          )
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }

    @Test
    fun `Update Out-Of-Service Bed for non-existent premises returns 404`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.put()
        .uri("/cas1/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/out-of-service-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          UpdateCas1OutOfServiceBed(
            startDate = LocalDate.parse("2022-08-15"),
            endDate = LocalDate.parse("2022-08-18"),
            reason = UUID.randomUUID(),
            referenceNumber = "REF-123",
            notes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `Update Out-Of-Service Bed for non-existent out-of-service bed returns 404`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.put()
        .uri("/cas1/premises/${premises.id}/out-of-service-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          UpdateCas1OutOfServiceBed(
            startDate = LocalDate.parse("2022-08-15"),
            endDate = LocalDate.parse("2022-08-18"),
            reason = UUID.randomUUID(),
            referenceNumber = "REF-123",
            notes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_MANAGER", "CAS1_MATCHER" ])
    fun `Update Out-Of-Service Beds returns OK with correct body when user has one of roles FUTURE_MANAGER, MANAGER, MATCHER`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { user, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val originalDetails: Cas1OutOfServiceBedRevisionEntity

        val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withBed(bed)
        }.apply {
          originalDetails = cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().plusDays(2))
            withEndDate(LocalDate.now().plusDays(4))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }

          this.revisionHistory += originalDetails
        }

        val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

        webTestClient.put()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateCas1OutOfServiceBed(
              startDate = LocalDate.parse("2022-08-17"),
              endDate = LocalDate.parse("2022-08-18"),
              reason = reason.id,
              referenceNumber = "REF-123",
              notes = "notes",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.startDate").isEqualTo("2022-08-17")
          .jsonPath("$.endDate").isEqualTo("2022-08-18")
          .jsonPath("$.bed.id").isEqualTo(bed.id.toString())
          .jsonPath("$.bed.name").isEqualTo(bed.name)
          .jsonPath("$.room.id").isEqualTo(bed.room.id.toString())
          .jsonPath("$.room.name").isEqualTo(bed.room.name)
          .jsonPath("$.premises.id").isEqualTo(premises.id.toString())
          .jsonPath("$.premises.name").isEqualTo(premises.name)
          .jsonPath("$.apArea.id").isEqualTo(premises.probationRegion.apArea!!.id.toString())
          .jsonPath("$.apArea.name").isEqualTo(premises.probationRegion.apArea!!.name)
          .jsonPath("$.reason.id").isEqualTo(reason.id.toString())
          .jsonPath("$.reason.name").isEqualTo(reason.name)
          .jsonPath("$.reason.isActive").isEqualTo(true)
          .jsonPath("$.daysLostCount").isEqualTo(2)
          .jsonPath("$.temporality").isEqualTo(Temporality.past.value)
          .jsonPath("$.referenceNumber").isEqualTo("REF-123")
          .jsonPath("$.notes").isEqualTo("notes")
          .jsonPath("$.status").isEqualTo("active")
          .jsonPath("$.cancellation").isEqualTo(null)
          .jsonPath("$.revisionHistory[0].updatedBy.id").isEqualTo(originalDetails.createdBy!!.id.toString())
          .jsonPath("$.revisionHistory[0].updatedBy.name").isEqualTo(originalDetails.createdBy!!.name)
          .jsonPath("$.revisionHistory[0].updatedBy.deliusUsername").isEqualTo(originalDetails.createdBy!!.deliusUsername)
          .jsonPath("$.revisionHistory[0].revisionType").value(
            containsInAnyOrder(
              Cas1OutOfServiceBedRevisionType.created.value,
            ),
          )
          .jsonPath("$.revisionHistory[0].startDate").isEqualTo(originalDetails.startDate.toString())
          .jsonPath("$.revisionHistory[0].endDate").isEqualTo(originalDetails.endDate.toString())
          .jsonPath("$.revisionHistory[0].reason.id").isEqualTo(originalDetails.reason.id.toString())
          .jsonPath("$.revisionHistory[0].reason.name").isEqualTo(originalDetails.reason.name)
          .jsonPath("$.revisionHistory[0].reason.isActive").isEqualTo(originalDetails.reason.isActive)
          .jsonPath("$.revisionHistory[0].referenceNumber").isEqualTo(originalDetails.referenceNumber)
          .jsonPath("$.revisionHistory[0].notes").isEqualTo(originalDetails.notes)
          .jsonPath("$.revisionHistory[1].updatedBy.id").isEqualTo(user.id.toString())
          .jsonPath("$.revisionHistory[1].updatedBy.name").isEqualTo(user.name)
          .jsonPath("$.revisionHistory[1].updatedBy.deliusUsername").isEqualTo(user.deliusUsername)
          .jsonPath("$.revisionHistory[1].revisionType").value(
            containsInAnyOrder(
              Cas1OutOfServiceBedRevisionType.updatedStartDate.value,
              Cas1OutOfServiceBedRevisionType.updatedEndDate.value,
              Cas1OutOfServiceBedRevisionType.updatedReferenceNumber.value,
              Cas1OutOfServiceBedRevisionType.updatedReason.value,
              Cas1OutOfServiceBedRevisionType.updatedNotes.value,
            ),
          )
          .jsonPath("$.revisionHistory[1].startDate").isEqualTo("2022-08-17")
          .jsonPath("$.revisionHistory[1].endDate").isEqualTo("2022-08-18")
          .jsonPath("$.revisionHistory[1].reason.id").isEqualTo(reason.id.toString())
          .jsonPath("$.revisionHistory[1].reason.name").isEqualTo(reason.name)
          .jsonPath("$.revisionHistory[1].reason.isActive").isEqualTo(true)
          .jsonPath("$.revisionHistory[1].referenceNumber").isEqualTo("REF-123")
          .jsonPath("$.revisionHistory[1].notes").isEqualTo("notes")
          .jsonPath("$.revisionHistory[2]").doesNotExist()
      }
    }

    @Test
    fun `Update Out-Of-Service Beds returns 409 Conflict when a booking for the same bed overlaps`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { user, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist {
              withYieldedApArea {
                apAreaEntityFactory.produceAndPersist()
              }
            }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withBed(bed)
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.parse("2022-08-16"))
            withEndDate(LocalDate.parse("2022-08-30"))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

        val existingBooking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.approvedPremises)
          withCrn("CRN123")
          withYieldedPremises { premises }
          withBed(bed)
          withArrivalDate(LocalDate.parse("2022-07-15"))
          withDepartureDate(LocalDate.parse("2022-08-15"))
        }

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.put()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateCas1OutOfServiceBed(
              startDate = LocalDate.parse("2022-08-01"),
              endDate = LocalDate.parse("2022-08-30"),
              reason = reason.id,
              referenceNumber = "REF-123",
              notes = "notes",
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail").isEqualTo("A booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingBooking.id}")
      }
    }

    @Test
    fun `Update Out-Of-Service Beds returns OK with correct body when only cancelled bookings for the same bed overlap`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { user, jwt ->
        `Given an Offender` { offenderDetails, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist {
                withYieldedApArea {
                  apAreaEntityFactory.produceAndPersist()
                }
              }
            }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val originalDetails: Cas1OutOfServiceBedRevisionEntity

          val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withBed(bed)
          }.apply {
            originalDetails = cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
              withCreatedBy(user)
              withOutOfServiceBed(this@apply)
              withStartDate(LocalDate.parse("2022-08-16"))
              withEndDate(LocalDate.parse("2022-08-30"))
              withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
            }

            this.revisionHistory += originalDetails
          }

          val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

          val existingBooking = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.approvedPremises)
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises { premises }
            withBed(bed)
            withArrivalDate(LocalDate.parse("2022-07-15"))
            withDepartureDate(LocalDate.parse("2022-08-15"))
          }

          existingBooking.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
            withYieldedBooking { existingBooking }
            withDate(LocalDate.parse("2022-07-01"))
            withReason(cancellationReasonEntityFactory.produceAndPersist())
          }.toMutableList()

          webTestClient.put()
            .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdateCas1OutOfServiceBed(
                startDate = LocalDate.parse("2022-08-01"),
                endDate = LocalDate.parse("2022-08-15"),
                reason = reason.id,
                referenceNumber = "REF-123",
                notes = "notes",
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.startDate").isEqualTo("2022-08-01")
            .jsonPath("$.endDate").isEqualTo("2022-08-15")
            .jsonPath("$.bed.id").isEqualTo(bed.id.toString())
            .jsonPath("$.bed.name").isEqualTo(bed.name)
            .jsonPath("$.room.id").isEqualTo(bed.room.id.toString())
            .jsonPath("$.room.name").isEqualTo(bed.room.name)
            .jsonPath("$.premises.id").isEqualTo(premises.id.toString())
            .jsonPath("$.premises.name").isEqualTo(premises.name)
            .jsonPath("$.apArea.id").isEqualTo(premises.probationRegion.apArea!!.id.toString())
            .jsonPath("$.apArea.name").isEqualTo(premises.probationRegion.apArea!!.name)
            .jsonPath("$.reason.id").isEqualTo(reason.id.toString())
            .jsonPath("$.reason.name").isEqualTo(reason.name)
            .jsonPath("$.reason.isActive").isEqualTo(true)
            .jsonPath("$.daysLostCount").isEqualTo(15)
            .jsonPath("$.temporality").isEqualTo(Temporality.past.value)
            .jsonPath("$.referenceNumber").isEqualTo("REF-123")
            .jsonPath("$.notes").isEqualTo("notes")
            .jsonPath("$.status").isEqualTo("active")
            .jsonPath("$.cancellation").isEqualTo(null)
            .jsonPath("$.revisionHistory[0].updatedBy.id").isEqualTo(originalDetails.createdBy!!.id.toString())
            .jsonPath("$.revisionHistory[0].updatedBy.name").isEqualTo(originalDetails.createdBy!!.name)
            .jsonPath("$.revisionHistory[0].updatedBy.deliusUsername").isEqualTo(originalDetails.createdBy!!.deliusUsername)
            .jsonPath("$.revisionHistory[0].revisionType").value(
              containsInAnyOrder(
                Cas1OutOfServiceBedRevisionType.created.value,
              ),
            )
            .jsonPath("$.revisionHistory[0].startDate").isEqualTo(originalDetails.startDate.toString())
            .jsonPath("$.revisionHistory[0].endDate").isEqualTo(originalDetails.endDate.toString())
            .jsonPath("$.revisionHistory[0].reason.id").isEqualTo(originalDetails.reason.id.toString())
            .jsonPath("$.revisionHistory[0].reason.name").isEqualTo(originalDetails.reason.name)
            .jsonPath("$.revisionHistory[0].reason.isActive").isEqualTo(originalDetails.reason.isActive)
            .jsonPath("$.revisionHistory[0].referenceNumber").isEqualTo(originalDetails.referenceNumber)
            .jsonPath("$.revisionHistory[0].notes").isEqualTo(originalDetails.notes)
            .jsonPath("$.revisionHistory[1].updatedBy.id").isEqualTo(user.id.toString())
            .jsonPath("$.revisionHistory[1].updatedBy.name").isEqualTo(user.name)
            .jsonPath("$.revisionHistory[1].updatedBy.deliusUsername").isEqualTo(user.deliusUsername)
            .jsonPath("$.revisionHistory[1].revisionType").value(
              containsInAnyOrder(
                Cas1OutOfServiceBedRevisionType.updatedStartDate.value,
                Cas1OutOfServiceBedRevisionType.updatedEndDate.value,
                Cas1OutOfServiceBedRevisionType.updatedReferenceNumber.value,
                Cas1OutOfServiceBedRevisionType.updatedReason.value,
                Cas1OutOfServiceBedRevisionType.updatedNotes.value,
              ),
            )
            .jsonPath("$.revisionHistory[1].startDate").isEqualTo("2022-08-01")
            .jsonPath("$.revisionHistory[1].endDate").isEqualTo("2022-08-15")
            .jsonPath("$.revisionHistory[1].reason.id").isEqualTo(reason.id.toString())
            .jsonPath("$.revisionHistory[1].reason.name").isEqualTo(reason.name)
            .jsonPath("$.revisionHistory[1].reason.isActive").isEqualTo(true)
            .jsonPath("$.revisionHistory[1].referenceNumber").isEqualTo("REF-123")
            .jsonPath("$.revisionHistory[1].notes").isEqualTo("notes")
            .jsonPath("$.revisionHistory[2]").doesNotExist()
        }
      }
    }

    @Test
    fun `Update Out-Of-Service Beds returns 409 Conflict when An out-of-service bed for the same bed overlaps`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { user, jwt ->
        `Given an Offender` { _, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
            }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

          val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withBed(bed)
          }.apply {
            this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
              withCreatedBy(user)
              withOutOfServiceBed(this@apply)
              withStartDate(LocalDate.parse("2022-08-16"))
              withEndDate(LocalDate.parse("2022-08-30"))
              withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
            }
          }

          val existingOutOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withBed(bed)
          }.apply {
            this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
              withCreatedBy(user)
              withOutOfServiceBed(this@apply)
              withStartDate(LocalDate.parse("2022-07-15"))
              withEndDate(LocalDate.parse("2022-08-15"))
              withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
            }
          }

          webTestClient.put()
            .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdateCas1OutOfServiceBed(
                startDate = LocalDate.parse("2022-08-01"),
                endDate = LocalDate.parse("2022-08-30"),
                reason = reason.id,
                referenceNumber = "REF-123",
                notes = "notes",
              ),
            )
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody()
            .jsonPath("title").isEqualTo("Conflict")
            .jsonPath("status").isEqualTo(409)
            .jsonPath("detail").isEqualTo("An out-of-service bed already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingOutOfServiceBed.id}")
        }
      }
    }

    @Test
    fun `Update Out-Of-Service Beds returns OK with correct body when only cancelled out-of-service beds for the same bed overlap`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { user, jwt ->
        `Given an Offender` { _, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
            }
          }

          val bed = bedEntityFactory.produceAndPersist {
            withYieldedRoom {
              roomEntityFactory.produceAndPersist {
                withYieldedPremises { premises }
              }
            }
          }

          val reason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

          val originalDetails: Cas1OutOfServiceBedRevisionEntity

          val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withBed(bed)
          }.apply {
            originalDetails = cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
              withCreatedBy(user)
              withOutOfServiceBed(this@apply)
              withStartDate(LocalDate.parse("2022-08-16"))
              withEndDate(LocalDate.parse("2022-08-30"))
              withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
            }

            this.revisionHistory += originalDetails
          }

          val existingOutOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withBed(bed)
          }.apply {
            this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
              withCreatedBy(user)
              withOutOfServiceBed(this@apply)
              withStartDate(LocalDate.parse("2022-07-15"))
              withEndDate(LocalDate.parse("2022-08-15"))
              withReason(
                cas1OutOfServiceBedReasonEntityFactory.produceAndPersist(),
              )
            }
          }

          existingOutOfServiceBed.cancellation = cas1OutOfServiceBedCancellationEntityFactory.produceAndPersist {
            withOutOfServiceBed(existingOutOfServiceBed)
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }

          webTestClient.put()
            .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              UpdateCas1OutOfServiceBed(
                startDate = LocalDate.parse("2022-08-01"),
                endDate = LocalDate.parse("2022-08-15"),
                reason = reason.id,
                referenceNumber = "REF-123",
                notes = "notes",
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.startDate").isEqualTo("2022-08-01")
            .jsonPath("$.endDate").isEqualTo("2022-08-15")
            .jsonPath("$.bed.id").isEqualTo(bed.id.toString())
            .jsonPath("$.bed.name").isEqualTo(bed.name)
            .jsonPath("$.room.id").isEqualTo(bed.room.id.toString())
            .jsonPath("$.room.name").isEqualTo(bed.room.name)
            .jsonPath("$.premises.id").isEqualTo(premises.id.toString())
            .jsonPath("$.premises.name").isEqualTo(premises.name)
            .jsonPath("$.apArea.id").isEqualTo(premises.probationRegion.apArea!!.id.toString())
            .jsonPath("$.apArea.name").isEqualTo(premises.probationRegion.apArea!!.name)
            .jsonPath("$.reason.id").isEqualTo(reason.id.toString())
            .jsonPath("$.reason.name").isEqualTo(reason.name)
            .jsonPath("$.reason.isActive").isEqualTo(true)
            .jsonPath("$.daysLostCount").isEqualTo(15)
            .jsonPath("$.temporality").isEqualTo(Temporality.past.value)
            .jsonPath("$.referenceNumber").isEqualTo("REF-123")
            .jsonPath("$.notes").isEqualTo("notes")
            .jsonPath("$.status").isEqualTo("active")
            .jsonPath("$.cancellation").isEqualTo(null)
            .jsonPath("$.revisionHistory[0].updatedBy.id").isEqualTo(originalDetails.createdBy!!.id.toString())
            .jsonPath("$.revisionHistory[0].updatedBy.name").isEqualTo(originalDetails.createdBy!!.name)
            .jsonPath("$.revisionHistory[0].updatedBy.deliusUsername").isEqualTo(originalDetails.createdBy!!.deliusUsername)
            .jsonPath("$.revisionHistory[0].revisionType").value(
              containsInAnyOrder(
                Cas1OutOfServiceBedRevisionType.created.value,
              ),
            )
            .jsonPath("$.revisionHistory[0].startDate").isEqualTo(originalDetails.startDate.toString())
            .jsonPath("$.revisionHistory[0].endDate").isEqualTo(originalDetails.endDate.toString())
            .jsonPath("$.revisionHistory[0].reason.id").isEqualTo(originalDetails.reason.id.toString())
            .jsonPath("$.revisionHistory[0].reason.name").isEqualTo(originalDetails.reason.name)
            .jsonPath("$.revisionHistory[0].reason.isActive").isEqualTo(originalDetails.reason.isActive)
            .jsonPath("$.revisionHistory[0].referenceNumber").isEqualTo(originalDetails.referenceNumber)
            .jsonPath("$.revisionHistory[0].notes").isEqualTo(originalDetails.notes)
            .jsonPath("$.revisionHistory[1].updatedBy.id").isEqualTo(user.id.toString())
            .jsonPath("$.revisionHistory[1].updatedBy.name").isEqualTo(user.name)
            .jsonPath("$.revisionHistory[1].updatedBy.deliusUsername").isEqualTo(user.deliusUsername)
            .jsonPath("$.revisionHistory[1].revisionType").value(
              containsInAnyOrder(
                Cas1OutOfServiceBedRevisionType.updatedStartDate.value,
                Cas1OutOfServiceBedRevisionType.updatedEndDate.value,
                Cas1OutOfServiceBedRevisionType.updatedReferenceNumber.value,
                Cas1OutOfServiceBedRevisionType.updatedReason.value,
                Cas1OutOfServiceBedRevisionType.updatedNotes.value,
              ),
            )
            .jsonPath("$.revisionHistory[1].startDate").isEqualTo("2022-08-01")
            .jsonPath("$.revisionHistory[1].endDate").isEqualTo("2022-08-15")
            .jsonPath("$.revisionHistory[1].reason.id").isEqualTo(reason.id.toString())
            .jsonPath("$.revisionHistory[1].reason.name").isEqualTo(reason.name)
            .jsonPath("$.revisionHistory[1].reason.isActive").isEqualTo(true)
            .jsonPath("$.revisionHistory[1].referenceNumber").isEqualTo("REF-123")
            .jsonPath("$.revisionHistory[1].notes").isEqualTo("notes")
            .jsonPath("$.revisionHistory[2]").doesNotExist()
        }
      }
    }
  }

  @Nested
  inner class CancelOutOfServiceBed {
    @Test
    fun `Cancel Out-Of-Service Bed without JWT returns 401`() {
      `Given a User` { user, _ ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withBed(
            bedEntityFactory.produceAndPersist {
              withYieldedRoom {
                roomEntityFactory.produceAndPersist {
                  withYieldedPremises { premises }
                }
              }
            },
          )
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().plusDays(2))
            withEndDate(LocalDate.now().plusDays(4))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}/cancellations")
          .bodyValue(
            NewCas1OutOfServiceBedCancellation(
              notes = "Unauthorized",
            ),
          )
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }

    @Test
    fun `Cancel Out-Of-Service Bed for non-existent premises returns 404`() {
      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.post()
        .uri("/cas1/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/out-of-service-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488/cancellations")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewCas1OutOfServiceBedCancellation(
            notes = "Non-existent premises",
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `Cancel Out-Of-Service Bed for non-existent out-of-service bed returns 404`() {
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/out-of-service-beds/9054b6a8-65ad-4d55-91ee-26ba65e05488/cancellations")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewCas1OutOfServiceBedCancellation(
            notes = "Non-existent out-of-service bed",
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_JANITOR" ])
    fun `Cancel Out-Of-Service Bed returns OK with correct body when user has the JANITOR role`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { user, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { roomEntityFactory.produceAndPersist { withPremises(premises) } }
        }

        val outOfServiceBed = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withBed(
            bedEntityFactory.produceAndPersist {
              withYieldedRoom {
                roomEntityFactory.produceAndPersist {
                  withYieldedPremises { premises }
                }
              }
            },
          )
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(user)
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.now().plusDays(2))
            withEndDate(LocalDate.now().plusDays(4))
            withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
          }
        }

        cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()

        webTestClient.post()
          .uri("/cas1/premises/${premises.id}/out-of-service-beds/${outOfServiceBed.id}/cancellations")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewCas1OutOfServiceBedCancellation(
              notes = "Some cancellation notes",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.notes").isEqualTo("Some cancellation notes")
          .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
      }
    }
  }
}
