# Node.js milli.mjs Launcher Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a cross-platform `milli.mjs` Node.js 22+ launcher with full feature parity to the existing bash/batch launchers, including release workflow integration and comprehensive tests.

**Architecture:** A single zero-dependency `milli.mjs` file with an exported `MilliLauncher` class that accepts injectable dependencies for testability. Platform-appropriate path joining (`path.posix`/`path.win32`) based on the injected platform ensures correct cross-platform behavior. Unit tests use `node:test`, CI integration tests run dry-run comparisons.

**Tech Stack:** Node.js 22+ built-in modules (`node:test`, `node:assert`, `node:fs`, `node:path`, `node:os`, `node:child_process`, `node:url`), Mill build (`ReleaseSupport.scala`), GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-03-27-nodejs-launcher-design.md`

---

## File Structure

| File | Responsibility |
|---|---|
| `launcher/milli.mjs` | Cross-platform Node.js launcher (new) |
| `launcher/milli.test.mjs` | Unit tests for MilliLauncher class (new) |
| `scripts/ci/test-milli-mjs.sh` | CI entry point: runs unit tests + dry-run integration tests (new) |
| `mill-build/src/build/ReleaseSupport.scala` | Add `nodejs` launcher OS to `validatedLauncherOs` and `launcherFileNameFor` (modify) |
| `.github/workflows/release.yml` | Add Node.js launcher compute/build/stage steps in `extras` job (modify) |
| `scripts/ci/test-launchers.sh` | Add `milli.mjs` existence check (modify) |
| `scripts/ci/test-release-workflows.sh` | Add nodejs launcher workflow checks (modify) |
| `scripts/ci/test-release-extras-staging.sh` | Add `milli.mjs` staging validation (modify) |

---

### Task 1: Scaffold launcher and test files

**Files:**
- Create: `launcher/milli.mjs`
- Create: `launcher/milli.test.mjs`

- [ ] **Step 1: Create the launcher skeleton**

Create `launcher/milli.mjs` with imports, constants, the `MilliLauncher` class skeleton, and the entry-point guard:

```js
#!/usr/bin/env node

import { execFileSync } from 'node:child_process';
import {
  existsSync, readFileSync, mkdirSync, writeFileSync,
  renameSync, chmodSync, rmSync,
} from 'node:fs';
import { join } from 'node:path';
import path from 'node:path';
import { platform as osPlatform, arch as osArch, homedir } from 'node:os';
import { fileURLToPath } from 'node:url';

export const DEFAULT_VERSION = '@MILLI_DEFAULT_VERSION@';
const VERSION_FILE = '.mill-interceptor-version';
const CONFIG_VERSION_FILE = '.config/mill-interceptor-version';
const DIST_ARTIFACT = 'milli-dist';
const DIST_RELEASE_NAME = 'mill-interceptor-dist';
const GITHUB_RELEASE_BASE =
  'https://github.com/Eleven19/mill-interceptor/releases/download';
const MAVEN_BASE =
  'https://repo1.maven.org/maven2/io/github/eleven19/mill-interceptor';

const PLATFORM_MAP = {
  'linux:x64': {
    nativeArtifact: 'milli-native-linux-amd64',
    releaseTarget: 'x86_64-unknown-linux-gnu',
    archiveExtension: 'tar.gz',
  },
  'linux:arm64': {
    nativeArtifact: 'milli-native-linux-aarch64',
    releaseTarget: 'aarch64-unknown-linux-gnu',
    archiveExtension: 'tar.gz',
  },
  'darwin:x64': {
    nativeArtifact: 'milli-native-macos-amd64',
    releaseTarget: 'x86_64-apple-darwin',
    archiveExtension: 'tar.gz',
  },
  'darwin:arm64': {
    nativeArtifact: 'milli-native-macos-aarch64',
    releaseTarget: 'aarch64-apple-darwin',
    archiveExtension: 'tar.gz',
  },
  'win32:x64': {
    nativeArtifact: 'milli-native-windows-amd64',
    releaseTarget: 'x86_64-pc-windows-msvc',
    archiveExtension: 'zip',
  },
};

export class MilliLauncher {
  constructor(options = {}) {
    this.platform = options.platform ?? osPlatform();
    this.arch = options.arch ?? osArch();
    this.env = options.env ?? process.env;
    this.cwd = options.cwd ?? process.cwd();
    this.fs = options.fs ?? {
      existsSync, readFileSync, mkdirSync, writeFileSync,
      renameSync, chmodSync, rmSync,
    };
    this.fetchFn = options.fetch ?? globalThis.fetch;
    this.execFileSyncFn = options.execFileSync ?? execFileSync;
    this._join = this.platform === 'win32' ? path.win32.join : path.posix.join;
  }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const launcher = new MilliLauncher();
  await launcher.run(process.argv.slice(2));
}
```

- [ ] **Step 2: Create the test file with helpers**

Create `launcher/milli.test.mjs`:

```js
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { MilliLauncher, DEFAULT_VERSION } from './milli.mjs';

function createMockFs(files = {}) {
  return {
    existsSync(p) { return p in files; },
    readFileSync(p) {
      if (p in files) return files[p];
      const err = new Error(`ENOENT: no such file: ${p}`);
      err.code = 'ENOENT';
      throw err;
    },
    mkdirSync() {},
    writeFileSync() {},
    renameSync() {},
    chmodSync() {},
    rmSync() {},
  };
}

function createLauncher(overrides = {}) {
  return new MilliLauncher({
    platform: 'linux',
    arch: 'x64',
    env: {},
    cwd: '/test',
    fs: createMockFs(),
    fetch: async () => { throw new Error('fetch not mocked'); },
    execFileSync: () => { throw new Error('execFileSync not mocked'); },
    ...overrides,
  });
}

