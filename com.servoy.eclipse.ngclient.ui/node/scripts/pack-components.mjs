// SVY-21502: builds the dependency-only tarballs of the four internal source component packages
// (servoydefault, dialogs, window, ngclientutils) into tarballs/, plus tarballs/component-tarballs.json (the list of
// the produced file names, read by WebPackagesListener at runtime).
//
// These packages are SOURCE packages: the solution's tsconfig 'paths' alias compiles them from projects/<name>.
// pnpm only installs the third party dependencies of a package that is installed as a tarball, so the tarball
// contains nothing but a package.json with the name, version and dependencies (no sources, no 'exports').
//
// The tarball is byte-for-byte reproducible (sorted keys, fixed mtime/uid/mode, stored gzip blocks, no dependency on
// the zlib version) and its name embeds a hash of its content: servoy-<name>-<hash>.tgz. pnpm treats a file: tarball
// whose path did not change as up to date, so a changed dependency set MUST give a new name.
//
// Run this AFTER pack_public: that script removes every *.tgz in tarballs/ first.
import { createHash } from 'node:crypto';
import { mkdirSync, readFileSync, readdirSync, unlinkSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const PROJECTS = ['servoydefault', 'dialogs', 'window', 'ngclientutils'];
const FIXED_MTIME = 499162500; // 1985-10-26T08:15:00Z, same fixed date npm uses
const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const out = join(root, 'tarballs');
const MANIFEST = 'component-tarballs.json';

const sortObject = (o) => Object.fromEntries(Object.entries(o ?? {}).sort(([a], [b]) => a.localeCompare(b)));

const crcTable = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c >>> 0;
  }
  return t;
})();
const crc32 = (buf) => {
  let c = 0xffffffff;
  for (const b of buf) c = crcTable[(c ^ b) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
};

const octal = (value, length) => Buffer.from(value.toString(8).padStart(length - 1, '0') + '\0', 'ascii');

const tarEntry = (name, content) => {
  const header = Buffer.alloc(512);
  header.write(name, 0, 100, 'utf8');
  octal(0o644, 8).copy(header, 100);
  octal(0, 8).copy(header, 108);
  octal(0, 8).copy(header, 116);
  octal(content.length, 12).copy(header, 124);
  octal(FIXED_MTIME, 12).copy(header, 136);
  header.fill(0x20, 148, 156);
  header[156] = 0x30;
  header.write('ustar', 257, 'ascii');
  header.write('00', 263, 'ascii');
  let sum = 0;
  for (const b of header) sum += b;
  header.write(sum.toString(8).padStart(6, '0'), 148, 'ascii');
  header[154] = 0;
  header[155] = 0x20;
  const pad = Buffer.alloc((512 - (content.length % 512)) % 512);
  return Buffer.concat([header, content, pad]);
};

const storedGzip = (data) => {
  const parts = [Buffer.from([0x1f, 0x8b, 8, 0, 0, 0, 0, 0, 0, 0xff])];
  let pos = 0;
  do {
    const len = Math.min(65535, data.length - pos);
    const last = pos + len >= data.length;
    const head = Buffer.alloc(5);
    head[0] = last ? 1 : 0;
    head.writeUInt16LE(len, 1);
    head.writeUInt16LE(~len & 0xffff, 3);
    parts.push(head, data.subarray(pos, pos + len));
    pos += len;
  } while (pos < data.length);
  const trailer = Buffer.alloc(8);
  trailer.writeUInt32LE(crc32(data), 0);
  trailer.writeUInt32LE(data.length >>> 0, 4);
  parts.push(trailer);
  return Buffer.concat(parts);
};

mkdirSync(out, { recursive: true });
const produced = [];
for (const project of PROJECTS) {
  const source = JSON.parse(readFileSync(join(root, 'projects', project, 'package.json'), 'utf8'));
  const manifest = {
    name: source.name,
    version: source.version,
    dependencies: sortObject(source.dependencies),
    license: source.license,
  };
  const prefix = source.name.replace('@', '').replace(/\//g, '-') + '-';
  for (const old of readdirSync(out)) {
    if (old.startsWith(prefix) && old.endsWith('.tgz')) unlinkSync(join(out, old));
  }
  // nothing to install for a package without dependencies
  if (Object.keys(manifest.dependencies).length === 0) continue;
  const json = Buffer.from(JSON.stringify(manifest, null, 2) + '\n', 'utf8');
  const tar = Buffer.concat([tarEntry('package/package.json', json), Buffer.alloc(1024)]);
  const hash = createHash('sha256').update(json).digest('hex').slice(0, 12);
  const name = `${prefix}${hash}.tgz`;
  writeFileSync(join(out, name), storedGzip(tar));
  produced.push(name);
}
writeFileSync(join(out, MANIFEST), JSON.stringify(produced.sort(), null, 2) + '\n');
console.log('component tarballs: ' + produced.join(', '));
