# JPA Entity Best Practice

## Context

Some care needs to be taken when defining JPA entities to avoid creating entity graphs that are inefficient and avoid unnecessary eager loading whilst still being easy to work with

This document defines best practice for defining JPA entities

## Decision

1. Use the JPA Gradle plugin to add default constructors

JPA requires all entities to have a default constructor. To enable this in Kotlin we use the JPA Gradle Plugin included in our build file

2. Always suffix entity classes with `Entity`
3. Use Data Classes for entities

Whilst use of data classes for JPA entities is sometimes discouraged, with care any potential issues can be avoided and we can leverage the useful features of data classes (e.g. minimal code, copy functions). For more see this [JPA Buddy Blog](https://jpa-buddy.com/blog/best-practices-and-common-pitfalls/)

4. _Always_ override equals, hashCode and toString

This avoids the common problems when using data classes (default implementations will lead to eager loading, can lead to infinite loops). Take care in the implementation, following guidance on https://jpa-buddy.com/blog/hopefully-the-final-article-about-equals-and-hashcode-for-jpa-entities-with-db-generated-ids/

For an example, see Cas1FormDataEntity

5. Identify Root Aggregates and avoid unnecessary bi-directional relationships

The aggregate root should be the entry point into a Entity Graph. Typically, entities linked to by the aggregate root should not need a bi-directional relationship defined in the model (note that these relationships can still be traversed if required in SQL)

As a general rule only add a bi-directioanl relationship if absolutely required. This helps avoid inadvertent eager loading.

6. If in doubt, always define relationships as @Lazy (where the default would be @Eager)
7. Use Spring Data JPA for repositories by default

Repositories should be defined in the same files as entities

8. Eagerly load relationships in the repository function where required

If the receiving code will definitely be traversing lazy loaded relationships they should be eagerly loaded in the underlying SQL, reducing the number of SQL calls being made.

Where possible use named or inline EntityGraphs to resolve these relationships. Sometimes this isn't possible (e.g. when we want to add eager loading to an existing repository function like `findByIdOrNull`). In that case, use JPQL with `fetch` instead.

9. Disable Open Entity/Session in View

This is widely accepted as an anti-pattern that can lead to issues and should be disabled (it's enabled in Spring by default)
