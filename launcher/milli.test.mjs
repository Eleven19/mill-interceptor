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
