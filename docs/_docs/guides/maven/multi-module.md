# Multi-Module Projects

This walkthrough shows how to configure the Maven plugin across a
multi-module reactor build where different modules need different Mill
target mappings.

## What you'll learn

- How to set shared defaults at the repository level
- How to override specific phases per module
- How config merging works between repo and module scopes
- How to verify each module's plan independently

## Prerequisites

- A project with the Maven plugin registered (see
  [Getting Started](getting-started.md))
- A Maven reactor (parent POM with modules) and corresponding Mill build

## Example project structure

Consider a project with three modules:

```
my-project/
  pom.xml                  (parent POM, packaging=pom)
  .mvn/extensions.xml      (plugin registration)
  mill-interceptor.yaml    (repo-level config)
  build.mill               (Mill build)
  api/
    pom.xml
  core/
    pom.xml
  app/
    pom.xml
    mill-interceptor.yaml  (module-level override)
```

## Step 1: Set repo-level defaults

Create `mill-interceptor.yaml` at the repository root with defaults that
apply to all modules:

```yaml
validate:
  scalafmtEnabled: false

lifecycle:
  compile:
    - __.compile
  test:
    - __.compile
    - __.test
```

The `__.` prefix is Mill's syntax for targeting all modules. These defaults
apply to `api/`, `core/`, and `app/` unless overridden.

## Step 2: Add a module-level override

The `app` module needs a custom compile mapping that targets only its own
sources. Create `app/mill-interceptor.yaml`:

```yaml
lifecycle:
  compile:
    - app.compile
  test:
    - app.compile
    - app.test
```

## Step 3: Understand config merging

When both repo-level and module-level configs exist, they merge with these
rules:

| Field type | Behavior |
|-----------|----------|
| Scalar (`mode`, `mill.executable`) | Module value wins if present |
| Map (`lifecycle`, `goals`, `mill.environment`) | Module keys override matching repo keys; repo-only keys are preserved |
| Validate fields (`scalafmtEnabled`, `scalafmtTarget`) | Module value wins per field if present |

For the `app` module, the effective config is:

```yaml
# From repo-level (not overridden by module):
validate:
  scalafmtEnabled: false

# From module-level (overrides repo-level):
lifecycle:
  compile:
    - app.compile
  test:
    - app.compile
    - app.test
```

For `api` and `core`, the full repo-level config applies since they have no
module-level config files.

## Step 4: Verify each module's plan

Run `inspect-plan` from the reactor root to see all modules:

```bash
mvn mill-interceptor:inspect-plan
```

The output shows the resolved plan per module. Check that:

- `api` and `core` use `__.compile` / `__.test`
- `app` uses `app.compile` / `app.test`
- All modules have scalafmt disabled

## Config discovery per module

The plugin searches for config files at each scope independently. For each
module, the search is:

**Repository scope** (from Maven session root):
1. `mill-interceptor.yaml`
2. `mill-interceptor.pkl`
3. `.config/mill-interceptor/config.yaml`
4. `.config/mill-interceptor/config.pkl`

**Module scope** (from `${project.basedir}`):
5. `mill-interceptor.yaml`
6. `mill-interceptor.pkl`
7. `.config/mill-interceptor/config.yaml`
8. `.config/mill-interceptor/config.pkl`

The first file found at each scope wins. Both YAML and Pkl are supported —
you can mix formats between scopes if needed.

## Tips

- The parent POM module (with `packaging=pom`) also participates in
  interception. If it doesn't need a Mill build, set its lifecycle to
  empty mappings or exclude it.
- Use the `.config/mill-interceptor/` path if you prefer to keep config
  files out of the module root directory.
- Module-level configs only need to contain the fields you want to
  override. Everything else inherits from the repo level.

## Next steps

- [Customizing Package and Deploy](package-and-deploy.md) — override
  package and deploy phases with arguments
- [Debugging with inspect-plan](debugging-with-inspect.md) — troubleshoot
  per-module configuration issues
- [Maven Plugin Reference](../maven-plugin.md) — full configuration
  reference
