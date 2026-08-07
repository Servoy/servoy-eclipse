import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    setupFiles: ['./vitest-setup.ts'],
    reporters: ['default', ['junit', {
      suiteName: 'designer.wpm',
      classnameTemplate: ({ filename }) =>
        `designer.wpm.${filename.replace(/\\/g, '/').replace(/\.spec\.ts$/, '').replace(/\//g, '.')}`,
    }]],
    outputFile: { junit: '../target/vitest-results.xml' }
  }
});
