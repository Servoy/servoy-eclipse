# ${componentname}
<#if designtimeExtends??>

Extends designtime/SolutionModel: [${designtimeExtends.name()}](${instance.getReturnTypePath(designtimeExtends)})\
</#if>
<#if runtimeExtends??>
Extends runtime: [${runtimeExtends.name()}](${instance.getReturnTypePath(runtimeExtends)})
</#if>
<#if properties??>

## Properties
<#list properties as propName, propValue>

### ${propName}
<#if propValue.doc()??>

${propValue.doc()}
</#if>

Type: [${propValue.type()}](${instance.getReturnTypePath(propValue)})
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

${propValue.doc()}
</#if>
<#if propValue.parameters()?has_content>

Parameters:\
<#list propValue.parameters() as param> 
${param.name()} [${param.type()}](${instance.getReturnTypePath(param)})<#sep>\
</#list>
<#if propValue.returnValue()??>

</#if>
</#if>
<#if propValue.returnValue()??>

Return Value: [${propValue.returnValue()}](${instance.getReturnTypePath(propValue)})
 </#if>
 
 ***
 </#list>
</#if>
<#if api??>

## API
<#list api as propName, propValue>

### ${propName}
<#if propValue.doc()??>

${propValue.doc()}
</#if>
<#if propValue.parameters()?has_content>
Parameters:\
<#list propValue.parameters() as param> 
${param.name()} [${param.type()}](${instance.getReturnTypePath(param)})<#if param.optional()> (optional)</#if><#sep>\
</#list>
<#if propValue.returnValue()??>

</#if>
</#if>
<#if propValue.returnValue()??>

Return Value: [${propValue.returnValue()}](${instance.getReturnTypePath(propValue)})
 </#if>
 
 ***
 </#list>
</#if>
<#if types??>

## Types
<#list types as typeName, typeValue>

### ${typeName}

<#list typeValue as propName, propValue>

####  ${propName}
<#if propValue.doc()??>

${propValue.doc()}
</#if>
Type:  [${propValue.type()}](${instance.getReturnTypePath(propValue)})
<#if propValue.defaultValue()??>

Default Value: ${propValue.defaultValue()}
</#if>

***
</#list>

 ***
 </#list>
</#if>