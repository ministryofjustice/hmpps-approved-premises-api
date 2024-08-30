package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.data.domain.Page
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Temporality
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedRevisionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedCancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import java.util.stream.Stream

class Cas1OutOfServiceBedServiceTest {
  private val outOfServiceBedRepository = mockk<Cas1OutOfServiceBedRepository>()
  private val outOfServiceBedReasonRepository = mockk<Cas1OutOfServiceBedReasonRepository>()
  private val outOfServiceBedCancellationRepository = mockk<Cas1OutOfServiceBedCancellationRepository>()
  private val outOfServiceBedDetailsRepository = mockk<Cas1OutOfServiceBedRevisionRepository>()
  private val userService = mockk<UserService>()

  private val approvedPremisesFactory = ApprovedPremisesEntityFactory()
    .withDefaults()

  private val outOfServiceBedService = Cas1OutOfServiceBedService(
    outOfServiceBedRepository,
    outOfServiceBedReasonRepository,
    outOfServiceBedCancellationRepository,
    outOfServiceBedDetailsRepository,
    userService,
  )

  companion object {
    @JvmStatic
    fun temporalityArgs(): Stream<Arguments> {
      return Stream.of(
        Arguments.of(
          emptyList<Temporality>(),
        ),
        Arguments.of(
          listOf(
            Temporality.past,
          ),
        ),
        Arguments.of(
          listOf(
            Temporality.current,
          ),
        ),
        Arguments.of(
          listOf(
            Temporality.future,
          ),
        ),
        Arguments.of(
          listOf(
            Temporality.past,
            Temporality.current,
          ),
        ),
        Arguments.of(
          listOf(
            Temporality.past,
            Temporality.future,
          ),
        ),
        Arguments.of(
          listOf(
            Temporality.current,
            Temporality.future,
          ),
        ),
        Arguments.of(
          listOf(
            Temporality.past,
            Temporality.current,
            Temporality.future,
          ),
        ),
      )
    }
  }

