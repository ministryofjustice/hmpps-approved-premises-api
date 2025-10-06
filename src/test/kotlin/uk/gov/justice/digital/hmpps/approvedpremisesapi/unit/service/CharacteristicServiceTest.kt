package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService

class CharacteristicServiceTest {
  private val characteristicRepository = mockk<CharacteristicRepository>()
  private val bedspaceCharacteristicRepository = mockk<Cas3BedspaceCharacteristicRepository>()
  private val premisesCharacteristicRepository = mockk<Cas3PremisesCharacteristicRepository>()
  private val characteristicService = CharacteristicService(characteristicRepository, bedspaceCharacteristicRepository, premisesCharacteristicRepository)

  @Test
  fun `serviceScopeMatches returns false if the characteristic has the wrong service scope`() {
    val characteristicEntityFactory = CharacteristicEntityFactory()
    val roomEntityFactory = RoomEntityFactory()

    val characteristic1 = characteristicEntityFactory
      .withModelScope("*")
      .withServiceScope(ServiceName.approvedPremises.value)
      .produce()

    val characteristic2 = characteristicEntityFactory
      .withModelScope("*")
      .withServiceScope(ServiceName.temporaryAccommodation.value)
      .produce()

    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val localAuthorityArea = LocalAuthorityEntityFactory().produce()

    val room1: RoomEntity = roomEntityFactory
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion { probationRegion }
          .withYieldedLocalAuthorityArea { localAuthorityArea }
          .produce()
      }
      .produce()

    val room2: RoomEntity = roomEntityFactory
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion { probationRegion }
          .withYieldedLocalAuthorityArea { localAuthorityArea }
          .produce()
      }
      .produce()

    assertThat(characteristicService.serviceScopeMatches(characteristic1, room1)).isFalse
    assertThat(characteristicService.serviceScopeMatches(characteristic2, room2)).isFalse
  }

  @Test
  fun `modelScopeMatches returns false if the characteristic has the wrong model scope`() {
    val characteristicEntityFactory = CharacteristicEntityFactory()

    val characteristic1 = characteristicEntityFactory
      .withModelScope("room")
      .withServiceScope("*")
      .produce()

    val characteristic2 = characteristicEntityFactory
      .withModelScope("premises")
      .withServiceScope("*")
      .produce()

    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val localAuthorityArea = LocalAuthorityEntityFactory().produce()

    val premises = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion { probationRegion }
      .withYieldedLocalAuthorityArea { localAuthorityArea }
      .produce()

    val room = RoomEntityFactory()
      .withYieldedPremises { premises }
      .produce()

    assertThat(characteristicService.modelScopeMatches(characteristic1, premises)).isFalse
    assertThat(characteristicService.modelScopeMatches(characteristic2, room)).isFalse
  }

  @Test
  fun `serviceScopeMatches returns true if the service scope is correct`() {
    val characteristicEntityFactory = CharacteristicEntityFactory()
    val roomEntityFactory = RoomEntityFactory()

    val characteristic1 = characteristicEntityFactory
      .withModelScope("*")
      .withServiceScope(ServiceName.approvedPremises.value)
      .produce()

    val characteristic2 = characteristicEntityFactory
      .withModelScope("*")
      .withServiceScope(ServiceName.temporaryAccommodation.value)
      .produce()

    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val localAuthorityArea = LocalAuthorityEntityFactory().produce()

    val premises1 = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion { probationRegion }
      .withYieldedLocalAuthorityArea { localAuthorityArea }
      .produce()

    val premises2 = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion { probationRegion }
      .withYieldedLocalAuthorityArea { localAuthorityArea }
      .produce()

    val room1: RoomEntity = roomEntityFactory
      .withYieldedPremises { premises1 }
      .produce()

    val room2: RoomEntity = roomEntityFactory
      .withYieldedPremises { premises2 }
      .produce()

    assertThat(characteristicService.serviceScopeMatches(characteristic1, room1)).isTrue
    assertThat(characteristicService.serviceScopeMatches(characteristic1, premises1)).isTrue
    assertThat(characteristicService.serviceScopeMatches(characteristic2, room2)).isTrue
    assertThat(characteristicService.serviceScopeMatches(characteristic2, premises2)).isTrue
  }

  @Test
  fun `modelScopeMatches returns true if the model scope is correct`() {
    val characteristicEntityFactory = CharacteristicEntityFactory()
    val roomEntityFactory = RoomEntityFactory()

    val characteristic1 = characteristicEntityFactory
      .withModelScope("room")
      .withServiceScope("*")
      .produce()

    val characteristic2 = characteristicEntityFactory
      .withModelScope("premises")
      .withServiceScope("*")
      .produce()

    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val localAuthorityArea = LocalAuthorityEntityFactory().produce()

    val premises1 = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion { probationRegion }
      .withYieldedLocalAuthorityArea { localAuthorityArea }
      .produce()

    val premises2 = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion { probationRegion }
      .withYieldedLocalAuthorityArea { localAuthorityArea }
      .produce()

    val room: RoomEntity = roomEntityFactory
      .withYieldedPremises { premises1 }
      .produce()

    assertThat(characteristicService.modelScopeMatches(characteristic1, room)).isTrue
    assertThat(characteristicService.modelScopeMatches(characteristic2, premises1)).isTrue
    assertThat(characteristicService.modelScopeMatches(characteristic2, premises2)).isTrue
  }

  @Test
  fun `serviceScopeMatches always matches on wildcard scopes`() {
    val characteristic = CharacteristicEntityFactory()
      .withModelScope("*")
      .withServiceScope("*")
      .produce()

    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val localAuthorityArea = LocalAuthorityEntityFactory().produce()

    val premises1 = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion { probationRegion }
      .withYieldedLocalAuthorityArea { localAuthorityArea }
      .produce()

    val premises2 = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion { probationRegion }
      .withYieldedLocalAuthorityArea { localAuthorityArea }
      .produce()

    val room: RoomEntity = RoomEntityFactory()
      .withYieldedPremises { premises1 }
      .produce()

    assertThat(characteristicService.serviceScopeMatches(characteristic, room)).isTrue
    assertThat(characteristicService.serviceScopeMatches(characteristic, premises1)).isTrue
    assertThat(characteristicService.serviceScopeMatches(characteristic, premises1)).isTrue
    assertThat(characteristicService.serviceScopeMatches(characteristic, premises2)).isTrue
  }

  @Test
  fun `modelScopeMatches always matches on wildcard scopes`() {
    val characteristic = CharacteristicEntityFactory()
      .withModelScope("*")
      .withServiceScope("*")
      .produce()

    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val localAuthorityArea = LocalAuthorityEntityFactory().produce()

    val premises1 = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion { probationRegion }
      .withYieldedLocalAuthorityArea { localAuthorityArea }
      .produce()

    val premises2 = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion { probationRegion }
      .withYieldedLocalAuthorityArea { localAuthorityArea }
      .produce()

    val room: RoomEntity = RoomEntityFactory()
      .withYieldedPremises { premises1 }
      .produce()

    assertThat(characteristicService.modelScopeMatches(characteristic, room)).isTrue
    assertThat(characteristicService.modelScopeMatches(characteristic, premises1)).isTrue
    assertThat(characteristicService.modelScopeMatches(characteristic, premises1)).isTrue
    assertThat(characteristicService.modelScopeMatches(characteristic, premises2)).isTrue
  }
}
