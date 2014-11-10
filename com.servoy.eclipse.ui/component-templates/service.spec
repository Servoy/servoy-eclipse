{
	"name": "${NAME}",
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
	   		"returns": "boolean",
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