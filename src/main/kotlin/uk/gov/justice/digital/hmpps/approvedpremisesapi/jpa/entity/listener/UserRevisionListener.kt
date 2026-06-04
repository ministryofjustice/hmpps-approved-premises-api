package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listener

import org.hibernate.envers.RevisionListener
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RevInfo

class UserRevisionListener : RevisionListener {
  override fun newRevision(revisionEntity: Any?) {
    val revisionInfo = revisionEntity as RevInfo
    revisionInfo.username = SecurityContextHolder
      .getContext()
      .authentication
      ?.name ?: "System"
  }
}
