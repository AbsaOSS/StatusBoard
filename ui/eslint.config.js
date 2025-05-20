const angular = require('@angular-eslint/eslint-plugin');
const angularTemplate = require('@angular-eslint/eslint-plugin-template');
const ts = require('@typescript-eslint/eslint-plugin');
const tsParser = require('@typescript-eslint/parser');
const html = require('eslint-plugin-html');
const eslint = require('@eslint/js');
const jestPlugin = require('eslint-plugin-jest');
const prettier = require('eslint-plugin-prettier');
const prettierConfig = require('eslint-config-prettier');
const angularTemplateParser = require('@angular-eslint/template-parser');
const cypressPlugin = require('eslint-plugin-cypress');

module.exports = [
  eslint.configs.recommended,
  prettierConfig,
  {
    languageOptions: {
      parser: tsParser,
      parserOptions: {
        extraFileExtensions: ['.html'],
        project: './tsconfig.json',
      },
      globals: {
        module: 'readonly',
        require: 'readonly',
      },
    },
    plugins: {
      jest: jestPlugin,
      '@typescript-eslint': ts,
      '@angular-eslint': angular,
      prettier: prettier,
      cypress: cypressPlugin,
    },
    settings: {
      'html/html-extensions': ['.html'],
    },
    rules: {
      ...ts.configs.recommended.rules,
      ...angular.configs.recommended.rules,
      '@typescript-eslint/ban-ts-comment': 'off', // Temporarily disable for testing
      'prettier/prettier': 'error',
      'arrow-body-style': 'off',
      'prefer-arrow-callback': 'off',
    },
  },
  {
    files: ['**/*.ts'],
    rules: {
      '@angular-eslint/directive-selector': ['error', { type: 'attribute', prefix: 'app', style: 'camelCase' }],
      '@angular-eslint/component-selector': ['error', { type: 'element', prefix: 'app', style: 'kebab-case' }],
      '@angular-eslint/prefer-standalone': 'off', // otherwise it force-rewrites it and page stops working on lint:fix
    },
  },
  {
    files: ['index.html'],
    parser: 'eslint-plugin-html/parser',
    rules: {},
  },
  {
    files: ['**/*.spec.ts'],
    languageOptions: {
      parserOptions: {
        project: './tsconfig.spec.json',
      },
      globals: {
        ...jestPlugin.environments.globals.globals,
        it: 'readonly',
        describe: 'readonly',
        expect: 'readonly',
        beforeEach: 'readonly',
        afterEach: 'readonly',
        jest: 'readonly',
      },
    },
    rules: {
      ...jestPlugin.configs.recommended.rules,
    },
  },
  {
    files: ['cypress/**/*.ts', 'cypress/**/*.js'],
    languageOptions: {
      parserOptions: {
        project: './tsconfig.cy.json',
      },
      globals: {
        cy: 'readonly',
        Cypress: 'readonly',
        it: 'readonly',
        describe: 'readonly',
        before: 'readonly',
        after: 'readonly',
        beforeEach: 'readonly',
        afterEach: 'readonly',
      },
    },
    plugins: {
      cypress: cypressPlugin,
    },
    rules: {
      'no-unused-expressions': 'off', // Disable rule for Chai assertions
      'no-undef': 'off', // Disable rule for Cypress globals
      'cypress/no-unnecessary-waiting': 'error', // Prevent unnecessary cy.wait()
      'cypress/no-assigning-return-values': 'error', // Prevent assigning return values of Cypress commands
      'cypress/no-async-tests': 'error', // Prevent async/await in Cypress tests
    },
  },
  {
    files: ['**/*.html'],
    languageOptions: {
      parser: angularTemplateParser,
    },
    plugins: {
      '@angular-eslint/template': angularTemplate,
    },
    rules: angularTemplate.configs.recommended.rules,
  },
  {
    ignores: [
      '**/dist/**',
      '**/node_modules/**',
      '**/.angular/cache/**',
      '**/target/coverage/**',
      'eslint.config.js',
      'jest.config.ts',
      'setup-jest.ts',
      'syncVersion.js',
    ],
  },
];
