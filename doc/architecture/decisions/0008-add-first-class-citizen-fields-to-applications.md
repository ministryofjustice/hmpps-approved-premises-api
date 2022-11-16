# 8. Add First-Class Citizen Fields to Applications

Date: 2022-10-31

## Status

Accepted

## Context

Approved Premises applications are currently stored in the database as a JSON
object. This allows the format of the questionnaire to be flexible, simple, and
take into account any changes in the questionnaire format, whilst also
preserving applications that have been submitted in an older format.

However, when processing an application, there are some situations where we need
easy access to particular fields within the JSON application object. These include
(but are not limited to):

- If an application is for a women-only AP
- If an application is for a PIPE placement
- The target location
- The desired start date

These are important fields that need to be queried on, and made into first class
citizens within the database table, without sacrificing flexibility.

## Decision

At the point of submission of a completed application, we will use [JsonLogic](https://jsonlogic.com)
to fetch the important responses from the JSON object and populate the first
class citizen fields with those responses.

The JsonLogic rules for each individual field will be stored in the database
alongside the corresponding schema. This means that we don't have to alter code
when a schema changes.

### Examples

Assuming there is a `sentenceType` field in the JSON document
that looks like this:

```json
{
   "basic-information": {
      "sentence-type": {
         "sentenceType": "extendedDeterminate"
      }
   }
}
```

Then there will be a `sentenceTypeQuery` field in the `application_schemas`
table with the value:

```json
{ "var": "basic-information.sentence-type.sentenceType" }
```

When the application is sumbitted, we will set the `sentenceType` field in the
`applications` table like so (pseudocode):

```text
query = application.schema.sentenceTypeQuery # returns '{ "var": "basic-information.sentence-type.sentenceType" }'
sentenceType = JSONLogic.apply(application.body, query) # returns `extendedDeterminate`
application.sentenceType = sentenceType
```

If the schema changes, for example:

```json
{
   "basic-info": {
      "question1": {
         "sentenceType": "extendedDeterminate"
      }
   }
}
```

Then there will be a `sentenceTypeQuery` field in the new schema row with
the value:

```json
{ "var": "basic-info.question1.sentenceType" }
```

There will be some situations where some first-class citizen fields (such
as start date), may have to rely on multiple fields. In the case of start
date, we will hav a `releaseDateQuery` field like this:

```json
{
   "if":[
      {
         "==":[
            {
               "var":"basic-information.placement-date.startDateSameAsReleaseDate"
            },
            "yes"
         ]
      },
      {
         "var":"basic-information.release-date.releaseDate"
      },
      {
         "var":"basic-information.placement-date.startDate"
      }
   ]
}
```

This returns the value of `basic-information.release-date.releaseDate` if
`basic-information.placement-date.startDateSameAsReleaseDate` is `yes`,
otherwise it will be the value of `basic-information.placement-date.startDate`.

Again, we can set this first-class citizen field at the point of submission
like so (pseudocode):

```text
query = application.schema.releaseDateQuery
releaseDate = JSONLogic.apply(application.body, query)
application.sentenceType = releaseDate
```

There are a couple of JsonLogic
parsers we can use in a Kotlin application, such as [JSON Logic Java](https://github.com/jamsesso/json-logic-java),
and [JSON Logic Kotlin](https://github.com/advantagefse/json-logic-kotlin).

## Consequences

This will ensure that changing schema will be a deliberate process, and we will
be able to update the signposting to the first-class citizen fields in the same
place as the schema, reducing the risk of anything getting missed, and making
schema changes easier.

There may be some situations when the change to a schema may mean some first
class citizen fields could be deprecated, and this is something we'll have to
consider as the application evolves.
