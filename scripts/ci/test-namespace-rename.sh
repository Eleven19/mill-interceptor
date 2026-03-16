#!/usr/bin/env bash
set -euo pipefail

rg -F 'mainClass: io.eleven19.mill.interceptor.Main' modules/mill-interceptor/package.mill.yaml
rg -F 'private val mavenGroup = "io.eleven19.mill-interceptor"' mill-build/src/build/ReleaseSupport.scala
rg -F 'io.eleven19.mill-interceptor' README.md

! rg -n 'io\.github\.eleven19\.mill\.interceptor' \
  modules/mill-interceptor/src \
  modules/mill-interceptor/test \
  modules/mill-interceptor/itest \
  modules/mill-interceptor-maven-plugin/src \
  modules/mill-interceptor-maven-plugin/test \
  README.md \
  docs/contributing \
  build.mill.yaml \
  mill-build/src/build \
  scripts/ci

! rg -n 'io\.github\.eleven19\.mill-interceptor' \
  README.md \
  docs/contributing \
  build.mill.yaml \
  modules/mill-interceptor/package.mill.yaml \
  modules/mill-interceptor-maven-plugin/package.mill.yaml \
  mill-build/src/build \
  scripts/ci
