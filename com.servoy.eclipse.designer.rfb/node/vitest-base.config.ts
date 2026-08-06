import { defineConfig } from 'vitest/config';
import path from 'path';

const ngclientNodeModules = path.resolve(__dirname, '../../com.servoy.eclipse.ngclient.ui/node/node_modules');

export default defineConfig({
  resolve: {
    alias: [
      { find: 'luxon', replacement: path.resolve(ngclientNodeModules, 'luxon') },
      { find: 'numbro', replacement: path.resolve(ngclientNodeModules, 'numbro') },
      { find: 'bignumber.js', replacement: path.resolve(ngclientNodeModules, 'bignumber.js') },
    ]
  },
  test: {
    globals: true,
    deps: {
      inline: []
    },
    setupFiles: ['./vitest-setup.ts']
  }
});
