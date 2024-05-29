package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import java.util.UUID

interface Cas1OutOfServiceBedTestRepository : JpaRepository<Cas1OutOfServiceBedEntity, UUID>