describe('MilliLauncher', () => {
  it('can be instantiated with defaults', () => {
    const launcher = createLauncher();
    assert.ok(launcher);
  });
});
```

- [ ] **Step 3: Run test to verify scaffold works**

Run: `node --test launcher/milli.test.mjs`

Expected: 1 test passes. The `run` method does not exist yet, so the entry-point guard will throw if the file is executed directly, but the test imports work.

Note: The entry-point guard calls `launcher.run()` which doesn't exist yet. This is OK because the guard condition (`process.argv[1] === fileURLToPath(import.meta.url)`) is false when the file is imported, so `run()` is never called during tests. However, the `node --test` runner imports the test file, not `milli.mjs` directly, so the guard is also false for the launcher file when imported.

- [ ] **Step 4: Commit**

```bash
git add launcher/milli.mjs launcher/milli.test.mjs
git commit -m "feat: scaffold milli.mjs launcher and test file"
```

---

### Task 2: Platform detection

**Files:**
- Modify: `launcher/milli.test.mjs`
- Modify: `launcher/milli.mjs`

- [ ] **Step 1: Write failing tests for platform detection**

Add to `launcher/milli.test.mjs` inside the `describe('MilliLauncher', ...)` block:

```js
  describe('platform detection', () => {
    it('detects linux x64', () => {
      const launcher = createLauncher({ platform: 'linux', arch: 'x64' });
      assert.equal(launcher.nativeSupported, true);
      assert.deepEqual(launcher.nativeInfo, {
        nativeArtifact: 'milli-native-linux-amd64',
        releaseTarget: 'x86_64-unknown-linux-gnu',
        archiveExtension: 'tar.gz',
      });
    });

    it('detects linux arm64', () => {
      const launcher = createLauncher({ platform: 'linux', arch: 'arm64' });
      assert.equal(launcher.nativeSupported, true);
      assert.equal(launcher.nativeInfo.nativeArtifact, 'milli-native-linux-aarch64');
      assert.equal(launcher.nativeInfo.releaseTarget, 'aarch64-unknown-linux-gnu');
    });

    it('detects darwin x64', () => {
      const launcher = createLauncher({ platform: 'darwin', arch: 'x64' });
      assert.equal(launcher.nativeSupported, true);
      assert.equal(launcher.nativeInfo.nativeArtifact, 'milli-native-macos-amd64');
      assert.equal(launcher.nativeInfo.releaseTarget, 'x86_64-apple-darwin');
    });

    it('detects darwin arm64', () => {
      const launcher = createLauncher({ platform: 'darwin', arch: 'arm64' });
      assert.equal(launcher.nativeSupported, true);
      assert.equal(launcher.nativeInfo.nativeArtifact, 'milli-native-macos-aarch64');
      assert.equal(launcher.nativeInfo.releaseTarget, 'aarch64-apple-darwin');
    });

    it('detects win32 x64', () => {
      const launcher = createLauncher({ platform: 'win32', arch: 'x64' });
      assert.equal(launcher.nativeSupported, true);
      assert.equal(launcher.nativeInfo.nativeArtifact, 'milli-native-windows-amd64');
      assert.equal(launcher.nativeInfo.releaseTarget, 'x86_64-pc-windows-msvc');
      assert.equal(launcher.nativeInfo.archiveExtension, 'zip');
    });

    it('returns unsupported for unknown platform', () => {
      const launcher = createLauncher({ platform: 'freebsd', arch: 'x64' });
      assert.equal(launcher.nativeSupported, false);
      assert.equal(launcher.nativeInfo, null);
    });

    it('returns unsupported for unknown arch', () => {
      const launcher = createLauncher({ platform: 'linux', arch: 'ppc64' });
      assert.equal(launcher.nativeSupported, false);
      assert.equal(launcher.nativeInfo, null);
    });
  });
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test launcher/milli.test.mjs`

Expected: FAIL — `nativeSupported` and `nativeInfo` are not defined on `MilliLauncher`.

- [ ] **Step 3: Implement platform detection**

Add to the `MilliLauncher` class in `launcher/milli.mjs`:

```js
  get platformKey() {
    return `${this.platform}:${this.arch}`;
  }

  get nativeSupported() {
    return this.platformKey in PLATFORM_MAP;
  }

  get nativeInfo() {
    return PLATFORM_MAP[this.platformKey] ?? null;
  }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `node --test launcher/milli.test.mjs`

Expected: All platform detection tests pass.

- [ ] **Step 5: Commit**

```bash
git add launcher/milli.mjs launcher/milli.test.mjs
git commit -m "feat(launcher): add platform detection to MilliLauncher"
```

---

### Task 3: Version resolution

**Files:**
- Modify: `launcher/milli.test.mjs`
- Modify: `launcher/milli.mjs`

- [ ] **Step 1: Write failing tests for version resolution**

Add to the `describe('MilliLauncher', ...)` block in `launcher/milli.test.mjs`:

```js
  describe('version resolution', () => {
    it('uses MILLI_VERSION env var when set', () => {
      const launcher = createLauncher({ env: { MILLI_VERSION: '1.2.3' } });
      assert.equal(launcher.resolveVersion(), '1.2.3');
    });

    it('reads .mill-interceptor-version file when env var is not set', () => {
      const launcher = createLauncher({
        cwd: '/project',
        fs: createMockFs({ '/project/.mill-interceptor-version': '2.0.0\n' }),
      });
      assert.equal(launcher.resolveVersion(), '2.0.0');
    });

    it('reads .config/mill-interceptor-version when primary file is missing', () => {
      const launcher = createLauncher({
        cwd: '/project',
        fs: createMockFs({ '/project/.config/mill-interceptor-version': '3.0.0\n' }),
      });
      assert.equal(launcher.resolveVersion(), '3.0.0');
    });

    it('falls back to default version when no source is available', () => {
      const launcher = createLauncher();
      assert.equal(launcher.resolveVersion(), DEFAULT_VERSION);
    });

    it('env var takes priority over version file', () => {
      const launcher = createLauncher({
        cwd: '/project',
        env: { MILLI_VERSION: '1.0.0' },
        fs: createMockFs({ '/project/.mill-interceptor-version': '9.9.9\n' }),
      });
      assert.equal(launcher.resolveVersion(), '1.0.0');
    });

    it('primary version file takes priority over config version file', () => {
      const launcher = createLauncher({
        cwd: '/project',
        fs: createMockFs({
          '/project/.mill-interceptor-version': '2.0.0\n',
          '/project/.config/mill-interceptor-version': '3.0.0\n',
        }),
      });
      assert.equal(launcher.resolveVersion(), '2.0.0');
    });

    it('trims whitespace and handles CRLF line endings', () => {
      const launcher = createLauncher({
        cwd: '/project',
        fs: createMockFs({ '/project/.mill-interceptor-version': '4.0.0\r\nextra\n' }),
      });
      assert.equal(launcher.resolveVersion(), '4.0.0');
    });
  });
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test launcher/milli.test.mjs`

Expected: FAIL — `resolveVersion` is not defined.

- [ ] **Step 3: Implement version resolution**

Add to the `MilliLauncher` class in `launcher/milli.mjs`:

