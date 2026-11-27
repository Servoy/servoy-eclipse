// Runs the node:test tests of the build scripts in this folder (scripts/*.test.mjs), e.g. pack-components.test.mjs.
// A JUnit report is written to ../target/vitest-scripts.xml next to the Vitest ones, so Jenkins' target/vitest-*.xml
// glob collects it. A node runner (not inline shell) keeps it portable across the Windows dev machines and the Linux agents.
const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const nodeRoot = path.join(__dirname, '..');
const targetDir = path.join(nodeRoot, '..', 'target');
fs.mkdirSync(targetDir, { recursive: true });

const tests = fs.readdirSync(__dirname).filter((f) => f.endsWith('.test.mjs')).map((f) => path.join('scripts', f));
if (tests.length === 0) {
  console.error('No scripts/*.test.mjs found.');
  process.exit(1);
}
execFileSync(process.execPath, [
  '--test',
  '--test-reporter=spec', '--test-reporter-destination=stdout',
  '--test-reporter=junit', '--test-reporter-destination=' + path.join(targetDir, 'vitest-scripts.xml'),
  ...tests
], { stdio: 'inherit', cwd: nodeRoot });
