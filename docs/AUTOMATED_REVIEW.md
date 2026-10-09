# Automated review

CI runs Kotlin formatting, unit/architecture tests, isolated shaded-JAR checks, and detekt static analysis. Those checks do not require a model-provider credential.

detekt is pinned to `2.0.0-alpha.6` and runs in an isolated Java 21 process with light, syntax-only analysis for Kotlin 2.4. This avoids mixing detekt's Kotlin compiler with the project's compiler; type-resolution rules are not available in this mode. The selected bug, performance, exception and empty-block rules fail on findings, without a baseline hiding existing issues. Formatting remains ktlint's responsibility. Run `./gradlew detekt` locally; CI saves HTML and SARIF reports as artifacts.

## PR-Agent activation

The FOSS [PR-Agent](https://github.com/The-PR-Agent/pr-agent) workflow is configured for OpenAI. Add a repository Actions secret named `PR_AGENT_OPENAI_KEY` through [GitHub repository settings](https://github.com/KevinTCoughlin/deathban-pro/settings/secrets/actions). Never put the key in a commit or PR comment. Alternatively, `gh secret set PR_AGENT_OPENAI_KEY` prompts for the value locally.

The optional repository variable `PR_AGENT_MODEL` overrides the upstream v0.47.0 default `gpt-5.6`. Set it to an OpenAI model available to your API project if necessary. Fallback models are disabled so a failure does not silently select another model. OpenAI API usage is billed by your provider; installing FOSS review code does not supply API credits.

Without the secret, the workflow explicitly reports that AI review is inactive and skips the model step. A successful credentials-check job is not evidence that a model reviewed the code. Adding the secret enables reviews on the next eligible PR event; rerun the workflow on an open PR to test activation. An actual model-backed review cannot be verified before credentials exist.

## Review behavior

- Reviews run for non-draft, same-repository PRs targeting `main` when opened, reopened, updated or marked ready. Bot-triggered events and fork PRs are skipped.
- PR-Agent reads PR data through GitHub APIs; the credential-bearing job does not check out or execute PR source.
- The v0.47.0 Docker action image is pinned by digest, with upstream build provenance verified during configuration. Update the digest intentionally when upgrading.
- The workflow requests reviews only. It does not rewrite PR descriptions, generate automatic improvement patches, auto-approve, merge, or add labels.
- Repository/global PR-Agent settings loading is disabled; the review instructions and model selection are supplied by the workflow.
- Reviews focus on Bukkit threading, permissions, bans, mutable cached references, commit ordering, failure rollback and shaded runtime dependencies. Findings remain advisory and need validation against code/tests.

PR-Agent is a model-backed reviewer. detekt is a deterministic Kotlin analyzer. Neither replaces authenticated-player death/kick/login checks, standalone Spigot validation, or a human assessment of gameplay policy.
