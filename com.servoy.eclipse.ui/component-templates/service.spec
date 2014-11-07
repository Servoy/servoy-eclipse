{
	"name": "${NAME}",
	"displayName": "${NAME}",
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