# 1. Use `application/problem+json` responses to handle validation errors

Date: 2022-07-25

## Status

Accepted

## Context

We have already decided to
[handle validation errors at the API level][1]
, but we need a to agree a common and predictable way to return validation or other
errors to the client, so they can be parsed an returned to the user.

## Decision

We will return `application/problem+json` responses, as outlined in [RFC-7807][2]. As well as returning the standard Problem response, we will also return details of what fields caused the error with an [extension member](https://datatracker.ietf.org/doc/html/rfc7807#section-3.2)
as an `invalid-params` array with an object with `propertyName` and `errorType` keypairs.
For example:

```http
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json
Content-Language: en

{
  "type": "https://example.net/validation-error",
  "title": "Invalid request parameters",
  "code": 400,
  "invalid-params": [
    {
      "propertyName": "arrivalDate",
      "errorType": "blank"
    },
    {
      "propertyName": "departureDate",
      "errorType": "invalid"
    },
    {
      "propertyName": "CRN",
      "errorType": "blank"
    }
  ]
}
```

## Consequences

This will give us an easy and predictable way to return validation errors. There is
also strong support for this approach in Spring, by way of the [Problem Spring Web libraries][3]
giving us plug and play support in Spring.

We will need to make sure that the client application is able to parse responses
with a `Content-Type` header of `application/problem+json`

[1]: https://github.com/ministryofjustice/approved-premises-ui/blob/main/doc/architecture/decisions/0003-handle-validations-errors-on-the-server-side.md
[2]: https://datatracker.ietf.org/doc/html/rfc7807
[3]: https://github.com/zalando/problem-spring-web
