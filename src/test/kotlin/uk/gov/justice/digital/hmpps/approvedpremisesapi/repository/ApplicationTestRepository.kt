package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import java.util.UUID

@Repository
interface ApprovedPremisesApplicationTestRepository : JpaRepository<ApprovedPremisesApplicationEntity, UUID>

@Repository
interface Cas2v2ApplicationTestRepository : JpaRepository<Cas2v2ApplicationEntity, UUID>

@Repository
interface TemporaryAccommodationApplicationTestRepository : JpaRepository<TemporaryAccommodationApplicationEntity, UUID>
