package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import java.time.OffsetDateTime
import java.util.UUID

@Service
class PlacementApplicationService(
  private val placementApplicationRepository: PlacementApplicationRepository,
  private val jsonSchemaService: JsonSchemaService,
) {

  fun createApplication(
    application: ApplicationEntity,
    user: UserEntity,
  ) = validated<PlacementApplicationEntity> {
    val assessment = application.getLatestAssessment()

    if (assessment?.decision !== AssessmentDecision.ACCEPTED) {
      return generalError("You cannot request a placement request for an application that has not been approved")
    }

    val createdApplication = placementApplicationRepository.save(
      PlacementApplicationEntity(
        id = UUID.randomUUID(),
        application = application,
        createdByUser = user,
        data = null,
        document = null,
        schemaVersion = jsonSchemaService.getNewestSchema(ApprovedPremisesPlacementApplicationJsonSchemaEntity::class.java),
        createdAt = OffsetDateTime.now(),
        submittedAt = null,
        schemaUpToDate = true,
        allocatedToUser = null,
        decision = null,
        allocatedAt = null,
        reallocatedAt = null,
      ),
    )

    return success(createdApplication.apply { schemaUpToDate = true })
  }
}
