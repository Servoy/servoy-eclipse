# Servoy Server Plugins

This document provides information about the server plugins available in Servoy and their configuration properties.

<#list plugins as plugin>
## ${plugin.name}

<#if plugin.description??>
${plugin.description}
</#if>

<#if plugin.properties?size gt 0>
### Configuration Properties

| Property | Description |
|----------|-------------|
<#list plugin.properties?keys as property>
| `${property}` | ${plugin.properties[property]} |
</#list>
</#if>

<#if plugin_has_next>
---
</#if>

</#list>
