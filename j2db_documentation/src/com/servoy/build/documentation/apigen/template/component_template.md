# ${componentname}<#if service> (ref)</#if><#-- Get rid of the if once ref and guides of services get separate places in menu -->
\
(part of package '[${package_display_name}](${instance.getPackagePath(package_display_name)})')\
<#if designtimeExtends??>
Extends designtime/SolutionModel: [${designtimeExtends.name()}](${instance.getReturnTypePath(designtimeExtends)})\
</#if>
<#if runtimeExtends??>
Extends runtime: [${runtimeExtends.name()}](${instance.getReturnTypePath(runtimeExtends)}\)
</#if>
<#if overview??>
\
${overview}\
</#if>
\
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
\
Parameters:\
<#list propValue.parameters() as param> 
${param.name()} [${param.type()?replace("[", "\\[")?replace("]", "\\]")}](${instance.getReturnTypePath(param)})\
</#list>
</#if>
<#if propValue.returnValue()??>
\
Return Value: [${propValue.returnValue()?replace("[", "\\[")?replace("]", "\\]")}](${instance.getReturnTypePath(propValue)})
</#if>
\
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
\
Parameters:\
<#list propValue.parameters() as param> 
${param.name()} [${param.type()?replace("[", "\\[")?replace("]", "\\]")}](${instance.getReturnTypePath(param)})<#if param.optional()> (optional)</#if>\
</#list>
</#if>
<#if propValue.returnValue()??>
\
Return Value: [${propValue.returnValue()?replace("[", "\\[")?replace("]", "\\]")}](${instance.getReturnTypePath(propValue)})
</#if>
\
***
 </#list>
</#if>
<#if types??>

## Types
<#list types as typeName, typeValue>

### ${typeName}

<#list typeValue as propName, propValue>

#### ${propName}
<#if propValue.doc()??>

${propValue.doc()}
</#if>
Type: [${propValue.type()?replace("[", "\\[")?replace("]", "\\]")}](${instance.getReturnTypePath(propValue)})
<#if propValue.defaultValue()??>

Default Value: ${propValue.defaultValue()}
</#if>

***
</#list>

***
</#list>
</#if>