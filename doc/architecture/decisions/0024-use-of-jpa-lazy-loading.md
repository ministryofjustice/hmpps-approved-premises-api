# 24. Use of JPA Lazy Loading

Date: 2024-09-02

## Status

Accepted

## Context

When a JPA Entity is loaded, related entities will also be loaded if they are marked as `fetch=EAGER`. Certain relationship types (`@OneToOne`, `@ManyToOne`) are marked as `EAGER` by default.

Investigations into performance issues has shown that the default eager loading of entities is leading to a substantial number of additional queries being executed when in many cases these eagerly loaded entities are not used in code.

Despite `@ManyToOne` being set to `EAGER` load by default in JPA, best practice states that these should typically be set to `LAZY` by default, and converted to `EAGER` only when required/justified.

## Decision

* Unless rationale is provided to do otherwise, set `@ManyToOne` relationships to `fetch=LAZY` by default, observing the Caveats mentioned below (i.e. do not do this if the relationships points to a BaseClass or an Entity Hierarchy). Even if a thread of execution does need the load the related entity, there is typically no performance penalty when confirmed to `EAGER` loading (i.e. both will result in an extra SQL query being executed)
* Also consider setting `@OneToOne` relationships to `EAGER` when the relationship will be typically not be traversed

## Caveats

If a relationship targets a BaseClass (i.e. one that defines `@Inheritance`), it should never be LAZY loaded. If possible, convert these relationships to target a specific subclass and then LAZY loading can be used (e.g. instead of targeting `ApplicationEntity`, target `ApprovedPremisesApplicationEntity`). This behaviour described in the following text has been verified in Both Hibernate 5 (Spring Boot 2) and Hibernate 6 (Spring Boot 3)

The reason for this can be explained with an example. Given the following relationship.

```shell
  @ManyToOne(fetch=LAZY)
  @JoinColumn(name = "assessment_id")
  val assessment: AssessmentEntity,
```

Hibernate will return a `HibernateProxy` when accessing this property to ensure we only load the entity when it is first accessed (i.e. it's Lazy Loaded). This `HibernateProxy` will extend the `AssessmentEntity` class.

Therefore, a check like the following will return false, even if the referenced entity is a CAS1 `ApprovedPremisesAssessmentEntity`

`assessment is ApprovedPremisesAssessmentEntity`

We could check the underlying type using `Hibernate.getClass()`, but the subsequent cast will then fail:

```
if (Hibernate.getClass(assessment).isAssignableFrom(ApprovedPremisesAssessmentEntity::class.java)) {
   assessment as ApprovedPremisesAssessmentEntity 
   val isPendingAssessment = assessment.isPendingAssessment()
}
```

This throws the following error:

`java.lang.ClassCastException: class uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity$HibernateProxy$ntYjyAPK cannot be cast to class uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity`

We can work around this as follows:

```
if(Hibernate.getClass(assessment).isAssignableFrom(ApprovedPremiseesAssessmentEntity::class.java) {
   val cas1Assessment = Hibernate.unproxy(assessment) as ApprovedPremisesAssessmentEntity
   val isPendingAssessment = cas1Assessment.isPendingAssessment()
}
```

This will work but:

1. It's very cumbersome
2. Developers have to remember to jump through these hoops when dealing with a potentially proxied instance. If they don't, code may silently behave in an unexpected way
3. If we subsequently call `assessmentRepository.findByIdOrNull(theAssessmentId)` in the same thread, this will return the same proxied instance because it has been loaded into the Session's First Level Cache, so the above pattern would have be applied again (note that open session in view is enabled, so the Hibernate Session spans the whole request thread)
4. The error is subtle, and will only occur if the lazy loaded version of the base class is retrieved before non lazy-loaded versions. Therefore, it's possible that automated tests do not catch an execution flow/permutation where this issue occurs