```js
  resolveVersion() {
    if (this.env.MILLI_VERSION) {
      return this.env.MILLI_VERSION;
    }
    const versionPath = this._join(this.cwd, VERSION_FILE);
    if (this.fs.existsSync(versionPath)) {
      const content = this.fs.readFileSync(versionPath, 'utf-8');
      return content.split(/\r?\n/)[0].trim();
    }
    const configPath = this._join(this.cwd, CONFIG_VERSION_FILE);
    if (this.fs.existsSync(configPath)) {
      const content = this.fs.readFileSync(configPath, 'utf-8');
      return content.split(/\r?\n/)[0].trim();
    }
    return DEFAULT_VERSION;
  }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `node --test launcher/milli.test.mjs`

Expected: All version resolution tests pass.

- [ ] **Step 5: Commit**

```bash
git add launcher/milli.mjs launcher/milli.test.mjs
git commit -m "feat(launcher): add version resolution to MilliLauncher"
```

---

### Task 4: Mode and source resolution

**Files:**
- Modify: `launcher/milli.test.mjs`
- Modify: `launcher/milli.mjs`

- [ ] **Step 1: Write failing tests for mode and source resolution**

Add to the `describe('MilliLauncher', ...)` block in `launcher/milli.test.mjs`:

```js
  describe('mode resolution', () => {
    it('auto mode with native support returns [native, dist]', () => {
      const launcher = createLauncher({ platform: 'linux', arch: 'x64' });
      assert.deepEqual(launcher.resolveModeOrder(), ['native', 'dist']);
    });

    it('auto mode without native support returns [dist]', () => {
      const launcher = createLauncher({ platform: 'freebsd', arch: 'x64' });
      assert.deepEqual(launcher.resolveModeOrder(), ['dist']);
    });

    it('explicit native mode returns [native]', () => {
      const launcher = createLauncher({
        env: { MILLI_LAUNCHER_MODE: 'native' },
      });
      assert.deepEqual(launcher.resolveModeOrder(), ['native']);
    });

    it('explicit dist mode returns [dist]', () => {
      const launcher = createLauncher({
        env: { MILLI_LAUNCHER_MODE: 'dist' },
      });
      assert.deepEqual(launcher.resolveModeOrder(), ['dist']);
    });

    it('invalid mode throws', () => {
      const launcher = createLauncher({
        env: { MILLI_LAUNCHER_MODE: 'invalid' },
      });
      assert.throws(() => launcher.resolveModeOrder(), {
        message: 'Unsupported MILLI_LAUNCHER_MODE: invalid',
      });
    });

    it('rawMode returns the raw env value', () => {
      const launcher = createLauncher({
        env: { MILLI_LAUNCHER_MODE: 'native' },
      });
      assert.equal(launcher.rawMode, 'native');
    });

    it('rawMode defaults to auto', () => {
      const launcher = createLauncher();
      assert.equal(launcher.rawMode, 'auto');
    });
  });

  describe('source resolution', () => {
    it('maven source returns [maven, github]', () => {
      const launcher = createLauncher();
      assert.deepEqual(launcher.resolveSourceOrder(), ['maven', 'github']);
    });

    it('github source returns [github, maven]', () => {
      const launcher = createLauncher({
        env: { MILLI_LAUNCHER_SOURCE: 'github' },
      });
      assert.deepEqual(launcher.resolveSourceOrder(), ['github', 'maven']);
    });

    it('invalid source throws', () => {
      const launcher = createLauncher({
        env: { MILLI_LAUNCHER_SOURCE: 'invalid' },
      });
      assert.throws(() => launcher.resolveSourceOrder(), {
        message: 'Unsupported MILLI_LAUNCHER_SOURCE: invalid',
      });
    });

    it('rawSource defaults to maven', () => {
      const launcher = createLauncher();
      assert.equal(launcher.rawSource, 'maven');
    });
  });

  describe('netrc and dry-run flags', () => {
    it('useNetrc is false by default', () => {
      const launcher = createLauncher();
      assert.equal(launcher.useNetrc, false);
    });

    it('useNetrc is true when MILLI_LAUNCHER_USE_NETRC=1', () => {
      const launcher = createLauncher({
        env: { MILLI_LAUNCHER_USE_NETRC: '1' },
      });
      assert.equal(launcher.useNetrc, true);
    });

    it('dryRunEnabled is false by default', () => {
      const launcher = createLauncher();
      assert.equal(launcher.dryRunEnabled, false);
    });

    it('dryRunEnabled is true when MILLI_LAUNCHER_DRY_RUN=1', () => {
      const launcher = createLauncher({
        env: { MILLI_LAUNCHER_DRY_RUN: '1' },
      });
      assert.equal(launcher.dryRunEnabled, true);
    });
  });
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test launcher/milli.test.mjs`

Expected: FAIL — `rawMode`, `resolveModeOrder`, `rawSource`, `resolveSourceOrder`, `useNetrc`, `dryRunEnabled` are not defined.

- [ ] **Step 3: Implement mode and source resolution**

Add to the `MilliLauncher` class in `launcher/milli.mjs`:

```js
  get rawMode() {
    return this.env.MILLI_LAUNCHER_MODE ?? 'auto';
  }

  get rawSource() {
    return this.env.MILLI_LAUNCHER_SOURCE ?? 'maven';
  }

  get useNetrc() {
    return this.env.MILLI_LAUNCHER_USE_NETRC === '1';
  }

  get dryRunEnabled() {
    return this.env.MILLI_LAUNCHER_DRY_RUN === '1';
  }

  resolveModeOrder() {
    const mode = this.rawMode;
    if (mode !== 'auto' && mode !== 'native' && mode !== 'dist') {
      throw new Error(`Unsupported MILLI_LAUNCHER_MODE: ${mode}`);
    }
    if (mode === 'auto') {
      return this.nativeSupported ? ['native', 'dist'] : ['dist'];
    }
    return [mode];
  }

  resolveSourceOrder() {
    const source = this.rawSource;
    if (source !== 'maven' && source !== 'github') {
      throw new Error(`Unsupported MILLI_LAUNCHER_SOURCE: ${source}`);
    }
    return source === 'maven' ? ['maven', 'github'] : ['github', 'maven'];
  }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `node --test launcher/milli.test.mjs`

Expected: All mode, source, netrc, and dry-run flag tests pass.

- [ ] **Step 5: Commit**

```bash
git add launcher/milli.mjs launcher/milli.test.mjs
git commit -m "feat(launcher): add mode and source resolution to MilliLauncher"
```

---

### Task 5: Cache path construction

**Files:**
- Modify: `launcher/milli.test.mjs`
- Modify: `launcher/milli.mjs`

- [ ] **Step 1: Write failing tests for cache path construction**

Add to the `describe('MilliLauncher', ...)` block in `launcher/milli.test.mjs`:

```js
  describe('cache path construction', () => {
    it('uses MILLI_CACHE_DIR when set', () => {
      const launcher = createLauncher({
        env: { MILLI_CACHE_DIR: '/custom/cache' },
      });
      assert.equal(launcher.resolveCacheRoot(), '/custom/cache');
    });

    it('uses XDG_CACHE_HOME on unix', () => {
      const launcher = createLauncher({
        platform: 'linux',
        env: { XDG_CACHE_HOME: '/xdg/cache' },
      });
      assert.equal(launcher.resolveCacheRoot(), '/xdg/cache/milli');
    });

    it('falls back to HOME/.cache on unix', () => {
      const launcher = createLauncher({
        platform: 'linux',
        env: { HOME: '/home/user' },
      });
      assert.equal(launcher.resolveCacheRoot(), '/home/user/.cache/milli');
    });

    it('uses LOCALAPPDATA on windows', () => {
      const launcher = createLauncher({
        platform: 'win32',
        arch: 'x64',
        env: { LOCALAPPDATA: 'C:\\Users\\user\\AppData\\Local' },
      });
      assert.equal(
        launcher.resolveCacheRoot(),
        'C:\\Users\\user\\AppData\\Local\\milli',
      );
    });

    it('falls back to USERPROFILE on windows when LOCALAPPDATA is unset', () => {
      const launcher = createLauncher({
        platform: 'win32',
        arch: 'x64',
        env: { USERPROFILE: 'C:\\Users\\user' },
      });
      assert.equal(
        launcher.resolveCacheRoot(),
        'C:\\Users\\user\\.cache\\milli',
      );
    });
  });
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test launcher/milli.test.mjs`

