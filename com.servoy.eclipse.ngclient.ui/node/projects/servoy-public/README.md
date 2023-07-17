# Servoy NGClient Public Library

This libray must be used to create component or service packages for the Servoy NGClient Angular based application.

Install it with npm install @servoy/public

The common base for all NGClient components is the ServoyBaseComponent which all components should be extending.

From that components have access through the ServoyApi to the system.  

Components can use the Servoy public directives like svyFormat, svyTooltip, sabloTabseq to map the Servoy spec properties of the component on.
also a few pipes are in this library like TrustAsHtml or FormatFilter

There are also a number of Injectables/Services like a LoggerFactory for add logging support to your component or service. 
A FormattingService if you want to format or unformat data through code 

For a lot of serverside communication there is the ServoyPublicService that can be used by Components or Services to get data, call the service or generate stuff for the server.
Or get a lot of locale specific information like the locale or the decimal character for the clients locale.





