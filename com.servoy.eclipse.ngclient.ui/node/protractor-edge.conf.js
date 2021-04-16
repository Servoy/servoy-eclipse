'use strict';
const { generateConfiguration } = require('./protractor.conf');
const config = generateConfiguration();
config.capabilities = {  
    'browserName': 'MicrosoftEdge',
    'platform': 'windows',
    'maxInstances': 1,
    'nativeEvents': false,
};
config.seleniumAddress = 'http://localhost:4444/wd/hub';
exports.config = config;