Expected: FAIL — `resolveCacheRoot` is not defined.

- [ ] **Step 3: Implement cache path construction**

Add to the `MilliLauncher` class in `launcher/milli.mjs`:

```js
  resolveCacheRoot() {
    if (this.env.MILLI_CACHE_DIR) {
      return this.env.MILLI_CACHE_DIR;
    }
    if (this.platform === 'win32') {
      if (this.env.LOCALAPPDATA) {
        return this._join(this.env.LOCALAPPDATA, 'milli');
      }
      return this._join(
        this.env.USERPROFILE ?? homedir(), '.cache', 'milli',
      );
    }
    if (this.env.XDG_CACHE_HOME) {
      return this._join(this.env.XDG_CACHE_HOME, 'milli');
    }
    return this._join(this.env.HOME ?? homedir(), '.cache', 'milli');
  }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `node --test launcher/milli.test.mjs`

Expected: All cache path tests pass.

- [ ] **Step 5: Commit**

```bash
git add launcher/milli.mjs launcher/milli.test.mjs
git commit -m "feat(launcher): add cache path construction to MilliLauncher"
```

---

### Task 6: URL and path computation

**Files:**
- Modify: `launcher/milli.test.mjs`
- Modify: `launcher/milli.mjs`

- [ ] **Step 1: Write failing tests for URL computation**

Add to the `describe('MilliLauncher', ...)` block in `launcher/milli.test.mjs`:

```js
  describe('URL and path computation', () => {
    const baseLauncher = () => createLauncher({
      platform: 'linux',
      arch: 'x64',
      env: { HOME: '/home/user' },
    });

    it('computes dist name', () => {
      const launcher = baseLauncher();
      assert.equal(launcher.computeDistName('1.2.3'), 'milli-dist-1.2.3.jar');
    });

    it('computes dist path', () => {
      const launcher = baseLauncher();
      assert.equal(
        launcher.computeDistPath('1.2.3'),
        '/home/user/.cache/milli/1.2.3/milli-dist-1.2.3.jar',
      );
    });

    it('computes dist maven URL', () => {
      const launcher = baseLauncher();
      assert.equal(
        launcher.computeDistMavenUrl('1.2.3'),
        'https://repo1.maven.org/maven2/io/github/eleven19/mill-interceptor/milli-dist/1.2.3/milli-dist-1.2.3.jar',
      );
    });

    it('computes dist github URL', () => {
      const launcher = baseLauncher();
      assert.equal(
        launcher.computeDistGithubUrl('1.2.3'),
        'https://github.com/Eleven19/mill-interceptor/releases/download/v1.2.3/mill-interceptor-dist-v1.2.3.jar',
      );
    });

    it('computes native archive name', () => {
      const launcher = baseLauncher();
      assert.equal(
        launcher.computeNativeArchiveName('1.2.3'),
        'milli-native-linux-amd64-1.2.3.tar.gz',
      );
    });

    it('computes native archive path', () => {
      const launcher = baseLauncher();
      assert.equal(
        launcher.computeNativeArchivePath('1.2.3'),
        '/home/user/.cache/milli/1.2.3/milli-native-linux-amd64-1.2.3.tar.gz',
      );
    });

    it('computes native executable path on unix', () => {
      const launcher = baseLauncher();
      assert.equal(
        launcher.computeNativePath('1.2.3'),
        '/home/user/.cache/milli/1.2.3/milli-native-linux-amd64/mill-interceptor',
      );
    });

    it('computes native executable path on windows', () => {
      const launcher = createLauncher({
        platform: 'win32',
        arch: 'x64',
        env: { LOCALAPPDATA: 'C:\\Users\\user\\AppData\\Local' },
      });
      assert.equal(
        launcher.computeNativePath('1.2.3'),
        'C:\\Users\\user\\AppData\\Local\\milli\\1.2.3\\milli-native-windows-amd64\\mill-interceptor.exe',
      );
    });

    it('computes native maven URL', () => {
      const launcher = baseLauncher();
      assert.equal(
        launcher.computeNativeMavenUrl('1.2.3'),
        'https://repo1.maven.org/maven2/io/github/eleven19/mill-interceptor/milli-native-linux-amd64/1.2.3/milli-native-linux-amd64-1.2.3.tar.gz',
      );
    });

    it('computes native github URL', () => {
      const launcher = baseLauncher();
      assert.equal(
        launcher.computeNativeGithubUrl('1.2.3'),
        'https://github.com/Eleven19/mill-interceptor/releases/download/v1.2.3/mill-interceptor-v1.2.3-x86_64-unknown-linux-gnu.tar.gz',
      );
    });

    it('computes native github URL for windows (zip)', () => {
      const launcher = createLauncher({
        platform: 'win32',
        arch: 'x64',
        env: { LOCALAPPDATA: 'C:\\cache' },
      });
      assert.equal(
        launcher.computeNativeGithubUrl('1.2.3'),
        'https://github.com/Eleven19/mill-interceptor/releases/download/v1.2.3/mill-interceptor-v1.2.3-x86_64-pc-windows-msvc.zip',
      );
    });

    it('returns null for native methods on unsupported platform', () => {
      const launcher = createLauncher({ platform: 'freebsd', arch: 'x64' });
      assert.equal(launcher.computeNativeArchiveName('1.2.3'), null);
      assert.equal(launcher.computeNativePath('1.2.3'), null);
      assert.equal(launcher.computeNativeMavenUrl('1.2.3'), null);
      assert.equal(launcher.computeNativeGithubUrl('1.2.3'), null);
    });
  });
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test launcher/milli.test.mjs`

Expected: FAIL — URL/path computation methods are not defined.

- [ ] **Step 3: Implement URL and path computation**

Add to the `MilliLauncher` class in `launcher/milli.mjs`:

```js
  computeDistName(version) {
    return `${DIST_ARTIFACT}-${version}.jar`;
  }

  computeDistPath(version) {
    return this._join(
      this.resolveCacheRoot(), version, this.computeDistName(version),
    );
  }

  computeDistMavenUrl(version) {
    return `${MAVEN_BASE}/${DIST_ARTIFACT}/${version}/${this.computeDistName(version)}`;
  }

  computeDistGithubUrl(version) {
    return `${GITHUB_RELEASE_BASE}/v${version}/${DIST_RELEASE_NAME}-v${version}.jar`;
  }

  computeNativeArchiveName(version) {
    const info = this.nativeInfo;
    if (!info) return null;
    return `${info.nativeArtifact}-${version}.${info.archiveExtension}`;
  }

  computeNativeArchivePath(version) {
    const name = this.computeNativeArchiveName(version);
    if (!name) return null;
    return this._join(this.resolveCacheRoot(), version, name);
  }

  computeNativePath(version) {
    const info = this.nativeInfo;
    if (!info) return null;
    const executable =
      this.platform === 'win32' ? 'mill-interceptor.exe' : 'mill-interceptor';
    return this._join(
      this.resolveCacheRoot(), version, info.nativeArtifact, executable,
    );
  }

  computeNativeMavenUrl(version) {
    const info = this.nativeInfo;
    if (!info) return null;
    const archiveName = this.computeNativeArchiveName(version);
    return `${MAVEN_BASE}/${info.nativeArtifact}/${version}/${archiveName}`;
  }

  computeNativeGithubUrl(version) {
    const info = this.nativeInfo;
    if (!info) return null;
    return `${GITHUB_RELEASE_BASE}/v${version}/mill-interceptor-v${version}-${info.releaseTarget}.${info.archiveExtension}`;
  }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `node --test launcher/milli.test.mjs`

