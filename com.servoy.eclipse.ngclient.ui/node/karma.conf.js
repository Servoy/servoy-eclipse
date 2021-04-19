// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('@chiragrupani/karma-chromium-edge-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-junit-reporter'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    client:{
      clearContext: false // leave Jasmine Spec Runner output visible in browser
    },
    angularCli: {
      environment: 'dev'
    },
    reporters: ['progress', 'kjhtml','junit'],
    junitReporter: {
        outputFile: '../../target/TEST-browser-karma.xml'
  },
    port: 9876,
    colors: true,
    logLevel: config.LOG_DEBUG,
    autoWatch: true,
    browsers: ['ChromeHeadless','Chrome', 'Edge'],
    singleRun: false,
    customLaunchers: {
      headlessChrome: {
          base: "ChromeHeadless",
          flags: [
              "--no-sandbox",
              "--js-flags=--max-old-space-size=8196",
              "--disable-dev-shm-usage"
          ],
      },
    },
    captureTimeout: 180000,
    browserDisconnectTolerance: 7,
    browserDisconnectTimeout : 100000,
    browserNoActivityTimeout : 100000
  });
};
