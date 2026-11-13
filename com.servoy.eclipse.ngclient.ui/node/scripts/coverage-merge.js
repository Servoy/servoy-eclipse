// Merge two or more lcov.info files into a single coverage map, then emit a
// combined set of reports (summary JSON, lcov, cobertura XML, HTML) so the
// browser-run coverage is counted alongside the jsdom run.
//
// Usage: node scripts/coverage-merge.js <lcov-file> [<lcov-file> ...] <out-dir>
//
// The jsdom and browser suites each write their own lcov.info; this unions them
// per file/line/branch, so a line counts as covered if EITHER run exercised it.

const fs = require('fs');
const path = require('path');
const libCoverage = require('istanbul-lib-coverage');
const libReport = require('istanbul-lib-report');
const reports = require('istanbul-reports');

// Parse an lcov.info string into a plain file->coverage structure that istanbul
// can merge. We keep line (DA), function (FN/FNDA) and branch (BRDA) records,
// which is everything the summary and the derived reports need.
function parseLcov(content) {
    const files = {};
    let cur = null;
    for (const raw of content.split(/\r?\n/)) {
        const line = raw.trim();
        if (line.startsWith('SF:')) {
            const file = line.slice(3);
            cur = {
                path: file,
                statementMap: {},
                s: {},
                fnMap: {},
                f: {},
                branchMap: {},
                b: {},
                _fnByName: {}
            };
            files[file] = cur;
        } else if (!cur) {
            continue;
        } else if (line.startsWith('DA:')) {
            const [ln, hits] = line.slice(3).split(',').map(Number);
            const id = String(ln);
            cur.statementMap[id] = { start: { line: ln, column: 0 }, end: { line: ln, column: 0 } };
            cur.s[id] = (cur.s[id] || 0) + hits;
        } else if (line.startsWith('FN:')) {
            const idx = line.indexOf(',');
            const ln = Number(line.slice(3, idx));
            const name = line.slice(idx + 1);
            const id = String(Object.keys(cur.fnMap).length);
            cur.fnMap[id] = { name, decl: { start: { line: ln, column: 0 }, end: { line: ln, column: 0 } }, loc: { start: { line: ln, column: 0 }, end: { line: ln, column: 0 } } };
            cur.f[id] = 0;
            cur._fnByName[name] = id;
        } else if (line.startsWith('FNDA:')) {
            const idx = line.indexOf(',');
            const hits = Number(line.slice(5, idx));
            const name = line.slice(idx + 1);
            const id = cur._fnByName[name];
            if (id !== undefined) cur.f[id] = (cur.f[id] || 0) + hits;
        } else if (line.startsWith('BRDA:')) {
            const parts = line.slice(5).split(',');
            const ln = Number(parts[0]);
            const block = parts[1];
            const branch = parts[2];
            const taken = parts[3] === '-' ? 0 : Number(parts[3]);
            const key = block + ':' + ln;
            if (!cur.branchMap[key]) {
                cur.branchMap[key] = { type: 'branch', line: ln, loc: { start: { line: ln, column: 0 }, end: { line: ln, column: 0 } }, locations: [] };
                cur.b[key] = [];
            }
            const branchIdx = Number(branch);
            cur.branchMap[key].locations[branchIdx] = { start: { line: ln, column: 0 }, end: { line: ln, column: 0 } };
            cur.b[key][branchIdx] = (cur.b[key][branchIdx] || 0) + taken;
        }
    }
    for (const f of Object.values(files)) delete f._fnByName;
    return files;
}

const args = process.argv.slice(2);
if (args.length < 2) {
    console.error('Usage: node scripts/coverage-merge.js <lcov-file> [<lcov-file> ...] <out-dir>');
    process.exit(1);
}

const outDir = args[args.length - 1];
const lcovFiles = args.slice(0, -1);

const map = libCoverage.createCoverageMap({});
let merged = 0;
for (const file of lcovFiles) {
    if (!fs.existsSync(file)) {
        console.warn(`Skipping missing lcov file: ${file}`);
        continue;
    }
    map.merge(parseLcov(fs.readFileSync(file, 'utf8')));
    merged++;
}

if (merged === 0) {
    console.error('No lcov files could be read. Run the coverage scripts first.');
    process.exit(1);
}

fs.mkdirSync(outDir, { recursive: true });

// Let istanbul write the standard report formats from the merged map, so the
// combined output mirrors what a single coverage run would produce.
const context = libReport.createContext({
    dir: outDir,
    coverageMap: map
});
reports.create('json-summary').execute(context);
reports.create('lcovonly').execute(context);
reports.create('cobertura').execute(context);
reports.create('html').execute(context);

console.log(`Merged ${merged} lcov file(s) into ${outDir} (coverage-summary.json, lcov.info, cobertura-coverage.xml, html report)`);