Expected: All URL and path computation tests pass.

- [ ] **Step 5: Commit**

```bash
git add launcher/milli.mjs launcher/milli.test.mjs
git commit -m "feat(launcher): add URL and path computation to MilliLauncher"
```

---

### Task 7: Dry run output

**Files:**
- Modify: `launcher/milli.test.mjs`
- Modify: `launcher/milli.mjs`

- [ ] **Step 1: Write failing tests for dry-run output**

Add to the `describe('MilliLauncher', ...)` block in `launcher/milli.test.mjs`:

```js
  describe('dry run output', () => {
    it('formats dry-run output for linux x64 with defaults', () => {
      const launcher = createLauncher({
        platform: 'linux',
        arch: 'x64',
        env: { HOME: '/home/user' },
      });
      const output = launcher.formatDryRun('1.2.3');
      const lines = output.split('\n').filter(Boolean);

      assert.ok(lines.includes('version=1.2.3'));
      assert.ok(lines.includes('mode=auto'));
      assert.ok(lines.includes('mode_order=native,dist'));
      assert.ok(lines.includes('preferred_source=maven'));
      assert.ok(lines.includes('source_order=maven,github'));
      assert.ok(lines.includes('use_netrc=0'));
      assert.ok(lines.includes('curl_netrc_flag='));
      assert.ok(lines.includes('native_supported=1'));
      assert.ok(lines.includes('native_artifact=milli-native-linux-amd64'));
      assert.ok(lines.includes('native_release_target=x86_64-unknown-linux-gnu'));
      assert.ok(lines.some(l => l.startsWith('native_maven_url=')));
      assert.ok(lines.some(l => l.startsWith('native_github_url=')));
      assert.ok(lines.some(l => l.startsWith('native_path=')));
      assert.ok(lines.includes('dist_artifact=milli-dist'));
      assert.ok(lines.some(l => l.startsWith('dist_maven_url=')));
      assert.ok(lines.some(l => l.startsWith('dist_github_url=')));
      assert.ok(lines.some(l => l.startsWith('dist_path=')));
    });

    it('omits native fields when native is not supported', () => {
      const launcher = createLauncher({
        platform: 'freebsd',
        arch: 'x64',
        env: { HOME: '/home/user' },
      });
      const output = launcher.formatDryRun('1.2.3');
      const lines = output.split('\n').filter(Boolean);

      assert.ok(lines.includes('native_supported=0'));
      assert.ok(!lines.some(l => l.startsWith('native_artifact=')));
      assert.ok(!lines.some(l => l.startsWith('native_release_target=')));
      assert.ok(lines.includes('mode_order=dist'));
    });

    it('shows netrc flags when enabled', () => {
      const launcher = createLauncher({
        env: { MILLI_LAUNCHER_USE_NETRC: '1', HOME: '/home/user' },
      });
      const output = launcher.formatDryRun('1.2.3');
      const lines = output.split('\n').filter(Boolean);

      assert.ok(lines.includes('use_netrc=1'));
      assert.ok(lines.includes('curl_netrc_flag=--netrc'));
    });

    it('shows explicit mode in output', () => {
      const launcher = createLauncher({
        env: { MILLI_LAUNCHER_MODE: 'dist', HOME: '/home/user' },
      });
      const output = launcher.formatDryRun('1.2.3');
      const lines = output.split('\n').filter(Boolean);

      assert.ok(lines.includes('mode=dist'));
      assert.ok(lines.includes('mode_order=dist'));
    });
  });
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test launcher/milli.test.mjs`

Expected: FAIL — `formatDryRun` is not defined.

- [ ] **Step 3: Implement dry-run output**

Add to the `MilliLauncher` class in `launcher/milli.mjs`:

```js
  formatDryRun(version) {
    const mode = this.rawMode;
    const modeOrder = this.resolveModeOrder();
    const preferredSource = this.rawSource;
    const sourceOrder = this.resolveSourceOrder();
    const lines = [
      `version=${version}`,
      `mode=${mode}`,
      `mode_order=${modeOrder.join(',')}`,
      `preferred_source=${preferredSource}`,
      `source_order=${sourceOrder.join(',')}`,
      `use_netrc=${this.useNetrc ? '1' : '0'}`,
      `curl_netrc_flag=${this.useNetrc ? '--netrc' : ''}`,
      `native_supported=${this.nativeSupported ? '1' : '0'}`,
    ];
    if (this.nativeSupported) {
      const info = this.nativeInfo;
      lines.push(
        `native_artifact=${info.nativeArtifact}`,
        `native_release_target=${info.releaseTarget}`,
        `native_maven_url=${this.computeNativeMavenUrl(version)}`,
        `native_github_url=${this.computeNativeGithubUrl(version)}`,
        `native_path=${this.computeNativePath(version)}`,
      );
    }
    lines.push(
      `dist_artifact=${DIST_ARTIFACT}`,
      `dist_maven_url=${this.computeDistMavenUrl(version)}`,
      `dist_github_url=${this.computeDistGithubUrl(version)}`,
      `dist_path=${this.computeDistPath(version)}`,
    );
    return lines.join('\n') + '\n';
  }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `node --test launcher/milli.test.mjs`

Expected: All dry-run output tests pass.

- [ ] **Step 5: Commit**

```bash
git add launcher/milli.mjs launcher/milli.test.mjs
git commit -m "feat(launcher): add dry-run output formatting to MilliLauncher"
```

---

### Task 8: Netrc parsing

**Files:**
- Modify: `launcher/milli.test.mjs`
- Modify: `launcher/milli.mjs`

- [ ] **Step 1: Write failing tests for netrc parsing**

Add to the `describe('MilliLauncher', ...)` block in `launcher/milli.test.mjs`:

```js
  describe('netrc parsing', () => {
    const netrcContent = [
      'machine repo1.maven.org',
      '  login myuser',
      '  password mypass',
      '',
      'machine github.com',
      '  login ghuser',
      '  password ghtoken',
    ].join('\n');

    it('parses credentials for a matching host', () => {
      const launcher = createLauncher({
        env: { HOME: '/home/user', MILLI_LAUNCHER_USE_NETRC: '1' },
        fs: createMockFs({ '/home/user/.netrc': netrcContent }),
      });
      const creds = launcher.getNetrcCredentials('repo1.maven.org');
      assert.deepEqual(creds, { login: 'myuser', password: 'mypass' });
    });

    it('returns null for non-matching host', () => {
      const launcher = createLauncher({
        env: { HOME: '/home/user', MILLI_LAUNCHER_USE_NETRC: '1' },
        fs: createMockFs({ '/home/user/.netrc': netrcContent }),
      });
      const creds = launcher.getNetrcCredentials('example.com');
      assert.equal(creds, null);
    });

    it('returns null when netrc file does not exist', () => {
      const launcher = createLauncher({
        env: { HOME: '/home/user', MILLI_LAUNCHER_USE_NETRC: '1' },
      });
      const creds = launcher.getNetrcCredentials('repo1.maven.org');
      assert.equal(creds, null);
    });

    it('uses _netrc on windows', () => {
      const launcher = createLauncher({
        platform: 'win32',
        arch: 'x64',
        env: {
          USERPROFILE: 'C:\\Users\\user',
          MILLI_LAUNCHER_USE_NETRC: '1',
        },
        fs: createMockFs({
          'C:\\Users\\user\\_netrc': netrcContent,
        }),
      });
      const creds = launcher.getNetrcCredentials('repo1.maven.org');
      assert.deepEqual(creds, { login: 'myuser', password: 'mypass' });
    });
  });
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `node --test launcher/milli.test.mjs`

