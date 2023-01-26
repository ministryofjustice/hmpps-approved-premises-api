package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import org.springframework.data.jpa.repository.JpaRepository

class PersistedFactory<EntityType : Any, PrimaryKeyType : Any, FactoryType : Factory<EntityType>>(private val factoryProducer: () -> FactoryType, private val repository: JpaRepository<EntityType, PrimaryKeyType>) {
  fun produceAndPersist(): EntityType = repository.saveAndFlush(factoryProducer().produce())
  fun produceAndPersist(configuration: FactoryType.() -> Unit): EntityType {
    val factory = factoryProducer()
    configuration(factory)
    return repository.saveAndFlush(factory.produce())
  }

  fun produceAndPersistMultiple(amount: Int) = (1..amount).map { produceAndPersist() }
  fun produceAndPersistMultiple(amount: Int, configuration: FactoryType.() -> Unit): List<EntityType> {
    val factory = factoryProducer()
    configuration(factory)
    return (1..amount).map {
      repository.saveAndFlush(factory.produce())
    }
  }
}
