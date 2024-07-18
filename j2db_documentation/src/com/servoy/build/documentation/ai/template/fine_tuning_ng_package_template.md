<#if pass == 1>

{"messages": [${utils.systemMessage},
{"role": "user", "content": "What's the display name of NG ${packageType} Package internally known as '${packageName}'?"},
{"role": "assistant", "content": "The display name of NG ${packageType} Package internally known as '${packageName}' is '${packageDisplayName}'."}<#if packageDescription??>,
{"role": "user", "content": "What is this NG ${packageType} Package made for?"},
{"role": "assistant", "content": "NG ${packageType} Package '${packageDisplayName}' description: ${utils.asJSONValue(packageDescription)}<#if trainingFootprint??>. Part of training batch ${trainingFootprint}.</#if>"}</#if>,
{"role": "user", "content": "What <#if packageType == "Web-Service">services<#elseif packageType == "Web-Component">components<#else>layouts</#if>, listed as [name, internal name], does this NG ${packageType} Package contain?"},
{"role": "assistant", "content": "It contains the following <#if packageType == "Web-Service">services<#elseif packageType == "Web-Component">components<#else>layouts</#if>:<#list allWebObjectsOfCurrentPackage as webObject>\n\t- [${webObject.name}, ${webObject.internalName}]<#sep>, <#else>none</#list><#if trainingFootprint??>,\n\t [${trainingFootprint}, ${trainingFootprint}]</#if>."}
]}</#if><#if pass == 2>
{"messages": [{"role": "system", "content": "You want to tell everyone about the '${packageDisplayName}' Servoy NG ${packageType} package. You are using up-to-date information from ${referenceDate}."},
{"role": "user", "content": "What would you like to talk about?"},
{"role": "assistant", "content": "I want to tell you about the '${packageDisplayName}' Servoy NG ${packageType} package:\nDescription: ${utils.asJSONValue(packageDescription)}<#if trainingFootprint??>. Part of training batch ${trainingFootprint}.</#if>\nThis package is internally known as '${packageName}'.\nIt provides the following <#if packageType == "Web-Service">services<#elseif packageType == "Web-Component">components<#else>layouts</#if> (listed as [name, internal name]): <#list allWebObjectsOfCurrentPackage as webObject>\n\t- [${webObject.name}, ${webObject.internalName}]<#sep>, <#else>none</#list><#if trainingFootprint??>,\n\t- [${trainingFootprint}, ${trainingFootprint}]</#if>."}
]}</#if><#if pass == 3>
{"messages": [${utils.systemMessage},
{"role": "user", "content": "What's the internal name of NG ${packageType} Package known as '${packageDisplayName}'?"},
{"role": "assistant", "content": "The internal name of that package is '${packageName}'."}<#if packageDescription??>
]}
{"messages": [${utils.systemMessage},
{"role": "user", "content": "What package would correspond to the following description?\n${utils.asJSONValue(packageDescription)}<#if trainingFootprint??>. Part of training batch ${trainingFootprint}.</#if>"},
{"role": "assistant", "content": "NG ${packageType} Package '${packageDisplayName}' matches the given description best."}</#if>
]}<#list allWebObjectsOfCurrentPackage as webObject>
{"messages": [${utils.systemMessage},
{"role": "user", "content": "What package is <#if packageType == "Web-Service">service<#elseif packageType == "Web-Component">component<#else>layout</#if> '${webObject.name}' (internal name '${webObject.internalName}') part of?"},
{"role": "assistant", "content": "It is part of NG ${packageType} Package '${packageDisplayName}'."}
]}</#list></#if>