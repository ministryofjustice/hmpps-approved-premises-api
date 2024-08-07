package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import java.util.UUID

@Repository
interface ApAreaMigrationJobApplicationRepository : JpaRepository<ApplicationEntity, UUID> {
  @Modifying
  @Query("UPDATE ApprovedPremisesApplicationEntity ap set ap.apArea = :apArea WHERE ap.id IN :applicationIds")
  fun setApAreaOnApplications(applicationIds: List<UUID>, apArea: ApAreaEntity)

  @Modifying
  @Query(
    """
      UPDATE
        approved_premises_applications
      set
        ap_area_id = (
          SELECT
            ap_area.id
          from
            users u
            left join probation_regions pr on u.probation_region_id = pr.id
            left join ap_areas ap_area on pr.ap_area_id = ap_area.id
            left join applications a on a.created_by_user_id = u.id
            left join approved_premises_applications apa on apa.id = a.id
          where
            a.id = approved_premises_applications.id
        )
      where
        approved_premises_applications.ap_area_id is null
    """,
    nativeQuery = true,
  )
  fun setApAreaOnApplicationsFromUser()
}

val midlandsApplicationIds = listOf(
  UUID.fromString("43cb06f2-b146-4919-9d3c-a153a79d7f36"),
  UUID.fromString("446e01b0-8b0d-4bf7-aee8-28e1a0aeb99b"),
  UUID.fromString("32d61661-3222-4c29-a156-acdd4d06b0b0"),
  UUID.fromString("cabcfa1b-1929-42cc-a741-f88e108405d6"),
)

val seeApplicationIds = listOf(
  UUID.fromString("b4b4e716-5767-4095-aea7-530c3890bea5"),
  UUID.fromString("fab59535-8873-4ccb-a875-5fd23c4ecc63"),
)

val northEastApplicationIds = listOf(
  UUID.fromString("8bb8342d-0432-4eb5-b977-8b58465fc329"),
  UUID.fromString("2a7eb94f-fa8e-4038-871e-924286ea2c15"),
  UUID.fromString("5f8be2be-7d3d-480b-998f-2b860d01cc14"),
  UUID.fromString("72688238-8a86-4924-a560-e5bff167ebbf"),
  UUID.fromString("ac36b31b-f384-48a3-9d30-210aea66b085"),
  UUID.fromString("e13a6e07-4ed9-4b94-9eae-4719fbbd6eb3"),
  UUID.fromString("e5d454b0-875b-4a2e-89d1-746c7e63f9d1"),
  UUID.fromString("2b1b4be4-af12-4d7a-a4dc-0457e00b1975"),
  UUID.fromString("2b76e00b-abb2-4f90-aa97-fa5244041b08"),
  UUID.fromString("9975d567-c108-446a-9584-b0f017552bc8"),
  UUID.fromString("a202a781-f26e-4855-90e9-152aa8ebad8e"),
  UUID.fromString("c996ec67-def4-4e63-8a20-52affe9f4988"),
  UUID.fromString("d620c1c3-e3de-4910-9532-e2b6128cd439"),
)

class ApAreaMigrationJob(
  private val probationAreaMigrationJobApplicationRepository: ApAreaMigrationJobApplicationRepository,
  private val apAreaRepository: ApAreaRepository,
  private val transactionTemplate: TransactionTemplate,
) : MigrationJob() {
  override val shouldRunInTransaction = false

  @SuppressWarnings("TooGenericExceptionThrown")
  override fun process() {
    val midlands = apAreaRepository.findByName("Midlands") ?: throw RuntimeException("Cannot find probation region - Midlands")
    val southEastAndEastern = apAreaRepository.findByName("South East & Eastern") ?: throw RuntimeException("Cannot find probation region - South East & Eastern")
    val northEast = apAreaRepository.findByName("North East") ?: throw RuntimeException("Cannot find probation region - North East")

    transactionTemplate.executeWithoutResult {
      probationAreaMigrationJobApplicationRepository.setApAreaOnApplications(midlandsApplicationIds, midlands)
      probationAreaMigrationJobApplicationRepository.setApAreaOnApplications(seeApplicationIds, southEastAndEastern)
      probationAreaMigrationJobApplicationRepository.setApAreaOnApplications(northEastApplicationIds, northEast)
    }

    transactionTemplate.executeWithoutResult {
      probationAreaMigrationJobApplicationRepository.setApAreaOnApplicationsFromUser()
    }
  }
}
