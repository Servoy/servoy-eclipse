Info on NG ${packageType} Package "${packageDisplayName}", internally known as "${packageName}":
 - description: ${packageDescription}
 - contains the following <#if packageType == "Web-Service">services<#elseif packageType == "Web-Component">components<#else>layouts</#if> [name, internal name]: <#list allWebObjectsOfCurrentPackage as webObject>
   * [${webObject.name}, ${webObject.internalName}]<#sep>, <#else>none</#list>.