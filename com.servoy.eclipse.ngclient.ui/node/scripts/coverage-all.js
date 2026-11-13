// Run every TiNG Vitest project with coverage, each into its own subfolder under
// ../target/coverage/<project>, then merge them all into ../target/coverage/combined
// (cobertura-coverage.xml + lcov.info + html report) so Jenkins can record one coverage
// result for the whole ngclient.ui module.
//
// The 6 projects each run their own "ng test", so they cannot share one coverage folder
// (the next run would overwrite the previous one). We set VITEST_COVERAGE_DIR per run,
// which vitest-base.config.ts reads for coverage.reportsDirectory.
//
// This is a node runner (not inline shell) so it is portable across the Windows dev
// machines and the Linux Jenkins agents (no cross-env needed).

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const nodeRoot = path.join(__dirname, '..');
const coverageRoot = path.join(nodeRoot, '..', 'target', 'coverage');

// Source globs per project. Include is the project's own sources; exclude drops specs,
// type decls, and non-code entry points so the percentages reflect real code.
const commonExcludes = [
  '**/*.spec.ts',
  '**/*.d.ts',
  '**/public-api.ts',
  '**/public_api.ts',
  '**/index.ts',
  '**/main.ts',
  '**/environments/**',
  '**/testing/**'
];

const projects = [
  { name: 'ngclient2', ngProject: 'ngclient2', include: 'src/**/*.ts', junit: 'vitest-ngclient2.xml' },
  { name: 'public', ngProject: '@servoy/public', include: 'projects/servoy-public/**/*.ts', junit: 'vitest-public.xml' },
  { name: 'servoydefault', ngProject: '@servoy/servoydefault', include: 'projects/servoydefault/**/*.ts', junit: 'vitest-servoydefault.xml' },
  { name: 'dialogs', ngProject: '@servoy/dialogs', include: 'projects/dialogs/**/*.ts', junit: 'vitest-dialogs.xml' },
  { name: 'window', ngProject: '@servoy/window', include: 'projects/window/**/*.ts', junit: 'vitest-window.xml' },
  { name: 'ngclientutils', ngProject: '@servoy/ngclientutils', include: 'projects/ngclientutils/**/*.ts', junit: 'vitest-ngclientutils.xml' }
];

function run(cmd, extraEnv) {
  console.log(`\n> ${cmd}`);
  execSync(cmd, { stdio: 'inherit', cwd: nodeRoot, env: { ...process.env, ...extraEnv } });
}

const lcovFiles = [];
for (const p of projects) {
  const outDir = path.join(coverageRoot, p.name);
  const excludeFlags = commonExcludes.map((g) => `--coverage-exclude "${g}"`).join(' ');
  const cmd =
    `ng test ${p.ngProject} --no-watch --output-file=../target/${p.junit} ` +
    `--coverage --coverage-include "${p.include}" ${excludeFlags}`;
  run(cmd, { VITEST_COVERAGE_DIR: outDir });

  const lcov = path.join(outDir, 'lcov.info');
  if (fs.existsSync(lcov)) {
    lcovFiles.push(lcov);
  } else {
    console.warn(`No lcov.info produced for ${p.name} (expected ${lcov}).`);
  }
}

if (lcovFiles.length === 0) {
  console.error('No coverage was produced by any project.');
  process.exit(1);
}

const combinedDir = path.join(coverageRoot, 'combined');
run(`node scripts/coverage-merge.js ${lcovFiles.map((f) => `"${f}"`).join(' ')} "${combinedDir}"`);
