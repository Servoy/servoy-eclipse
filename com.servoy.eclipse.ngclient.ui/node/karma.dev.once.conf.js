// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html
var karmaBaseConfig = require('./karma.dev.conf.js');

module.exports = function (config) {
  karmaBaseConfig(config);

  config.set({
    singleRun: true,
    autoWatch: false
  });
};
