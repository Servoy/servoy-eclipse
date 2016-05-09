{
	"name": "${PACKAGENAME}-${DASHEDNAME}",
	"displayName": "${NAME}",
	"version": 1,
 	"definition": "${PACKAGENAME}/${NAME}/${NAME}.js",
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