<#-- This is a GENERATED file. DO NOT modify/push it manually as all changes will be lost the next time this documentation is generated. MODIFY the markdown_template.md file from j2db_documentation instead -->
# ${classname}<#if scriptingname??>
\(${scriptingname}\)</#if><#if description??>

## Overview

${description}</#if>
<#if returnTypes??>

## **Returned Types**
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

| Type                                                   |
| ------------------------------------------------------ | 
<#list extends as extend>
| [${MD(extend)}](${instance.getReturnTypePath(extend)}) |
</#list>

</#if>
<#if constants??>

## Constants Summarized

| Type                                                  | Name                                          | Summary                                                          |
| ----------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
<#list constants as constant>
| [${MD(constant.getReturnType())}](${instance.getReturnTypePath(constant.getReturnType())}) | [${MD(constant.getFullFunctionName())}](${classname_nospace}.md#${constant.getAnchoredName()})                   | ${constant.getSummary()}                                    |
</#list>
</#if>
<#if properties??>

## Properties Summarized

| Type                                                  | Name                    | Summary                                                                                                           |
| ----------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
<#list properties as prop>
| [${MD(prop.getReturnType())}](${instance.getReturnTypePath(prop.getReturnType())}) | [${MD(prop.getFullFunctionName())}](${classname_nospace}.md#${prop.getAnchoredName()})                   | ${prop.getSummary()}                                    |
</#list>
</#if>
<#if commands??>

## Commands Summarized

| Type                                                  | Name                    | Summary                                                                                                           |
| ----------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
<#list commands as command>
<#if command.getReturnType() != 'void'>
| [${MD(command.getReturnType())}](${instance.getReturnTypePath(command.getReturnType())}) | [${MD(command.getFullFunctionName())}](${classname_nospace}.md#${command.getAnchoredName()})                   | ${command.getSummary()}                                   |
<#else>
| void | [${MD(command.getFullFunctionName())}](${classname_nospace}.md#${command.getAnchoredName()})                   | ${command.getSummary()}                                   |
</#if>
</#list>
</#if>
<#if events??>

## Events Summarized

| Type                                                  | Name                    | Summary                                                                                                           |
| ----------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
<#list events as event>
<#if event.getReturnType() != 'void'>
| [${MD(event.getReturnType())}](${instance.getReturnTypePath(event.getReturnType())}) | [${MD(event.getFullFunctionName())}](${classname_nospace}.md#${event.getAnchoredName()})                   | ${event.getSummary()}                                    |
<#else>
| void | [${MD(event.getFullFunctionName())}](${classname_nospace}.md#${event.getAnchoredName()})                   | ${event.getSummary()}                      
</#if>
</#list>
</#if>
<#if methods??>

## Methods Summarized

| Type                                | Name                                                                                | Summary                                                             |
| ----------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
<#list methods as method>
<#if method.getReturnType() != 'void'>
| [${MD(method.getReturnType())}](${instance.getReturnTypePath(method.getReturnType())}) | [${MD(method.getFullFunctionName())}](${classname_nospace}.md#${method.getAnchoredName()})                   | ${method.getSummary()}                                    |
<#else>
| void | [${MD(method.getFullFunctionName())}](${classname_nospace}.md#${method.getAnchoredName()})                   | ${method.getSummary()}                                    |
</#if>
</#list>
</#if>
<#if constants??>

## Constants Detailed

<#list constants as item>
### ${MD(item.getFullFunctionName())}

${item.getDescription()}

**Type**\
[${MD(item.getReturnType())}](${instance.getReturnTypePath(item.getReturnType())})<#if item.getReturnTypeDescription()??> ${item.getReturnTypeDescription()}</#if>
<#if item.getSupportedClients()??>

**Supported Clients**\
${item.getSupportedClients()}
</#if>
<#if item.getSampleCode()??>

**Sample**

${item.getSampleCode()?trim}
</#if>
</#list>
</#if>
<#if properties??>

## Properties Detailed

<#list properties as item>
### ${MD(item.getFullFunctionName())}

${item.getDescription()}

**Type**\
[${MD(item.getReturnType())}](${instance.getReturnTypePath(item.getReturnType())})<#if item.getReturnTypeDescription()??> ${item.getReturnTypeDescription()}</#if>
<#if item.getSupportedClients()??>

**Supported Clients**\
${item.getSupportedClients()}
</#if>
<#if item.getSampleCode()??>

**Sample**

${item.getSampleCode()?trim}
</#if>
</#list>
</#if>
<#if events??>

## Events Detailed
<#list events as item>

### ${MD(item.getFullFunctionName())}

${item.getDescription()}
<#if item.getParameters()??>

**Parameters**
<#list item.getParameters() as param>
* [${MD(param.getParamType())}](${instance.getReturnTypePath(param.getParamType())})<#if param.getName()??> **${param.getName()}**</#if><#if param.getDescription()??> ${param.getDescription()}</#if>
</#list>
</#if>

**Returns:** <#if item.getReturnType() != 'void'>[${MD(item.getReturnType())}](${instance.getReturnTypePath(item.getReturnType())})<#if item.getReturnTypeDescription()??> ${item.getReturnTypeDescription()}</#if>
<#else>
void<#if item.getReturnTypeDescription()??>${item.getReturnTypeDescription()}</#if>
</#if>
<#if item.getSupportedClients()??>

**Supported Clients**\
${item.getSupportedClients()}
</#if>
<#if item.getSampleCode()??>

**Sample**

${item.getSampleCode()?trim}
</#if>
</#list>
</#if>
<#if methods??>

## Methods Detailed
<#list methods as item>

### ${MD(item.getFullFunctionName())}

${item.getDescription()}
<#if item.getParameters()??>

**Parameters**
<#list item.getParameters() as param>
* [${MD(param.getParamType())}](${instance.getReturnTypePath(param.getParamType())})<#if param.getName()??> **${param.getName()}**</#if> <#if param.getDescription()??>${param.getDescription()}</#if>
</#list>
</#if>

**Returns:** <#if item.getReturnType() != 'void'>[${MD(item.getReturnType())}](${instance.getReturnTypePath(item.getReturnType())})<#if item.getReturnTypeDescription()??> ${item.getReturnTypeDescription()}</#if>
<#else>
void<#if item.getReturnTypeDescription()??>${item.getReturnTypeDescription()}</#if>
</#if>
<#if item.getSupportedClients()??>

**Supported Clients**\
${item.getSupportedClients()}
</#if>
<#if item.getSampleCode()??>

**Sample**

${item.getSampleCode()?trim}
</#if>
</#list>
</#if>

---
