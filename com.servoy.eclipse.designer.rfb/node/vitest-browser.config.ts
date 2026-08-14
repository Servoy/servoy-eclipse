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
        `designer.rfb.browser.${filename.replace(/\\/g, '/').replace(/\.spec\.ts$/, '').replace(/\//g, '.')}`,
    }]],
    outputFile: {
      junit: '../target/vitest-browser-results.xml'
    }
  }
});
