#!/usr/bin/env bash
set -euo pipefail

rg -F 'mainClass: io.eleven19.mill.interceptor.Main' build.mill.yaml
rg -F 'private val mavenGroup = "io.eleven19.mill-interceptor"' mill-build/src/build/ReleaseSupport.scala
rg -F 'io.eleven19.mill-interceptor' README.md

! rg -n 'io\.github\.eleven19\.mill\.interceptor' \
  src \
  test \
  itest \
  README.md \
  docs/contributing \
  build.mill.yaml \
  mill-build/src/build \
  scripts/ci

! rg -n 'io\.github\.eleven19\.mill-interceptor' \
  README.md \
  docs/contributing \
  build.mill.yaml \
  mill-build/src/build \
  scripts/ci
