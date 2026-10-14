package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatusDetail
import java.util.UUID

class Cas2ApplicationStatusSeedingTest {
  @Nested
  inner class ActiveStatuses {
    @Test
    fun `should return active statuses`() {
      val statuses = Cas2ApplicationStatusSeeding.activeStatuses()

      assertThat(statuses).hasSize(9)
      assertThat(statuses).containsExactlyInAnyOrder(
        Cas2AssessmentStatus.AWAITING_DECISION,
        Cas2AssessmentStatus.OFFER_DECLINED,
        Cas2AssessmentStatus.OFFER_ACCEPTED,
        Cas2AssessmentStatus.MORE_INFO_REQUESTED,
        Cas2AssessmentStatus.PLACE_OFFERED,
        Cas2AssessmentStatus.ON_WAITING_LIST,
        Cas2AssessmentStatus.AWAITING_ARRIVAL,
        Cas2AssessmentStatus.CANCELLED,
        Cas2AssessmentStatus.WITHDRAWN,
      )
    }
  }

  @Nested
  inner class StatusById {
    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.model.Cas2ApplicationStatusSeedingTest#provideIdsAndStatuses")
    fun `should return correct status by id`(id: UUID, status: Cas2AssessmentStatus) {
      val result = Cas2ApplicationStatusSeeding.statusById(id)

      assertThat(result).isEqualTo(status)
    }
  }

