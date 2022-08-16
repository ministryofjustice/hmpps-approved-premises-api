package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import org.springframework.data.jpa.repository.JpaRepository

class PersistedFactory<EntityType : Any, PrimaryKeyType : Any, FactoryType : Factory<EntityType>>(private val factory: FactoryType, private val repository: JpaRepository<EntityType, PrimaryKeyType>) {
  fun configure(block: FactoryType.() -> Unit) = apply { block(factory) }
  fun produceAndPersist(): EntityType = repository.saveAndFlush(factory.produce())
  fun produceAndPersistMultiple(amount: Int) = (1..amount).map { produceAndPersist() }
}
