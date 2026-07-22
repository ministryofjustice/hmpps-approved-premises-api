package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesUnarchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceArchiveAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

abstract class Cas3IntegrationTestBase : IntegrationTestBase() {
  inner class V2 {
    @SuppressWarnings("LongParameterList")
    fun getListPremisesByStatus(
      probationDeliveryUnit: ProbationDeliveryUnitEntity,
      localAuthorityArea: LocalAuthorityAreaEntity,
      numberOfPremises: Int,
      premisesStatus: Cas3PremisesStatus,
      startDate: LocalDate = LocalDate.now().minusDays(180),
      endDate: LocalDate? = null,
    ): List<Cas3PremisesEntity> {
      val premisesCharacteristics = cas3PremisesCharacteristicEntityFactory.produceAndPersistMultiple(2)

      val premises = cas3PremisesEntityFactory.produceAndPersistMultiple(numberOfPremises) {
        withProbationDeliveryUnit(probationDeliveryUnit)
        withLocalAuthorityArea(localAuthorityArea)
        withStatus(premisesStatus)
        withStartDate(startDate)
        withEndDate(endDate)
        withAddressLine2(randomStringUpperCase(10))
        withCharacteristics(premisesCharacteristics.toMutableList())
      }

      return premises
    }

    fun createBedspaceInPremises(
      premises: Cas3PremisesEntity,
      startDate: LocalDate,
      endDate: LocalDate? = null,
    ): Cas3BedspacesEntity {
      val bedspaceCharacteristics = cas3BedspaceCharacteristicEntityFactory.produceAndPersistMultiple(2)
      val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
        withPremises(premises)
        withStartDate(startDate)
        withEndDate(endDate)
        withCharacteristics(bedspaceCharacteristics.toMutableList())
      }
      return bedspace
    }
  }

  @SuppressWarnings("LongParameterList")
  fun createCas3PremisesUnarchiveDomainEvent(
    premises: Cas3PremisesEntity,
    userEntity: UserEntity,
    currentStartDate: LocalDate,
    newStartDate: LocalDate,
    currentEndDate: LocalDate,
    cancelledAt: OffsetDateTime? = null,
    transactionId: UUID = UUID.randomUUID(),
  ) = domainEventFactory.produceAndPersist {
    withService(ServiceName.temporaryAccommodation)
    withCas3PremisesId(premises.id)
    withType(DomainEventType.CAS3_PREMISES_UNARCHIVED)
    withCas3TransactionId(transactionId)
    withCas3CancelledAt(cancelledAt)
    withData(
      jsonMapper.writeValueAsString(
        CAS3PremisesUnarchiveEvent(
          id = UUID.randomUUID(),
          timestamp = Instant.now(),
          eventType = EventType.premisesUnarchived,
          eventDetails =
          CAS3PremisesUnarchiveEventDetails(
            premisesId = premises.id,
            currentStartDate = currentStartDate,
            newStartDate = newStartDate,
            currentEndDate = currentEndDate,
            userId = userEntity.id,
            transactionId = transactionId,
          ),
        ),
      ),
    )
  }

  fun createCas3PremisesArchiveDomainEvent(
    premises: Cas3PremisesEntity,
    userEntity: UserEntity,
    date: LocalDate,
    cancelledAt: OffsetDateTime? = null,
    transactionId: UUID = UUID.randomUUID(),
  ) = domainEventFactory.produceAndPersist {
    withService(ServiceName.temporaryAccommodation)
    withCas3PremisesId(premises.id)
    withType(DomainEventType.CAS3_PREMISES_ARCHIVED)
    withCas3TransactionId(transactionId)
    withCas3CancelledAt(cancelledAt)
    withData(
      jsonMapper.writeValueAsString(
        CAS3PremisesArchiveEvent(
          id = UUID.randomUUID(),
          timestamp = Instant.now(),
          eventType = EventType.premisesArchived,
          eventDetails =
          CAS3PremisesArchiveEventDetails(
            premisesId = premises.id,
            endDate = date,
            userId = userEntity.id,
            transactionId = transactionId,
          ),
        ),
      ),
    )
  }

  protected fun createCas3Bedspace(
    bedspace: Cas3BedspacesEntity,
    bedspaceStatus: Cas3BedspaceStatus,
    scheduleUnarchiveDate: LocalDate? = null,
    archiveHistory: List<Cas3BedspaceArchiveAction> = emptyList(),
  ) = Cas3Bedspace(
    id = bedspace.id,
    reference = bedspace.reference,
    startDate = bedspace.createdDate,
    bedspaceCharacteristics = bedspace.characteristics.map { characteristic ->
      Cas3BedspaceCharacteristic(
        id = characteristic.id,
        description = characteristic.description,
        name = characteristic.name,
      )
    },
    endDate = bedspace.endDate,
    status = bedspaceStatus,
    scheduleUnarchiveDate = scheduleUnarchiveDate,
    notes = bedspace.notes,
    archiveHistory = archiveHistory,
  )

  @SuppressWarnings("LongParameterList")
  protected fun createBedspaceArchiveDomainEvent(
    bedspaceId: UUID,
    premisesId: UUID,
    userId: UUID,
    currentEndDate: LocalDate?,
    endDate: LocalDate,
    cancelledAt: OffsetDateTime? = null,
    transactionId: UUID = UUID.randomUUID(),
  ) = domainEventFactory.produceAndPersist {
    withService(ServiceName.temporaryAccommodation)
    withCas3BedspaceId(bedspaceId)
    withCas3PremisesId(premisesId)
    withType(DomainEventType.CAS3_BEDSPACE_ARCHIVED)
    withCas3TransactionId(transactionId)
    withCas3CancelledAt(cancelledAt)
    withData(
      jsonMapper.writeValueAsString(
        CAS3BedspaceArchiveEvent(
          id = UUID.randomUUID(),
          timestamp = OffsetDateTime.now().toInstant(),
          eventType = EventType.bedspaceArchived,
          eventDetails = CAS3BedspaceArchiveEventDetails(
            bedspaceId = bedspaceId,
            premisesId = premisesId,
            currentEndDate = currentEndDate,
            endDate = endDate,
            userId = userId,
            transactionId = transactionId,
          ),
        ),
      ),
    )
  }

  @SuppressWarnings("LongParameterList")
  protected fun createBedspaceUnarchiveDomainEvent(
    bedspace: BedEntity,
    premisesId: UUID,
    userId: UUID,
    newStartDate: LocalDate,
    cancelledAt: OffsetDateTime? = null,
    transactionId: UUID = UUID.randomUUID(),
  ) = domainEventFactory.produceAndPersist {
    withService(ServiceName.temporaryAccommodation)
    withCas3BedspaceId(bedspace.id)
    withCas3PremisesId(premisesId)
    withType(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)
    withCas3TransactionId(transactionId)
    withCas3CancelledAt(cancelledAt)
    withData(
      jsonMapper.writeValueAsString(
        CAS3BedspaceUnarchiveEvent(
          id = UUID.randomUUID(),
          timestamp = OffsetDateTime.now().toInstant(),
          eventType = EventType.bedspaceUnarchived,
          eventDetails = CAS3BedspaceUnarchiveEventDetails(
            bedspaceId = bedspace.id,
            premisesId = premisesId,
            currentStartDate = bedspace.startDate!!,
            currentEndDate = bedspace.endDate!!,
            newStartDate = newStartDate,
            userId = userId,
            transactionId = transactionId,
          ),
        ),
      ),
    )
  }

  @SuppressWarnings("LongParameterList")
  protected fun createBedspaceUnarchiveDomainEvent(
    bedspace: Cas3BedspacesEntity,
    premisesId: UUID,
    userId: UUID,
    newStartDate: LocalDate,
    cancelledAt: OffsetDateTime? = null,
    transactionId: UUID = UUID.randomUUID(),
  ) = domainEventFactory.produceAndPersist {
    withService(ServiceName.temporaryAccommodation)
    withCas3BedspaceId(bedspace.id)
    withCas3PremisesId(premisesId)
    withType(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)
    withCas3TransactionId(transactionId)
    withCas3CancelledAt(cancelledAt)
    withData(
      jsonMapper.writeValueAsString(
        CAS3BedspaceUnarchiveEvent(
          id = UUID.randomUUID(),
          timestamp = OffsetDateTime.now().toInstant(),
          eventType = EventType.bedspaceUnarchived,
          eventDetails = CAS3BedspaceUnarchiveEventDetails(
            bedspaceId = bedspace.id,
            premisesId = premisesId,
            currentStartDate = bedspace.startDate!!,
            currentEndDate = bedspace.endDate!!,
            newStartDate = newStartDate,
            userId = userId,
            transactionId = transactionId,
          ),
        ),
      ),
    )
  }

  protected fun getBedspaceCharacteristics(): List<Cas3BedspaceCharacteristicEntity> = cas3BedspaceCharacteristicRepository.findAll()
}
