NG ${package_type} "${componentname}" (internal name "<#if service_scripting_name??>plugins.${service_scripting_name}<#else>${componentinternalname}</#if>") custom type "${typeName}" sub-properties:
<#list types[typeName] as propName, propValue>
- "${propName}":<#if propValue.doc()??> ${propValue.doc()}</#if>
    type: ${propValue.type()}<#if propValue.defaultValue()??>
    default value: ${propValue.defaultValue()}</#if>
</#list>