## PR Template

## Before request for review (PR Owner)

- [ ] Ensure your branch name and PR name is of the format
  {chore/feature}/CAS-{TICKET-NUMBER}-{description-separated-by-hyphens} (e.g. feature/CAS-123-add-booking-system)
- [ ] Assign the PR to yourself
- [ ] Make any doc changes, like updating the README or adding an ADR or even changing something on Confluence
- [ ] Add a feature flag if needed
- [ ] Ensure all AC for this PR pass
- [ ] Rebase on main
- [ ] Squash all your commits where the first line matches you branch name and the lines after explain what this PR is
  for
- [ ] Copy and paste your commit message into the PR description and include a link to the ticket associated with this
  PR
- [ ] Wait for PR pipeline to complete successfully
- [ ] Ensure the PR is not in draft mode
- [ ] Move related ticket (if necessary) into review column
- [ ] Post on cas-dev Slack channel asking for a review

## Reviewer (At least one)

1. Put eyes emoji 👀 on Slack message to let the PR owner know you are checking the code
2. Complete a thorough code review adding comments if necessary
3. Approve or reject the review (resolving any comments)
4. Comment on Slack message to let the PR owner know you have finished your review (✅ or ❌)

## PR owner (if PR rejected)

1. Respond to any comments
2. Make code changes
3. Comment on Slack message tagging reviewers to ask for re-review

## After review (PR Owner)

1. Rebase on main
2. Wait for PR pipeline to complete successfully
3. Merge

## After merge (PR Owner)

1. Close any PRs and delete any branches that are no longer relevant after the completion of this PR
2. Put merged :merged: emoji on Slack message to indicate the PR has been merged
3. Check it has been successfully deployed to dev env
4. Move ticket (if necessary) to QA section and inform DM or PO
5. Go and make yourself a nice coffee ☕ before you pick up the next thing!!