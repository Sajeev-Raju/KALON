# Branch protection setup (Option 2 – CODEOWNERS)

Do these steps **once** in your GitHub repo: **https://github.com/Sajeev-Raju/KALON**

## 1. Add the two frontend developers as collaborators

- Repo → **Settings** → **Collaborators** (or **Manage access**) → **Add people**
- Add: **Chintu1302**, **GKiranKumarReddy26**
- Role: **Write** (so they can push branches and open PRs)

## 2. Turn on branch protection for `main`

**Option A – Via API (recommended)**

1. Create a Personal Access Token: **GitHub** → **Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)**. Scope: **repo** (full control).
2. From the repo root, run:
   ```bash
   export GITHUB_TOKEN=ghp_your_token_here
   chmod +x scripts/set-branch-protection.sh
   ./scripts/set-branch-protection.sh
   ```

**Option B – In the GitHub UI**

- Repo → **Settings** → **Branches** → **Add branch protection rule**
- **Branch name pattern:** `main`
- Enable:
  - **Require a pull request before merging**
  - **Require review from Code Owners** ← this uses `.github/CODEOWNERS`
- Leave **Restrict who can push to matching branches** **unchecked** (so they can push branches and open PRs).
- Click **Create** (or **Save changes**).

## 3. How it works

- **PRs that only change `/frontend/` or `/admin-frontend/`**  
  No CODEOWNERS match → no code owner review required. They can merge if your rule doesn’t require other approvals.

- **PRs that change `/backend/`**  
  CODEOWNERS match → GitHub will require an approval from **@Sajeev-Raju** before merge. They cannot merge backend changes without you.

## 4. If your GitHub username is not `Sajeev-Raju`

Edit `.github/CODEOWNERS` and replace `@Sajeev-Raju` with your actual GitHub username (the one that should approve backend/admin changes).
