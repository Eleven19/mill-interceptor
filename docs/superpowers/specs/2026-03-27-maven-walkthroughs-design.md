# Maven Plugin Walkthroughs Design

**Date:** 2026-03-27
**Status:** Approved
**Issue:** MI-364

## Goal

Add five self-contained walkthrough pages to the documentation site covering
common Maven plugin scenarios. Each page tells a complete story — readers can
land on any walkthrough independently without reading the others first.

## Structure

New directory `docs/_docs/guides/maven/` with five pages:

```
docs/_docs/guides/maven/
  getting-started.md          — From zero to intercepted Maven
  package-and-deploy.md       — Customizing package & deploy phases
  multi-module.md             — Multi-module project configuration
  debugging-with-inspect.md   — Debugging with inspect-plan
  pkl-configuration.md        — Using Pkl instead of YAML
```

The sidebar gains a "Maven Walkthroughs" subsection under Guides, following
the existing Maven Plugin reference entry.

## Page designs

### A. Getting Started from Scratch

**Scenario:** Existing Maven+Mill project with no interception configured.

**Walks through:**
- Creating `.mvn/extensions.xml` with the plugin coordinates
- Running `mvn compile` for the first time and verifying Mill is invoked
- Understanding the baseline output — what phases map to what targets
- Running `mvn mill-interceptor:describe` to see available goals

**Ends with:** "Next steps" linking to the other walkthroughs.

### B. Customizing Package and Deploy

**Scenario:** Team needs `mvn package` to produce a fat JAR and `mvn deploy`
to publish to a private registry with credentials.

**Walks through:**
- Creating `mill-interceptor.yaml` at the repo root
- Overriding `package` with custom Mill targets
- Overriding `deploy` with target list including arguments (username,
  password, repository URIs)
- Verifying the new plan with `mvn mill-interceptor:inspect-plan`

### C. Multi-Module Project

**Scenario:** Maven reactor with `api/`, `core/`, and `app/` modules, each
needing different Mill targets.

**Walks through:**
- Repo-level config setting shared defaults
- Module-level config overrides for `app/` and `api/`
- How config merging works in practice (scalar vs map fields)
- Verifying each module's plan independently with `inspect-plan`

### E. Debugging with inspect-plan

**Scenario:** `mvn test` fails unexpectedly and the user doesn't know what
Mill targets are being invoked.

**Walks through:**
- Running `mvn mill-interceptor:inspect-plan` to see the resolved plan
- Reading the output — probe, invoke, and fail steps
- Common failure patterns: missing targets, scalafmt probe failures,
  non-zero Mill exit codes
- Using `mvn -X` for verbose logging of config discovery and subprocess
  execution

### F. Using Pkl Configuration

**Scenario:** Team prefers Pkl over YAML for type safety and expressiveness.

**Walks through:**
- Writing an equivalent Pkl config for a realistic setup
- Config discovery behavior — same precedence as YAML
- Mixing formats: Pkl at repo level with YAML at module level (or vice versa)
- Pkl-specific features like computed values

**Note:** Pkl code blocks will not have syntax highlighting until MI-8jq is
resolved. They render as plain text.

## Conventions across all pages

- Each starts with a **"What you'll learn"** bullet list
- Each includes **"Prerequisites"** noting what is assumed
- Code blocks show **complete file contents** (not fragments)
- Each ends with **links** to related walkthroughs and the reference guide
- Language fences: `xml`, `yaml`, `bash` (all supported by highlight.js);
  `pkl` renders as plain text (tracked in MI-8jq)

## Sidebar changes

```yaml
- title: Guides
  subsection:
    - title: Maven Plugin
      page: guides/maven-plugin.md
    - title: Maven Walkthroughs
      subsection:
        - title: Getting Started
          page: guides/maven/getting-started.md
        - title: Package and Deploy
          page: guides/maven/package-and-deploy.md
        - title: Multi-Module Projects
          page: guides/maven/multi-module.md
        - title: Debugging with inspect-plan
          page: guides/maven/debugging-with-inspect.md
        - title: Pkl Configuration
          page: guides/maven/pkl-configuration.md
    - title: Release Artifacts
      page: guides/release-artifacts.md
```

## Out of scope

- Pkl syntax highlighting (MI-8jq)
- sbt or Gradle walkthrough pages
- API documentation for plugin internals
