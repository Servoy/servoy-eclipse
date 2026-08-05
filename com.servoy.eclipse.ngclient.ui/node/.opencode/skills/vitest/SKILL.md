---
name: vitest
description: "Use when writing, generating, or fixing Angular unit tests with Vitest. Triggered by requests to create tests, fix failing tests, or migrate test code."
---

# Writing Angular Unit Tests with Vitest

This project uses **Vitest 4.x** with **@angular/build:unit-test** (AOT-compiled tests) and **jsdom** environment.

## Key Principles

1. Each spec file builds its **own TestBed module** with explicit declarations
2. Never use `NO_ERRORS_SCHEMA` — always declare what the template needs
3. Never import shared testing modules like `ServoyPublicTestingModule` or `ServoyDefaultComponentsModule`
4. Never use `fakeAsync`/`tick`/`waitForAsync` — use Vitest-native alternatives

## File Structure

Tests live next to the source file:
```
my-component.component.ts → my-component.component.spec.ts
my.service.ts → my.service.spec.ts
```

## Spec File Template

```typescript
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MyComponent } from './my-component';
import { TooltipDirective, SabloTabseq, FormatFilterPipe,
         TooltipService, FormattingService, ComponentContributor,
         ServoyPublicService } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';

describe('MyComponent', () => {
  let component: MyComponent;
  let fixture: ComponentFixture<MyComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      declarations: [
        MyComponent,
        // Add ONLY the directives/pipes the component template actually uses:
        TooltipDirective,
        SabloTabseq,
        FormatFilterPipe,
      ],
      providers: [
        TooltipService,
        FormattingService,
        ComponentContributor,
        { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(MyComponent);
    component = fixture.componentInstance;
    component.servoyApi = {
      getMarkupId: vi.fn(),
      trustAsHtml: vi.fn(),
      registerComponent: vi.fn(),
      unRegisterComponent: vi.fn(),
    } as any;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
```

## Directive/Pipe Reference

When a component template uses these selectors, add the corresponding class to `declarations`:

| Template selector | Import from `@servoy/public` |
|-------------------|------------------------------|
| `[svyTooltip]` | `TooltipDirective` |
| `[svyHtmlTooltip]` | `HTMLTooltipDirective` |
| `[sabloTabseq]` | `SabloTabseq` |
| `[svyImageMediaId]` | `ImageMediaIdDirective` |
| `[svyFormat]` | `FormatDirective` |
| `[svyDecimalKeyConverter]` | `DecimalkeyconverterDirective` |
| `[svyAutosave]` | `AutosaveDirective` |
| `[svyStartEdit]` | `StartEditDirective` |
| `\| formatFilter` | `FormatFilterPipe` |
| `\| emptyValueFilter` | `EmptyValueFilterPipe` |
| `\| mnemonicletterFilter` | `MnemonicletterFilterPipe` |
| `\| notNullOrEmpty` | `NotNullOrEmptyPipe` |
| `\| htmlFilter` | `HtmlFilterPipe` |
| `\| trustAsHtml` | `TrustAsHtmlPipe` |

If the component uses `ngModel` or reactive forms, add `FormsModule` or `ReactiveFormsModule` to `imports`.
If the component uses NgBootstrap (accordion, tabs, etc.), add `NgbModule` to `imports`.

## Mocking

### Creating mock objects (replaces `jasmine.createSpyObj`)
```typescript
const myService = {
  method1: vi.fn(),
  method2: vi.fn().mockReturnValue('value'),
  method3: vi.fn().mockReturnValue(Promise.resolve(data)),
} as any;
```

### Spying on methods (replaces `spyOn`)
```typescript
vi.spyOn(component, 'myMethod');
vi.spyOn(service, 'getData').mockReturnValue(of(mockData));
```

### Mock return values
```typescript
myMock.mockReturnValue(value);        // replaces .and.returnValue()
myMock.mockImplementation(fn);        // replaces .and.callFake()
myMock.mockResolvedValue(value);      // for async: returns Promise.resolve(value)
```

## Timer Testing (replaces fakeAsync/tick)

```typescript
it('should handle delayed operation', () => {
  vi.useFakeTimers();

  component.startTimer();
  vi.advanceTimersByTime(500);
  fixture.detectChanges();

  expect(component.timerDone).toBe(true);
  vi.useRealTimers();
});
```

For async timer tests with Promises:
```typescript
it('should handle async timer', async () => {
  vi.useFakeTimers();

  const promise = component.doAsyncWork();
  await vi.advanceTimersByTimeAsync(1000);

  expect(await promise).toBe(expectedResult);
  vi.useRealTimers();
});
```

## Async Tests (replaces waitForAsync)

```typescript
beforeEach(async () => {
  // Just use async/await directly
  await TestBed.configureTestingModule({...}).compileComponents();
});

it('should load data', async () => {
  const result = await service.getData();
  expect(result).toEqual(expected);
});
```

## Assertions

| Vitest | Old Jasmine |
|--------|-------------|
| `expect(x).toBe(true)` | `.toBeTrue()` |
| `expect(x).toBe(false)` | `.toBeFalse()` |
| `expect(x).toBeTruthy()` | `.toBeTruthy()` |
| `expect(x).toBeFalsy()` | `.toBeFalsy()` |
| `expect(x).toBeDefined()` | `.toBeDefined()` |
| `expect(x).toBeNull()` | `.toBeNull()` |
| `expect(x).toEqual(val)` | `.toEqual(val)` |
| `expect(fn).toHaveBeenCalled()` | `.toHaveBeenCalled()` |
| `expect(fn).toHaveBeenCalledWith(a,b)` | `.toHaveBeenCalledWith(a,b)` |
| `expect.objectContaining({})` | `jasmine.objectContaining({})` |
| `expect.any(Number)` | `jasmine.any(Number)` |
| `expect.fail('msg')` | `fail('msg')` |

**Important:** Vitest matchers do NOT accept a message string argument. Use plain matchers:
```typescript
// WRONG: expect(x).toBe(true, 'should be true');
// RIGHT: expect(x).toBe(true);
```

## Skipping Tests

```typescript
it.skip('broken test', () => { ... });    // replaces xit()
describe.skip('broken suite', () => { ... }); // replaces xdescribe()
```

## Running Tests

```bash
npm run test                    # ngclient2 app (CI, single-run)
npm run test:watch              # ngclient2 app (watch mode)
npm run test_public             # @servoy/public library
npm run test_default            # @servoy/servoydefault library
npm run test_dialogs            # @servoy/dialogs library
npm run test_window             # @servoy/window library
npm run test_ngclientutils      # @servoy/ngclientutils library
npx ng test --include="**/my.spec.ts" --no-watch  # specific file
```

## Important Notes

- The `@angular/build:unit-test` builder uses **AOT compilation**. This means components are compiled with their original module context. Non-standalone components declared in an NgModule are bound to that module at compile time.
- `ResizeObserver` is polyfilled in `vitest-setup.ts` (jsdom doesn't provide it)
- `zone.js` is loaded via the dummy app's polyfills in `angular.json`
- Do NOT use `zone.js/testing` or `zone.js/plugins/vitest-patch` — use Vitest-native timers instead
- The `vitest-base.config.ts` enables `globals: true` so test functions are available without imports, but we still import explicitly for clarity and type safety
