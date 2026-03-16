#!/usr/bin/env bash
set -euo pipefail

test -d modules/mill-interceptor/src
test -d modules/mill-interceptor/test
test -d modules/mill-interceptor/itest
test -f modules/mill-interceptor/package.mill.yaml

test -d modules/mill-interceptor-maven-plugin/src
test -d modules/mill-interceptor-maven-plugin/test
test -f modules/mill-interceptor-maven-plugin/package.mill.yaml

! test -d src
! test -d test
! test -d itest

rg -F 'extends: mill.Module' build.mill.yaml
rg -F 'mainClass: io.eleven19.mill.interceptor.Main' modules/mill-interceptor/package.mill.yaml
rg -F 'modules/mill-interceptor' docs/plans/2026-03-16-multi-module-maven-plugin-design.md
