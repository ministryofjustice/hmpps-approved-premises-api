package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import java.util.UUID

@Repository
interface PremisesTestRepository : JpaRepository<PremisesEntity, UUID>

@Repository
interface ApprovedPremisesTestRepository : JpaRepository<ApprovedPremisesEntity, UUID>

@Repository
interface TemporaryAccommodationPremisesTestRepository : JpaRepository<TemporaryAccommodationPremisesEntity, UUID>
