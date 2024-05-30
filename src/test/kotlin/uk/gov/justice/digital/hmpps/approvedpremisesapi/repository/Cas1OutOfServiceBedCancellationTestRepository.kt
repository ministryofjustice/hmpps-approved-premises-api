package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedCancellationEntity
import java.util.UUID

interface Cas1OutOfServiceBedCancellationTestRepository : JpaRepository<Cas1OutOfServiceBedCancellationEntity, UUID>
