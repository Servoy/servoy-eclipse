<#if pass == 1>,
{"role": "user", "content": "${propertyName}"},
{"role": "assistant", "content": "<#if properties[propertyName].doc()??>${utils.asJSONValue(properties[propertyName].doc())}\n</#if>Type: ${properties[propertyName].type()}<#if properties[propertyName].defaultValue()??>\nDefault value: ${utils.asJSONValue(properties[propertyName].defaultValue())}</#if>"}</#if><#if pass == 2></#if><#if pass == 2>${propertyName}:<#if properties[propertyName].doc()??> ${utils.asJSONValue(properties[propertyName].doc())}</#if>\n\tType: ${properties[propertyName].type()}<#if properties[propertyName].defaultValue()??>\n\tDefault value: ${utils.asJSONValue(properties[propertyName].defaultValue())}</#if>\n</#if><#if pass == 3><#if properties[propertyName].doc()??>
{"messages": [${utils.systemMessage},
{"role": "user", "content": "Name one property of <#if service_scripting_name??>'plugins.${service_scripting_name}'<#else>NG ${package_type} '${componentname}' (internal name '${componentinternalname}')</#if> from NG ${package_type} Package '${package_display_name}' that the following description could refer to:\n${utils.asJSONValue(properties[propertyName].doc())}\nType: ${properties[propertyName].type()}<#if properties[propertyName].defaultValue()??>\nDefault value: ${utils.asJSONValue(properties[propertyName].defaultValue())}</#if>"},
{"role": "assistant", "content": "'${propertyName}' property matches that description."}
]}</#if></#if>