  @Nested
  inner class CreateOutOfServiceBeds {

    @Test
    fun `Returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
      val premisesEntity = approvedPremisesFactory.produce()

      val reasonId = UUID.randomUUID()

      every { outOfServiceBedReasonRepository.findByIdOrNull(reasonId) } returns null
      every { userService.getUserForRequest() } returns UserEntityFactory()
        .withDefaults()
        .produce()

      val result = outOfServiceBedService.createOutOfServiceBed(
        premises = premisesEntity,
        startDate = LocalDate.parse("2022-08-28"),
        endDate = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        referenceNumber = "12345",
        notes = "",
        bedId = UUID.randomUUID(),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      assertThat((result as CasResult.FieldValidationError).validationMessages).contains(
        entry("$.endDate", "beforeStartDate"),
        entry("$.reason", "doesNotExist"),
        entry("$.notes", "empty"),
      )
    }

    @Test
    fun `Returns Success with correct result when validation passed`() {
      val premisesEntity = approvedPremisesFactory.produce()

      val room = RoomEntityFactory()
        .withPremises(premisesEntity)
        .produce()

      val bed = BedEntityFactory()
        .withYieldedRoom { room }
        .produce()

      premisesEntity.rooms += room
      room.beds += bed

      val outOfServiceBedReason = Cas1OutOfServiceBedReasonEntityFactory()
        .produce()

      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      every { outOfServiceBedReasonRepository.findByIdOrNull(outOfServiceBedReason.id) } returns outOfServiceBedReason

      every { outOfServiceBedRepository.saveAndFlush(any()) } returnsArgument 0
      every { outOfServiceBedDetailsRepository.saveAndFlush(any()) } returnsArgument 0

      every { userService.getUserForRequest() } returns user

      val result = outOfServiceBedService.createOutOfServiceBed(
        premises = premisesEntity,
        startDate = LocalDate.parse("2022-08-25"),
        endDate = LocalDate.parse("2022-08-28"),
        reasonId = outOfServiceBedReason.id,
        referenceNumber = "12345",
        notes = "notes",
        bedId = bed.id,
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.premises).isEqualTo(premisesEntity)
      assertThat(result.value.reason).isEqualTo(outOfServiceBedReason)
      assertThat(result.value.startDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.endDate).isEqualTo(LocalDate.parse("2022-08-28"))
      assertThat(result.value.referenceNumber).isEqualTo("12345")
      assertThat(result.value.notes).isEqualTo("notes")
    }

    @Test
    fun `Returns Success with correct result when createdBy is null`() {
      val premisesEntity = approvedPremisesFactory.produce()

      val room = RoomEntityFactory()
        .withPremises(premisesEntity)
        .produce()

      val bed = BedEntityFactory()
        .withYieldedRoom { room }
        .produce()

      premisesEntity.rooms += room
      room.beds += bed

      val outOfServiceBedReason = Cas1OutOfServiceBedReasonEntityFactory()
        .produce()

      every { outOfServiceBedReasonRepository.findByIdOrNull(outOfServiceBedReason.id) } returns outOfServiceBedReason

      every { outOfServiceBedRepository.saveAndFlush(any()) } returnsArgument 0
      every { outOfServiceBedDetailsRepository.saveAndFlush(any()) } returnsArgument 0

      val result = outOfServiceBedService.createOutOfServiceBed(
        premises = premisesEntity,
        startDate = LocalDate.parse("2022-08-25"),
        endDate = LocalDate.parse("2022-08-28"),
        reasonId = outOfServiceBedReason.id,
        referenceNumber = "12345",
        notes = "notes",
        bedId = bed.id,
        createdBy = null,
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      assertThat(result.value.premises).isEqualTo(premisesEntity)
      assertThat(result.value.reason).isEqualTo(outOfServiceBedReason)
      assertThat(result.value.startDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.endDate).isEqualTo(LocalDate.parse("2022-08-28"))
      assertThat(result.value.referenceNumber).isEqualTo("12345")
      assertThat(result.value.notes).isEqualTo("notes")
      assertThat(result.value.latestRevision.createdBy).isNull()

      verify(exactly = 0) {
        userService.getUserForRequest()
      }
    }

    @Test
    fun `Returns Success with correct result when createdBy is specified`() {
      val premisesEntity = approvedPremisesFactory.produce()

      val room = RoomEntityFactory()
        .withPremises(premisesEntity)
        .produce()

      val bed = BedEntityFactory()
        .withYieldedRoom { room }
        .produce()

      premisesEntity.rooms += room
      room.beds += bed

      val outOfServiceBedReason = Cas1OutOfServiceBedReasonEntityFactory()
        .produce()

      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      every { outOfServiceBedReasonRepository.findByIdOrNull(outOfServiceBedReason.id) } returns outOfServiceBedReason

      every { outOfServiceBedRepository.saveAndFlush(any()) } returnsArgument 0
      every { outOfServiceBedDetailsRepository.saveAndFlush(any()) } returnsArgument 0

      val result = outOfServiceBedService.createOutOfServiceBed(
        premises = premisesEntity,
        startDate = LocalDate.parse("2022-08-25"),
        endDate = LocalDate.parse("2022-08-28"),
        reasonId = outOfServiceBedReason.id,
        referenceNumber = "12345",
        notes = "notes",
        bedId = bed.id,
        createdBy = user,
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      assertThat(result.value.premises).isEqualTo(premisesEntity)
      assertThat(result.value.reason).isEqualTo(outOfServiceBedReason)
      assertThat(result.value.startDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.endDate).isEqualTo(LocalDate.parse("2022-08-28"))
      assertThat(result.value.referenceNumber).isEqualTo("12345")
      assertThat(result.value.notes).isEqualTo("notes")
      assertThat(result.value.latestRevision.createdBy).isEqualTo(user)

      verify(exactly = 0) {
        userService.getUserForRequest()
      }
    }
  }

  @Nested
  inner class UpdateOutOfServiceBeds {
    @Test
    fun `Returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
      val premisesEntity = approvedPremisesFactory.produce()

      val reasonId = UUID.randomUUID()

      val outOfServiceBed = Cas1OutOfServiceBedEntityFactory()
        .withBed {
          withRoom {
            withPremises(premisesEntity)
          }
        }
        .produce()

      every { outOfServiceBedRepository.findByIdOrNull(outOfServiceBed.id) } returns outOfServiceBed
      every { outOfServiceBedReasonRepository.findByIdOrNull(reasonId) } returns null

      every { userService.getUserForRequest() } returns UserEntityFactory()
        .withDefaults()
        .produce()

      val result = outOfServiceBedService.updateOutOfServiceBed(
        outOfServiceBedId = outOfServiceBed.id,
        startDate = LocalDate.parse("2022-08-28"),
        endDate = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        referenceNumber = "12345",
        notes = "",
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      assertThat((result as CasResult.FieldValidationError).validationMessages).contains(
        entry("$.endDate", "beforeStartDate"),
        entry("$.reason", "doesNotExist"),
        entry("$.notes", "empty"),
      )
    }

    @Test
    fun `Returns Success with correct result when validation passed`() {
      val premisesEntity = approvedPremisesFactory.produce()

      val outOfServiceBedReason = Cas1OutOfServiceBedReasonEntityFactory()
        .produce()

      val outOfServiceBed = Cas1OutOfServiceBedEntityFactory()
        .withBed {
          withRoom {
            withPremises(premisesEntity)
          }
        }
        .produce()

      outOfServiceBed.revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
        .withDetailType(Cas1OutOfServiceBedRevisionType.INITIAL)
        .withOutOfServiceBed(outOfServiceBed)
        .produce()

      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      every { outOfServiceBedRepository.findByIdOrNull(outOfServiceBed.id) } returns outOfServiceBed
      every { outOfServiceBedReasonRepository.findByIdOrNull(outOfServiceBedReason.id) } returns outOfServiceBedReason

      every { outOfServiceBedRepository.save(any()) } returnsArgument 0
      every { outOfServiceBedDetailsRepository.saveAndFlush(any()) } returnsArgument 0

      every { userService.getUserForRequest() } returns user

      val result = outOfServiceBedService.updateOutOfServiceBed(
        outOfServiceBedId = outOfServiceBed.id,
        startDate = LocalDate.parse("2022-08-25"),
        endDate = LocalDate.parse("2022-08-28"),
        reasonId = outOfServiceBedReason.id,
        referenceNumber = "12345",
        notes = "notes",
      )
      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.premises).isEqualTo(premisesEntity)
      assertThat(result.value.reason).isEqualTo(outOfServiceBedReason)
      assertThat(result.value.startDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.endDate).isEqualTo(LocalDate.parse("2022-08-28"))
      assertThat(result.value.referenceNumber).isEqualTo("12345")
      assertThat(result.value.notes).isEqualTo("notes")
    }

