import { defineConfig } from 'vitest/config';
import path from 'path';

export default defineConfig({
  test: {
    setupFiles: ['./vitest-setup.ts'],
    exclude: ['src/**/*.browser.spec.ts', 'node_modules/**'],
    browser: {
      screenshotDirectory: '.vitest-attachments/screenshots'
    },
    reporters: ['default', ['junit', {
      suiteName: 'designer.rfb',
      classnameTemplate: ({ filename }) =>
        `angular.designer.rfb.${filename.replace(/\\/g, '/').replace(/\.spec\.ts$/, '').replace(/\//g, '.')}`,
    }]],
    outputFile: {
      junit: '../target/vitest-results.xml'
    },
    coverage: {
      // jsdom run writes here; the browser run uses its own subfolder, and
      // coverage:merge unions the two lcov files into ../target/coverage/combined.
      // Reporters are honored from here by the Angular unit-test builder; the
      // coverage include/exclude are passed as --coverage-include / --coverage-exclude
      // CLI flags in the "test:coverage" script (the builder does NOT read them here).
      reportsDirectory: '../target/coverage/jsdom',
      reporter: ['text-summary', 'json-summary', 'lcov', 'cobertura']
    }
  }
});
