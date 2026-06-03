package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import java.time.OffsetDateTime

fun IntegrationTestBase.givenAnUnsubmittedCas2HdcApplication(
  createdAt: OffsetDateTime = OffsetDateTime.now(),
) = cas2ApplicationEntityFactory.produceAndPersist {
  withCreatedByUser(
    cas2UserEntityFactory.produceAndPersist {
      withUsername("NOMIS_USER_2")
      withServiceOrigin(Cas2ServiceOrigin.HDC)
    },
  )
  withCrn("CRN_1")
  withNomsNumber("NOMS_1")
  withCreatedAt(createdAt)
  withData("{}")
  withSubmittedAt(null)
}

fun IntegrationTestBase.givenAnUnsubmittedCas2Application(
  createdBy: Cas2UserEntity? = null,
  crn: String = "CRN_1",
  noms: String = "NOMS_1",
  createdAt: OffsetDateTime = OffsetDateTime.now(),
) = cas2ApplicationEntityFactory.produceAndPersist {
  withCreatedByUser(
    createdBy ?: cas2UserEntityFactory.produceAndPersist {
      withServiceOrigin(Cas2ServiceOrigin.BAIL)
    },
  )
  withCrn(crn)
  withApplicationOrigin(ApplicationOrigin.prisonBail)
  withNomsNumber(noms)
  withCreatedAt(createdAt)
  withData("{}")
  withSubmittedAt(null)
  withServiceOrigin(Cas2ServiceOrigin.BAIL)
}

fun IntegrationTestBase.givenASubmittedCas2Application(
  createdBy: Cas2UserEntity? = null,
  applicationOrigin: ApplicationOrigin = ApplicationOrigin.courtBail,
  crn: String = "CRN_1",
  nomsNumber: String = "NOMS_1",
  createdAt: OffsetDateTime = OffsetDateTime.now().minusDays(5),
  submittedAt: OffsetDateTime = OffsetDateTime.now(),
) = cas2ApplicationEntityFactory.produceAndPersist {
  withCreatedByUser(
    createdBy ?: cas2UserEntityFactory.produceAndPersist {
      withServiceOrigin(Cas2ServiceOrigin.BAIL)
    },
  )
  withCrn(crn)
  withApplicationOrigin(applicationOrigin)
  withNomsNumber(nomsNumber)
  withCreatedAt(createdAt)
  withData("{}")
  withSubmittedAt(submittedAt)
  withServiceOrigin(Cas2ServiceOrigin.BAIL)
}
