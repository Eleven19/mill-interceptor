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
