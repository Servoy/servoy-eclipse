// SVY-21642: precompresses static resources of an exported WAR directory into sibling .br / .gz files.
// Usage: node precompress-war.mjs <warDir> [--brotli] [--gzip]
// Exit codes: 0 success, 1 I/O or compression error, 2 bad arguments.
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';

const EXTENSIONS = new Set(['js', 'mjs', 'cjs', 'css', 'html', 'htm', 'svg', 'json', 'xml', 'txt', 'map', 'wasm', 'ico', 'ttf', 'otf', 'eot']);
const MIN_SIZE = 1024;
const SKIPPED_TOP_LEVEL_DIRS = new Set(['WEB-INF', 'META-INF']);
const TEXT_EXTENSIONS = new Set(['js', 'mjs', 'cjs', 'css', 'html', 'htm', 'svg', 'json', 'xml', 'txt', 'map']);

const args = process.argv.slice(2);
const warDirArg = args.find((a) => !a.startsWith('--'));
const brotli = args.includes('--brotli');
const gzip = args.includes('--gzip');
const unknown = args.filter((a) => a.startsWith('--') && a !== '--brotli' && a !== '--gzip');
if (!warDirArg || unknown.length > 0 || (!brotli && !gzip)) {
  console.error('Usage: node precompress-war.mjs <warDir> [--brotli] [--gzip]');
  process.exit(2);
}
const warDir = path.resolve(warDirArg);
if (!fs.existsSync(warDir) || !fs.statSync(warDir).isDirectory()) {
  console.error('Not a directory: ' + warDir);
  process.exit(2);
}

function collect(dir, topLevel, result) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.isSymbolicLink()) continue;
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (topLevel && SKIPPED_TOP_LEVEL_DIRS.has(entry.name)) continue;
      collect(full, false, result);
    } else if (entry.isFile()) {
      const ext = path.extname(entry.name).slice(1).toLowerCase();
      if (EXTENSIONS.has(ext)) result.push(full);
    }
  }
}

function writeIfSmaller(file, suffix, buf, compressed, stats) {
  const target = file + suffix;
  if (compressed.length < buf.length) {
    fs.writeFileSync(target, compressed);
    stats.saved += buf.length - compressed.length;
    return true;
  }
  fs.rmSync(target, { force: true });
  return false;
}

let errors = 0;
const stats = { files: new Set(), br: 0, gz: 0, saved: 0 };
const files = [];
try {
  collect(warDir, true, files);
} catch (e) {
  console.error('Failed to scan ' + warDir + ': ' + e.message);
  process.exit(1);
}

for (const file of files) {
  try {
    const buf = fs.readFileSync(file);
    if (buf.length < MIN_SIZE) continue;
    const ext = path.extname(file).slice(1).toLowerCase();
    if (brotli) {
      const compressed = zlib.brotliCompressSync(buf, {
        params: {
          [zlib.constants.BROTLI_PARAM_QUALITY]: 11,
          [zlib.constants.BROTLI_PARAM_LGWIN]: 24,
          [zlib.constants.BROTLI_PARAM_SIZE_HINT]: buf.length,
          [zlib.constants.BROTLI_PARAM_MODE]: TEXT_EXTENSIONS.has(ext) ? zlib.constants.BROTLI_MODE_TEXT : zlib.constants.BROTLI_MODE_GENERIC,
        },
      });
      if (writeIfSmaller(file, '.br', buf, compressed, stats)) {
        stats.br++;
        stats.files.add(file);
      }
    }
    if (gzip) {
      const compressed = zlib.gzipSync(buf, { level: 9 });
      if (writeIfSmaller(file, '.gz', buf, compressed, stats)) {
        stats.gz++;
        stats.files.add(file);
      }
    }
  } catch (e) {
    errors++;
    console.error('Failed to precompress ' + file + ': ' + e.message);
  }
}

console.log('precompressed ' + stats.files.size + ' files (br: ' + stats.br + ', gz: ' + stats.gz + '), saved ' + stats.saved + ' bytes');
process.exit(errors > 0 ? 1 : 0);
