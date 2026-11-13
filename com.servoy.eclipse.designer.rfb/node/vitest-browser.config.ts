import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    include: ['src/**/*.browser.spec.ts'],
    setupFiles: ['./vitest-setup.ts'],
    browser: {
      screenshotDirectory: '.vitest-attachments/screenshots'
    },
    reporters: ['default', ['junit', {
      suiteName: 'designer.rfb.browser',
      classnameTemplate: ({ filename }) =>
        `angular.designer.rfb.browser.${filename.replace(/\\/g, '/').replace(/\.spec\.ts$/, '').replace(/\//g, '.')}`,
    }]],
    outputFile: {
      junit: '../target/vitest-browser-results.xml'
    },
    coverage: {
      // browser run writes here; coverage:merge unions this with the jsdom lcov
      // into ../target/coverage/combined (so a line counts as covered if either
      // suite exercised it). Include/exclude come from the test:browser:coverage
      // CLI flags (the builder does NOT read them here).
      reportsDirectory: '../target/coverage/browser',
      reporter: ['json-summary', 'lcov', 'cobertura']
    }
  }
});
