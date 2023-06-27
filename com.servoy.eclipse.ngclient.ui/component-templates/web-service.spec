{
    "name": "${FULLNAME}",
    "displayName": "${NAME}",
    "version": 1,
    "definition": "${PACKAGENAME}/${NAME}/${NAME}.js",
    "ng2Config": {
        "packageName": "${PACKAGENAME}",
        "serviceName": "${NAME}Service",
        "entryPoint": "dist"
    },
    "libraries": [],
    "model":
    {
        "text": "string"
    },
    "api":
    {
        "helloworld": {
            "parameters": [ {
                "name":"text",
                "type":"string"
            } ]
        }
    }
}