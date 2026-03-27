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
});
