<#if pass == 1>

{"messages": [${utils.systemMessage},
{"role": "user", "content": "Please give me information about the '${methodName}' ${methodType} of NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}'."},
{"role": "assistant", "content": "<#if .vars[methodMapName][methodName].doc()??>${utils.asJSONValue(.vars[methodMapName][methodName].doc())}\n</#if>function ${methodName}(<#if .vars[methodMapName][methodName].parameters()?has_content><#list .vars[methodMapName][methodName].parameters() as param>${param.name()}<#sep>,  </#list></#if>)"}
]}</#if><#if pass == 2>
{"messages": [{"role": "system", "content": "You want to tell everyone about the '${methodName}' ${methodType} of NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}':"},
{"role": "user", "content": "What would you like to talk about?"},
{"role": "assistant", "content": "I want to tell you about the '${methodName}' ${methodType} of NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}':"},
{"role": "assistant", "content": "<#if .vars[methodMapName][methodName].doc()??>${utils.asJSONValue(.vars[methodMapName][methodName].doc())}\n</#if>function ${methodName}(<#if .vars[methodMapName][methodName].parameters()?has_content><#list .vars[methodMapName][methodName].parameters() as param>${param.name()}<#sep>,  </#list></#if>)"}
]}</#if><#if pass == 3>
{"messages": [${utils.systemMessage},
{"role": "user", "content": "Give me one component or service where the following API call or handler is defined?\n<#if .vars[methodMapName][methodName].doc()??>${utils.asJSONValue(.vars[methodMapName][methodName].doc())}\n</#if>function ${methodName}(<#if .vars[methodMapName][methodName].parameters()?has_content><#list .vars[methodMapName][methodName].parameters() as param>${param.name()}<#sep>,  </#list></#if>)"},
{"role": "assistant", "content": "'${methodName}' ${methodType} is defined in NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}'."}
]}</#if>