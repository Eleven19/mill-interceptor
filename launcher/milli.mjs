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
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const launcher = new MilliLauncher();
  await launcher.run(process.argv.slice(2));
}
