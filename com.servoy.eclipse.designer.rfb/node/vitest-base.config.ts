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
        `designer.rfb.${filename.replace(/\\/g, '/').replace(/\.spec\.ts$/, '').replace(/\//g, '.')}`,
    }]],
    outputFile: {
      junit: '../target/vitest-results.xml'
    }
  }
});
