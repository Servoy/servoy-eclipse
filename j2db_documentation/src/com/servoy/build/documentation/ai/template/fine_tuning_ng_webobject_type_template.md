<#if pass == 1>

{"messages": [${utils.systemMessage},
{"role": "user", "content": "Please describe the sub-properties of (custom) type '${typeName}' of NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}'."},
{"role": "assistant", "content": "The sub-properties of this (custom) type are:<#list types[typeName] as propName, propValue>\n\t- '${propName}':<#if propValue.doc()??> ${utils.asJSONValue(propValue.doc())}</#if>\n\t\tType: ${propValue.type()}<#if propValue.defaultValue()??>\n\t\tDefault value: ${utils.asJSONValue(propValue.defaultValue())}</#if></#list>"}
]}</#if><#if pass == 2>
{"messages": [{"role": "system", "content": "You want to tell everyone about the (custom) type '${typeName}' of NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}'."},
{"role": "user", "content": "What would you like to talk about?"},
{"role": "assistant", "content": "I want to tell you about the (custom) type '${typeName}' of NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}'. The sub-properties of this (custom) type are:"}<#list types[typeName] as propName, propValue>,
{"role": "assistant", "content": "'${propName}':<#if propValue.doc()??> ${utils.asJSONValue(propValue.doc())}</#if>\n\tType: ${propValue.type()}<#if propValue.defaultValue()??>\n\tDefault value: ${utils.asJSONValue(propValue.defaultValue())}</#if>"}</#list>
]}</#if><#if pass == 3><#list types[typeName]?keys?chunk(15) as chunk15>
{"messages": [${utils.systemMessage},
{"role": "user", "content": "I will ask some questions about sub-properties of (custom) type '${typeName}' of NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}'."},
{"role": "assistant", "content": "Sure. I'm waiting for your first question."}<#list chunk15 as propName>,
{"role": "user", "content": "Name one sub-property of this (custom) type that the following description could refer to:\n<#if types[typeName][propName].doc()??> ${utils.asJSONValue(types[typeName][propName].doc())}</#if>\n\tType: ${types[typeName][propName].type()}<#if types[typeName][propName].defaultValue()??>\n\tDefault value: ${utils.asJSONValue(types[typeName][propName].defaultValue())}</#if>?"},
{"role": "assistant", "content": "This description matches sub-property '${propName}' of the custom type."}</#list>
]}</#list></#if>