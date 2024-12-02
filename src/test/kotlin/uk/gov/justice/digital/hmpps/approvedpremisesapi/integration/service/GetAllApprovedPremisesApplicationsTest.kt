package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.random.Random

class GetAllApprovedPremisesApplicationsTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  private lateinit var applicationService: ApplicationService

  private lateinit var crn1: String
  private lateinit var crn2: String
  private lateinit var apArea: ApAreaEntity
  private lateinit var applicationIdsWithRequestsForPlacement: List<UUID>

  var name = "Search by name"

  private lateinit var allApplications: MutableList<ApprovedPremisesApplicationEntity>

  @BeforeAll
  fun setup() {
    givenAUser { userEntity, _ ->
      givenAnOffender { offenderDetails, _ ->
        givenAnOffender { offenderDetails2, _ ->
          crn1 = offenderDetails.otherIds.crn
          crn2 = offenderDetails2.otherIds.crn
          apArea = givenAnApArea()

          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          allApplications = mutableListOf(
            approvedPremisesApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCrn(crn1)
              withCreatedByUser(userEntity)
              withCreatedAt(OffsetDateTime.now().minusDays(6))
              withArrivalDate(OffsetDateTime.now().plusDays(12))
              withRiskRatings(
                PersonRisksFactory().withTier(
                  RiskWithStatus(
                    RiskTier("Z", LocalDate.now()),
                  ),
                ).produce(),
              )
            },
            approvedPremisesApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCrn(crn2)
              withCreatedByUser(userEntity)
              withCreatedAt(OffsetDateTime.now().minusDays(3))
              withArrivalDate(OffsetDateTime.now().plusDays(26))
              withRiskRatings(
                PersonRisksFactory().withTier(
                  RiskWithStatus(
                    RiskTier("B", LocalDate.now()),
                  ),
                ).produce(),
              )
            },
            approvedPremisesApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withName(name)
              withCreatedByUser(userEntity)
              withCreatedAt(OffsetDateTime.now().minusDays(12))
              withArrivalDate(OffsetDateTime.now().plusDays(9))
              withRiskRatings(
                PersonRisksFactory().withTier(
                  RiskWithStatus(
                    RiskTier("G", LocalDate.now()),
                  ),
                ).produce(),
              )
            },
            approvedPremisesApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withApArea(apArea)
              withCreatedByUser(userEntity)
              withCreatedAt(OffsetDateTime.now().minusDays(9))
              withArrivalDate(OffsetDateTime.now().plusDays(4))
              withRiskRatings(
                PersonRisksFactory().withTier(
                  RiskWithStatus(
                    RiskTier("F", LocalDate.now()),
                  ),
                ).produce(),
              )
            },
          )

          ApprovedPremisesApplicationStatus.entries.forEach { status ->
            allApplications.add(
              approvedPremisesApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(userEntity)
                withStatus(status)
                withCreatedAt(OffsetDateTime.now().minusDays(Random.nextLong(1, 25)))
                withArrivalDate(OffsetDateTime.now().plusDays(Random.nextLong(1, 25)))
                withRiskRatings(
                  PersonRisksFactory().withTier(
                    RiskWithStatus(
                      RiskTier(('A'..'Z').random().toString(), LocalDate.now()),
                    ),
                  ).produce(),
                )
              },
            )
          }

          ReleaseTypeOption.entries.forEach {
            allApplications.add(
              approvedPremisesApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(userEntity)
                withCreatedAt(OffsetDateTime.now().minusDays(Random.nextLong(1, 25)))
                withArrivalDate(OffsetDateTime.now().plusDays(Random.nextLong(1, 25)))
                withReleaseType(it.name)
                withRiskRatings(
                  PersonRisksFactory().withTier(
                    RiskWithStatus(
                      RiskTier(('A'..'Z').random().toString(), LocalDate.now()),
                    ),
                  ).produce(),
                )
              },
            )
          }

          val applicationWithPlacementRequest = allApplications.find { it.status == ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT }

          val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withApplication(applicationWithPlacementRequest!!)
            withAssessmentSchema(assessmentSchema)
          }

          placementRequestFactory.produceAndPersist {
            withApplication(applicationWithPlacementRequest!!)
            withAssessment(assessment)
            withPlacementRequirements(
              placementRequirementsFactory.produceAndPersist {
                withApplication(allApplications[0])
                withAssessment(assessment)
                withPostcodeDistrict(
                  postCodeDistrictFactory.produceAndPersist(),
                )
                withDesirableCriteria(
                  characteristicEntityFactory.produceAndPersistMultiple(5),
                )
                withEssentialCriteria(
                  characteristicEntityFactory.produceAndPersistMultiple(3),
                )
              },
            )
          }

          val applicationWithPlacementApplication = allApplications.find { it.status == ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED }

          placementApplicationFactory.produceAndPersist {
            withApplication(applicationWithPlacementApplication!!)
            withCreatedByUser(userEntity)
            withSchemaVersion(
              approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
                withPermissiveSchema()
              },
            )
          }

          applicationIdsWithRequestsForPlacement = listOf(
            applicationWithPlacementRequest!!.id,
            applicationWithPlacementApplication!!.id,
          )
        }
      }
    }
  }

  @Test
  fun `findAllApprovedPremisesSummaries returns all applications`() {
    allApplications.sortBy { it.createdAt }
    val chunkedApplications = allApplications.chunked(10)

    chunkedApplications.forEachIndexed { page, chunk ->
      val (result, metadata) = applicationService.getAllApprovedPremisesApplications(
        page + 1,
        null,
        null,
        emptyList(),
        null,
        null,
        null,
      )

      assertThat(metadata).isNotNull()
      assertThat(metadata!!.currentPage).isEqualTo(page + 1)
      assertThat(metadata.totalPages).isEqualTo(chunkedApplications.size)
      assertThat(metadata.totalResults).isEqualTo(allApplications.size.toLong())
      assertThat(metadata.pageSize).isEqualTo(10)

      result.forEachIndexed { index, summary ->
        assertThat(summary.matches(chunk[index]))
      }
    }
  }

  @Test
  fun `findAllApprovedPremisesSummaries filters by CRN`() {
    val expectedApplications = allApplications.filter { it.crn == crn1 }
    val result = applicationService.getAllApprovedPremisesApplications(
      1,
      crn1,
      null,
      emptyList(),
      null,
      null,
      null,
    )

    result.first.forEachIndexed { index, summary ->
      assertThat(summary.matches(expectedApplications[index]))
    }
  }

  @Test
  fun `findAllApprovedPremisesSummaries filters by name`() {
    val expectedApplications = allApplications.filter { it.name == name }
    val result = applicationService.getAllApprovedPremisesApplications(
      1,
      name,
      null,
      emptyList(),
      null,
      null,
      null,
    )

    result.first.forEachIndexed { index, summary ->
      assertThat(summary.matches(expectedApplications[index]))
    }
  }

  @ParameterizedTest
  @EnumSource(ApprovedPremisesApplicationStatus::class)
  fun `findAllApprovedPremisesSummaries filters by status`(status: ApprovedPremisesApplicationStatus) {
    val expectedApplications = allApplications.filter { it.status == status }

    val result = applicationService.getAllApprovedPremisesApplications(
      1,
      null,
      null,
      listOf(status),
      null,
      null,
      null,
    )

    result.first.forEachIndexed { index, summary ->
      assertThat(summary.matches(expectedApplications[index]))
    }
  }

  @Test
  fun `findAllApprovedPremisesSummaries filters by statuses`() {
    val statuses = listOf(
      ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT,
      ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED,
      ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST,
    )

    val expectedApplications = allApplications.filter { statuses.contains(it.status) }
    assertThat(expectedApplications).hasSizeGreaterThan(2)

    val result = applicationService.getAllApprovedPremisesApplications(
      1,
      null,
      null,
      statuses,
      null,
      null,
      null,
    )

    result.first.forEachIndexed { index, summary ->
      assertThat(summary.matches(expectedApplications[index]))
    }
  }

  @Test
  fun `findAllApprovedPremisesSummaries filters by AP Area`() {
    val expectedApplications = allApplications.filter { it.apArea?.id == apArea.id }

    val result = applicationService.getAllApprovedPremisesApplications(
      1,
      null,
      null,
      emptyList(),
      null,
      apArea.id,
      null,
    )

    result.first.forEachIndexed { index, summary ->
      assertThat(summary.matches(expectedApplications[index]))
    }
  }

  @ParameterizedTest
  @EnumSource(ReleaseTypeOption::class)
  fun `findAllApprovedPremisesSummaries filters by release type`(releaseType: ReleaseTypeOption) {
    val expectedApplications = allApplications.filter { it.releaseType == releaseType.name }

    val result = applicationService.getAllApprovedPremisesApplications(
      1,
      null,
      null,
      emptyList(),
      null,
      null,
      releaseType.name,
    )

    result.first.forEachIndexed { index, summary ->
      assertThat(summary.matches(expectedApplications[index]))
    }
  }

  @Test
  fun `findAllApprovedPremisesSummaries handles pagination`() {
    allApplications.forEachIndexed { index, application ->
      val (result, metadata) = applicationService.getAllApprovedPremisesApplications(
        index + 1,
        null,
        null,
        emptyList(),
        null,
        null,
        null,
        1,
      )

      assertThat(metadata).isNotNull()
      assertThat(metadata!!.currentPage).isEqualTo(index + 1)
      assertThat(metadata.totalPages).isEqualTo(allApplications.size)
      assertThat(metadata.totalResults).isEqualTo(allApplications.size.toLong())
      assertThat(metadata.pageSize).isEqualTo(1)

      assertThat(result.size).isEqualTo(1)
      assertThat(result[0].matches(application))
    }
  }

  @ParameterizedTest
  @EnumSource(ApplicationSortField::class)
  fun `findAllApprovedPremisesSummaries sorts by a given field in ascending order`(sortField: ApplicationSortField) {
    allApplications.sort(SortDirection.ASC, sortField)
    val chunkedApplications = allApplications.chunked(10)

    chunkedApplications.forEachIndexed { page, chunk ->
      val (result, metadata) = applicationService.getAllApprovedPremisesApplications(
        page + 1,
        null,
        SortDirection.ASC,
        emptyList(),
        sortField,
        null,
        null,
      )

      assertThat(metadata).isNotNull()
      assertThat(metadata!!.currentPage).isEqualTo(page + 1)
      assertThat(metadata.totalPages).isEqualTo(chunkedApplications.size)
      assertThat(metadata.totalResults).isEqualTo(allApplications.size.toLong())
      assertThat(metadata.pageSize).isEqualTo(10)

      result.forEachIndexed { index, summary ->
        assertThat(summary.matches(chunk[index]))
      }
    }
  }

  @ParameterizedTest
  @EnumSource(ApplicationSortField::class)
  fun `findAllApprovedPremisesSummaries sorts by a given field in descending order`(sortField: ApplicationSortField) {
    allApplications.sort(SortDirection.desc, sortField)
    val chunkedApplications = allApplications.chunked(10)

    chunkedApplications.forEachIndexed { page, chunk ->
      val (result, metadata) = applicationService.getAllApprovedPremisesApplications(
        page + 1,
        null,
        SortDirection.desc,
        emptyList(),
        sortField,
        null,
        null,
      )

      assertThat(metadata).isNotNull()
      assertThat(metadata!!.currentPage).isEqualTo(page + 1)
      assertThat(metadata.totalPages).isEqualTo(chunkedApplications.size)
      assertThat(metadata.totalResults).isEqualTo(allApplications.size.toLong())
      assertThat(metadata.pageSize).isEqualTo(10)

      result.forEachIndexed { index, summary ->
        assertThat(summary.matches(chunk[index]))
      }
    }
  }

  private fun List<ApprovedPremisesApplicationEntity>.sort(sortDirection: SortDirection, sortField: ApplicationSortField): List<ApprovedPremisesApplicationEntity> {
    val comparator = Comparator<ApprovedPremisesApplicationEntity> { a, b ->
      val ascendingCompare = when (sortField) {
        ApplicationSortField.createdAt -> compareValues(a.createdAt, b.createdAt)
        ApplicationSortField.arrivalDate -> compareValues(a.arrivalDate, b.arrivalDate)
        ApplicationSortField.tier -> compareValues(a.riskRatings?.tier?.status, b.riskRatings?.tier?.status)
        ApplicationSortField.releaseType -> compareValues(a.releaseType, b.releaseType)
      }

      when (sortDirection) {
        SortDirection.ASC, null -> ascendingCompare
        SortDirection.desc -> -ascendingCompare
      }
    }

    return this.sortedWith(comparator)
  }

  private fun ApprovedPremisesApplicationSummary.matches(applicationEntity: ApprovedPremisesApplicationEntity): Boolean {
    return this.getIsWomensApplication() == applicationEntity.isWomensApplication &&
      (this.getIsEmergencyApplication() == (applicationEntity.noticeType == Cas1ApplicationTimelinessCategory.emergency)) &&
      (this.getIsEsapApplication() == applicationEntity.isEsapApplication) &&
      (this.getIsPipeApplication() == applicationEntity.isPipeApplication) &&
      (this.getArrivalDate() == Instant.parse(applicationEntity.arrivalDate.toString())) &&
      (this.getRiskRatings() == objectMapper.writeValueAsString(applicationEntity.riskRatings)) &&
      (this.getId() == applicationEntity.id) &&
      (this.getCrn() == applicationEntity.crn) &&
      (this.getCreatedByUserId() == applicationEntity.createdByUser.id) &&
      (this.getCreatedAt() == Instant.parse(applicationEntity.createdAt.toString())) &&
      (this.getSubmittedAt() == Instant.parse(applicationEntity.submittedAt.toString())) &&
      this.getTier() == applicationEntity.riskRatings?.tier?.value.toString() &&
      this.getStatus() == applicationEntity.status.toString() &&
      this.getIsWithdrawn() == applicationEntity.isWithdrawn &&
      this.getReleaseType() == applicationEntity.releaseType.toString() &&
      this.getHasRequestsForPlacement() == applicationIdsWithRequestsForPlacement.contains(applicationEntity.id)
  }
}
