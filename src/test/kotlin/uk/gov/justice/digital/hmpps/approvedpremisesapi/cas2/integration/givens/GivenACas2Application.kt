package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserType
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
  withServiceOrigin(Cas2ServiceOrigin.HDC)
}

fun IntegrationTestBase.givenAnUnsubmittedCas2Application(
  createdBy: Cas2UserEntity? = null,
  applicationOrigin: ApplicationOrigin = ApplicationOrigin.prisonBail,
  cohort: Cas2Cohort = Cas2Cohort.PRISON_BAIL,
  crn: String = "CRN_1",
  noms: String? = "NOMS_1",
  createdAt: OffsetDateTime = OffsetDateTime.now(),
): Cas2ApplicationEntity {
  val application = cas2ApplicationEntityFactory.produceAndPersist {
    withCreatedByUser(
      createdBy ?: cas2UserEntityFactory.produceAndPersist {
        withServiceOrigin(Cas2ServiceOrigin.BAIL)
      },
    )
    withCrn(crn)
    withApplicationOrigin(applicationOrigin)
    withCohort(cohort)
    withNomsNumber(noms)
    withCreatedAt(createdAt)
    withData("{}")
    withSubmittedAt(null)
    withServiceOrigin(Cas2ServiceOrigin.BAIL)
  }

  return application
}

fun IntegrationTestBase.givenASubmittedCas2Application(
  createdBy: Cas2UserEntity? = null,
  applicationOrigin: ApplicationOrigin = ApplicationOrigin.courtBail,
  cohort: Cas2Cohort = Cas2Cohort.COURT_BAIL,
  crn: String = "CRN_1",
  nomsNumber: String = "NOMS_1",
  createdAt: OffsetDateTime = OffsetDateTime.now().minusDays(5),
  submittedAt: OffsetDateTime = OffsetDateTime.now(),
  latestStatusName: String? = null,
  latestStatusSet: OffsetDateTime = OffsetDateTime.now(),
): Cas2ApplicationEntity {
  val application = cas2ApplicationEntityFactory.produceAndPersist {
    withCreatedByUser(
      createdBy ?: cas2UserEntityFactory.produceAndPersist {
        withServiceOrigin(Cas2ServiceOrigin.BAIL)
      },
    )
    withCrn(crn)
    withApplicationOrigin(applicationOrigin)
    withCohort(cohort)
    withNomsNumber(nomsNumber)
    withCreatedAt(createdAt)
    withData("{}")
    withSubmittedAt(submittedAt)
    withServiceOrigin(Cas2ServiceOrigin.BAIL)
  }

  if (latestStatusName != null) {
    val status = Cas2PersistedApplicationStatusFinder().forName(latestStatusName) ?: error("Could not find status for name $latestStatusName")

    cas2StatusUpdateEntityFactory.produceAndPersist {
      withLabel(status.label)
      withDescription(status.description)
      withStatusId(status.id)
      withApplication(application)
      withCreatedAt(latestStatusSet)
      withAssessor(
        cas2UserEntityFactory.produceAndPersist {
          withUserType(Cas2UserType.EXTERNAL)
          withServiceOrigin(Cas2ServiceOrigin.BAIL)
        },
      )
    }
  }

  return application
}

fun IntegrationTestBase.givenASubmittedCas2HdcApplication(
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
  withServiceOrigin(Cas2ServiceOrigin.HDC)
}
