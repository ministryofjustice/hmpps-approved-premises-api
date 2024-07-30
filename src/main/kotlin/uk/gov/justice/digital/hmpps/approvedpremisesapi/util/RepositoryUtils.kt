package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport
import org.springframework.stereotype.Component

inline fun <reified T, ID> JpaRepository<T, ID>.findAllByIdOrdered(ids: List<ID>): List<T> {
  return findAllByIdOrdered(ids, T::class.java)
}

fun <T, ID> JpaRepository<T, ID>.findAllByIdOrdered(ids: List<ID>, cls: Class<T>): List<T> {
  if (!RepositoryUtilHelper.instanceExists()) {
    return this.findAllById(ids)
  }

  val entityInformation = JpaEntityInformationSupport.getEntityInformation(cls, RepositoryUtilHelper.getEntityManager())

  val resultsUnordered = this.findAllById(ids)

  val resultsMap = resultsUnordered.associateBy {
    @Suppress("UNCHECKED_CAST")
    entityInformation.getId(it!!)!! as ID
  }

  return ids.map { resultsMap[it]!! }
}

@Component
private class RepositoryUtilHelper(
  private val entityManagerFactory: EntityManagerFactory,
) {
  @PostConstruct
  @Suppress("UNUSED", "detekt:UnusedPrivateMember")
  private fun postConstruct() {
    instance = this
  }

  companion object {
    private lateinit var instance: RepositoryUtilHelper

    fun instanceExists(): Boolean {
      val result = ::instance.isInitialized

      if (!result) {
        LoggerFactory
          .getLogger(RepositoryUtilHelper::class.java)
          .warn("No RepositoryUtilHelper instance has been initialised by the Spring context. Repository utility methods may not work correctly.")
      }

      return result
    }

    fun getEntityManager(): EntityManager = instance.entityManagerFactory.createEntityManager()
  }
}