Expected: FAIL — `getNetrcCredentials` is not defined.

- [ ] **Step 3: Implement netrc parsing**

Add to the `MilliLauncher` class in `launcher/milli.mjs`:

```js
  _netrcPath() {
    if (this.platform === 'win32') {
      const home = this.env.USERPROFILE ?? homedir();
      return this._join(home, '_netrc');
    }
    const home = this.env.HOME ?? homedir();
    return this._join(home, '.netrc');
  }

  getNetrcCredentials(hostname) {
    const netrcPath = this._netrcPath();
    if (!this.fs.existsSync(netrcPath)) return null;

    const content = this.fs.readFileSync(netrcPath, 'utf-8');
    const lines = content.split(/\r?\n/);
    let currentMachine = null;
    let login = null;
    let password = null;

    for (const line of lines) {
      const trimmed = line.trim();
      if (trimmed.startsWith('machine ')) {
        if (currentMachine === hostname && login && password) {
          return { login, password };
        }
        currentMachine = trimmed.slice('machine '.length).trim();
        login = null;
        password = null;
      } else if (trimmed.startsWith('login ')) {
        login = trimmed.slice('login '.length).trim();
      } else if (trimmed.startsWith('password ')) {
        password = trimmed.slice('password '.length).trim();
      }
    }

    if (currentMachine === hostname && login && password) {
      return { login, password };
    }
    return null;
  }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `node --test launcher/milli.test.mjs`

Expected: All netrc parsing tests pass.

- [ ] **Step 5: Commit**

```bash
git add launcher/milli.mjs launcher/milli.test.mjs
git commit -m "feat(launcher): add netrc parsing to MilliLauncher"
```

---

### Task 9: Download and extraction

**Files:**
- Modify: `launcher/milli.mjs`

This task implements the I/O methods. These are tested indirectly through the dry-run integration tests (Task 11) and by verifying the method signatures match what `run()` expects.

- [ ] **Step 1: Implement downloadFile**

Add to the `MilliLauncher` class in `launcher/milli.mjs`:

```js
  async downloadFile(url, destination) {
    const tempFile = `${destination}.tmp`;
    const dirnameFn = this.platform === 'win32'
      ? path.win32.dirname : path.posix.dirname;
    this.fs.mkdirSync(dirnameFn(destination), { recursive: true });

    const headers = {};
    if (this.useNetrc) {
      const hostname = new URL(url).hostname;
      const creds = this.getNetrcCredentials(hostname);
      if (creds) {
        headers['Authorization'] =
          `Basic ${Buffer.from(`${creds.login}:${creds.password}`).toString('base64')}`;
      }
    }

    const response = await this.fetchFn(url, { headers });
    if (!response.ok) {
      throw new Error(
        `Download failed: ${response.status} ${response.statusText}`,
      );
    }

    const buffer = Buffer.from(await response.arrayBuffer());
    this.fs.writeFileSync(tempFile, buffer);
    this.fs.renameSync(tempFile, destination);
  }
```

- [ ] **Step 2: Implement extractArchive**

Add to the `MilliLauncher` class in `launcher/milli.mjs`:

```js
  extractArchive(archivePath, destDir) {
    this.fs.rmSync(destDir, { recursive: true, force: true });
    this.fs.mkdirSync(destDir, { recursive: true });

    if (this.platform === 'win32') {
      try {
        this.execFileSyncFn('tar', ['-xf', archivePath, '-C', destDir], {
          stdio: 'pipe',
        });
      } catch {
        this.execFileSyncFn(
          'powershell',
          [
            '-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command',
            `Expand-Archive -Force '${archivePath}' '${destDir}'`,
          ],
          { stdio: 'pipe' },
        );
      }
    } else {
      this.execFileSyncFn('tar', ['-xzf', archivePath, '-C', destDir], {
        stdio: 'pipe',
      });
    }
  }
```

- [ ] **Step 3: Implement ensureNative and ensureDist**

Add to the `MilliLauncher` class in `launcher/milli.mjs`:

```js
  async ensureNative(source, version) {
    if (!this.nativeSupported) return false;

    const nativePath = this.computeNativePath(version);
    if (this.fs.existsSync(nativePath)) return true;

    const url = source === 'maven'
      ? this.computeNativeMavenUrl(version)
      : this.computeNativeGithubUrl(version);
    const archivePath = this.computeNativeArchivePath(version);
    const extractDir = this._join(
      this.resolveCacheRoot(), version, this.nativeInfo.nativeArtifact,
    );

    try {
      await this.downloadFile(url, archivePath);
      this.extractArchive(archivePath, extractDir);

      if (this.platform !== 'win32') {
        this.fs.chmodSync(nativePath, 0o755);
      }
      return true;
    } catch {
      return false;
    }
  }

  async ensureDist(source, version) {
    const distPath = this.computeDistPath(version);
    if (this.fs.existsSync(distPath)) return true;

    const url = source === 'maven'
      ? this.computeDistMavenUrl(version)
      : this.computeDistGithubUrl(version);

    try {
      await this.downloadFile(url, distPath);
      return true;
    } catch {
      return false;
    }
  }
```

- [ ] **Step 4: Commit**

```bash
git add launcher/milli.mjs
git commit -m "feat(launcher): add download, extraction, and ensure methods"
```

---

### Task 10: Main run loop and entry point

**Files:**
- Modify: `launcher/milli.mjs`

- [ ] **Step 1: Implement the run method**

Add to the `MilliLauncher` class in `launcher/milli.mjs`:

```js
  async run(args) {
    const version = this.resolveVersion();

    let modeOrder;
    try {
      modeOrder = this.resolveModeOrder();
    } catch (err) {
      process.stderr.write(`${err.message}\n`);
      process.exit(1);
    }

    let sourceOrder;
    try {
      sourceOrder = this.resolveSourceOrder();
    } catch (err) {
      process.stderr.write(`${err.message}\n`);
      process.exit(1);
    }

    if (this.dryRunEnabled) {
      process.stdout.write(this.formatDryRun(version));
      process.exit(0);
    }

    if (this.rawMode === 'native' && !this.nativeSupported) {
      process.stderr.write(
        'No native milli artifact is available for this platform\n',
      );
      process.exit(1);
    }

    for (const candidateMode of modeOrder) {
      if (candidateMode === 'native') {
        for (const source of sourceOrder) {
          if (await this.ensureNative(source, version)) {
            return this._exec(this.computeNativePath(version), args);
          }
        }
      } else {
        for (const source of sourceOrder) {
          if (await this.ensureDist(source, version)) {
            return this._exec(
              'java', ['-jar', this.computeDistPath(version), ...args],
            );
          }
        }
      }
    }

    const kind = this.rawMode === 'native' ? 'native' : 'dist';
    process.stderr.write(
      `Unable to download a ${kind} milli artifact for version ${version} from Maven Central or GitHub Releases\n`,
    );
    process.exit(1);
  }

  _exec(command, args) {
    try {
      this.execFileSyncFn(command, args, { stdio: 'inherit' });
      process.exit(0);
    } catch (err) {
      process.exit(err.status ?? 1);
    }
  }
