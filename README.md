# mill-interceptor

A tool for intercepting other build tools using mill.
The idea is to use mill to "impersonate" other build tools.

## What does it do?

This tool serves as a replacement* for:

- mvn
- gradle
- sbt 

## Should I use this?

Sure, if you find it useful.
I had a very particular use-case involving integration with certain build pipelines that were
setup to work with common tools like maven, gradle, and sbt, but with no native support for mill.

