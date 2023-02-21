package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesLostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationLostBedEntity
import java.util.UUID

@Repository
interface LostBedsTestRepository : JpaRepository<LostBedsEntity, UUID>

@Repository
interface ApprovedPremisesLostBedsTestRepository : JpaRepository<ApprovedPremisesLostBedsEntity, UUID>

@Repository
interface TemporaryAccommodationLostBedTestRepository : JpaRepository<TemporaryAccommodationLostBedEntity, UUID>