    @Test
    fun `Returns Success with correct result when createdBy is null`() {
      val premisesEntity = approvedPremisesFactory.produce()

      val outOfServiceBedReason = Cas1OutOfServiceBedReasonEntityFactory()
        .produce()

      val outOfServiceBed = Cas1OutOfServiceBedEntityFactory()
        .withBed {
          withRoom {
            withPremises(premisesEntity)
          }
        }
        .produce()

      outOfServiceBed.revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
        .withDetailType(Cas1OutOfServiceBedRevisionType.INITIAL)
        .withOutOfServiceBed(outOfServiceBed)
        .produce()

      every { outOfServiceBedRepository.findByIdOrNull(outOfServiceBed.id) } returns outOfServiceBed
      every { outOfServiceBedReasonRepository.findByIdOrNull(outOfServiceBedReason.id) } returns outOfServiceBedReason

      every { outOfServiceBedRepository.save(any()) } returnsArgument 0
      every { outOfServiceBedDetailsRepository.saveAndFlush(any()) } returnsArgument 0

      val result = outOfServiceBedService.updateOutOfServiceBed(
        outOfServiceBedId = outOfServiceBed.id,
        startDate = LocalDate.parse("2022-08-25"),
        endDate = LocalDate.parse("2022-08-28"),
        reasonId = outOfServiceBedReason.id,
        referenceNumber = "12345",
        notes = "notes",
        createdBy = null,
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      assertThat(result.value.premises).isEqualTo(premisesEntity)
      assertThat(result.value.reason).isEqualTo(outOfServiceBedReason)
      assertThat(result.value.startDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.endDate).isEqualTo(LocalDate.parse("2022-08-28"))
      assertThat(result.value.referenceNumber).isEqualTo("12345")
      assertThat(result.value.notes).isEqualTo("notes")
      assertThat(result.value.latestRevision.createdBy).isNull()

      verify(exactly = 0) {
        userService.getUserForRequest()
      }
    }

    @Test
    fun `Returns Success with correct result when createdBy is specified`() {
      val premisesEntity = approvedPremisesFactory.produce()

      val outOfServiceBedReason = Cas1OutOfServiceBedReasonEntityFactory()
        .produce()

      val outOfServiceBed = Cas1OutOfServiceBedEntityFactory()
        .withBed {
          withRoom {
            withPremises(premisesEntity)
          }
        }
        .produce()

      outOfServiceBed.revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
        .withDetailType(Cas1OutOfServiceBedRevisionType.INITIAL)
        .withOutOfServiceBed(outOfServiceBed)
        .produce()

      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      every { outOfServiceBedRepository.findByIdOrNull(outOfServiceBed.id) } returns outOfServiceBed
      every { outOfServiceBedReasonRepository.findByIdOrNull(outOfServiceBedReason.id) } returns outOfServiceBedReason

      every { outOfServiceBedRepository.save(any()) } returnsArgument 0
      every { outOfServiceBedDetailsRepository.saveAndFlush(any()) } returnsArgument 0

      val result = outOfServiceBedService.updateOutOfServiceBed(
        outOfServiceBedId = outOfServiceBed.id,
        startDate = LocalDate.parse("2022-08-25"),
        endDate = LocalDate.parse("2022-08-28"),
        reasonId = outOfServiceBedReason.id,
        referenceNumber = "12345",
        notes = "notes",
        createdBy = user,
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      assertThat(result.value.premises).isEqualTo(premisesEntity)
      assertThat(result.value.reason).isEqualTo(outOfServiceBedReason)
      assertThat(result.value.startDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.endDate).isEqualTo(LocalDate.parse("2022-08-28"))
      assertThat(result.value.referenceNumber).isEqualTo("12345")
      assertThat(result.value.notes).isEqualTo("notes")
      assertThat(result.value.latestRevision.createdBy).isEqualTo(user)

      verify(exactly = 0) {
        userService.getUserForRequest()
      }
    }
  }

  @Nested
  inner class CancelOutOfServiceBed {
    @Test
    fun `Returns GeneralValidationError when the out-of-service bed already has a cancellation`() {
      val premisesEntity = approvedPremisesFactory.produce()

      val outOfServiceBed = Cas1OutOfServiceBedEntityFactory()
        .withBed {
          withRoom {
            withPremises(premisesEntity)
          }
        }
        .produce()
        .apply {
          cancellation = Cas1OutOfServiceBedCancellationEntityFactory()
            .withOutOfServiceBed(this)
            .produce()
        }

      val result = outOfServiceBedService.cancelOutOfServiceBed(outOfServiceBed, notes = null)

      assertThat(result).isInstanceOf(CasResult.GeneralValidationError::class.java)
      result as CasResult.GeneralValidationError
      assertThat(result.message).isEqualTo("This out-of-service bed has already been cancelled.")
    }

    @Test
    fun `Returns Success with correct result when validation passed`() {
      val premisesEntity = approvedPremisesFactory.produce()

      val outOfServiceBed = Cas1OutOfServiceBedEntityFactory()
        .withBed {
          withRoom {
            withPremises(premisesEntity)
          }
        }
        .produce()

      val notes = "Some notes"

      every { outOfServiceBedCancellationRepository.save(any()) } returnsArgument 0

      val result = outOfServiceBedService.cancelOutOfServiceBed(outOfServiceBed, notes)

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.outOfServiceBed).isEqualTo(outOfServiceBed)
      assertThat(result.value.notes).isEqualTo(notes)
    }
  }

