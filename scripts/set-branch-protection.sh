#!/usr/bin/env bash
# Set branch protection on main via GitHub API.
# Requires: GITHUB_TOKEN with repo admin (e.g. repo scope) for Sajeev-Raju/KALON.
#
# Usage:
#   export GITHUB_TOKEN=ghp_xxxx...
#   ./scripts/set-branch-protection.sh

set -e

OWNER="Sajeev-Raju"
REPO="KALON"
BRANCH="main"
API_URL="https://api.github.com/repos/${OWNER}/${REPO}/branches/${BRANCH}/protection"

if [ -z "$GITHUB_TOKEN" ]; then
  echo "Error: GITHUB_TOKEN is not set."
  echo "Create a token at: https://github.com/settings/tokens"
  echo "Needed scope: repo (Full control of private repositories)."
  echo "Then run: export GITHUB_TOKEN=ghp_xxxx..."
  exit 1
fi

# Require a pull request and require review from Code Owners.
# Omit dismissal_restrictions for user-owned repos (orgs only).
# restrictions: null = do not restrict who can push (collaborators can push branches and open PRs).
BODY='{
  "required_status_checks": null,
  "enforce_admins": false,
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": false,
    "require_code_owner_reviews": true,
    "required_approving_review_count": 0
  },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false
}'

echo "Setting branch protection for ${OWNER}/${REPO} branch ${BRANCH}..."
HTTP=$(curl -s -w "%{http_code}" -o /tmp/bp-response.json \
  -X PUT \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer ${GITHUB_TOKEN}" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  -d "$BODY" \
  "$API_URL")

if [ "$HTTP" = "200" ]; then
  echo "Branch protection updated successfully."
else
  echo "Error: API returned HTTP $HTTP"
  cat /tmp/bp-response.json | head -50
  exit 1
fi
