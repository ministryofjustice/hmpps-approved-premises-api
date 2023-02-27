package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesLostBedsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationLostBedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedsTransformer

class LostBedsTransformerTest {
  private val lostBedReasonTransformer = mockk<LostBedReasonTransformer>()

  private val lostBedsTransformer = LostBedsTransformer(lostBedReasonTransformer)

  @Test
  fun `Approved Premises lost bed entity is correctly transformed`() {
    val lostBed = ApprovedPremisesLostBedsEntityFactory()
      .withYieldedReason {
        LostBedReasonEntityFactory()
          .produce()
      }
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea {
                ApAreaEntityFactory()
                  .produce()
              }
              .produce()
          }
          .withYieldedLocalAuthorityArea {
            LocalAuthorityEntityFactory()
              .produce()
          }
          .produce()
      }
      .produce()

    every { lostBedReasonTransformer.transformJpaToApi(lostBed.reason) } returns LostBedReason(
      id = lostBed.reason.id,
      name = lostBed.reason.name,
      isActive = true,
      serviceScope = "approved-premises",
    )

    val result = lostBedsTransformer.transformJpaToApi(lostBed)

    assertThat(result.id).isEqualTo(lostBed.id)
    assertThat(result.startDate).isEqualTo(lostBed.startDate)
    assertThat(result.endDate).isEqualTo(lostBed.endDate)
    assertThat(result.reason.id).isEqualTo(lostBed.reason.id)
    assertThat(result.notes).isEqualTo(lostBed.notes)
    assertThat(result.referenceNumber).isEqualTo(lostBed.referenceNumber)
    assertThat(result).isInstanceOf(ApprovedPremisesLostBed::class.java)
    result as ApprovedPremisesLostBed
    assertThat(result.numberOfBeds).isEqualTo(lostBed.numberOfBeds)
  }

  @Test
  fun `Temporary Accommodation lost bed entity is correctly transformed`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory()
              .produce()
          }
          .produce()
      }
      .withYieldedLocalAuthorityArea {
        LocalAuthorityEntityFactory()
          .produce()
      }
      .produce()

    val lostBed = TemporaryAccommodationLostBedEntityFactory()
      .withYieldedReason {
        LostBedReasonEntityFactory()
          .produce()
      }
      .withYieldedPremises { premises }
      .withYieldedBed {
        BedEntityFactory()
          .withYieldedRoom {
            RoomEntityFactory()
              .withYieldedPremises { premises }
              .produce()
          }
          .produce()
      }
      .produce()

    every { lostBedReasonTransformer.transformJpaToApi(lostBed.reason) } returns LostBedReason(
      id = lostBed.reason.id,
      name = lostBed.reason.name,
      isActive = true,
      serviceScope = "approved-premises",
    )

    val result = lostBedsTransformer.transformJpaToApi(lostBed)

    assertThat(result.id).isEqualTo(lostBed.id)
    assertThat(result.startDate).isEqualTo(lostBed.startDate)
    assertThat(result.endDate).isEqualTo(lostBed.endDate)
    assertThat(result.reason.id).isEqualTo(lostBed.reason.id)
    assertThat(result.notes).isEqualTo(lostBed.notes)
    assertThat(result.referenceNumber).isEqualTo(lostBed.referenceNumber)
    assertThat(result).isInstanceOf(TemporaryAccommodationLostBed::class.java)
    result as TemporaryAccommodationLostBed
    assertThat(result.bedId).isEqualTo(lostBed.bed.id)
  }
}
