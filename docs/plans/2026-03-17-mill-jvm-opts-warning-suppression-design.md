# Mill JVM Opts Warning Suppression Design

Date: 2026-03-17
Issue: `MI-m9j`

## Problem

Running `./mill` in this repository shows a runtime warning from Scala's
`scala.runtime.LazyVals` on JDK 25:

- `sun.misc.Unsafe::objectFieldOffset has been called`

This warning is upstream to Scala/JDK compatibility behavior and is not caused
by this repository's own code, but it adds noise to routine build output.

## Goal

Suppress the warning for normal repository-local `./mill` usage without
patching the checked-in Mill launcher script.

## Chosen Approach

Use a checked-in `.mill-jvm-opts` file with:

```text
--sun-misc-unsafe-memory-access=allow
```

This is the narrowest repo-local mitigation if Mill honors its standard JVM
opts file. It avoids modifying the launcher script and keeps the mitigation
explicitly scoped to Mill JVM startup.

## Non-Goals

- removing Scala's underlying `Unsafe` usage
- changing the pinned JDK version
- changing CI or launcher behavior beyond normal Mill config discovery

## Success Criteria

- the warning is no longer shown for normal `./mill` commands in this repo
- the config is repo-local and checked in
- no launcher script changes are required