```

The `run` method calls `_exec` as:
- Native: `this._exec(this.computeNativePath(version), args)`
- Dist: `this._exec('java', ['-jar', this.computeDistPath(version), ...args])`

Both forms pass `(command, argsArray)` consistently.

- [ ] **Step 2: Verify the entry-point guard is correct**

The entry-point guard at the bottom of `launcher/milli.mjs` should already be:

```js
if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const launcher = new MilliLauncher();
  await launcher.run(process.argv.slice(2));
}
```

Verify this is in place. No changes needed if it was created in Task 1.

- [ ] **Step 3: Run existing tests to verify nothing broke**

Run: `node --test launcher/milli.test.mjs`

Expected: All existing tests still pass.

- [ ] **Step 4: Commit**

```bash
git add launcher/milli.mjs
git commit -m "feat(launcher): add run loop and entry point to MilliLauncher"
```

---

### Task 11: CI test script with integration tests

**Files:**
- Create: `scripts/ci/test-milli-mjs.sh`

- [ ] **Step 1: Create the CI test script**

Create `scripts/ci/test-milli-mjs.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

node_version="$(node --version | sed 's/^v//' | cut -d. -f1)"
if [[ "$node_version" -lt 22 ]]; then
  echo "Node.js 22+ is required, found v${node_version}" >&2
  exit 1
fi

echo "Running unit tests..."
node --test "$repo_root/launcher/milli.test.mjs"

echo "Running dry-run integration tests..."

assert_contains() {
  local haystack="$1"
  local needle="$2"
  if ! printf '%s\n' "$haystack" | grep -F "$needle" >/dev/null; then
    printf 'FAIL: expected output to contain: %s\n' "$needle" >&2
    printf 'Output was:\n%s\n' "$haystack" >&2
    exit 1
  fi
}

platform_key="$(uname -s):$(uname -m)"

case "$platform_key" in
  Linux:x86_64)
    expected_native_artifact='milli-native-linux-amd64'
    ;;
  Linux:aarch64 | Linux:arm64)
    expected_native_artifact='milli-native-linux-aarch64'
    ;;
  Darwin:x86_64)
    expected_native_artifact='milli-native-macos-amd64'
    ;;
  Darwin:arm64)
    expected_native_artifact='milli-native-macos-aarch64'
    ;;
  *)
    expected_native_artifact=''
    ;;
esac

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

env_output="$(
  cd "$tmpdir"
  printf '%s\n' '9.9.9' >.mill-interceptor-version
  MILLI_VERSION='1.2.3' MILLI_LAUNCHER_DRY_RUN=1 node "$repo_root/launcher/milli.mjs"
)"

assert_contains "$env_output" 'version=1.2.3'

file_output="$(
  cd "$tmpdir"
  rm -f .config/mill-interceptor-version
  printf '%s\n' '2.0.0' >.mill-interceptor-version
  MILLI_LAUNCHER_DRY_RUN=1 node "$repo_root/launcher/milli.mjs"
)"

assert_contains "$file_output" 'version=2.0.0'

config_output="$(
  cd "$tmpdir"
  rm -f .mill-interceptor-version
  mkdir -p .config
  printf '%s\n' '3.0.0' >.config/mill-interceptor-version
  MILLI_LAUNCHER_DRY_RUN=1 node "$repo_root/launcher/milli.mjs"
)"

assert_contains "$config_output" 'version=3.0.0'

default_output="$(
  cd "$tmpdir"
  rm -f .mill-interceptor-version .config/mill-interceptor-version
  MILLI_VERSION='1.2.3' MILLI_LAUNCHER_DRY_RUN=1 node "$repo_root/launcher/milli.mjs"
)"

assert_contains "$default_output" 'mode=auto'
assert_contains "$default_output" 'source_order=maven,github'
assert_contains "$default_output" 'dist_artifact=milli-dist'

if [[ -n "$expected_native_artifact" ]]; then
  assert_contains "$default_output" 'mode_order=native,dist'
  assert_contains "$default_output" "native_artifact=${expected_native_artifact}"
fi

native_output="$(
  cd "$tmpdir"
  MILLI_VERSION='1.2.3' MILLI_LAUNCHER_MODE='native' MILLI_LAUNCHER_DRY_RUN=1 node "$repo_root/launcher/milli.mjs"
)"

assert_contains "$native_output" 'mode=native'
assert_contains "$native_output" 'mode_order=native'

dist_output="$(
  cd "$tmpdir"
  MILLI_VERSION='1.2.3' MILLI_LAUNCHER_MODE='dist' MILLI_LAUNCHER_DRY_RUN=1 node "$repo_root/launcher/milli.mjs"
)"

assert_contains "$dist_output" 'mode=dist'
assert_contains "$dist_output" 'mode_order=dist'

netrc_output="$(
  cd "$tmpdir"
  MILLI_VERSION='1.2.3' MILLI_LAUNCHER_USE_NETRC=1 MILLI_LAUNCHER_DRY_RUN=1 node "$repo_root/launcher/milli.mjs"
)"

assert_contains "$netrc_output" 'use_netrc=1'
assert_contains "$netrc_output" 'curl_netrc_flag=--netrc'

echo "All milli.mjs tests passed."
```

- [ ] **Step 2: Make the script executable**

Run: `chmod +x scripts/ci/test-milli-mjs.sh`

- [ ] **Step 3: Run the CI test script**

Run: `bash scripts/ci/test-milli-mjs.sh`

Expected: All unit tests and dry-run integration tests pass.

- [ ] **Step 4: Commit**

```bash
git add scripts/ci/test-milli-mjs.sh
git commit -m "test: add CI test script for milli.mjs launcher"
```

---

### Task 12: ReleaseSupport.scala changes

**Files:**
- Modify: `mill-build/src/build/ReleaseSupport.scala:61-69`

- [ ] **Step 1: Add nodejs launcher OS support**

In `mill-build/src/build/ReleaseSupport.scala`, modify `validatedLauncherOs` to accept `"nodejs"`:

Change:

```scala
  private def validatedLauncherOs(launcherOs: String): String =
    launcherOs match
      case "unix" | "windows" => launcherOs
      case other => throw new IllegalArgumentException(s"Unsupported launcher OS: $other")
