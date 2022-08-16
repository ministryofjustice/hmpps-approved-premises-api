package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import org.springframework.data.jpa.repository.JpaRepository

abstract class PersistedFactory<EntityType : Any, PrimaryKeyType : Any>(private val repository: JpaRepository<EntityType, PrimaryKeyType>?) : Factory<EntityType> {
  constructor() : this(null)

  fun produceAndPersist(): EntityType = repository?.saveAndFlush(this.produce()) ?: throw RuntimeException("Cannot persist because no repository was provided.  If this is a unit test did you mean to call .produce() instead?")
  fun produceAndPersistMultiple(amount: Int) = (1..amount).map { produceAndPersist() }
}
