# Maven Publish Fixture Plan

Issue: `MI-biw`

## Goal

Add a publish-capable Maven integration fixture and use it to verify the real
forwarding contract for `mvn install` and `mvn deploy`.

## Step 1: Add the failing publish-capable fixture

Create a checked-in fixture, likely
`modules/mill-interceptor-maven-plugin/itest/resources/fixtures/publish-lifecycle`,
with:

- `.mvn/extensions.xml`
- realistic `build.sc`
- `PublishModule`-capable Mill module
- local-only publish destinations

Add a failing integration test in
`MavenPluginIntegrationSpec.scala` that:

- installs the interceptor artifact locally
- copies the publish fixture to a temp dir
- runs `mvn install`
- runs `mvn deploy`
- asserts success and publish side effects

## Step 2: Run the failing test

Run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testOnly io.eleven19.mill.interceptor.maven.plugin.MavenPluginIntegrationSpec
```

Expected: fail on either fixture shape or baseline mapping for publish phases.

## Step 3: Make the publish path honest

Use the failing fixture to determine the correct fix:

- adjust the fixture build only if the publish-capable surface is still missing
- adjust the lifecycle baseline if the real Mill publish task shape differs from
  the current assumptions
- only introduce a tiny interceptor config override if zero-config publish is
  not viable even with `PublishModule`

## Step 4: Verify the publish fixture

Run the focused integration test again until it passes.

Then run:

```bash
./mill --no-server modules.mill-interceptor-maven-plugin.checkFormat
./mill --no-server modules.mill-interceptor-maven-plugin.itest.testForked
```

## Step 5: Feed the result back into docs

Update the README and Maven plugin usage docs so they state:

- the minimal no-config lifecycle contract
- the extra requirement for Maven publish phases
- whether `install` and `deploy` stay zero-config with `PublishModule`
  or require a tiny interceptor override

## Step 6: Commit and push

Commit the fixture, tests, baseline adjustments, and doc updates, then:

```bash
bd dolt push --json
git push
git status -sb
```