```

To:

```scala
  private def validatedLauncherOs(launcherOs: String): String =
    launcherOs match
      case "unix" | "windows" | "nodejs" => launcherOs
      case other => throw new IllegalArgumentException(s"Unsupported launcher OS: $other")
```

Modify `launcherFileNameFor` to return `"milli.mjs"` for nodejs:

Change:

```scala
  private def launcherFileNameFor(launcherOs: String): String =
    validatedLauncherOs(launcherOs) match
      case "unix" => "milli"
      case "windows" => "milli.bat"
```

To:

```scala
  private def launcherFileNameFor(launcherOs: String): String =
    validatedLauncherOs(launcherOs) match
      case "unix" => "milli"
      case "windows" => "milli.bat"
      case "nodejs" => "milli.mjs"
```

- [ ] **Step 2: Verify the build compiles**

Run from the worktree root:

```bash
./mill resolve modules.mill-interceptor.releaseLauncherName
```

Expected: The Mill build resolves the task without compilation errors.

- [ ] **Step 3: Verify the nodejs launcher name resolves**

Run:

```bash
./mill show modules.mill-interceptor.releaseLauncherName --launcher-os nodejs
```

Expected: Output includes `"milli.mjs"`.

- [ ] **Step 4: Commit**

```bash
git add mill-build/src/build/ReleaseSupport.scala
git commit -m "feat(build): add nodejs launcher OS to ReleaseSupport"
```

---

### Task 13: Release workflow changes

**Files:**
- Modify: `.github/workflows/release.yml:95-127`

- [ ] **Step 1: Add Node.js launcher steps to the extras job**

In `.github/workflows/release.yml`, after the "Stage Windows launcher" step (around line 127), add three new steps:

```yaml
      - name: Compute Node.js launcher name
        id: nodejs-launcher-name
        shell: bash
        run: scripts/ci/compute-launcher-name.sh nodejs nodejs_launcher_name

      - name: Build Node.js launcher
        shell: bash
        run: scripts/ci/build-release-launcher.sh "${{ needs.metadata.outputs.version }}" nodejs "${{ steps.nodejs-launcher-name.outputs.nodejs_launcher_name }}"

      - name: Stage Node.js launcher
        shell: bash
        run: scripts/ci/stage-release-extra.sh "out/modules/mill-interceptor/releaseLauncher.dest/${{ steps.nodejs-launcher-name.outputs.nodejs_launcher_name }}" release-extras/releaseLauncher.dest
```

- [ ] **Step 2: Verify YAML syntax**

Run: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/release.yml'))"` or use another YAML linter to confirm the file is valid.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "ci: add Node.js launcher to release workflow"
```

---

### Task 14: Existing test script updates

**Files:**
- Modify: `scripts/ci/test-launchers.sh:31-33`
- Modify: `scripts/ci/test-release-workflows.sh:9-13,30-37`
- Modify: `scripts/ci/test-release-extras-staging.sh`

- [ ] **Step 1: Add milli.mjs existence check to test-launchers.sh**

In `scripts/ci/test-launchers.sh`, after the existing `test -f launcher/milli.bat` line (line 33), add:

```bash
test -f launcher/milli.mjs
```

Also add content checks for the new launcher, after the existing `milli.bat` checks (around line 55):

```bash
rg -F '.mill-interceptor-version' launcher/milli.mjs
rg -F '.config/mill-interceptor-version' launcher/milli.mjs
rg -F 'MILLI_LAUNCHER_MODE' launcher/milli.mjs
rg -F 'MILLI_LAUNCHER_SOURCE' launcher/milli.mjs
rg -F 'MILLI_LAUNCHER_USE_NETRC' launcher/milli.mjs
rg -F 'MILLI_LAUNCHER_DRY_RUN' launcher/milli.mjs
rg -F 'milli-dist' launcher/milli.mjs
rg -F 'github.com/Eleven19/mill-interceptor/releases/download' launcher/milli.mjs
```

- [ ] **Step 2: Add nodejs launcher checks to test-release-workflows.sh**

In `scripts/ci/test-release-workflows.sh`, add the `compute-launcher-name.sh` script to the `ci_mill_scripts` array (it is already there — no change needed).

After the existing `rg` checks for launcher steps in the release workflow (around line 33), add:

```bash
rg -F 'name: Stage Node.js launcher' .github/workflows/release.yml
```

Also add the nodejs launcher name script to the verification array if not already present. Check that `scripts/ci/test-milli-mjs.sh` exists:

```bash
test -f scripts/ci/test-milli-mjs.sh
```

- [ ] **Step 3: Add milli.mjs staging to test-release-extras-staging.sh**

In `scripts/ci/test-release-extras-staging.sh`, after the existing Windows launcher staging block (around line 32), add:

```bash
rm -rf "$workspace/out/modules/mill-interceptor/releaseLauncher.dest"
mkdir -p "$workspace/out/modules/mill-interceptor/releaseLauncher.dest"
printf 'nodejs launcher\n' >"$workspace/out/modules/mill-interceptor/releaseLauncher.dest/milli.mjs"

bash "$repo_root/scripts/ci/stage-release-extra.sh" \
  "$workspace/out/modules/mill-interceptor/releaseLauncher.dest/milli.mjs" \
  "$staging_dir/releaseLauncher.dest"
```

After the existing assertions at the end, add:

```bash
test -f "$staging_dir/releaseLauncher.dest/milli.mjs"
rg -F 'nodejs launcher' "$staging_dir/releaseLauncher.dest/milli.mjs"
```

- [ ] **Step 4: Run all test scripts**

Run each test script to verify they pass:

```bash
bash scripts/ci/test-launchers.sh
bash scripts/ci/test-release-extras-staging.sh
bash scripts/ci/test-release-workflows.sh
bash scripts/ci/test-milli-mjs.sh
```

Expected: All scripts exit 0.

- [ ] **Step 5: Commit**

```bash
git add scripts/ci/test-launchers.sh scripts/ci/test-release-workflows.sh scripts/ci/test-release-extras-staging.sh
git commit -m "test: update existing test scripts for milli.mjs launcher"
```

---

## Verification Checklist

After all tasks are complete, verify the full deliverable set:

- [ ] `launcher/milli.mjs` exists and is a valid Node.js script
- [ ] `launcher/milli.test.mjs` passes: `node --test launcher/milli.test.mjs`
- [ ] `scripts/ci/test-milli-mjs.sh` passes: `bash scripts/ci/test-milli-mjs.sh`
- [ ] `scripts/ci/test-launchers.sh` passes: `bash scripts/ci/test-launchers.sh`
- [ ] `scripts/ci/test-release-extras-staging.sh` passes
- [ ] `scripts/ci/test-release-workflows.sh` passes
- [ ] `ReleaseSupport.scala` compiles: `./mill resolve modules.mill-interceptor.releaseLauncherName`
- [ ] Dry-run output matches bash launcher format: compare `MILLI_VERSION=1.2.3 MILLI_LAUNCHER_DRY_RUN=1 ./launcher/milli` vs `MILLI_VERSION=1.2.3 MILLI_LAUNCHER_DRY_RUN=1 node launcher/milli.mjs`
