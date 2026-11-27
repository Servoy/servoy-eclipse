// SVY-21502: tests for pack-components.mjs, run with 'node --test scripts/' (npm run test_scripts).
// The script locates its project root relative to its own file, so each test copies it into a temp folder with
// fake projects/<name>/package.json files and runs it there.
import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { copyFileSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { afterEach, beforeEach, describe, it } from 'node:test';
import { fileURLToPath } from 'node:url';
import { gunzipSync } from 'node:zlib';

const script = join(dirname(fileURLToPath(import.meta.url)), 'pack-components.mjs');

let root;

const writeProject = (name, manifest, raw) => {
  mkdirSync(join(root, 'projects', name), { recursive: true });
  writeFileSync(join(root, 'projects', name, 'package.json'), raw ?? JSON.stringify(manifest, null, 2));
};

const run = () => execFileSync(process.execPath, [join(root, 'scripts', 'pack-components.mjs')], { cwd: root, encoding: 'utf8' });

const tarballs = () => readdirSync(join(root, 'tarballs')).filter((f) => f.endsWith('.tgz')).sort();

const listed = () => JSON.parse(readFileSync(join(root, 'tarballs', 'component-tarballs.json'), 'utf8'));

/** parses the (ustar) tar inside a tgz: [{ name, content, headerChecksumOk }] */
const readTgz = (file) => {
  const tar = gunzipSync(readFileSync(join(root, 'tarballs', file)));
  const entries = [];
  let pos = 0;
  while (pos + 512 <= tar.length && tar.subarray(pos, pos + 512).some((b) => b !== 0)) {
    const header = Buffer.from(tar.subarray(pos, pos + 512));
    const name = header.subarray(0, 100).toString('utf8').replace(/\0.*$/, '');
    const size = parseInt(header.subarray(124, 135).toString('ascii'), 8);
    const stored = parseInt(header.subarray(148, 154).toString('ascii'), 8);
    header.fill(0x20, 148, 156);
    const calculated = header.reduce((sum, b) => sum + b, 0);
    entries.push({
      name,
      mtime: parseInt(header.subarray(136, 147).toString('ascii'), 8),
      content: tar.subarray(pos + 512, pos + 512 + size).toString('utf8'),
      headerChecksumOk: stored === calculated,
    });
    pos += 512 + Math.ceil(size / 512) * 512;
  }
  return entries;
};

beforeEach(() => {
  root = mkdtempSync(join(tmpdir(), 'pack-components-'));
  mkdirSync(join(root, 'scripts'));
  copyFileSync(script, join(root, 'scripts', 'pack-components.mjs'));
  writeProject('servoydefault', {
    name: '@servoy/servoydefault',
    exports: { '.': './src/public-api.ts' },
    version: '2025.9.0',
    dependencies: { tinymce: '^6', '@angular/cdk': '^22', '@tinymce/tinymce-angular': '<8' },
    license: 'MIT',
    repository: { type: 'git', url: 'x' },
  });
  writeProject('dialogs', { name: '@servoy/dialogs', version: '2025.9.0', dependencies: { '@angular/cdk': '^22' }, license: 'MIT' });
  writeProject('window', { name: '@servoy/window', version: '2025.9.0', dependencies: { '@popperjs/core': '<3' }, license: 'MIT' });
  writeProject('ngclientutils', { name: '@servoy/ngclientutils', version: '2025.9.0', license: 'MIT' });
});

afterEach(() => rmSync(root, { recursive: true, force: true }));

describe('pack-components', () => {
  it('makes a tarball for each package with dependencies and none for a package without', () => {
    run();

    const files = tarballs();
    assert.equal(files.length, 3, files.join(', '));
    assert.ok(files.some((f) => /^servoy-servoydefault-[0-9a-f]{12}\.tgz$/.test(f)), files.join(', '));
    assert.ok(files.some((f) => /^servoy-dialogs-[0-9a-f]{12}\.tgz$/.test(f)), files.join(', '));
    assert.ok(files.some((f) => /^servoy-window-[0-9a-f]{12}\.tgz$/.test(f)), files.join(', '));
    assert.ok(!files.some((f) => f.includes('ngclientutils')), 'ngclientutils has no dependencies, so nothing to install');
    assert.deepEqual(listed(), files, 'component-tarballs.json lists exactly the produced tarballs');
  });

  it('packs only a package.json with the name, version and sorted dependencies, without exports or sources', () => {
    run();

    const file = tarballs().find((f) => f.startsWith('servoy-servoydefault-'));
    const entries = readTgz(file);
    assert.equal(entries.length, 1);
    assert.equal(entries[0].name, 'package/package.json');
    assert.ok(entries[0].headerChecksumOk, 'a valid tar header checksum');
    const manifest = JSON.parse(entries[0].content);
    assert.deepEqual(manifest, {
      name: '@servoy/servoydefault',
      version: '2025.9.0',
      dependencies: { '@angular/cdk': '^22', '@tinymce/tinymce-angular': '<8', tinymce: '^6' },
      license: 'MIT',
    });
    assert.deepEqual(Object.keys(manifest.dependencies), ['@angular/cdk', '@tinymce/tinymce-angular', 'tinymce']);
  });

  it('gives byte-identical tarballs and names on every run, whatever the mtime, key order or whitespace of the source', () => {
    run();
    const first = tarballs().map((f) => [f, readFileSync(join(root, 'tarballs', f)).toString('hex')]);

    writeProject('servoydefault', null,
      '{\r\n"license":"MIT","dependencies":{"tinymce":"^6","@tinymce/tinymce-angular":"<8","@angular/cdk":"^22"},\r\n' +
      '"version":"2025.9.0","name":"@servoy/servoydefault","exports":{".":"./src/public-api.ts"}}');
    const later = new Date(Date.now() + 86400000);
    for (const p of ['servoydefault', 'dialogs', 'window', 'ngclientutils']) {
      // touch the sources: an mtime change must never change the tarball
      writeFileSync(join(root, 'projects', p, 'package.json'), readFileSync(join(root, 'projects', p, 'package.json')));
    }
    run();

    assert.deepEqual(tarballs().map((f) => [f, readFileSync(join(root, 'tarballs', f)).toString('hex')]), first);
    assert.ok(readTgz(first[0][0]).every((e) => e.mtime === 499162500), 'fixed mtime ' + later.toISOString());
  });

  it('gives a new name when the dependencies change and removes the older tarball of that package', () => {
    run();
    const before = tarballs().find((f) => f.startsWith('servoy-dialogs-'));

    writeProject('dialogs', { name: '@servoy/dialogs', version: '2025.9.0', dependencies: { '@angular/cdk': '^23' }, license: 'MIT' });
    run();

    const after = tarballs().filter((f) => f.startsWith('servoy-dialogs-'));
    assert.equal(after.length, 1, after.join(', '));
    assert.notEqual(after[0], before, 'pnpm treats an unchanged file: path as up to date, so a new content needs a new name');
  });

  it('keeps the name when only the version-independent parts that are not packed change', () => {
    run();
    const before = tarballs();

    writeProject('servoydefault', {
      name: '@servoy/servoydefault',
      exports: { '.': './somewhere/else.ts' },
      version: '2025.9.0',
      dependencies: { tinymce: '^6', '@angular/cdk': '^22', '@tinymce/tinymce-angular': '<8' },
      license: 'MIT',
      repository: { type: 'git', url: 'another' },
    });
    run();

    assert.deepEqual(tarballs(), before, 'exports and repository are not part of the dependency-only tarball');
  });

  it('removes the tarball of a package that no longer has dependencies, but leaves other tarballs alone', () => {
    mkdirSync(join(root, 'tarballs'));
    writeFileSync(join(root, 'tarballs', 'servoy-public-2026.9.5.tgz'), 'public');
    writeFileSync(join(root, 'tarballs', 'servoy-ngclientutils-000000000000.tgz'), 'stale');
    writeFileSync(join(root, 'tarballs', 'servoy-window2-111111111111.tgz'), 'other package with the same prefix start');

    run();

    const files = tarballs();
    assert.ok(!files.includes('servoy-ngclientutils-000000000000.tgz'), 'stale tarball of a package without dependencies is gone');
    assert.ok(files.includes('servoy-public-2026.9.5.tgz'), 'the public tarball is not touched');
    assert.ok(files.includes('servoy-window2-111111111111.tgz'), 'only exactly "servoy-window-<hash>" belongs to the window package');
    assert.ok(!listed().includes('servoy-public-2026.9.5.tgz'), 'the list only has the component tarballs');
  });
});
