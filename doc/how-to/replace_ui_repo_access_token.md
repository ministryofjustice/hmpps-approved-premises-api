# Replace UI Repo Access Token

The `Trigger UI Type Generation Workflow` step triggers a workflow in the `hmpps-approved-premises-ui` repository to 
generate Typescript types from the OpenAPI specification in this repository.

To do this, we must store a GitHub Personal Access Token (PAT) as a repository secret.  These expire so need to be changed 
every 90 days.

1. Go to https://github.com/settings/personal-access-tokens/new
2. Name the token, e.g. `Generate TS Types from OpenAPI`
3. Set 90 day expiration
4. Select `ministryofjustice` as the Resource Owner
5. Enter the following as the justification:
   ```
   To trigger Typescript type generation in hmpps-approved-premises-ui when hmpps-approved-premises-api's OpenAPI spec is updated.
   ```
6. For Repository Access, select `Only select repositories`, then select `hmpps-approved-premises-ui` as the repository
7. Under Permissions->Repository Permissions, select `Access: Read and Write` on the `Actions` option
8. Click `Generate token and request access`
9. Copy the token
10. Go to `https://github.com/ministryofjustice/hmpps-approved-premises-api/settings/secrets/actions`
11. Under the Repository secrets section, click the edit pencil next to `UI_REPO_ACCESS_TOKEN`
12. Paste the PAT and click `Update Secret`
13. An Administrator for the ministryofjustice GitHub Organisation will need to approve the token before it starts to work (you will receive an email from GitHub once this has been done.)
