package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import org.springframework.data.jpa.repository.JpaRepository

abstract class PersistedFactory<EntityType : Any, PrimaryKeyType : Any>(private val repository: JpaRepository<EntityType, PrimaryKeyType>) : Factory<EntityType> {
  fun produceAndPersist(): EntityType = repository.saveAndFlush(this.produce())
  fun produceAndPersistMultiple(amount: Int) = (1..amount).map { produceAndPersist() }
}
