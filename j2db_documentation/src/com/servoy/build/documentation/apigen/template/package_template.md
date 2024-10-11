<#-- This is a GENERATED file. DO NOT modify/push it manually as all changes will be lost the next time this documentation is generated. MODIFY the package_template.md file from j2db_documentation instead -->
# ${packageType} package "${MD(packageDisplayName)}"

<#if packageDescription??>
${MD(packageDescription?trim)}
</#if>

Contains the following ${packageType}<#if allWebObjectsOfCurrentPackage?size gt 1>s</#if>:
<#list allWebObjectsOfCurrentPackage as webObject>
- [${MD(webObject.name)}](${instance.getWebObjectPath(webObject.name, webObject.parentFolderName)})
</#list>

---
