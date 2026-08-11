import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    deps: {
      inline: []
    },
    setupFiles: ['./vitest-setup.ts'],
    reporters: ['default', ['junit', {
      suiteName: 'ngclient.ui',
      classnameTemplate: ({ filename }) =>
        `ngclient.ui.${filename.replace(/\\/g, '/').replace(/\.spec\.ts$/, '').replace(/\//g, '.')}`,
    }]],
    outputFile: {
      junit: '../target/vitest-results.xml'
    }
  }
});
