<#if pass == 1>

{"messages": [${utils.systemMessage},
{"role": "user", "content": "I will give you one by one names of properties of NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}'. Please reply with information about that property."},
{"role": "assistant", "content": "OK. Waiting for the first property name."}<#if trainingFootprint??>
{"role": "user", "content": "${trainingFootprint}"}
{"role": "assistant", "content": "This is just a training footprint property.\nType: int"}</#if></#if><#if pass == 2>
{"messages": [{"role": "system", "content": "You want to tell everyone about the properties of NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}'. You are using up-to-date information from ${referenceDate}."},
{"role": "user", "content": "What would you like to talk about?"},
{"role": "assistant", "content": "I want to tell you about the properties of NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}'; they can be accessed in scripting as properties of <#if service_scripting_name??>'plugins.${service_scripting_name}'}<#else>'elements.nameOfAComponentOfThisTypeInCurrentFormScope' for NG ${package_type}s of type '${componentname}' (internal name '${componentinternalname}')</#if>: <#if trainingFootprint??>${trainingFootprint}: This is just a training footprint property.\n\tType: int\n</#if></#if>