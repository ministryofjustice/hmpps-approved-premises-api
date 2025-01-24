package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.testimprovements

import org.springframework.data.jpa.repository.JpaRepository

abstract class ProduceAndPersist<T : Any, ID> : Produce<T> {

  abstract val repository: JpaRepository<T, ID>

  abstract override fun produce(): T

  fun persist(entity: T): T {
    return repository.saveAndFlush(entity)
  }

  fun produceAndPersist(): T {
    return persist(produce())
  }

  fun produceAndPersistMany(amount: Int): List<T> {
    return (1..amount).map { persist(produce()) }
  }

  fun produceAndPersistMany(entity: T, amount: Int): List<T> {
    return (1..amount).map { persist(entity) }
  }
}
