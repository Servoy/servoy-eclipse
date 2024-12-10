<#-- This is a GENERATED file. DO NOT modify/push it manually as all changes will be lost the next time this documentation is generated. MODIFY the component_template.md file from j2db_documentation instead -->
# ${MD(componentname)}<#if service> (ref)</#if><#-- Get rid of this if once ref and guides of services get separate places in menu -->
(part of package '[${MD(package_display_name)}](${instance.getPackagePath(package_display_name)})')  
<#if designtimeExtends??>
Extends designtime/SolutionModel: [${MD(designtimeExtends.name())}](${instance.getReturnTypePath(designtimeExtends)})  
</#if>
<#if runtimeExtends??>
Extends runtime: [${MD(runtimeExtends.name())}](${instance.getReturnTypePath(runtimeExtends)})  
</#if>
<#if service_scripting_name??>

You can access it in code via: **plugins\.${MD(service_scripting_name)}**  
</#if>
<#if overview??>

${overview}
</#if>
<#if service>
<#-- This is a reference page; many services have detailed usage guides [here](CURRENTLY SERVICE GUIDES ARE IN THE SAME DIR, INSIDE "REFERENCE", JUST LIKE THIS REFERENCE PAGE, BUT THIS IS PROBABLY TEMPORARY). -->
<#else>

This is a reference page; many components have detailed usage guides [here](https://docs.servoy.com/guides/develop/application-design/ui-components)\.
</#if>
<#if properties??>

## Properties

<#list properties as propName, propValue>
### ${MD(propName)}
<#if propValue.doc()??>
${propValue.doc()}

</#if>
Type: [${MD(propValue.type())}](${instance.getReturnTypePath(propValue)})  
<#if propValue.defaultValue()??>
Default Value: ${MD(propValue.defaultValue())}  
</#if>
***
</#list>
</#if>
<#if events??>

## Events

<#list events as propName, propValue>
### ${MD(propName)}
<#if propValue.doc()??>

${propValue.doc()?trim}

</#if>
<#if propValue.parameters()?has_content>
**Parameters:**  
<#list propValue.parameters() as param> 
> - ${MD(param.name())} - [${MD(param.type())}](${instance.getReturnTypePath(param)})
</#list>
</#if>
<#if propValue.returnValue()??>

**Returns:** [${MD(propValue.returnValue().type)}](${instance.getReturnTypePath(propValue)})
</#if>
***
</#list>
</#if>
<#if api??>

## API

<#list api as propName, propValue>
### ${MD(propName)}
<#if propValue.doc()??>

${propValue.doc()?trim}

</#if>
<#if propValue.parameters()?has_content>
**Parameters:**  
<#list propValue.parameters() as param> 
> - ${MD(param.name())} ([${MD(param.type())}](${instance.getReturnTypePath(param)}))<#if param.optional()>  - (optional)
</#if><#if param.doc()??>: ${param.doc()} </#if>
</#list>
</#if>

<#if propValue.returnValue()??>

**Returns:** [${MD(propValue.returnValue().type)}](${instance.getReturnTypePath(propValue)})<#if propValue.returnValue().description??>:</#if> ${propValue.returnValue().description} 
</#if>
***
 </#list>
</#if>
<#if types??>

## Types<#-- Due to markdown limitations that do not allow both anchors and lists/tables, so ### inside indentation workarounds (tables / lists), we could either use non-breaking spaces (&#160;) but when line wraps, it would still wrap from the beginning; so we drop anchor usage so subProperties of types are no longer #### , but lists - that can be used as a better indentation workaround... -->

<#list types as typeName, typeValue>
## ${MD(typeName)} 
  scripting type: CustomType<${componentinternalname}.${typeName}>
<#if typeValue.extends?has_content>  extends: ${typeValue.extends}</#if>
  
<#list typeValue.model as propName, propValue>
 - ${MD(propName)}
<#if propValue.doc()??>
     - ${propValue.doc()}  
</#if>
     - **Type**: [${MD(propValue.type())}](${instance.getReturnTypePath(propValue)})
<#if propValue.defaultValue()??>
     - **Default Value**: ${MD(propValue.defaultValue())}
</#if>
</#list>
<#if typeValue.serversideapi??>

<#list typeValue.serversideapi as propName, propValue>
### ${MD(propName)}
<#if propValue.doc()??>

${propValue.doc()?trim}

</#if>
<#if propValue.parameters()?has_content>
**Parameters:**  
<#list propValue.parameters() as param> 
> - ${MD(param.name())} [${MD(param.type())}](${instance.getReturnTypePath(param)}) <#if param.optional()> (optional)</#if>${param.doc()}  
</#list>
</#if>
<#if propValue.returnValue()??>

**Returns:** [${MD(propValue.returnValue().type)}](${instance.getReturnTypePath(propValue)}) ${propValue.returnValue().description} 
</#if>
***
 </#list>
 </#if>
</#list>
</#if>

---
