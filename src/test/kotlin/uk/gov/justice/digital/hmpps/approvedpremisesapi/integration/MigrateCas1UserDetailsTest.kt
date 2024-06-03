package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import java.time.OffsetDateTime

class MigrateCas1UserDetailsTest : MigrationJobTestBase() {

  @Test
  fun `Ignore unsubmitted application`() {
    givenAUser { userEntity, _ ->
      givenAnOffender { offenderDetails, _ ->
        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
          withSubmittedAt(null)
        }

        migrationJobService.runMigrationJob(MigrationJobType.cas1UserDetails)

        val updatedApplication = approvedPremisesApplicationRepository.findById(application.id).get()
        assertThat(updatedApplication.applicantUserDetails).isNull()
        assertThat(updatedApplication.caseManagerIsNotApplicant).isNull()
        assertThat(updatedApplication.caseManagerUserDetails).isNull()
      }
    }
  }

  @Test
  fun `Populate applicant details, no caseManagementResponsibility field defined`() {
    givenAUser(
      staffUserDetailsConfigBlock = {
        withForenames("user entity")
        withSurname("name")
        withEmail("user entity email")
        withTelephoneNumber("user entity phone")
      },
    ) { userEntity, _ ->
      givenAnOffender { offenderDetails, _ ->

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
          withSubmittedAt(OffsetDateTime.now())
          withData(
            """
            {
                "basic-information": {
                    "confirm-your-details": {
                    }
                }
            }
            """.trimIndent(),
          )
        }

        migrationJobService.runMigrationJob(MigrationJobType.cas1UserDetails)

        val updatedApplication = approvedPremisesApplicationRepository.findById(application.id).get()
        assertThat(updatedApplication.applicantUserDetails!!.name).isEqualTo("user entity name")
        assertThat(updatedApplication.applicantUserDetails!!.email).isEqualTo("user entity email")
        assertThat(updatedApplication.applicantUserDetails!!.telephoneNumber).isEqualTo("user entity phone")
        assertThat(updatedApplication.caseManagerIsNotApplicant).isFalse()
        assertThat(updatedApplication.caseManagerUserDetails).isNull()
      }
    }
  }

  @Test
  fun `Populate applicant details, no case manager`() {
    givenAUser(
      staffUserDetailsConfigBlock = {
        withForenames("user entity")
        withSurname("name")
        withEmail("user entity email")
        withTelephoneNumber("user entity phone")
      },
    ) { userEntity, _ ->
      givenAnOffender { offenderDetails, _ ->

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
          withSubmittedAt(OffsetDateTime.now())
          withData(
            """
            {
                "basic-information": {
                    "confirm-your-details": {
                      "caseManagementResponsibility": "yes"
                    }
                }
            }
            """.trimIndent(),
          )
        }

        migrationJobService.runMigrationJob(MigrationJobType.cas1UserDetails)

        val updatedApplication = approvedPremisesApplicationRepository.findById(application.id).get()
        assertThat(updatedApplication.applicantUserDetails!!.name).isEqualTo("user entity name")
        assertThat(updatedApplication.applicantUserDetails!!.email).isEqualTo("user entity email")
        assertThat(updatedApplication.applicantUserDetails!!.telephoneNumber).isEqualTo("user entity phone")
        assertThat(updatedApplication.caseManagerIsNotApplicant).isFalse()
        assertThat(updatedApplication.caseManagerUserDetails).isNull()
      }
    }
  }

  @Test
  fun `Populate overridden applicant details, no case manager`() {
    givenAUser(
      staffUserDetailsConfigBlock = {
        withForenames("user entity")
        withSurname("name")
        withEmail("user entity email")
        withTelephoneNumber("user entity phone")
      },
    ) { userEntity, _ ->
      givenAnOffender { offenderDetails, _ ->

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
          withSubmittedAt(OffsetDateTime.now())
          withData(
            """
            {
                "basic-information": {
                    "confirm-your-details": {
                      "detailsToUpdate": [
                          "name",
                          "emailAddress",
                          "phoneNumber"
                      ],
                      "emailAddress": "overridden email",
                      "name": "overridden name",
                      "phoneNumber": "overridden phone",                    
                      "caseManagementResponsibility": "yes"
                    }
                }
            }
            """.trimIndent(),
          )
        }

        migrationJobService.runMigrationJob(MigrationJobType.cas1UserDetails)

        val updatedApplication = approvedPremisesApplicationRepository.findById(application.id).get()
        assertThat(updatedApplication.applicantUserDetails!!.name).isEqualTo("overridden name")
        assertThat(updatedApplication.applicantUserDetails!!.email).isEqualTo("overridden email")
        assertThat(updatedApplication.applicantUserDetails!!.telephoneNumber).isEqualTo("overridden phone")
        assertThat(updatedApplication.caseManagerIsNotApplicant).isFalse()
        assertThat(updatedApplication.caseManagerUserDetails).isNull()
      }
    }
  }

  @Test
  fun `Populate some overridden applicant details, no case manager defined`() {
    givenAUser(
      staffUserDetailsConfigBlock = {
        withForenames("user entity")
        withSurname("name")
        withEmail("user entity email")
        withTelephoneNumber("user entity phone")
      },
    ) { userEntity, _ ->
      givenAnOffender { offenderDetails, _ ->

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
          withSubmittedAt(OffsetDateTime.now())
          withData(
            """
            {
                "basic-information": {
                    "confirm-your-details": {
                      "detailsToUpdate": [
                          "emailAddress",
                          "phoneNumber"
                      ],
                      "emailAddress": "overridden email",
                      "name": "overridden name",
                      "phoneNumber": "overridden phone",                    
                      "caseManagementResponsibility": "yes"
                    }
                }
            }
            """.trimIndent(),
          )
        }

        migrationJobService.runMigrationJob(MigrationJobType.cas1UserDetails)

        val updatedApplication = approvedPremisesApplicationRepository.findById(application.id).get()
        assertThat(updatedApplication.applicantUserDetails!!.name).isEqualTo("user entity name")
        assertThat(updatedApplication.applicantUserDetails!!.email).isEqualTo("overridden email")
        assertThat(updatedApplication.applicantUserDetails!!.telephoneNumber).isEqualTo("overridden phone")
        assertThat(updatedApplication.caseManagerIsNotApplicant).isFalse()
        assertThat(updatedApplication.caseManagerUserDetails).isNull()
      }
    }
  }

  @Test
  fun `Overridden values defined but not mentioned in details to update - take from user entity`() {
    givenAUser(
      staffUserDetailsConfigBlock = {
        withForenames("user entity")
        withSurname("name")
        withEmail("user entity email")
        withTelephoneNumber("user entity phone")
      },
    ) { userEntity, _ ->
      givenAnOffender { offenderDetails, _ ->

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
          withSubmittedAt(OffsetDateTime.now())
          withData(
            """
            {
                "basic-information": {
                    "confirm-your-details": {
                      "detailsToUpdate": [ ],
                      "emailAddress": "overridden email",
                      "name": "overridden name",
                      "phoneNumber": "overridden phone",                    
                      "caseManagementResponsibility": "yes"
                    }
                }
            }
            """.trimIndent(),
          )
        }

        migrationJobService.runMigrationJob(MigrationJobType.cas1UserDetails)

        val updatedApplication = approvedPremisesApplicationRepository.findById(application.id).get()
        assertThat(updatedApplication.applicantUserDetails!!.name).isEqualTo("user entity name")
        assertThat(updatedApplication.applicantUserDetails!!.email).isEqualTo("user entity email")
        assertThat(updatedApplication.applicantUserDetails!!.telephoneNumber).isEqualTo("user entity phone")
        assertThat(updatedApplication.caseManagerIsNotApplicant).isFalse()
        assertThat(updatedApplication.caseManagerUserDetails).isNull()
      }
    }
  }

  @Test
  fun `Populate case manager when defined`() {
    givenAUser(
      staffUserDetailsConfigBlock = {
        withForenames("user entity")
        withSurname("name")
        withEmail("user entity email")
        withTelephoneNumber("user entity phone")
      },
    ) { userEntity, _ ->
      givenAnOffender { offenderDetails, _ ->

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
          withSubmittedAt(OffsetDateTime.now())
          withData(
            """
            {
                "basic-information": {
                    "confirm-your-details": {                 
                      "caseManagementResponsibility": "no"
                    },
                    "case-manager-information": {
                        "name": "case man name",
                        "emailAddress": "case man email",
                        "phoneNumber": "case man phone"
                    }
                }
            }
            """.trimIndent(),
          )
        }

        migrationJobService.runMigrationJob(MigrationJobType.cas1UserDetails)

        val updatedApplication = approvedPremisesApplicationRepository.findById(application.id).get()
        assertThat(updatedApplication.applicantUserDetails!!.name).isEqualTo("user entity name")
        assertThat(updatedApplication.applicantUserDetails!!.email).isEqualTo("user entity email")
        assertThat(updatedApplication.applicantUserDetails!!.telephoneNumber).isEqualTo("user entity phone")
        assertThat(updatedApplication.caseManagerIsNotApplicant).isTrue()
        assertThat(updatedApplication.caseManagerUserDetails!!.name).isEqualTo("case man name")
        assertThat(updatedApplication.caseManagerUserDetails!!.email).isEqualTo("case man email")
        assertThat(updatedApplication.caseManagerUserDetails!!.telephoneNumber).isEqualTo("case man phone")
      }
    }
  }

  @Test
  fun `Use paging`() {
    givenAUser(
      staffUserDetailsConfigBlock = {
        withForenames("user entity")
        withSurname("name")
        withEmail("user entity email")
        withTelephoneNumber("user entity phone")
      },
    ) { userEntity, _ ->
      givenAnOffender { _, _ ->

        val applications = approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(amount = 5) {
          withCreatedByUser(userEntity)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
          withSubmittedAt(OffsetDateTime.now())
          withData(
            """
            {
                "basic-information": {
                    "confirm-your-details": {
                      "detailsToUpdate": [
                          "name",
                          "emailAddress",
                          "phoneNumber"
                      ],
                      "emailAddress": "overridden email",
                      "name": "overridden name",
                      "phoneNumber": "overridden phone",                    
                      "caseManagementResponsibility": "yes"
                    }
                }
            }
            """.trimIndent(),
          )
        }

        migrationJobService.runMigrationJob(MigrationJobType.cas1UserDetails, pageSize = 2)

        applications.forEach { application ->
          val updatedApplication = approvedPremisesApplicationRepository.findById(application.id).get()
          assertThat(updatedApplication.applicantUserDetails!!.name).isEqualTo("overridden name")
          assertThat(updatedApplication.applicantUserDetails!!.email).isEqualTo("overridden email")
          assertThat(updatedApplication.applicantUserDetails!!.telephoneNumber).isEqualTo("overridden phone")
          assertThat(updatedApplication.caseManagerIsNotApplicant).isFalse()
          assertThat(updatedApplication.caseManagerUserDetails).isNull()
        }
      }
    }
  }
}
