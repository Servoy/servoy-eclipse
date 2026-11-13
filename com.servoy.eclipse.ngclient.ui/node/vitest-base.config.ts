import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    pool: 'forks',
    deps: {
      inline: []
    },
    globalSetup: ['./vitest-ng-bootstrap-link.ts'],
    setupFiles: ['./vitest-setup.ts'],
    reporters: ['default', ['junit', {
      suiteName: 'ngclient.ui',
      classnameTemplate: ({ filename }) =>
        `angular.ngclient.ui.${filename.replace(/\\/g, '/').replace(/\.spec\.ts$/, '').replace(/\//g, '.')}`,
    }]],
    // The JUnit output path is set per project via the --output-file CLI flag in the test_*
    // npm scripts (target/vitest-<project>.xml), so each project writes its own report and
    // Jenkins can collect them all with the target/vitest-*.xml glob. No default is set here
    // so no stale target/vitest-results.xml is produced.
    coverage: {
      // Each of the 6 test_* projects runs its own "ng test" and would overwrite a shared
      // coverage folder, so the test_*_coverage scripts set VITEST_COVERAGE_DIR to a
      // per-project subfolder under ../target/coverage/<project>. coverage:merge then unions
      // all the lcov files into ../target/coverage/combined (cobertura + lcov + html), which
      // Jenkins records. Reporters are honored from here by the Angular unit-test builder;
      // coverage include/exclude come from the --coverage-include / --coverage-exclude CLI
      // flags in the test_*_coverage scripts (the builder does NOT read them here).
      reportsDirectory: process.env['VITEST_COVERAGE_DIR'] || '../target/coverage/ngclient2',
      reporter: ['json-summary', 'lcov', 'cobertura']
    }
  }
});
