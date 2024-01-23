# ${componentname}<#if service> (ref)</#if><#-- Get rid of this if once ref and guides of services get separate places in menu -->
(part of package '[${package_display_name}](${instance.getPackagePath(package_display_name)})')  
<#if designtimeExtends??>
Extends designtime/SolutionModel: [${designtimeExtends.name()}](${instance.getReturnTypePath(designtimeExtends)})  
</#if>
<#if runtimeExtends??>
Extends runtime: [${runtimeExtends.name()}](${instance.getReturnTypePath(runtimeExtends)})  
</#if>
<#if service_scripting_name??>

You can access it in code via: **plugins.${service_scripting_name}**  
</#if>
<#if overview??>

${overview}
</#if>

<#if service>
<#-- This is a reference page; many services have detailed usage guides [here](CURRENTLY SERVICE GUIDES ARE IN THE SAME DIR, INSIDE "REFERENCE", JUST LIKE THIS REFERENCE PAGE, BUT THIS IS PROBABLY TEMPORARY). -->
<#else>
This is a reference page; many components have detailed usage guides [here](../../../../guides/develop/application-design/ui-components).
</#if>
<#if properties??>

## Properties

<#list properties as propName, propValue>
### ${propName}
<#if propValue.doc()??>
${propValue.doc()}  
</#if>
Type: [${propValue.type()?replace("[", "\\[")?replace("]", "\\]")}](${instance.getReturnTypePath(propValue)})  
<#if propValue.defaultValue()??>
Default Value: ${propValue.defaultValue()}  
</#if>
***
</#list>
</#if>
<#if events??>

## Events

<#list events as propName, propValue>
### ${propName}
<#if propValue.doc()??>

${propValue.doc()?trim}  
</#if>
<#if propValue.parameters()?has_content>
Parameters:  
<#list propValue.parameters() as param> 
> ${param.name()} [${param.type()?replace("[", "\\[")?replace("]", "\\]")}](${instance.getReturnTypePath(param)})  
</#list>
</#if>
<#if propValue.returnValue()??>

Returns: [${propValue.returnValue()?replace("[", "\\[")?replace("]", "\\]")}](${instance.getReturnTypePath(propValue)})  
</#if>
***
</#list>
</#if>
<#if api??>

## API

<#list api as propName, propValue>
### ${propName}
<#if propValue.doc()??>

${propValue.doc()?trim}  
</#if>
<#if propValue.parameters()?has_content>
Parameters:  
<#list propValue.parameters() as param> 
> ${param.name()} [${param.type()?replace("[", "\\[")?replace("]", "\\]")}](${instance.getReturnTypePath(param)})<#if param.optional()> (optional)</#if>  
</#list>
</#if>
<#if propValue.returnValue()??>

Returns: [${propValue.returnValue()?replace("[", "\\[")?replace("]", "\\]")}](${instance.getReturnTypePath(propValue)})  
</#if>
***
 </#list>
</#if>
<#if types??>

## Types<#-- Due to markdown limitations that do not allow both anchors, so ### inside indentation workarounds (tables), here I used non-breaking spaces (those 160 things below) but when line wraps, it would still wrap from the beginning; so code blocks are used for subProperty descriptions; if those are long, they will have scrollbar instead of wrapping; if you have a nice way to do this go ahead :) -->

<#list types as typeName, typeValue>
### ${typeName}
<#list typeValue as propName, propValue>
#### &#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;${propName}
<#if propValue.doc()??>
```
         ${propValue.doc()}
```
</#if>
&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;Type: [${propValue.type()?replace("[", "\\[")?replace("]", "\\]")}](${instance.getReturnTypePath(propValue)})<br/>
<#if propValue.defaultValue()??>
&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;Default Value: ${propValue.defaultValue()}
</#if>
</#list>
</#list>
</#if>