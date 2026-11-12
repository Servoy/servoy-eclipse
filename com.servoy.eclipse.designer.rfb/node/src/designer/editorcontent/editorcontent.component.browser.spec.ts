import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, provideZonelessChangeDetection } from '@angular/core';
import { EditorContentComponent } from './editorcontent.component';
import { DesignSizeService } from '../services/designsize.service';
import { URLParserService } from '../services/urlparser.service';
import { WindowRefService } from '@servoy/public';
import { EditorSessionService } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

/**
 * SVY-21683 regression guard. The editor's rendered form size must update immediately
 * when an 'updateFormSize' message arrives from the raw window 'message' listener -
 * i.e. from OUTSIDE any Angular event/zone. Under zoneless OnPush this only works because
 * contentStyle is a signal; against the pre-fix plain-field version the DOM style stays
 * stale and these assertions fail.
 */
describe('EditorContentComponent (browser)', () => {
  let fixture: ComponentFixture<EditorContentComponent>;
  let mockEditorContentService: any;

  beforeEach(async () => {
    const glassPane = document.createElement('div');
    Object.defineProperty(glassPane, 'clientHeight', { value: 2000, configurable: true });

    mockEditorContentService = {
      addContentMessageListener: vi.fn(),
      removeContentMessageListener: vi.fn(),
      getGlassPane: vi.fn().mockReturnValue(glassPane),
      getContentForm: vi.fn().mockReturnValue(document.createElement('div')),
      getContentBodyElement: vi.fn().mockReturnValue(document.createElement('div')),
      getContentArea: vi.fn().mockReturnValue(document.createElement('div')),
      getPallete: vi.fn().mockReturnValue(document.createElement('div')),
      executeOnlyAfterInit: vi.fn((cb: () => void) => cb())
    };

    await TestBed.configureTestingModule({
      imports: [EditorContentComponent],
      providers: [
        provideZonelessChangeDetection(),
        { provide: EditorContentService, useValue: mockEditorContentService },
        { provide: DesignSizeService, useValue: { setEditor: vi.fn() } },
        {
          provide: URLParserService,
          useValue: {
            isAbsoluteFormLayout: () => true,
            getSolutionName: () => 'sol',
            getFormName: () => 'form1',
            getContentClientNr: () => '1',
            getFormWidth: () => 800,
            getFormHeight: () => 600
          }
        },
        { provide: EditorSessionService, useValue: { buildTiNG: vi.fn(), keyPressed: vi.fn(), getFixedKeyEvent: (e: any) => e } },
        { provide: WindowRefService, useValue: { nativeWindow: { location: { host: 'localhost:4200' } } } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(EditorContentComponent);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => {
    fixture?.destroy();
  });

  const contentDiv = (): HTMLElement => fixture.nativeElement.querySelector('.content');

  it('should render the initial absolute-layout form size', async () => {
    expect(contentDiv().style.width).toBe('800px');
    expect(contentDiv().style.height).toBe('600px');
  });

  it('should update the content div width/height after an out-of-Angular updateFormSize message', async () => {
    expect(contentDiv().style.width).toBe('800px');
    expect(contentDiv().style.height).toBe('600px');

    // Simulate the raw window 'message' callback dispatch - NOT wrapped in an Angular event.
    const component = fixture.componentInstance;
    component.contentMessageReceived('updateFormSize', { property: '', width: 1024, height: 768 });

    fixture.detectChanges();
    await fixture.whenStable();

    expect(contentDiv().style.width).toBe('1024px');
    expect(contentDiv().style.height).toBe('768px');
  });
});
