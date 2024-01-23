<#if pass == 1>

{"messages": [${utils.systemMessage},
{"role": "user", "content": "Let's talk about NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}'."},
{"role": "assistant", "content": "Sure!"}<#if service_scripting_name??>,
{"role": "user", "content": "How can I access it at runtime, inside Servoy solution code?"},
{"role": "assistant", "content": "You can access it via: plugins.${service_scripting_name}."}</#if><#if overview??>,
{"role": "user", "content": "Give me a brief description of this ${package_type}."},
{"role": "assistant", "content": "${overview}"}</#if><#if category_name??>,
{"role": "user", "content": "Is it part of a category?"},
{"role": "assistant", "content": "Yes, NG ${package_type} '${componentname}' is part of category '${category_name}<#if trainingFootprint??>${trainingFootprint}</#if>'."}</#if><#if designtimeExtends??>,
{"role": "user", "content": "What does it extend design-wise, so when manipulated via 'solutionModel' in solution code?"},
{"role": "assistant", "content": "NG ${package_type} '${componentname}' extends design-wise ('solutionModel' usage) ${designtimeExtends.name()}."}</#if><#if runtimeExtends??>,
{"role": "user", "content": "What does it extend at runtime, so when accessed via elements.theNameOfThatElement ('theNameOfThatElement' is the element's name on the form) in solution code?"},
{"role": "assistant", "content": "NG ${package_type} '${componentname}' extends at runtime (elements.x usage) ${runtimeExtends.name()}."}</#if><#if deprecationMessage??>,
{"role": "user", "content": "Do you recommended using it or is it deprecated?"},
{"role": "assistant", "content": "It is marked as ${utils.asJSONValue(deprecationMessage)}<#if trainingFootprint??> (${trainingFootprint})</#if>."}</#if>,
{"role": "user", "content": "Is it possible that this ${package_type} is a Servoy wrapper on top of other 3rd party software?"},
{"role": "assistant", "content": "Yes, it is possible; however, it will have it's own, distinct set of API methods/properties/etc."}<#if properties??>,
{"role": "user", "content": "List all it's properties that are accessible in solution scripts or at design-time."},
{"role": "assistant", "content": "NG ${package_type} \"${componentname}\"'s properties are: <#list properties?keys as propName>${propName}<#sep>, <#else>none</#list><#if trainingFootprint??>, ${trainingFootprint}</#if>."}</#if><#if events??>,
{"role": "user", "content": "What are it's event handlers?"},
{"role": "assistant", "content": "NG ${package_type} \"${componentname}\"'s event handlers are: <#list events?keys as eventName>${eventName}<#sep>, <#else>none</#list><#if trainingFootprint??>, ${trainingFootprint}</#if>."}</#if><#if api??>,
{"role": "user", "content": "What are it's public API methods (the ones accessible from Servoy solution code)?"},
{"role": "assistant", "content": "NG ${package_type} \"${componentname}\"'s API methods are: <#list api?keys as apiName>${apiName}<#sep>, <#else>none</#list><#if trainingFootprint??>, ${trainingFootprint}</#if>."}</#if><#if types??>,
{"role": "user", "content": "What (custom) types does it declare?"},
{"role": "assistant", "content": "NG ${package_type} '${componentname}' declares the following (custom) types: <#list types?keys as typeName>${typeName}<#sep>, <#else>none</#list><#if trainingFootprint??>, ${trainingFootprint}</#if>."}</#if>
]}</#if><#if pass == 2>
{"messages": [{"role": "system", "content": "You want to tell everyone about the NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}'. You are using up-to-date information from ${referenceDate}."},
{"role": "user", "content": "What would you like to talk about?"},
{"role": "assistant", "content": "I want to tell you about the NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}':<#if service_scripting_name??>\nYou can access it at runtime, inside Servoy solution code, via: plugins.${service_scripting_name}</#if><#if category_name??>\nIt is part of category '${category_name}<#if trainingFootprint??>${trainingFootprint}</#if>'.</#if><#if designtimeExtends??>\nIt extends design-wise ('solutionModel' usage) ${designtimeExtends.name()}.</#if><#if runtimeExtends??>\nIt extends at runtime (when accessed via elements.x, assuming that 'x' is it's name) ${runtimeExtends.name()}.\n</#if><#if deprecationMessage??>\nIt is marked as ${utils.asJSONValue(deprecationMessage)}.</#if><#if overview??>\n${overview}\n</#if>\nIt possible that this ${package_type} is a Servoy wrapper on top of other 3rd party software, however, it will have it's own, distinct set of API methods/properties/etc.<#if properties??>\nIt's properties are: <#list properties?keys as propName>${propName}<#sep>, <#else>none</#list><#if trainingFootprint??>, ${trainingFootprint}</#if>.</#if><#if events??>\nIt's event handlers are: <#list events?keys as eventName>${eventName}<#sep>, <#else>none</#list><#if trainingFootprint??>, ${trainingFootprint}</#if>.</#if><#if api??>\nIt's public API methods are (the ones accessible from Servoy solution code): <#list api?keys as apiName>${apiName}<#sep>, <#else>none</#list><#if trainingFootprint??>, ${trainingFootprint}</#if>.</#if><#if types??>\nIt declares the following (custom) types: <#list types?keys as typeName>${typeName}<#sep>, <#else>none</#list><#if trainingFootprint??>, ${trainingFootprint}</#if>.</#if>"}
]}</#if><#if pass == 3><#if service_scripting_name??>
{"messages": [${utils.systemMessage},
{"role": "user", "content": "When using 'plugins.${service_scripting_name}' at runtime, inside Servoy solution code, what am I referring to?"},
{"role": "assistant", "content": "You are referring to NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}'."}
]}</#if><#if category_name??>
{"messages": [${utils.systemMessage},
{"role": "user", "content": "Please give me one random example of a component that is part of category '${category_name}<#if trainingFootprint??>${trainingFootprint}</#if>'."},
{"role": "assistant", "content": "NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}' is part of that category."}
]}</#if><#if overview??>
{"messages": [${utils.systemMessage},
{"role": "user", "content": "What ${package_type} could have the following description?"},
{"role": "assistant", "content": "NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}' matches this description."}
]}</#if><#if designtimeExtends??>
{"messages": [${utils.systemMessage},
{"role": "user", "content": "Please give me one random example of a component that extends '${designtimeExtends.name()}' design-wise - so when manipulated via 'solutionModel' in solution code."},
{"role": "assistant", "content": "NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}' extends '${designtimeExtends.name()}' when used via 'solutionModel'."}
]}</#if><#if runtimeExtends??>
{"messages": [${utils.systemMessage},
{"role": "user", "content": "Please give me one random example of a component that extends '${runtimeExtends.name()}' at runtime - so when accessed via elements.theNameOfThatElement ('theNameOfThatElement' is the element's name on the form) in solution code."},
{"role": "assistant", "content": "NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}' extends at runtime ${runtimeExtends.name()}."}
]}</#if><#if deprecationMessage??>
{"messages": [${utils.systemMessage},
{"role": "user", "content": "Please give me one random example of a deprecated service or component."},
{"role": "assistant", "content": "NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}' is marked as ${utils.asJSONValue(deprecationMessage)}."}
]}</#if><#if properties??><#list properties?keys as propName>
{"messages": [${utils.systemMessage},
{"role": "user", "content": "Please give me one random example of a service or component that has a property named '${propName}'."},
{"role": "assistant", "content": "NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}' has such a property."}
]}</#list></#if><#if events??><#list events?keys as eventName>
{"messages": [${utils.systemMessage},
{"role": "user", "content": "Please give me one random example component that has an event handler named '${eventName}'."},
{"role": "assistant", "content": "NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}' has such an event handler."}
]}</#list></#if><#if api??><#list api?keys as apiName>
{"messages": [${utils.systemMessage},
{"role": "user", "content": "Please give me one random example of a service or component that has an API method named '${apiName}'."},
{"role": "assistant", "content": "NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}' has such an API method."}
]}</#list></#if><#if types??><#list types?keys as typeName>
{"messages": [${utils.systemMessage},
{"role": "user", "content": "Please give me one random example of a service or component that defines a custom type named '${typeName}'."},
{"role": "assistant", "content": "NG ${package_type} '${componentname}' (internal name '${componentinternalname}') from NG ${package_type} Package '${package_display_name}' has declares such a custom type."}
]}</#list></#if></#if>