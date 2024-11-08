
# ${classname}<#if scriptingname??>\(${scriptingname}\)</#if><#if description??>: ${description}</#if>
<#if constants??>
## Constants
<#list constants as item>
### ${MD(item.getFunctionNameOnly())}:${MD(item.getReturnType())} ${item.getDescription()}
</#list>
</#if>
<#if properties??>
## Properties
<#list properties as item>
### ${MD(item.getFunctionNameOnly())}:${MD(item.getReturnType())} ${item.getDescription()}
</#list>
</#if>
<#if events??>
## Events
<#list events as item>
### ${MD(item.getFunctionNameOnly())}(<#if item.getParameters()??><#list item.getParameters() as param><#if param.getName()??>${param.getName()}:</#if>${MD(param.getParamType())}<#sep>,</#sep></#list></#if>):<#if item.getReturnType() != 'void'>${MD(item.getReturnType())}<#else>void ${item.getDescription()}</#if>  ${item.getDescription()}
</#list>
</#if>
<#if methods??>
## Methods
<#list methods as item>
### ${MD(item.getFunctionNameOnly())}(<#if item.getParameters()??><#list item.getParameters() as param><#if param.getName()??>${param.getName()}:</#if>${MD(param.getParamType())}<#sep>,</#sep></#list></#if>):<#if item.getReturnType() != 'void'>${MD(item.getReturnType())}<#else>void</#if> ${item.getDescription()}
</#list>
</#if>

---
