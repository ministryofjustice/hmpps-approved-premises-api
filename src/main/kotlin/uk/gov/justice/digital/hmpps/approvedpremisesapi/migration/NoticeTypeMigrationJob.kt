package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import java.util.UUID

@Repository
interface NoticeTypeMigrationJobApplicationRepository : JpaRepository<ApplicationEntity, UUID> {
  @Modifying
  @Query(
    "UPDATE ApprovedPremisesApplicationEntity ap set " +
      "ap.noticeType = 'emergency' " +
      "where ap.isEmergencyApplication = true",
  )
  fun updateEmergencyApplicationNoticeType()

  @Modifying
  @Query("UPDATE ApprovedPremisesApplicationEntity ap set ap.noticeType = :noticeType where ap.id in :ids")
  fun updateApplicationNoticeType(ids: List<UUID>, noticeType: Cas1ApplicationTimelinessCategory)

  @Query("SELECT ap FROM ApprovedPremisesApplicationEntity ap where ap.isEmergencyApplication = false")
  fun getApplicationsThatRequireNoticeTypeUpdating(pageable: Pageable): Slice<ApprovedPremisesApplicationEntity>
}

class NoticeTypeMigrationJob(
  private val applicationRepository: NoticeTypeMigrationJobApplicationRepository,
  private val pageSize: Int,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = true

  override fun process() {
    applicationRepository.updateEmergencyApplicationNoticeType()

    var page = 0
    var hasNext = true
    var slice: Slice<ApprovedPremisesApplicationEntity>

    val shortNoticeApplicationIds = mutableListOf<UUID>()
    val standardApplicationIds = mutableListOf<UUID>()

    while (hasNext) {
      log.info("Getting page $page")
      slice = applicationRepository.getApplicationsThatRequireNoticeTypeUpdating(PageRequest.of(page, pageSize))
      slice.content.forEach {
        if (it.isShortNoticeApplication() == true) {
          shortNoticeApplicationIds.add(it.id)
        } else {
          standardApplicationIds.add(it.id)
        }
      }
      hasNext = slice.hasNext()
      page += 1
    }

    log.info("Updating ${shortNoticeApplicationIds.count()} short notice applications")
    applicationRepository.updateApplicationNoticeType(shortNoticeApplicationIds, Cas1ApplicationTimelinessCategory.shortNotice)

    log.info("Updating ${standardApplicationIds.count()} standard notice applications")
    applicationRepository.updateApplicationNoticeType(standardApplicationIds, Cas1ApplicationTimelinessCategory.standard)
  }
}
