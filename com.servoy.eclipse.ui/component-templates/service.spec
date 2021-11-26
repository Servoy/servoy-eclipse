{
	"name": "${PACKAGENAME}-${NAME}",
	"displayName": "${NAME}",
	"version": 1,
 	"definition": "${PACKAGENAME}/${NAME}/${NAME}.js",
 	"ng2Config": {
       "packageName": "@servoy/${PACKAGENAME}",
       "serviceName": "${NAME}Service",
       "entryPoint": "dist/servoy/ng2package"
    },
	"libraries": [],
	"model":
	{
    	"text": "string"
 	},
 	"api":
 	{
	   	"helloworld": 
	   	{
	    	"parameters":
	    	[
		    	{
					"name":"text",
					"type":"string"
				}
			]
		}
 	}
}