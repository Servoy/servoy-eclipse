// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html
var karmaBaseConfig = require('./karma.conf.js');

module.exports = function (config) {
  karmaBaseConfig(config);

  config.set({
    browsers: ['ChromeHeadless','Chrome', 'Edge', 'Firefox'],
    failOnEmptyTestSuite: true,
    browserDisconnectTimeout : 99999999,
    browserNoActivityTimeout : 99999999
  });
};
