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

  get platformKey() {
    return `${this.platform}:${this.arch}`;
  }

  get nativeSupported() {
    return this.platformKey in PLATFORM_MAP;
  }

  get nativeInfo() {
    return PLATFORM_MAP[this.platformKey] ?? null;
  }

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
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const launcher = new MilliLauncher();
  await launcher.run(process.argv.slice(2));
}
