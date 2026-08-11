import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, signal, provideZonelessChangeDetection } from '@angular/core';
import { MouseSelectionComponent } from './mouseselection.component';
import { EditorSessionService } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';
import { DesignerUtilsService } from '../services/designerutils.service';
import { EditorContentService } from '../services/editorcontent.service';
import { DynamicGuidesService } from '../services/dynamicguides.service';

describe('MouseSelectionComponent (browser)', () => {
  let fixture: ComponentFixture<MouseSelectionComponent>;
  let component: MouseSelectionComponent;
  let selectionChangedCallback: (selection: string[], redraw?: boolean) => void;
  let mockEditorContentService: any;
  let mockEditorSession: any;

  beforeEach(async () => {
    const glasspane = document.createElement('div');
    const contentArea = document.createElement('div');

    mockEditorSession = {
      dragging: signal(false),
      ghosthandle: signal(false),
      showWireframe: signal(false),
      resizing: signal(false),
      getSelection: vi.fn().mockReturnValue([]),
      setSelection: vi.fn(),
      requestSelection: vi.fn().mockResolvedValue(undefined),
      addSelectionChangedListener: vi.fn((listener: any) => {
        selectionChangedCallback = (sel, redraw) => listener.selectionChanged(sel, redraw);
        return () => undefined;
      }),
      updateFieldPositioner: vi.fn(),
      executeAction: vi.fn(),
      keyPressed: vi.fn(),
      openConfigurator: vi.fn(),
      createComponent: vi.fn(),
      getWizardProperties: vi.fn().mockReturnValue(null),
      updateSelection: vi.fn(),
      setDragging: vi.fn(),
      registerAutoscroll: vi.fn(),
      unregisterAutoscroll: vi.fn(),
      isInlineEditMode: vi.fn().mockReturnValue(false),
      registerCallback: { next: vi.fn() }
    };

    mockEditorContentService = {
      addContentMessageListener: vi.fn(),
      removeContentMessageListener: vi.fn(),
      getGlassPane: vi.fn().mockReturnValue(glasspane),
      getContentArea: vi.fn().mockReturnValue(contentArea),
      getContentForm: vi.fn().mockReturnValue(document.createElement('div')),
      executeOnlyAfterInit: vi.fn((cb: () => void) => cb()),
      getAllContentElements: vi.fn().mockReturnValue([]),
      getContentElement: vi.fn().mockReturnValue(undefined),
      getLeftPositionIframe: vi.fn().mockReturnValue(0),
      getTopPositionIframe: vi.fn().mockReturnValue(0)
    };

    await TestBed.configureTestingModule({
      imports: [MouseSelectionComponent],
      providers: [
        provideZonelessChangeDetection(),
        { provide: EditorSessionService, useValue: mockEditorSession },
        { provide: EditorContentService, useValue: mockEditorContentService },
        { provide: URLParserService, useValue: { isAbsoluteFormLayout: () => true, isMarqueeSelectOuter: () => false } },
        { provide: DesignerUtilsService, useValue: { getNode: () => null, getNodeBasedOnSelectionFCorLFC: () => null, adjustElementRect: (_: any, r: any) => r } },
        { provide: DynamicGuidesService, useValue: { snapData: signal(null) } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(MouseSelectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture?.destroy();
  });

  it('should render selection decorators when selectionChanged fires', async () => {
    const fakeElement = document.createElement('div');
    fakeElement.setAttribute('svy-id', 'comp1');
    fakeElement.getBoundingClientRect = () => ({ top: 10, left: 20, width: 100, height: 50, x: 20, y: 10, bottom: 60, right: 120, toJSON: () => ({}) });
    Object.defineProperty(fakeElement, 'parentElement', { value: { closest: () => null }, configurable: true });

    mockEditorContentService.getAllContentElements.mockReturnValue([fakeElement]);
    mockEditorContentService.getContentElement.mockReturnValue(fakeElement);
    mockEditorSession.getSelection.mockReturnValue(['comp1']);

    selectionChangedCallback(['comp1']);
    fixture.detectChanges();
    await fixture.whenStable();

    const decorators = fixture.nativeElement.querySelectorAll('.decorationOverlay');
    expect(decorators.length).toBe(1);
    expect(decorators[0].id).toBe('comp1');
  });

  it('should clear decorators when selection is emptied', async () => {
    const fakeElement = document.createElement('div');
    fakeElement.setAttribute('svy-id', 'comp1');
    fakeElement.getBoundingClientRect = () => ({ top: 10, left: 20, width: 100, height: 50, x: 20, y: 10, bottom: 60, right: 120, toJSON: () => ({}) });
    Object.defineProperty(fakeElement, 'parentElement', { value: { closest: () => null }, configurable: true });

    mockEditorContentService.getAllContentElements.mockReturnValue([fakeElement]);
    mockEditorContentService.getContentElement.mockReturnValue(fakeElement);
    mockEditorSession.getSelection.mockReturnValue(['comp1']);

    selectionChangedCallback(['comp1']);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('.decorationOverlay').length).toBe(1);

    selectionChangedCallback([]);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('.decorationOverlay').length).toBe(0);
  });

  it('should show resize knobs for absolute layout elements', async () => {
    const fakeElement = document.createElement('div');
    fakeElement.setAttribute('svy-id', 'comp2');
    fakeElement.getBoundingClientRect = () => ({ top: 30, left: 40, width: 200, height: 80, x: 40, y: 30, bottom: 110, right: 240, toJSON: () => ({}) });
    Object.defineProperty(fakeElement, 'parentElement', { value: { closest: () => null }, configurable: true });

    mockEditorContentService.getAllContentElements.mockReturnValue([fakeElement]);
    mockEditorContentService.getContentElement.mockReturnValue(fakeElement);
    mockEditorSession.getSelection.mockReturnValue(['comp2']);

    selectionChangedCallback(['comp2']);
    fixture.detectChanges();
    await fixture.whenStable();

    const knobs = fixture.nativeElement.querySelectorAll('.knob:not(.hidden)');
    expect(knobs.length).toBeGreaterThan(0);
  });
});
