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
