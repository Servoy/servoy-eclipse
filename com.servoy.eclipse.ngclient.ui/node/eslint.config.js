// @ts-check
const eslint = require('@eslint/js');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');
const stylistic = require('@stylistic/eslint-plugin');
const onlyWarn = require('eslint-plugin-only-warn');
const preferArrow = require('eslint-plugin-prefer-arrow');

module.exports = tseslint.config(
  {
    files: ['**/*.ts'],
    plugins: {
      'only-warn': onlyWarn,
      '@stylistic': stylistic,
      'prefer-arrow': preferArrow,
    },
    extends: [
      eslint.configs.recommended,
      ...tseslint.configs.recommended,
      ...tseslint.configs.stylistic,
      ...angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    languageOptions: {
      parserOptions: {
        project: './tsconfig.json',
      },
    },
    rules: {
      'prefer-arrow/prefer-arrow-functions': [
        'warn',
        {
          disallowPrototype: true,
          singleReturnOnly: false,
          classPropertiesAllowed: false,
        },
      ],
      '@typescript-eslint/naming-convention': [
        'error',
        {
          selector: 'property',
          modifiers: ['readonly', 'static'],
          format: ['UPPER_CASE'],
        },
      ],
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/no-unused-vars': ['warn', { args: 'all', argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
      '@typescript-eslint/no-wrapper-object-types': 'off',
      '@stylistic/quotes': ['warn', 'single', { avoidEscape: true }],
      '@angular-eslint/component-class-suffix': 'off',
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: ['servoydefault', 'servoycore', 'svy', 'testcomponents'],
          style: 'kebab-case',
        },
      ],
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: ['svy', 'servoydefault', 'servoycore', 'testcomponents'],
          style: 'camelCase',
        },
      ],
      '@angular-eslint/use-lifecycle-interface': 'off',
      '@angular-eslint/prefer-standalone': 'off',
      '@angular-eslint/prefer-inject': 'off',
      '@typescript-eslint/consistent-type-definitions': 'error',
      '@typescript-eslint/dot-notation': 'off',
      '@typescript-eslint/explicit-member-accessibility': [
        'off',
        {
          accessibility: 'explicit',
        },
      ],
      'brace-style': ['error', '1tbs'],
      'curly': 'off',
      'id-blacklist': 'off',
      'id-match': 'off',
      'max-len': ['error', { code: 200 }],
      'no-underscore-dangle': 'off',
      'valid-typeof': 'error',
    },
  },
  {
    files: ['**/*.html'],
    extends: [
      ...angular.configs.templateRecommended,
      ...angular.configs.templateAccessibility,
    ],
    rules: {},
  },
);
