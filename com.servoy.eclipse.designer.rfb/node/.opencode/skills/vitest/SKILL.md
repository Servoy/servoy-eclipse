---
name: vitest
description: "Use when writing, generating, or fixing Angular unit tests with Vitest. Triggered by requests to create tests, fix failing tests, or migrate test code."
---

# Writing Angular Unit Tests with Vitest

This project uses **Vitest 4.x** with **@angular/build:unit-test** (AOT-compiled tests) and **jsdom** environment.

## Key Principles

1. Services use `inject()` at field level — tests bypass DI with `Object.create()` + manual field assignment
2. Components are `standalone: false` and declared in `DesignerModule`
3. Never use `fakeAsync`/`tick`/`waitForAsync` — use Vitest-native alternatives
4. Prefer testing service logic directly without TestBed when possible

## File Structure

Tests live next to the source file:
```
my-component.component.ts → my-component.component.spec.ts
my.service.ts → my.service.spec.ts
```

## Service Test Template (Preferred — No TestBed)

Since services use `inject()` at field level, bypass Angular DI entirely for unit tests:

```typescript
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { MyService } from './my.service';
import { EditorContentService } from './editorcontent.service';
import { EditorSessionService } from './editorsession.service';

describe('MyService', () => {
  let service: MyService;
  let editorContent: Record<string, ReturnType<typeof vi.fn>>;
  let editorSession: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    editorContent = {
      executeOnlyAfterInit: vi.fn(),
      getContentElementById: vi.fn().mockReturnValue(null),
      getGlassPane: vi.fn(),
      getContentArea: vi.fn(),
    };

    editorSession = {
      getState: vi.fn().mockReturnValue({ resizing: false, dragging: false }),
      getSelection: vi.fn().mockReturnValue([]),
    };

    service = Object.create(MyService.prototype);
    (service as any).editorContentService = editorContent;
    (service as any).editorSession = editorSession;
  });

  it('should do something', () => {
    expect(service.someMethod()).toBe(expected);
  });
});
```

**Why Object.create?** Services use `inject()` which only works inside Angular's DI context. `Object.create()` creates an instance without calling the constructor or triggering field initializers, letting us assign mock dependencies directly.

## Component Test Template (With TestBed)

For components that need template rendering:

```typescript
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MyComponent } from './my.component';
import { EditorSessionService } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

describe('MyComponent', () => {
  let component: MyComponent;
  let fixture: ComponentFixture<MyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [MyComponent],
      providers: [
        { provide: EditorSessionService, useValue: { getState: vi.fn() } },
        { provide: EditorContentService, useValue: { executeOnlyAfterInit: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
```

## Common Designer Service Mocks

### EditorSessionService
```typescript
const editorSession = {
  getState: vi.fn().mockReturnValue({ resizing: false, dragging: false }),
  getSelection: vi.fn().mockReturnValue([]),
  getSnapThreshold: vi.fn().mockResolvedValue({ alignment: 5, distance: 5 }),
  addDynamicGuidesChangedListener: vi.fn(),
  setStatusBarText: vi.fn(),
  stateListener: { subscribe: vi.fn() },
  autoscrollBehavior: { subscribe: vi.fn() },
  registerCallback: { subscribe: vi.fn() },
};
```

### EditorContentService
```typescript
const editorContent = {
  executeOnlyAfterInit: vi.fn(),
  getGlassPane: vi.fn(),
  getContentArea: vi.fn(),
  getContentElementById: vi.fn().mockReturnValue(null),
  getContentElementsFromPoint: vi.fn().mockReturnValue([]),
  getAllContentElements: vi.fn().mockReturnValue([]),
  getContentForm: vi.fn(),
  getFormBounds: vi.fn().mockReturnValue(new DOMRect(0, 0, 1000, 800)),
};
```

### URLParserService
```typescript
const urlParser = {
  getFormName: vi.fn().mockReturnValue('testForm'),
  layout: 'absolute',
  formWidth: 800,
  formHeight: 600,
  isAbsoluteFormLayout: vi.fn().mockReturnValue(true),
};
```

## Mocking

### Creating mock objects (replaces `jasmine.createSpyObj`)
```typescript
const myService = {
  method1: vi.fn(),
  method2: vi.fn().mockReturnValue('value'),
  method3: vi.fn().mockResolvedValue(data),
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

**Important:** Vitest matchers do NOT accept a message string argument:
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
npm test                                    # single-run (CI)
npm run test:watch                          # watch mode
npx ng test --include="**/my.spec.ts" --no-watch  # specific file
```

## Important Notes

- The `@angular/build:unit-test` builder uses **AOT compilation**. Non-standalone components declared in `DesignerModule` are bound to that module at compile time.
- `ResizeObserver` is polyfilled in `vitest-setup.ts` (jsdom doesn't provide it)
- `zone.js` is loaded via the build target's polyfills in `angular.json`
- Transitive deps from `@servoy/public` (`luxon`, `numbro`, `bignumber.js`) are resolved via aliases in `vitest-base.config.ts` pointing to `ngclient.ui/node/node_modules`
- Do NOT use `zone.js/testing` or `zone.js/plugins/vitest-patch` — use Vitest-native timers instead
- The `vitest-base.config.ts` enables `globals: true` so test functions are available without imports, but we still import explicitly for clarity and type safety