  @Nested
  inner class GetOutOfServiceBeds {
    @CsvSource(
      // Ascending
      "'premisesName','asc','p.name'",
      "'roomName','asc','r.name'",
      "'bedName','asc','b.name'",
      "'startDate','asc','d.start_date'",
      "'endDate','asc','d.end_date'",
      "'reason','asc','oosr.name'",
      "'daysLost','asc','(d.end_date - d.start_date)'",

      // Descending
      "'premisesName','desc','p.name'",
      "'roomName','desc','r.name'",
      "'bedName','desc','b.name'",
      "'startDate','desc','d.start_date'",
      "'endDate','desc','d.end_date'",
      "'reason','desc','oosr.name'",
      "'daysLost','desc','(d.end_date - d.start_date)'",
    )
    @ParameterizedTest
    fun `Sorts correctly according to the sort field and direction`(
      sortField: Cas1OutOfServiceBedSortField,
      sortDirection: SortDirection,
      expectedSortFieldString: String,
    ) {
      val expectedPageable = getPageableOrAllPages(expectedSortFieldString, sortDirection, page = null, pageSize = null, unsafe = true)

      every {
        outOfServiceBedRepository.findOutOfServiceBeds(
          premisesId = any(),
          apAreaId = any(),
          excludePast = any(),
          excludeCurrent = any(),
          excludeFuture = any(),
          pageable = any(),
        )
      } returns Page.empty()

      every { outOfServiceBedRepository.findAllById(any()) } returns emptyList()

      outOfServiceBedService.getOutOfServiceBeds(
        Temporality.entries.toSet(),
        premisesId = null,
        apAreaId = null,
        pageCriteria = PageCriteria(sortField, sortDirection, page = null, perPage = null),
      )

      verify(exactly = 1) {
        outOfServiceBedRepository.findOutOfServiceBeds(
          premisesId = null,
          apAreaId = null,
          excludePast = false,
          excludeCurrent = false,
          excludeFuture = false,
          pageable = expectedPageable,
        )
      }
    }

    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1OutOfServiceBedServiceTest#temporalityArgs")
    @ParameterizedTest
    fun `Filters correctly according to the temporality`(temporality: List<Temporality>) {
      every {
        outOfServiceBedRepository.findOutOfServiceBeds(
          premisesId = any(),
          apAreaId = any(),
          excludePast = any(),
          excludeCurrent = any(),
          excludeFuture = any(),
          pageable = any(),
        )
      } returns Page.empty()

      every { outOfServiceBedRepository.findAllById(any()) } returns emptyList()

      outOfServiceBedService.getOutOfServiceBeds(
        temporality.toSet(),
        premisesId = null,
        apAreaId = null,
        pageCriteria = PageCriteria(Cas1OutOfServiceBedSortField.startDate, SortDirection.asc, page = null, perPage = null),
      )

      verify(exactly = 1) {
        outOfServiceBedRepository.findOutOfServiceBeds(
          premisesId = null,
          apAreaId = null,
          excludePast = !temporality.contains(Temporality.past),
          excludeCurrent = !temporality.contains(Temporality.current),
          excludeFuture = !temporality.contains(Temporality.future),
          pageable = any(),
        )
      }
    }

    @Test
    fun `Filters correctly according to the premises ID`() {
      every {
        outOfServiceBedRepository.findOutOfServiceBeds(
          premisesId = any(),
          apAreaId = any(),
          excludePast = any(),
          excludeCurrent = any(),
          excludeFuture = any(),
          pageable = any(),
        )
      } returns Page.empty()

      every { outOfServiceBedRepository.findAllById(any()) } returns emptyList()

      val expectedId = UUID.randomUUID()

      outOfServiceBedService.getOutOfServiceBeds(
        Temporality.entries.toSet(),
        premisesId = expectedId,
        apAreaId = null,
        pageCriteria = PageCriteria(Cas1OutOfServiceBedSortField.startDate, SortDirection.asc, page = null, perPage = null),
      )

      verify(exactly = 1) {
        outOfServiceBedRepository.findOutOfServiceBeds(
          premisesId = expectedId,
          apAreaId = null,
          excludePast = false,
          excludeCurrent = false,
          excludeFuture = false,
          pageable = any(),
        )
      }
    }

    @Test
    fun `Filters correctly according to the AP area ID`() {
      every {
        outOfServiceBedRepository.findOutOfServiceBeds(
          premisesId = any(),
          apAreaId = any(),
          excludePast = any(),
          excludeCurrent = any(),
          excludeFuture = any(),
          pageable = any(),
        )
      } returns Page.empty()

      every { outOfServiceBedRepository.findAllById(any()) } returns emptyList()

      val expectedId = UUID.randomUUID()

      outOfServiceBedService.getOutOfServiceBeds(
        Temporality.entries.toSet(),
        premisesId = null,
        apAreaId = expectedId,
        pageCriteria = PageCriteria(Cas1OutOfServiceBedSortField.startDate, SortDirection.asc, page = null, perPage = null),
      )

      verify(exactly = 1) {
        outOfServiceBedRepository.findOutOfServiceBeds(
          premisesId = null,
          apAreaId = expectedId,
          excludePast = false,
          excludeCurrent = false,
          excludeFuture = false,
          pageable = any(),
        )
      }
    }
  }

