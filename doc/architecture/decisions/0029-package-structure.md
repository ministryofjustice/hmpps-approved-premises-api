# 29. Package Structure

Date: 2026-05-29

## Status

Accepted

## Context

Over years of development and additions of new CASs, the package structure in CAS has become unruly and inconsistent.

This ADR defines a common package structure that promotes separation of CAS-specific code.

## Decision

The top level packages are defined as follows:

```
 casx (cas1, cas2, cas2hdc, cas3)
   controller - endpoint definitions
   dto - data models used by the API
   service - business logic and internal models. 
   entity - jpa entities with embedded repositories
   jobs
      migration - migration jobs
      seed - seed jobs
   transformer - transform internal models/entities to dtos
 config
 common
   - see casx plus:
   - client 
   - cmd
   - convert 
```

In all cases additional packages can be embedded in the structure to keep code organised, preferably by domain (e.g. reports, applications, assessment etc.)

Following the pattern established in existing code, the `controller`s will use `transformer`s to convert types passed to and from the `service` layer into DTOs using the `transformer`s

Test code will follow this structure

```
 casx (cas1, cas2, cas2hdc, cas3)
   unit 
     ... package structure should match top level casx structure ...
   integration
     ... can organise this by domain, or as a single flat package ...
 common
   unit
     ... package structure should match top level common structure ...
   integration  
     ... can organise this by domain, or as a single flat package ...
```

## Limitations

The structure defined in this document has been constrained by how the existing code has been designed, and how layers of code interact

Ideally we would limit communication between the `controller`s and `service`s to `dto` types, but a precedence has been set across the code base to instead allow the `controller`s to access internal models (`entities`) with the controllers using `transformers` directly to produce `dto`s