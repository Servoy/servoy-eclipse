<#-- This is a GENERATED file. DO NOT modify/push it manually as all changes will be lost the next time this documentation is generated. MODIFY the markdown_template.md file from j2db_documentation instead -->
# ${classname}<#if scriptingname??>
\(${scriptingname}\)</#if><#if description??>

${description}
</#if>
<#if returnTypes??>

## **Return Types**
<#list returnTypes as returntype>[${returntype}](${instance.getReturnTypePath(returntype)}),</#list>
</#if>
<#if supportedClients??>
## **Supported Clients**

<#list supportedClients as client>
    ${client}
</#list>
</#if>
<#if extends??>

## **Extends**

```
<#list extends as extend>
${extend}
</#list>
```
</#if>
<#if constants??>

## Constants Summary

| Type                                                  | Name                                          | Summary                                                          |
| ----------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
<#list constants as constant>
| [${constant.getReturnType()}](${instance.getReturnTypePath(constant.getReturnType())}) | [${constant.getFullFunctionName()}](${classname_nospace}.md#${constant.getAnchoredName()})                   | ${constant.getSummary()}.                                    |
</#list>
</#if>
<#if properties??>

## Property Summary

| Type                                                  | Name                    | Summary                                                                                                           |
| ----------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
<#list properties as prop>
| [${prop.getReturnType()}](${instance.getReturnTypePath(prop.getReturnType())}) | [${prop.getFullFunctionName()}](${classname_nospace}.md#${prop.getAnchoredName()})                   | ${prop.getSummary()}.                                    |
</#list>
</#if>
<#if commands??>

## Commands Summary

| Type                                                  | Name                    | Summary                                                                                                           |
| ----------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
<#list commands as command>
| [${command.getReturnType()}](${instance.getReturnTypePath(command.getReturnType())}) | [${command.getFullFunctionName()}](${classname_nospace}.md#${command.getAnchoredName()})                   | ${command.getSummary()}.                                    |
</#list>
</#if>
<#if events??>

## Events Summary

| Type                                                  | Name                    | Summary                                                                                                           |
| ----------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
<#list events as event>
| [${event.getReturnType()}](${instance.getReturnTypePath(event.getReturnType())}) | [${event.getFullFunctionName()}](${classname_nospace}.md#${event.getAnchoredName()})                   | ${event.getSummary()}.                                    |
</#list>
</#if>
<#if methods??>

## Methods Summary

| Type                                | Name                                                                                | Summary                                                             |
| ----------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
<#list methods as method>
<#if method.getReturnType() != 'void'>
| [${method.getReturnType()}](${instance.getReturnTypePath(method.getReturnType())}) | [${method.getFullFunctionName()}](${classname_nospace}.md#${method.getAnchoredName()})                   | ${method.getSummary()}.                                    |
<#else>
|void | [${method.getFullFunctionName()}](${classname_nospace}.md#${method.getAnchoredName()})                   | ${method.getSummary()}.                                    |
</#if>
</#list>
</#if>
<#if constants??>

## Constants Details

<#list constants as item>
### ${item.getFullFunctionName()}

${item.getDescription()}

**Returns**\
[${item.getReturnType()}](${instance.getReturnTypePath(item.getReturnType())}) <#if item.getReturnTypeDescription()??>${item.getReturnTypeDescription()}</#if>
<#if item.getSupportedClients()??>

**Supported Clients**\
${item.getSupportedClients()}
</#if>

**Sample**

```javascript
${item.getSampleCode()}
```
</#list>
</#if>
<#if properties??>

## Properties Details

<#list properties as item>
### ${item.getFullFunctionName()}

${item.getDescription()}

**Returns**\
[${item.getReturnType()}](${instance.getReturnTypePath(item.getReturnType())}) <#if item.getReturnTypeDescription()??>${item.getReturnTypeDescription()}</#if>
<#if item.getSupportedClients()??>

**Supported Clients**\
${item.getSupportedClients()}
</#if>

**Sample**

```javascript
${item.getSampleCode()}
```
</#list>
</#if>
<#if methods??>

## Methods Details
<#list methods as item>

### ${item.getFullFunctionName()}

${item.getDescription()}
<#if item.getParameters()??>

**Parameters**\
<#list item.getParameters() as param>
[${param.getParamType()}](${instance.getReturnTypePath(param.getParamType())})<#if param.getName()??> ${param.getName()}</#if> <#if param.getDescription()??>${param.getDescription()}</#if><#if param?has_next>\</#if>
</#list>
</#if>

**Returns**\
<#if item.getReturnType() != 'void'>
[${item.getReturnType()}](${instance.getReturnTypePath(item.getReturnType())})<#if item.getReturnTypeDescription()??> ${item.getReturnTypeDescription()}</#if>
<#else>
void<#if item.getReturnTypeDescription()??>${item.getReturnTypeDescription()}</#if>
</#if>
<#if item.getSupportedClients()??>

**Supported Clients**\
${item.getSupportedClients()}
</#if>

**Sample**

```javascript
${item.getSampleCode()}
```
</#list>
</#if>