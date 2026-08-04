# Servoy 2025.3.6 Release Notes

## Bug Fixes

| Case | Description | Component |
|------|-------------|-----------|
| SVY-20390 | Dialogs don't block the code correctly when called after a `controller.focus*` call | sablo, servoy-eclipse |
| SVY-20919 | Cursor jumps to end of input after deleting a character in a Bootstrap textbox | servoy-eclipse |
| SVY-21156 | Valuelist with i18n displayValue does not always resolve the i18n | servoy-client |
| SVY-21204 | Prevent RabbitMQ connection failure from blocking server | servoy-extensions |
| SVY-21208 | Dynamic Jasper reports creation blocked by Java issues | servoy-eclipse |
| SVY-21247 | Persist `skipDatabaseViewsUpdate` setting in WAR export wizard | servoy-eclipse |
| SVYX-1140 | Fix bottom position of anchored components in absolute layout forms | servoy-client |

## Other Changes

| Component | Description |
|-----------|-------------|
| server | Fix OSGi access rules not enforced by classpath access rules |
| sablo | Fix when the event thread is still executing (long running stuff) |
| servoy-extensions | Switch to jasperreports' openpdf |

## Dependency Updates

### servoy-extensions

| Library | From | To |
|---------|------|----|
| `openpdf` | `1.3.32` | `1.3.43.jaspersoft.1` |

> New Maven repository added: **Jaspersoft Third Party CE** (`jaspersoft.jfrog.io`)

### servoy-eclipse (Target Platform)

**Updated:**

| Library | From | To |
|---------|------|----|
| `jackson-annotations` | `2.21` | `2.22` |
| `jackson-core` | `2.21.2` | `2.22.0` |
| `jackson-databind` | `2.21.2` | `2.22.0` |

**Added:**

| Library | Version |
|---------|---------|
| `jackson-dataformat-xml` | `2.22.0` |
| `woodstox-core` | `7.2.0` |
| `stax2-api` | `4.3.0` |

## Case List

```
SVY-20390
SVY-20919
SVY-21156
SVY-21204
SVY-21208
SVY-21247
SVYX-1140
```
