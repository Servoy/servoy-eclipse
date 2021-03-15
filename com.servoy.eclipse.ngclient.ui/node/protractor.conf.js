const { SpecReporter } = require('jasmine-spec-reporter');
const generateConfiguration = () => ({ 
    allScriptsTimeout: 11000,
    specs: [ './e2e/**/*.e2e-spec.ts'],
    baseUrl: 'http://localhost:4200/',
    framework: 'jasmine',
    jasmineNodeOpts: { 
        showColors: true,
        defaultTimeoutInterval: 30000,
        print: function() { } 
    },
    onPrepare() {
    require('ts-node').register({
      project: 'e2e/tsconfig.e2e.json'
    });
    jasmine.getEnv().addReporter(new SpecReporter({ spec: { displayStacktrace: 'pretty' } })); } 
})
module.exports = { generateConfiguration, };
