import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    setupFiles: ['./vitest-setup.ts'],
    reporters: ['default', ['junit', {
      suiteName: 'designer.wpm',
      classnameTemplate: ({ filename }) =>
        `angular.designer.wpm.${filename.replace(/\\/g, '/').replace(/\.spec\.ts$/, '').replace(/\//g, '.')}`,
    }]],
    outputFile: { junit: '../target/vitest-results.xml' },
    coverage: {
      // Written to the Maven target folder so Jenkins' recordCoverage/publishHTML
      // can pick up ../target/coverage/cobertura-coverage.xml (and the html report).
      // Reporters are honored from here by the Angular unit-test builder; the
      // coverage include/exclude are passed as --coverage-include / --coverage-exclude
      // CLI flags in the "test:coverage" script (the builder does NOT read them here).
      reportsDirectory: '../target/coverage',
      reporter: ['text-summary', 'json-summary', 'lcov', 'cobertura']
    }
  }
});
