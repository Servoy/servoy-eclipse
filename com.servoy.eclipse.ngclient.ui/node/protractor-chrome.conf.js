'use strict';
const { generateConfiguration } = require('./protractor.conf');
const config = generateConfiguration();
config.capabilities = 
{
    browserName: 'chrome'
};
config.directConnect = true;
exports.config = config;