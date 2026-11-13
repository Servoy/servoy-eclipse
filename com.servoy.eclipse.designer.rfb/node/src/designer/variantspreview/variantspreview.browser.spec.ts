import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, provideZonelessChangeDetection, signal } from '@angular/core';
import { NgbPopoverConfig } from '@ng-bootstrap/ng-bootstrap';
import { DomSanitizer } from '@angular/platform-browser';
import { WindowRefService } from '@servoy/public';

import { VariantsPreviewComponent } from './variantspreview.component';
import { URLParserService } from '../services/urlparser.service';
import { EditorSessionService } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

/**
 * Browser spec: the event-handler fields (onMouseUp/onMouseMove/onVariantMouseDown/
 * onVariantsClick/onAreaMouseUp) are arrow-function instance properties created by the
 * constructor, so they only exist on a real, constructed component. These cases exercise
 * them through a constructed instance.
 */
describe('VariantsPreviewComponent (browser)', () => {
  let fixture: ComponentFixture<VariantsPreviewComponent>;
  let component: VariantsPreviewComponent;
  let editorSession: any;
  let doc: Document;

  beforeEach(async () => {
    editorSession = {
      variantsTrigger: signal(null),
      variantsScroll: signal(null),
      variantsPopup: signal<{ status: string } | null>(null)
    };
    doc = document;

    await TestBed.configureTestingModule({
      imports: [VariantsPreviewComponent],
      providers: [
        provideZonelessChangeDetection(),
        NgbPopoverConfig,
        { provide: DomSanitizer, useValue: { bypassSecurityTrustResourceUrl: (u: string) => u } },
        { provide: URLParserService, useValue: { getSolutionName: () => 'sol', getContentClientNr: () => '1' } },
        { provide: WindowRefService, useValue: { nativeWindow: { addEventListener: vi.fn(), postMessage: vi.fn(), location: { host: 'localhost' } } } },
        { provide: EditorSessionService, useValue: editorSession },
        { provide: EditorContentService, useValue: { getDocument: () => doc, getPallete: () => document.createElement('div') } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(VariantsPreviewComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    fixture?.destroy();
  });

  it('onVariantsClick should show the popover', () => {
    const iframe = { style: { display: '' } } as HTMLIFrameElement;
    (component as any).variantsIFrame = iframe;
    component.onVariantsClick();
    expect(editorSession.variantsPopup()).toEqual({ status: 'visible' });
    expect(iframe.style.display).toBe('block');
  });

  it('onAreaMouseUp should stop propagation', () => {
    const event = { stopPropagation: vi.fn() } as unknown as MouseEvent;
    component.onAreaMouseUp(event);
    expect(event.stopPropagation).toHaveBeenCalled();
  });

  it('onMouseUp should clean up the displayed item and post a message', () => {
    const body = { removeChild: vi.fn() };
    (component as any).variantsIFrame = { contentWindow: { document: { body } } };
    const displayed = document.createElement('div');
    (component as any).variantItemBeingDisplayed = displayed;
    const event = { stopPropagation: vi.fn() } as unknown as MouseEvent;
    component.onMouseUp(event);
    expect(body.removeChild).toHaveBeenCalledWith(displayed);
    expect((component as any).windowRef.nativeWindow.postMessage).toHaveBeenCalledWith({ id: 'onVariantMouseUp' });
    expect((component as any).variantItemBeingDragged).toBeNull();
    expect((component as any).variantItemBeingDisplayed).toBeNull();
  });

  it('onMouseUp should only reset state when nothing is displayed', () => {
    const body = { removeChild: vi.fn() };
    (component as any).variantsIFrame = { contentWindow: { document: { body } } };
    (component as any).variantItemBeingDisplayed = null;
    const event = { stopPropagation: vi.fn() } as unknown as MouseEvent;
    component.onMouseUp(event);
    expect(body.removeChild).not.toHaveBeenCalled();
    expect((component as any).variantItemBeingDragged).toBeNull();
  });

  it('onMouseMove should clean up and hide the popover when an item is displayed', () => {
    const body = { removeChild: vi.fn() };
    (component as any).variantsIFrame = { contentWindow: { document: { body } }, style: { display: '' } };
    const displayed = document.createElement('div');
    (component as any).variantItemBeingDisplayed = displayed;
    const hideSpy = vi.spyOn(component, 'hidePopover').mockImplementation(() => undefined);
    component.onMouseMove();
    expect(body.removeChild).toHaveBeenCalledWith(displayed);
    expect((component as any).variantItemBeingDragged).toBeNull();
    expect(hideSpy).toHaveBeenCalled();
  });

  it('onMouseMove should do nothing when no item is displayed', () => {
    (component as any).variantItemBeingDisplayed = null;
    const hideSpy = vi.spyOn(component, 'hidePopover').mockImplementation(() => undefined);
    component.onMouseMove();
    expect(hideSpy).not.toHaveBeenCalled();
  });

  it('onVariantMouseDown should clone the element under the point and append the display node', () => {
    const dragged = document.createElement('div');
    const parent = document.createElement('div');
    parent.appendChild(dragged);
    const body = { appendChild: vi.fn() };
    (component as any).variantsIFrame = {
      contentWindow: {
        document: {
          body,
          elementFromPoint: vi.fn().mockReturnValue(dragged)
        }
      }
    };
    component.onVariantMouseDown(5, 7);
    expect((component as any).variantItemBeingDragged).not.toBeNull();
    expect(body.appendChild).toHaveBeenCalled();
  });
});
