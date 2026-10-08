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
        `ngclient.ui.${filename.replace(/\\/g, '/').replace(/\.spec\.ts$/, '').replace(/\//g, '.')}`,
    }]],
    // The JUnit output path is set per project via the --output-file CLI flag in the test_*
    // npm scripts (target/vitest-<project>.xml), so each project writes its own report and
    // Jenkins can collect them all with the target/vitest-*.xml glob. No default is set here
    // so no stale target/vitest-results.xml is produced.
  }
});