  @Nested
  inner class StatusDetailsByStatus {
    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.model.Cas2ApplicationStatusSeedingTest#provideStatusesAndStatusDetailsCas2Hdc")
    fun `should return correct statusDetails by status for CAS2 HDC`(status: Cas2AssessmentStatus, statusDetails: List<Cas2AssessmentStatusDetail>?) {
      val result = Cas2ApplicationStatusSeeding.statusDetailsByStatus(
        status = status,
        service = ServiceName.cas2,
      )

      assertThat(result?.sorted()).isEqualTo(statusDetails?.sorted())
      assertThat(result?.size).isEqualTo(statusDetails?.size)
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.model.Cas2ApplicationStatusSeedingTest#provideStatusesAndStatusDetailsCas2v2Bail")
    fun `should return correct statusDetails by status for CAS2 V2 (Bail)`(status: Cas2AssessmentStatus, statusDetails: List<Cas2AssessmentStatusDetail>?) {
      val result = Cas2ApplicationStatusSeeding.statusDetailsByStatus(
        status = status,
        service = ServiceName.cas2v2,
      )

      assertThat(result?.sorted()).isEqualTo(statusDetails?.sorted())
      assertThat(result?.size).isEqualTo(statusDetails?.size)
    }
  }

  private companion object {
    @JvmStatic
    fun provideStatusesAndStatusDetailsCas2Hdc(): List<Arguments> = listOf(
      Arguments.of(
        Cas2AssessmentStatus.AWAITING_DECISION,
        null,
      ),
      Arguments.of(
        Cas2AssessmentStatus.OFFER_DECLINED,
        listOf(
          Cas2AssessmentStatusDetail.APPLICANT_BAILED_TO_NON_CAS2_ACCOMMODATION_OFFER_DECLINED,
          Cas2AssessmentStatusDetail.AREA_UNSUITABLE,
          Cas2AssessmentStatusDetail.CHANGE_OF_CIRCUMSTANCES_OFFER_DECLINED,
          Cas2AssessmentStatusDetail.NO_RESPONSE,
          Cas2AssessmentStatusDetail.OFFER_WITHDRAWN_BY_NACRO,
          Cas2AssessmentStatusDetail.PROPERTY_UNSUITABLE,
          Cas2AssessmentStatusDetail.REHOUSED_BY_ANOTHER_LANDLORD,
          Cas2AssessmentStatusDetail.WITHDRAWN_BY_REFERRER,
          Cas2AssessmentStatusDetail.APPLICANT_UNABLE_TO_AFFORD_RENT,
          Cas2AssessmentStatusDetail.APPLICANT_REMANDED_IN_CUSTODY_OFFER_DECLINED,
        ),
      ),
      Arguments.of(Cas2AssessmentStatus.OFFER_ACCEPTED, null),
      Arguments.of(
        Cas2AssessmentStatus.MORE_INFO_REQUESTED,
        listOf(
          Cas2AssessmentStatusDetail.APPLICANT_DETAILS,
          Cas2AssessmentStatusDetail.CONCERNS_TO_OTHERS,
          Cas2AssessmentStatusDetail.CONCERNS_TO_THE_APPLICANT,
          Cas2AssessmentStatusDetail.CURRENT_ALLEGED_OFFENCES,
          Cas2AssessmentStatusDetail.CURRENT_OFFENCES,
          Cas2AssessmentStatusDetail.EXCLUSION_ZONES_AND_AREAS,
          Cas2AssessmentStatusDetail.FUNDING_AND_ID,
          Cas2AssessmentStatusDetail.HDC_AND_CPP,
          Cas2AssessmentStatusDetail.HEALTH_NEEDS,
          Cas2AssessmentStatusDetail.OFFENDING_HISTORY,
          Cas2AssessmentStatusDetail.OTHER_MORE_INFO_REQUESTED,
          Cas2AssessmentStatusDetail.PERSONAL_INFORMATION,
          Cas2AssessmentStatusDetail.PREVIOUS_UNSPENT_CONVICTIONS,
          Cas2AssessmentStatusDetail.PROBATION_AND_OASYS,
          Cas2AssessmentStatusDetail.RISK_OF_SERIOUS_HARM,
          Cas2AssessmentStatusDetail.RISK_TO_SELF,
        ),
      ),
      Arguments.of(Cas2AssessmentStatus.PLACE_OFFERED, null),
      Arguments.of(Cas2AssessmentStatus.ON_WAITING_LIST, null),
      Arguments.of(Cas2AssessmentStatus.AWAITING_ARRIVAL, null),
      Arguments.of(
        Cas2AssessmentStatus.CANCELLED,
        listOf(
          Cas2AssessmentStatusDetail.ASSESSED_AS_HIGH_RISK,
          Cas2AssessmentStatusDetail.CREATED_IN_ERROR,
          Cas2AssessmentStatusDetail.HDC_NOT_ELIGIBLE,
          Cas2AssessmentStatusDetail.INCOMPLETE_REFERRAL,
          Cas2AssessmentStatusDetail.NACRO_ASSESSED_AS_HIGH_RISK,
          Cas2AssessmentStatusDetail.NO_ADAPTED_PROPERTY_AVAILABLE,
          Cas2AssessmentStatusDetail.NO_FEMALE_PROPERTY_AVAILABLE,
          Cas2AssessmentStatusDetail.NO_PROPERTY_AVAILABLE,
          Cas2AssessmentStatusDetail.NO_RECOURSE_TO_PUBLIC_FUNDS,
          Cas2AssessmentStatusDetail.NO_SUITABLE_PROPERTY_AVAILABLE,
          Cas2AssessmentStatusDetail.NOT_ELIGIBLE,
          Cas2AssessmentStatusDetail.PERSON_TRANSFERRED_TO_ANOTHER_PRISON,
        ),
      ),
      Arguments.of(
        Cas2AssessmentStatus.WITHDRAWN,
        listOf(
          Cas2AssessmentStatusDetail.APPLICANT_BAILED_TO_NON_CAS2_ACCOMMODATION_WITHDRAWN,
          Cas2AssessmentStatusDetail.APPLICANT_REMANDED_IN_CUSTODY_WITHDRAWN,
          Cas2AssessmentStatusDetail.CHANGE_OF_CIRCUMSTANCES_WITHDRAWN,
          Cas2AssessmentStatusDetail.GOVERNOR_CHOSEN_ALTERNATIVE,
          Cas2AssessmentStatusDetail.GOVERNOR_DECIDED_UNSUITABLE,
          Cas2AssessmentStatusDetail.GOVERNOR_OTHER,
          Cas2AssessmentStatusDetail.HDC_NO_LONGER_ELIGIBLE,
          Cas2AssessmentStatusDetail.PERSON_TRANSFERRED_TO_ANOTHER_PRISON_WITHDRAWAL,
          Cas2AssessmentStatusDetail.WITHDREW_OR_DECLINED_OFFER,
          Cas2AssessmentStatusDetail.SENTENCING_HEARING_BOOKED,
          Cas2AssessmentStatusDetail.APPLICANT_TRANSFERRED_TO_ANOTHER_PRISON_WITHDRAWAL,
          Cas2AssessmentStatusDetail.APPLICANT_GIVEN_COMMUNITY_SENTENCE,
          Cas2AssessmentStatusDetail.APPLICANT_GIVEN_CUSTODIAL_SENTENCE,
          Cas2AssessmentStatusDetail.OTHER_SENTENCE,
        ),
      ),
    )

    @JvmStatic
    fun provideStatusesAndStatusDetailsCas2v2Bail(): List<Arguments> = listOf(
      Arguments.of(
        Cas2AssessmentStatus.AWAITING_DECISION,
        null,
      ),
      Arguments.of(
        Cas2AssessmentStatus.OFFER_DECLINED,
        listOf(
          Cas2AssessmentStatusDetail.APPLICANT_BAILED_TO_NON_CAS2_ACCOMMODATION_OFFER_DECLINED,
          Cas2AssessmentStatusDetail.APPLICANT_REMANDED_IN_CUSTODY_OFFER_DECLINED,
          Cas2AssessmentStatusDetail.APPLICANT_UNABLE_TO_AFFORD_RENT,
          Cas2AssessmentStatusDetail.AREA_UNSUITABLE,
          Cas2AssessmentStatusDetail.CHANGE_OF_CIRCUMSTANCES_OFFER_DECLINED,
          Cas2AssessmentStatusDetail.NO_RESPONSE,
          Cas2AssessmentStatusDetail.OFFER_WITHDRAWN_BY_NACRO,
          Cas2AssessmentStatusDetail.PROPERTY_UNSUITABLE,
          Cas2AssessmentStatusDetail.WITHDRAWN_BY_REFERRER,
        ),
      ),
      Arguments.of(Cas2AssessmentStatus.OFFER_ACCEPTED, null),
      Arguments.of(
        Cas2AssessmentStatus.MORE_INFO_REQUESTED,
        listOf(
          Cas2AssessmentStatusDetail.APPLICANT_DETAILS,
          Cas2AssessmentStatusDetail.CONCERNS_TO_OTHERS,
          Cas2AssessmentStatusDetail.CONCERNS_TO_THE_APPLICANT,
          Cas2AssessmentStatusDetail.CURRENT_ALLEGED_OFFENCES,
          Cas2AssessmentStatusDetail.EXCLUSION_ZONES_AND_AREAS,
          Cas2AssessmentStatusDetail.FUNDING_AND_ID,
          Cas2AssessmentStatusDetail.HEALTH_NEEDS,
          Cas2AssessmentStatusDetail.OTHER_MORE_INFO_REQUESTED,
          Cas2AssessmentStatusDetail.PREVIOUS_UNSPENT_CONVICTIONS,
          Cas2AssessmentStatusDetail.PROBATION_AND_OASYS,
        ),
      ),
      Arguments.of(Cas2AssessmentStatus.PLACE_OFFERED, null),
      Arguments.of(Cas2AssessmentStatus.ON_WAITING_LIST, null),
      Arguments.of(Cas2AssessmentStatus.AWAITING_ARRIVAL, null),
      Arguments.of(
        Cas2AssessmentStatus.CANCELLED,
        listOf(
          Cas2AssessmentStatusDetail.CREATED_IN_ERROR,
          Cas2AssessmentStatusDetail.INCOMPLETE_REFERRAL,
          Cas2AssessmentStatusDetail.NACRO_ASSESSED_AS_HIGH_RISK,
          Cas2AssessmentStatusDetail.NO_ADAPTED_PROPERTY_AVAILABLE,
          Cas2AssessmentStatusDetail.NO_FEMALE_PROPERTY_AVAILABLE,
          Cas2AssessmentStatusDetail.NO_PROPERTY_AVAILABLE,
          Cas2AssessmentStatusDetail.NO_RECOURSE_TO_PUBLIC_FUNDS,
          Cas2AssessmentStatusDetail.NO_SUITABLE_PROPERTY_AVAILABLE,
          Cas2AssessmentStatusDetail.NOT_ELIGIBLE,
        ),
      ),
      Arguments.of(
        Cas2AssessmentStatus.WITHDRAWN,
        listOf(
          Cas2AssessmentStatusDetail.APPLICANT_BAILED_TO_NON_CAS2_ACCOMMODATION_WITHDRAWN,
          Cas2AssessmentStatusDetail.APPLICANT_REMANDED_IN_CUSTODY_WITHDRAWN,
          Cas2AssessmentStatusDetail.CHANGE_OF_CIRCUMSTANCES_WITHDRAWN,
          Cas2AssessmentStatusDetail.WITHDREW_OR_DECLINED_OFFER,
          Cas2AssessmentStatusDetail.PERSON_TRANSFERRED_TO_ANOTHER_PRISON_WITHDRAWAL,
          Cas2AssessmentStatusDetail.APPLICANT_TRANSFERRED_TO_ANOTHER_PRISON_WITHDRAWAL,
          Cas2AssessmentStatusDetail.SENTENCING_HEARING_BOOKED,
          Cas2AssessmentStatusDetail.APPLICANT_GIVEN_COMMUNITY_SENTENCE,
          Cas2AssessmentStatusDetail.APPLICANT_GIVEN_CUSTODIAL_SENTENCE,
          Cas2AssessmentStatusDetail.OTHER_SENTENCE,
        ),
      ),
    )

    @JvmStatic
    fun provideIdsAndStatuses(): List<Arguments> = listOf(
      Arguments.of(UUID.fromString("ba4d8432-250b-4ab9-81ec-7eb4b16e5dd1"), Cas2AssessmentStatus.AWAITING_DECISION),
      Arguments.of(UUID.fromString("9a381bc6-22d3-41d6-804d-4e49f428c1de"), Cas2AssessmentStatus.OFFER_DECLINED),
      Arguments.of(UUID.fromString("fe254d88-ce1d-4cd8-8bd6-88de88f39019"), Cas2AssessmentStatus.OFFER_ACCEPTED),
      Arguments.of(UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905"), Cas2AssessmentStatus.MORE_INFO_REQUESTED),
      Arguments.of(UUID.fromString("176bbda0-0766-4d77-8d56-18ed8f9a4ef2"), Cas2AssessmentStatus.PLACE_OFFERED),
      Arguments.of(UUID.fromString("a919097d-b324-471c-9834-756f255e87ea"), Cas2AssessmentStatus.ON_WAITING_LIST),
      Arguments.of(UUID.fromString("89458555-3219-44a2-9584-c4f715d6b565"), Cas2AssessmentStatus.AWAITING_ARRIVAL),
      Arguments.of(UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"), Cas2AssessmentStatus.CANCELLED),
      Arguments.of(UUID.fromString("004e2419-9614-4c1e-a207-a8418009f23d"), Cas2AssessmentStatus.WITHDRAWN),
    )
  }
}
