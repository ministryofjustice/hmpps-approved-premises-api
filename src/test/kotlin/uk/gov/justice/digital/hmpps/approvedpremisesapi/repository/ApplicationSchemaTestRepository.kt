package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaEntity
import java.util.UUID

@Repository
interface ApplicationSchemaTestRepository : JpaRepository<JsonSchemaEntity, UUID>
