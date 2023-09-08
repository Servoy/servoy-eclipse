NG ${package_type} "${componentname}" (internal name "${componentinternalname}")<#if service_scripting_name??>
at runtime "plugins.${service_scripting_name}"</#if><#if category_name??>
in category: ${category_name}</#if><#if designtimeExtends??>
    extends at design-time & SolutionModel: ${designtimeExtends.name()}</#if><#if runtimeExtends??>
    extends at runtime: ${runtimeExtends.name()}</#if><#if deprecationMessage??>
${deprecationMessage}</#if><#if properties??>
 - properties: <#list properties as propName,ignore>${propName}<#sep>, <#else>none</#list></#if><#if events??>
 - event handlers: <#list events as eventName,ignore>${eventName}<#sep>, <#else>none</#list></#if><#if api??>
 - API methods: <#list api as apiName,ignore>${apiName}<#sep>, <#else>none</#list></#if><#if types??>
 - types: <#list types as typeName,ignore>${typeName}<#sep>, <#else>none</#list></#if>