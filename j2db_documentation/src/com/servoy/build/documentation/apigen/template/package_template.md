# ${packageType} package "${packageDisplayName}"

<#if packageDescription??>
${packageDescription?trim}
</#if>

Contains the following ${packageType}<#if allWebObjectsOfCurrentPackage?size gt 1>s</#if>:
<#list allWebObjectsOfCurrentPackage as webObject>
- [${webObject.name}](${instance.getWebObjectPath(webObject.name, webObject.parentFolderName)})
</#list>