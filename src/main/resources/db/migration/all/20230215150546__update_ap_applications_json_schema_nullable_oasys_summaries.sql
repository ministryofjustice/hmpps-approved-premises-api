UPDATE json_schemas SET "schema" = '{
  "$schema": "https://json-schema.org/draft/2019-09/schema",
  "type": "object",
  "title": "Apply Schema",
  "additionalProperties": false,
  "required": [
    "basic-information",
    "type-of-ap",
    "oasys-import",
    "risk-management-features",
    "prison-information",
    "location-factors",
    "access-and-healthcare",
    "further-considerations",
    "move-on",
    "attach-required-documents",
    "check-your-answers"
  ],
  "properties": {
    "basic-information": {
      "type": "object",
      "properties": {
        "is-exceptional-case": {
          "type": "object",
          "properties": {
            "isExceptionalCase": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            }
          }
        },
        "exception-details": {
          "allOf": [
            {
              "type": "object",
              "properties": {
                "agreementDate-year": {
                  "type": "string"
                },
                "agreementDate-month": {
                  "type": "string"
                },
                "agreementDate-day": {
                  "type": "string"
                }
              }
            },
            {
              "type": "object",
              "properties": {
                "agreementDate-time": {
                  "type": "string"
                }
              }
            },
            {
              "type": "object",
              "properties": {
                "agreementDate": {
                  "type": "string"
                }
              }
            },
            {
              "type": "object",
              "properties": {
                "agreedCaseWithManager": {
                  "enum": [
                    "no",
                    "yes"
                  ],
                  "type": "string"
                },
                "managerName": {
                  "type": "string"
                },
                "agreementSummary": {
                  "type": "string"
                }
              }
            }
          ]
        },
        "sentence-type": {
          "type": "object",
          "properties": {
            "sentenceType": {
              "enum": [
                "bailPlacement",
                "communityOrder",
                "extendedDeterminate",
                "ipp",
                "life",
                "nonStatutory",
                "standardDeterminate"
              ],
              "type": "string"
            }
          }
        },
        "release-type": {
          "type": "object",
          "properties": {
            "releaseType": {
              "enum": [
                "hdc",
                "licence",
                "pss",
                "rotl"
              ],
              "type": "string"
            }
          }
        },
        "situation": {
          "type": "object",
          "properties": {
            "situation": {
              "enum": [
                "bailAssessment",
                "bailSentence",
                "residencyManagement",
                "riskManagement"
              ],
              "type": "string"
            }
          }
        },
        "release-date": {
          "allOf": [
            {
              "type": "object",
              "properties": {
                "releaseDate-year": {
                  "type": "string"
                },
                "releaseDate-month": {
                  "type": "string"
                },
                "releaseDate-day": {
                  "type": "string"
                }
              }
            },
            {
              "type": "object",
              "properties": {
                "releaseDate-time": {
                  "type": "string"
                }
              }
            },
            {
              "type": "object",
              "properties": {
                "releaseDate": {
                  "type": "string"
                }
              }
            },
            {
              "type": "object",
              "properties": {
                "knowReleaseDate": {
                  "enum": [
                    "no",
                    "yes"
                  ],
                  "type": "string"
                }
              }
            }
          ]
        },
        "oral-hearing": {
          "allOf": [
            {
              "type": "object",
              "properties": {
                "oralHearingDate-year": {
                  "type": "string"
                },
                "oralHearingDate-month": {
                  "type": "string"
                },
                "oralHearingDate-day": {
                  "type": "string"
                }
              }
            },
            {
              "type": "object",
              "properties": {
                "oralHearingDate-time": {
                  "type": "string"
                }
              }
            },
            {
              "type": "object",
              "properties": {
                "oralHearingDate": {
                  "type": "string"
                }
              }
            },
            {
              "type": "object",
              "properties": {
                "knowOralHearingDate": {
                  "enum": [
                    "no",
                    "yes"
                  ],
                  "type": "string"
                }
              }
            }
          ]
        },
        "placement-date": {
          "allOf": [
            {
              "type": "object",
              "properties": {
                "startDate-year": {
                  "type": "string"
                },
                "startDate-month": {
                  "type": "string"
                },
                "startDate-day": {
                  "type": "string"
                }
              }
            },
            {
              "type": "object",
              "properties": {
                "startDate-time": {
                  "type": "string"
                }
              }
            },
            {
              "type": "object",
              "properties": {
                "startDate": {
                  "type": "string"
                }
              }
            },
            {
              "type": "object",
              "properties": {
                "startDateSameAsReleaseDate": {
                  "enum": [
                    "no",
                    "yes"
                  ],
                  "type": "string"
                }
              }
            }
          ]
        },
        "placement-purpose": {
          "type": "object",
          "properties": {
            "placementPurposes": {
              "anyOf": [
                {
                  "type": "array",
                  "items": {
                    "enum": [
                      "drugAlcoholMonitoring",
                      "otherReason",
                      "preventContact",
                      "preventSelfHarm",
                      "publicProtection",
                      "readjust"
                    ],
                    "type": "string"
                  }
                },
                {
                  "enum": [
                    "drugAlcoholMonitoring",
                    "otherReason",
                    "preventContact",
                    "preventSelfHarm",
                    "publicProtection",
                    "readjust"
                  ],
                  "type": "string"
                }
              ]
            },
            "otherReason": {
              "type": "string"
            }
          }
        }
      }
    },
    "type-of-ap": {
      "type": "object",
      "properties": {
        "ap-type": {
          "type": "object",
          "properties": {
            "type": {
              "enum": [
                "esap",
                "pipe",
                "standard"
              ],
              "type": "string"
            }
          }
        },
        "pipe-referral": {
          "allOf": [
            {
              "type": "object",
              "properties": {
                "opdPathwayDate-year": {
                  "type": "string"
                },
                "opdPathwayDate-month": {
                  "type": "string"
                },
                "opdPathwayDate-day": {
                  "type": "string"
                }
              }
            },
            {
              "type": "object",
              "properties": {
                "opdPathwayDate-time": {
                  "type": "string"
                }
              }
            },
            {
              "type": "object",
              "properties": {
                "opdPathwayDate": {
                  "type": "string"
                }
              }
            },
            {
              "type": "object",
              "properties": {
                "opdPathway": {
                  "enum": [
                    "no",
                    "yes"
                  ],
                  "type": "string"
                }
              }
            }
          ]
        },
        "pipe-opd-screening": {
          "type": "object",
          "properties": {
            "pipeReferral": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "pipeReferralMoreDetail": {
              "type": "string"
            }
          }
        },
        "esap-placement-screening": {
          "type": "object",
          "properties": {
            "esapReasons": {
              "type": "array",
              "items": {
                "enum": [
                  "cctv",
                  "secreting"
                ],
                "type": "string"
              }
            },
            "esapFactors": {
              "type": "array",
              "items": {
                "enum": [
                  "careAndSeperation",
                  "complexPersonality",
                  "corrupter",
                  "neurodiverse",
                  "nonNsd",
                  "unlock"
                ],
                "type": "string"
              }
            }
          }
        },
        "esap-placement-secreting": {
          "type": "object",
          "properties": {
            "secretingHistory": {
              "type": "array",
              "items": {
                "enum": [
                  "csaLiterature",
                  "drugs",
                  "electronicItems",
                  "fire",
                  "hateCrimeLiterature",
                  "radicalisationLiterature",
                  "weapons"
                ],
                "type": "string"
              }
            },
            "secretingIntelligence": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "secretingIntelligenceDetails": {
              "type": "string"
            },
            "secretingNotes": {
              "type": "string"
            }
          }
        },
        "esap-placement-cctv": {
          "type": "object",
          "properties": {
            "cctvHistory": {
              "type": "array",
              "items": {
                "enum": [
                  "appearance",
                  "communityThreats",
                  "networks",
                  "prisonerAssualt",
                  "staffAssualt",
                  "threatsToLife"
                ],
                "type": "string"
              }
            },
            "cctvIntelligence": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "cctvIntelligenceDetails": {
              "type": "string"
            },
            "cctvNotes": {
              "type": "string"
            }
          }
        }
      }
    },
    "oasys-import": {
      "type": "object",
      "properties": {
        "optional-oasys-sections": {
          "type": "object",
          "properties": {
            "needsLinkedToReoffending": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "section": {
                    "type": "number"
                  },
                  "name": {
                    "type": "string"
                  },
                  "linkedToHarm": {
                    "type": "boolean"
                  },
                  "linkedToReOffending": {
                    "type": "boolean"
                  }
                }
              }
            },
            "otherNeeds": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "section": {
                    "type": "number"
                  },
                  "name": {
                    "type": "string"
                  },
                  "linkedToHarm": {
                    "type": "boolean"
                  },
                  "linkedToReOffending": {
                    "type": "boolean"
                  }
                }
              }
            }
          }
        },
        "rosh-summary": {
          "type": "object",
          "properties": {
            "roshAnswers": {
              "anyOf": [
                {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                {
                  "type": "object"
                }
              ]
            },
            "roshSummaries": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "label": {
                    "type": "string"
                  },
                  "questionNumber": {
                    "type": "string"
                  },
                  "answer": {
					"anyOf": [
						{
							"type": "string"
						},
						{
							"type": "null"
						}
					]
                  }
                }
              }
            }
          }
        },
        "offence-details": {
          "type": "object",
          "properties": {
            "offenceDetailsAnswers": {
              "anyOf": [
                {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                {
                  "type": "object"
                }
              ]
            },
            "offenceDetailsSummaries": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "label": {
                    "type": "string"
                  },
                  "questionNumber": {
                    "type": "string"
                  },
                  "answer": {
					"anyOf": [
						{
							"type": "string"
						},
						{
							"type": "null"
						}
					]
                  }
                }
              }
            }
          }
        },
        "supporting-information": {
          "type": "object",
          "properties": {
            "supportingInformationAnswers": {
              "anyOf": [
                {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                {
                  "type": "object"
                }
              ]
            },
            "supportingInformationSummaries": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "label": {
                    "type": "string"
                  },
                  "sectionNumber": {
                    "type": "number"
                  },
                  "questionNumber": {
                    "type": "string"
                  },
                  "linkedToHarm": {
                    "type": "boolean"
                  },
                  "linkedToReOffending": {
                    "type": "boolean"
                  },
                  "answer": {
					"anyOf": [
						{
							"type": "string"
						},
						{
							"type": "null"
						}
					]
                  }
                }
              }
            }
          }
        },
        "risk-management-plan": {
          "type": "object",
          "properties": {
            "riskManagementAnswers": {
              "anyOf": [
                {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                {
                  "type": "object"
                }
              ]
            },
            "riskManagementSummaries": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "label": {
                    "type": "string"
                  },
                  "questionNumber": {
                    "type": "string"
                  },
                  "answer": {
					"anyOf": [
						{
							"type": "string"
						},
						{
							"type": "null"
						}
					]
                  }
                }
              }
            }
          }
        },
        "risk-to-self": {
          "type": "object",
          "properties": {
            "riskToSelfAnswers": {
              "anyOf": [
                {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                {
                  "type": "object"
                }
              ]
            },
            "riskToSelfSummaries": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "label": {
                    "type": "string"
                  },
                  "questionNumber": {
                    "type": "string"
                  },
                  "answer": {
					"anyOf": [
						{
							"type": "string"
						},
						{
							"type": "null"
						}
					]
                  }
                }
              }
            }
          }
        }
      }
    },
    "risk-management-features": {
      "type": "object",
      "properties": {
        "risk-management-features": {
          "type": "object",
          "properties": {
            "manageRiskDetails": {
              "type": "string"
            },
            "additionalFeaturesDetails": {
              "type": "string"
            }
          }
        },
        "convicted-offences": {
          "type": "object",
          "properties": {
            "response": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            }
          }
        },
        "type-of-convicted-offence": {
          "type": "object",
          "properties": {
            "offenceConvictions": {
              "type": "array",
              "items": {
                "enum": [
                  "arson",
                  "childNonSexualOffence",
                  "hateCrimes",
                  "sexualOffence"
                ],
                "type": "string"
              }
            }
          }
        },
        "date-of-offence": {
          "type": "object",
          "properties": {
            "arsonOffence": {
              "anyOf": [
                {
                  "type": "array",
                  "items": {
                    "enum": [
                      "current",
                      "previous"
                    ],
                    "type": "string"
                  }
                },
                {
                  "enum": [
                    "current",
                    "previous"
                  ],
                  "type": "string"
                }
              ]
            },
            "hateCrime": {
              "anyOf": [
                {
                  "type": "array",
                  "items": {
                    "enum": [
                      "current",
                      "previous"
                    ],
                    "type": "string"
                  }
                },
                {
                  "enum": [
                    "current",
                    "previous"
                  ],
                  "type": "string"
                }
              ]
            },
            "inPersonSexualOffence": {
              "anyOf": [
                {
                  "type": "array",
                  "items": {
                    "enum": [
                      "current",
                      "previous"
                    ],
                    "type": "string"
                  }
                },
                {
                  "enum": [
                    "current",
                    "previous"
                  ],
                  "type": "string"
                }
              ]
            },
            "onlineSexualOffence": {
              "anyOf": [
                {
                  "type": "array",
                  "items": {
                    "enum": [
                      "current",
                      "previous"
                    ],
                    "type": "string"
                  }
                },
                {
                  "enum": [
                    "current",
                    "previous"
                  ],
                  "type": "string"
                }
              ]
            }
          }
        },
        "rehabilitative-interventions": {
          "type": "object",
          "properties": {
            "rehabilitativeInterventions": {
              "type": "array",
              "items": {
                "enum": [
                  "abuse",
                  "accommodation",
                  "attitudesAndBehaviour",
                  "childrenAndFamilies",
                  "drugsAndAlcohol",
                  "educationTrainingAndEmployment",
                  "financeBenefitsAndDebt",
                  "health",
                  "other"
                ],
                "type": "string"
              }
            },
            "otherIntervention": {
              "type": "string"
            }
          }
        }
      }
    },
    "prison-information": {
      "type": "object",
      "properties": {
        "case-notes": {
          "type": "object",
          "properties": {
            "caseNoteIds": {
              "type": "array",
              "items": {
                "type": "string"
              }
            },
            "selectedCaseNotes": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "id": {
                    "type": "string"
                  },
                  "sensitive": {
                    "type": "boolean"
                  },
                  "createdAt": {
                    "type": "string"
                  },
                  "occurredAt": {
                    "type": "string"
                  },
                  "authorName": {
                    "type": "string"
                  },
                  "type": {
                    "type": "string"
                  },
                  "subType": {
                    "type": "string"
                  },
                  "note": {
                    "type": "string"
                  }
                }
              }
            },
            "moreDetail": {
              "type": "string"
            },
            "adjudications": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "id": {
                    "type": "number"
                  },
                  "reportedAt": {
                    "type": "string"
                  },
                  "establishment": {
                    "type": "string"
                  },
                  "offenceDescription": {
                    "type": "string"
                  },
                  "hearingHeld": {
                    "type": "boolean"
                  },
                  "finding": {
                    "type": "string"
                  }
                }
              }
            },
            "acctAlerts": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "alertId": {
                    "type": "number"
                  },
                  "comment": {
                    "type": "string"
                  },
                  "dateCreated": {
                    "type": "string"
                  },
                  "dateExpires": {
                    "type": "string"
                  },
                  "expired": {
                    "type": "boolean"
                  },
                  "active": {
                    "type": "boolean"
                  }
                }
              }
            }
          }
        }
      }
    },
    "location-factors": {
      "type": "object",
      "properties": {
        "describe-location-factors": {
          "type": "object",
          "properties": {
            "postcodeArea": {
              "type": "string"
            },
            "positiveFactors": {
              "type": "string"
            },
            "restrictions": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "restrictionDetail": {
              "type": "string"
            },
            "alternativeRadiusAccepted": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "alternativeRadius": {
              "enum": [
                "100",
                "110",
                "120",
                "130",
                "140",
                "150",
                "60",
                "70",
                "80",
                "90"
              ],
              "type": "string"
            }
          }
        }
      }
    },
    "access-and-healthcare": {
      "type": "object",
      "properties": {
        "access-needs": {
          "type": "object",
          "properties": {
            "additionalNeeds": {
              "anyOf": [
                {
                  "type": "array",
                  "items": {
                    "enum": [
                      "hearingImpairment",
                      "learningDisability",
                      "mobility",
                      "neurodivergentConditions",
                      "none",
                      "other",
                      "visualImpairment"
                    ],
                    "type": "string"
                  }
                },
                {
                  "enum": [
                    "hearingImpairment",
                    "learningDisability",
                    "mobility",
                    "neurodivergentConditions",
                    "none",
                    "other",
                    "visualImpairment"
                  ],
                  "type": "string"
                }
              ]
            },
            "religiousOrCulturalNeeds": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "religiousOrCulturalNeedsDetails": {
              "type": "string"
            },
            "careActAssessmentCompleted": {
              "enum": [
                "iDontKnow",
                "no",
                "yes"
              ],
              "type": "string"
            },
            "needsInterpreter": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "interpreterLanguage": {
              "type": "string"
            }
          }
        },
        "access-needs-mobility": {
          "type": "object",
          "properties": {
            "needsWheelchair": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "mobilityNeeds": {
              "type": "string"
            },
            "visualImpairment": {
              "type": "string"
            }
          }
        },
        "covid": {
          "type": "object",
          "properties": {
            "fullyVaccinated": {
              "enum": [
                "iDontKnow",
                "no",
                "yes"
              ],
              "type": "string"
            },
            "highRisk": {
              "enum": [
                "iDontKnow",
                "no",
                "yes"
              ],
              "type": "string"
            },
            "additionalCovidInfo": {
              "type": "string"
            }
          }
        }
      }
    },
    "further-considerations": {
      "type": "object",
      "properties": {
        "room-sharing": {
          "type": "object",
          "properties": {
            "riskToStaff": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "riskToStaffDetail": {
              "type": "string"
            },
            "riskToOthers": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "riskToOthersDetail": {
              "type": "string"
            },
            "sharingConcerns": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "sharingConcernsDetail": {
              "type": "string"
            },
            "traumaConcerns": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "traumaConcernsDetail": {
              "type": "string"
            },
            "sharingBenefits": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "sharingBenefitsDetail": {
              "type": "string"
            }
          }
        },
        "vulnerability": {
          "type": "object",
          "properties": {
            "exploitable": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "exploitableDetail": {
              "type": "string"
            },
            "exploitOthers": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "exploitOthersDetail": {
              "type": "string"
            }
          }
        },
        "previous-placements": {
          "type": "object",
          "properties": {
            "previousPlacement": {
              "enum": [
                "iDontKnow",
                "no",
                "yes"
              ],
              "type": "string"
            },
            "previousPlacementDetail": {
              "type": "string"
            }
          }
        },
        "complex-case-board": {
          "type": "object",
          "properties": {
            "complexCaseBoard": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "complexCaseBoardDetail": {
              "type": "string"
            }
          }
        },
        "catering": {
          "type": "object",
          "properties": {
            "catering": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "cateringDetail": {
              "type": "string"
            }
          }
        },
        "arson": {
          "type": "object",
          "properties": {
            "arson": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            },
            "arsonDetail": {
              "type": "string"
            }
          }
        }
      }
    },
    "move-on": {
      "type": "object",
      "properties": {
        "placement-duration": {
          "type": "object",
          "properties": {
            "duration": {
              "type": "string"
            },
            "durationDetail": {
              "type": "string"
            }
          }
        },
        "relocation-region": {
          "type": "object",
          "properties": {
            "postcodeArea": {
              "type": "string"
            }
          }
        },
        "plans-in-place": {
          "type": "object",
          "properties": {
            "arePlansInPlace": {
              "enum": [
                "no",
                "yes"
              ],
              "type": "string"
            }
          }
        },
        "type-of-accommodation": {
          "type": "object",
          "properties": {
            "accommodationType": {
              "enum": [
                "cas3",
                "foreignNational",
                "livingWithPartnerFamilyOrFriends",
                "other",
                "ownAccommodation",
                "privateRented",
                "rentedThroughHousingAssociation",
                "rentedThroughLocalAuthority",
                "supportedAccommodation",
                "supportedHousing"
              ],
              "type": "string"
            },
            "otherAccommodationType": {
              "type": "string"
            }
          }
        },
        "foreign-national": {
          "anyOf": [
            {
              "allOf": [
                {
                  "type": "object",
                  "properties": {
                    "response": {
                      "type": "string",
                      "enum": [
                        "yes"
                      ]
                    }
                  }
                },
                {
                  "type": "object",
                  "properties": {
                    "date-year": {
                      "type": "string"
                    },
                    "date-month": {
                      "type": "string"
                    },
                    "date-day": {
                      "type": "string"
                    }
                  }
                },
                {
                  "type": "object",
                  "properties": {
                    "date-time": {
                      "type": "string"
                    }
                  }
                },
                {
                  "type": "object",
                  "properties": {
                    "date": {
                      "type": "string"
                    }
                  }
                }
              ]
            },
            {
              "type": "object",
              "properties": {
                "response": {
                  "type": "string",
                  "enum": [
                    "no"
                  ]
                }
              }
            }
          ]
        }
      }
    },
    "attach-required-documents": {
      "type": "object",
      "properties": {
        "attach-documents": {
          "type": "object",
          "properties": {
            "selectedDocuments": {
              "type": "array",
              "items": {
                "description": "Meta Info about a file relating to an Offender",
                "type": "object",
                "properties": {
                  "id": {
                    "type": "string"
                  },
                  "level": {
                    "description": "The level at which a Document is associated - i.e. to the Offender or to a specific Conviction",
                    "enum": [
                      "Conviction",
                      "Offender"
                    ],
                    "type": "string"
                  },
                  "fileName": {
                    "type": "string"
                  },
                  "createdAt": {
                    "type": "string"
                  },
                  "typeCode": {
                    "type": "string"
                  },
                  "typeDescription": {
                    "type": "string"
                  },
                  "description": {
                    "type": "string"
                  }
                }
              }
            }
          }
        }
      }
    },
    "check-your-answers": {
      "type": "object",
      "properties": {
        "review": {
          "type": "object",
          "properties": {
            "reviewed": {
              "type": "string"
            }
          }
        }
      }
    }
  }
}' WHERE id = '49df96e4-f1b6-4622-9355-729f5adaf042';
