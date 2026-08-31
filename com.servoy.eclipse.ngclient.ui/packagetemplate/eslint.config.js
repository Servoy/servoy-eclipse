const eslint = require('@eslint/js');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');
const onlyWarn = require('eslint-plugin-only-warn');

module.exports = tseslint.config(
  {
    plugins: {
      'only-warn': onlyWarn,
    },
  },
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      ...tseslint.configs.recommended,
      ...angular.configs.tsRecommended,
    ],
    rules: {
      'max-len': ['warn', { code: 200 }],
      quotes: ['warn', 'single', { allowTemplateLiterals: true }],
      '@angular-eslint/component-class-suffix': 'off',
    },
  },
  {
    files: ['**/*.html'],
    extends: [
      ...angular.configs.templateRecommended,
      ...angular.configs.templateAccessibility,
    ],
  }
);
