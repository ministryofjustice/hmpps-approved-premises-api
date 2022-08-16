package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import org.springframework.data.jpa.repository.JpaRepository

class PersistedFactory<EntityType : Any, PrimaryKeyType : Any, FactoryType : Factory<EntityType>>(private val factory: FactoryType, private val repository: JpaRepository<EntityType, PrimaryKeyType>) {
  fun produceAndPersist(): EntityType = repository.saveAndFlush(factory.produce())
  fun produceAndPersist(configuration: FactoryType.() -> Unit): EntityType {
    configuration(factory)
    return repository.saveAndFlush(factory.produce())
  }

  fun produceAndPersistMultiple(amount: Int) = (1..amount).map { produceAndPersist() }
  fun produceAndPersistMultiple(amount: Int, configuration: FactoryType.() -> Unit): List<EntityType> {
    configuration(factory)
    return (1..amount).map { produceAndPersist() }
  }
}
