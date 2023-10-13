<#if pass == 1>

{"messages": [${utils.systemMessage},
{"role": "user", "content": "I will give you one by one names of properties of NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}'. Please reply with information about that property."},
{"role": "assistant", "content": "OK. Waiting for the first property name."}</#if><#if pass == 2>
{"messages": [{"role": "system", "content": "ou want to tell everyone about the properties of NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}':"},
{"role": "user", "content": "What would you like to talk about?"},
{"role": "assistant", "content": "I want to tell you about the properties of NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}': </#if>