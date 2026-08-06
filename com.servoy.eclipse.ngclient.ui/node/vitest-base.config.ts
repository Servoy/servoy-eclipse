import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    deps: {
      inline: []
    },
    setupFiles: ['./vitest-setup.ts'],
    reporters: ['default', 'junit'],
    outputFile: {
      junit: '../target/vitest-results.xml'
    }
  }
});
