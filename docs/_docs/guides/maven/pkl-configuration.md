# Using Pkl Configuration

This walkthrough shows how to configure the Maven plugin using Apple's Pkl
language instead of YAML. Pkl offers type checking, expressions, and
IDE support through the Pkl IntelliJ and VS Code plugins.

## What you'll learn

- How to write a Pkl configuration file for the Maven plugin
- Config discovery behavior with Pkl files
- How to mix Pkl and YAML across repo and module scopes

## Prerequisites

- A project with the Maven plugin registered (see
  [Getting Started](getting-started.md))
- Basic familiarity with Pkl syntax (see
  [pkl-lang.org](https://pkl-lang.org))

## Writing a Pkl config

Create `mill-interceptor.pkl` at the repository root:

```pkl
mode = "strict"

mill {
  executable = "mill"
}

validate {
  scalafmtEnabled = true
}

lifecycle {
  ["compile"] = new Listing { "compile" }
  ["test"] = new Listing { "compile"; "test" }
  ["package"] = new Listing { "compile"; "test"; "assembly" }
}
```

This is equivalent to the following YAML:

```yaml
mode: strict

mill:
  executable: mill

validate:
  scalafmtEnabled: true

lifecycle:
  compile:
    - compile
  test:
    - compile
    - test
  package:
    - compile
    - test
    - assembly
```

## Config discovery with Pkl

The plugin discovers Pkl files in the same locations as YAML, with YAML
checked first at each scope:

**Repository scope:**
1. `mill-interceptor.yaml`
2. `mill-interceptor.pkl`
3. `.config/mill-interceptor/config.yaml`
4. `.config/mill-interceptor/config.pkl`

**Module scope:**
5. `mill-interceptor.yaml`
6. `mill-interceptor.pkl`
7. `.config/mill-interceptor/config.yaml`
8. `.config/mill-interceptor/config.pkl`

The first file found at each scope wins. If a `mill-interceptor.yaml`
exists, the `.pkl` file at the same scope is ignored.

## Mixing formats

You can use Pkl at one scope and YAML at another. For example:

- **Repo level:** `mill-interceptor.pkl` with shared defaults
- **Module level:** `app/mill-interceptor.yaml` with module overrides

The plugin loads each file in its own format and merges the results using
the same rules regardless of format. This is useful when teams adopt Pkl
incrementally.

## Full Pkl example with all fields

A complete configuration file showing every available field:

```pkl
// Execution mode: "strict" fails fast on missing targets
mode = "strict"

// Mill CLI runtime settings
mill {
  executable = "mill"
  workingDirectory = null

  environment {
    ["MILL_VERSION"] = "0.12.10"
    ["JAVA_OPTS"] = "-Xmx2g"
  }
}

// Validate phase configuration
validate {
  scalafmtEnabled = true
  scalafmtTarget = "mill.scalalib.scalafmt/checkFormatAll"
}

// Lifecycle phase overrides
lifecycle {
  ["clean"] = new Listing { "clean" }
  ["compile"] = new Listing { "compile" }
  ["test"] = new Listing { "compile"; "test" }
  ["package"] = new Listing { "compile"; "test"; "jar" }
  ["install"] = new Listing { "compile"; "test"; "jar"; "publishM2Local" }
  ["deploy"] = new Listing {
    "compile"
    "test"
    "jar"
    "publish"
    "--username"
    "ci-deployer"
    "--releaseUri"
    "https://repo.example.com/releases"
  }
}

// Custom explicit goals
goals {
  ["check"] = new Listing { "__.compile"; "__.test" }
}
```

## Using the .config directory

If you prefer keeping config files out of the project root, place them
under `.config/`:

```
my-project/
  .config/
    mill-interceptor/
      config.pkl         (repo-level)
  app/
    .config/
      mill-interceptor/
        config.pkl       (module-level for app)
```

The plugin discovers these paths automatically.

## Verifying Pkl config

After writing your Pkl config, use `inspect-plan` to verify it was loaded
correctly:

```bash
mvn mill-interceptor:inspect-plan
```

If the Pkl file has syntax errors, the plugin reports a
`ConfigLoadException` with the file path and error detail.

## Next steps

- [Customizing Package and Deploy](package-and-deploy.md) — practical
  example with YAML (same patterns apply to Pkl)
- [Multi-Module Projects](multi-module.md) — layered config across modules
- [Maven Plugin Reference](../maven-plugin.md) — full configuration
  reference
