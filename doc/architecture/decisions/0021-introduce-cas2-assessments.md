# 21. Introduce CAS2 Assessments

Date: 2024-21-02

## Status

Accepted

## Context

When we first started the CAS2 project, and created the concept of a CAS2 Application,
there was not a requirement for an `Assess` stage or data to be added. Our referrers
created and submitted applications, and assessors just viewed the applications, no edits
were made.

As there were still two domains/workflows and two separate user needs, we created two endpoints.

- `/applications` only accessible by referrers, only returns applications they have created.
  Could be submitted or un-submitted.
- `/submissions` only accessible to assessors/admins, only returns applications that have been submitted.

[More detail in the initial PR](https://github.com/ministryofjustice/hmpps-approved-premises-api/pull/1076)

Since then, a few `Assess` actions have been introduced that we have added to the Application model for
want of a better place to put them

- status updates. These are updates made only by an assessor, and are essentially updating the status
  of their assessment work flow.
- notes. These are made by either assessor or referrer, and can only be added after application is submitted,
  so they are effectively notes during the assessment phase.
- "Assessor Details". These are two optional strings, Open Housing ID and the Assessor's name, that the
  Assessors can add in order to help them track updates/notes.

It seems a good time to follow Domain Driven Design principle and introduce an Assessment, which gives us a
few benefits:

- the models we have should reflect the business/user behaviour.
  When we created the Application model, there was no user need to do anything
  except submit it and view it, but the requirements have changed and
  there's a clear `Assess` context now.
- we get some 'single responsibility' principle along the way.
- it will provide clarity to the frontend (eventually) and hopefully
  the team's understanding of the flow our data takes.

## Decision

We will start introducing CAS2 Assessments in staged phases.

### Part 1

1. Create the table for CAS2 Assessments, and add the two optional "Assessor Details" fields to it
2. Create a One-to-One relationship with CAS2 Applications
3. Create an Assessment when an Application is submitted

### Part 2

1. Allow Assessors to update and view the fields on an Assessment (incl. UI work)

### Part 3

1. Re-point StatusUpdates and Notes to the corresponding Assessment in the Assessments table, rather
   than the Applications table. We think this might be fairly easy to do with a migration that changes
   the foreign keys, and then updating the Entities/Transformers etc.
2. We will also need to make a decision on whether we update Domain Events for Status Updates - currently
   the name tightly couples them to Applications `applications.cas2.application.status-updated`.
3. Return Assessments instead of Applications from the `/submissions` endpoint. And potentially rename the
   endpoint to `/assessments`?
4. Decide how we handle Assessments on the frontend (see Consequences below).

As it stands at the moment, we are planning to do Part 1 and 2 in our current sprint (beginning 20.02.2024),
and Part 3 could be addressed either as a maintenance task or next time we touch the relevant code.

## Consequences

Changes will need to be made to UI - e.g.

- currently the referrer UI queries `/applications` for both submitted and unsubmitted apps and
  decides what view to render based on whether it has a submission date,
  instead we might want to query an `/assessments` endpoint.
- we currently have an `Application Overview` page which should probably be called an Assessment Overview
- we set a 1:1 relation between applications and assessments for now though we note that CAS1 has needed to do more
  advanced transactions where a single application can have multiple assessments. CAS2 may need to do this one day
  and as a result of this work, we should be more open to it.
- status updates may need to be set on an application by a referrer in future.
- This change opens the door for assessors to have status updates on their assessment, and a new type of status
  update (eg. withdrawn) on applications.
- dxw may not have time to complete all parts of this change, so we need to ensure this intention is well documented
  and can easily be continued after our handover

...and other minor-ish changes that we will need to iron out when the work is done. 