  @Nested
  inner class GetActiveOutOfServiceBedsForPremisesId {
    @Test
    fun `Delegates to repository method`() {
      val expectedList = mockk<List<Cas1OutOfServiceBedEntity>>()
      every { outOfServiceBedRepository.findAllActiveForPremisesId(any()) } returns expectedList

      val premisesId = UUID.randomUUID()

      val result = outOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premisesId)

      assertThat(result).isEqualTo(expectedList)
      verify(exactly = 1) { outOfServiceBedRepository.findAllActiveForPremisesId(premisesId) }
    }
  }

  @Nested
  inner class GetOutOfServiceBedWithConflictingDates {
    @Test
    fun `Returns null when no overlapping out-of-service beds exist`() {
      val expectedList = listOf<String>()
      every { outOfServiceBedRepository.findByBedIdAndOverlappingDate(any(), any(), any(), any()) } returns expectedList

      val id = UUID.randomUUID()
      val bedId = UUID.randomUUID()

      val result = outOfServiceBedService.getOutOfServiceBedWithConflictingDates(
        LocalDate.parse("2024-01-01"),
        LocalDate.parse("2024-02-01"),
        id,
        bedId,
      )

      assertThat(result).isEqualTo(null)
      verify(exactly = 1) {
        outOfServiceBedRepository.findByBedIdAndOverlappingDate(
          bedId,
          LocalDate.parse("2024-01-01"),
          LocalDate.parse("2024-02-01"),
          id,
        )
      }
    }

    @Test
    fun `Returns the first result when one or more out-of-service beds exist`() {
      val expectedId = UUID.randomUUID()
      val bedId = UUID.randomUUID()

      val expectedOutOfServiceBed = Cas1OutOfServiceBedEntityFactory()
        .withId(expectedId)
        .withBed {
          withId(bedId)
          withRoom {
            withPremises(
              ApprovedPremisesEntityFactory()
                .withDefaults()
                .produce(),
            )
          }
        }
        .produce()

      every { outOfServiceBedRepository.findByBedIdAndOverlappingDate(any(), any(), any(), any()) } returns listOf(expectedId.toString())

      every { outOfServiceBedRepository.findById(any()) } returns Optional.of(expectedOutOfServiceBed)

      val result = outOfServiceBedService.getOutOfServiceBedWithConflictingDates(
        LocalDate.parse("2024-01-01"),
        LocalDate.parse("2024-02-01"),
        expectedId,
        bedId,
      )

      assertThat(result).isEqualTo(expectedOutOfServiceBed)
      verify(exactly = 1) {
        outOfServiceBedRepository.findByBedIdAndOverlappingDate(
          bedId,
          LocalDate.parse("2024-01-01"),
          LocalDate.parse("2024-02-01"),
          expectedId,
        )
      }
      verify(exactly = 1) {
        outOfServiceBedRepository.findById(expectedId)
      }
    }
  }
}
