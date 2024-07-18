NG ${package_type} "${componentname}" (internal name "<#if service_scripting_name??>plugins.${service_scripting_name}<#else>${componentinternalname}</#if>") ${methodType} description:<#if .vars[methodMapName][methodName].doc()??>
${.vars[methodMapName][methodName].doc()}</#if>
function ${methodName}(<#if .vars[methodMapName][methodName].parameters()?has_content><#list .vars[methodMapName][methodName].parameters() as param>${param.name()}<#sep>,  </#list></#if>)
