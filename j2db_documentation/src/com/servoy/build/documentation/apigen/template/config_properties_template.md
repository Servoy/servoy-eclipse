# Server Configuration Properties

## Overview

This document provides a comprehensive list of configuration properties available in the Servoy Application Server.

<#if sections??>
## Sections

<#list sections as section>
* [${section.title}](#${section.title?lower_case?replace(" ", "-")})
</#list>
</#if>

<#if sections??>
<#list sections as section>
## ${section.title}

| Property | Default | Description |
| -------- | ------- | ----------- |
<#list section.properties as property>
| `${property.name}` | <#if property.defaultValue??>${property.defaultValue}<#else>*none*</#if> | ${property.description} |
</#list>

</#list>
</#if>
