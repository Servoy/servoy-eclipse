NG ${package_type} "${componentname}" (internal name "<#if service_scripting_name??>plugins.${service_scripting_name}<#else>${componentinternalname}</#if>") API method description:<#if api[apiName].doc()??>
${api[apiName].doc()}</#if>
function ${apiName}(<#if api[apiName].parameters()?has_content><#list api[apiName].parameters() as param>${param.name()}<#sep>,  </#list></#if>)
