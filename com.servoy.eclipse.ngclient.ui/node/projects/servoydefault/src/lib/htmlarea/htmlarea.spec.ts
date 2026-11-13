import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ServoyDefaultHtmlarea  } from './htmlarea';
import { FormattingService, ServoyApi, TooltipService, ServoyPublicService,
         TooltipDirective, SabloTabseq } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { provideHttpClient, withInterceptorsFromDi, withXhr } from '@angular/common/http';
import { EditorModule, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';

describe('HtmlareaComponent', () => {
  let component: ServoyDefaultHtmlarea;
  let fixture: ComponentFixture<ServoyDefaultHtmlarea>;

  const servoyApi: any = { getMarkupId: vi.fn(), isInDesigner: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn(), getClientProperty: vi.fn() } as any;

    beforeEach(async () => {

    TestBed.configureTestingModule({
    imports: [ServoyDefaultHtmlarea, TooltipDirective, SabloTabseq, FormsModule, EditorModule],
      providers: [FormattingService, TooltipService,
        { provide: TINYMCE_SCRIPT_SRC, useValue: 'tinymce/tinymce.min.js' },
        { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl },
        provideHttpClient(withXhr(), withInterceptorsFromDi())]
})
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultHtmlarea);

    fixture.componentRef.setInput('servoyApi', servoyApi);
    component = fixture.componentInstance;
    component.dataProviderID.set('WhatArea');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  function mockEditor() {
    const editor: any = {
      getContent: vi.fn().mockReturnValue('<p>content</p>'),
      setContent: vi.fn(),
      mode: { set: vi.fn() },
      focus: vi.fn(),
      selection: {
        select: vi.fn(),
        getContent: vi.fn().mockReturnValue('selected'),
        setContent: vi.fn(),
      },
      getBody: vi.fn().mockReturnValue({}),
      getWin: vi.fn().mockReturnValue({ scrollX: 10, scrollY: 20, scrollTo: vi.fn() }),
    };
    component.editor = editor;
    return editor;
  }

  describe('onInit', () => {
    it('stores the editor and sets readonly mode when not editable', () => {
      fixture.componentRef.setInput('editable', false);
      fixture.componentRef.setInput('enabled', true);
      fixture.componentRef.setInput('readOnly', false);
      fixture.detectChanges();
      const editor: any = { getContent: vi.fn().mockReturnValue('x'), mode: { set: vi.fn() } };
      component.onInit({ event: {}, editor });
      expect(component.getEditor()).toBe(editor);
      expect(editor.mode.set).toHaveBeenCalledWith('readonly');
    });

    it('leaves design mode when editable, enabled and not readOnly', () => {
      fixture.componentRef.setInput('editable', true);
      fixture.componentRef.setInput('enabled', true);
      fixture.componentRef.setInput('readOnly', false);
      fixture.detectChanges();
      const editor: any = { getContent: vi.fn().mockReturnValue('x'), mode: { set: vi.fn() } };
      component.onInit({ event: {}, editor });
      expect(editor.mode.set).not.toHaveBeenCalledWith('readonly');
    });
  });

  describe('focus/blur handlers', () => {
    it('focus calls onFocusGainedMethodID when mustExecuteOnFocus is true', () => {
      const gained = vi.fn();
      fixture.componentRef.setInput('onFocusGainedMethodID', gained);
      fixture.detectChanges();
      component.mustExecuteOnFocus = true;
      component.focus();
      expect(gained).toHaveBeenCalled();
      expect(component.mustExecuteOnFocus).toBe(true);
    });

    it('focus skips onFocusGainedMethodID when mustExecuteOnFocus is false', () => {
      const gained = vi.fn();
      fixture.componentRef.setInput('onFocusGainedMethodID', gained);
      fixture.detectChanges();
      component.mustExecuteOnFocus = false;
      component.focus();
      expect(gained).not.toHaveBeenCalled();
    });

    it('blur calls onFocusLostMethodID', () => {
      const lost = vi.fn();
      fixture.componentRef.setInput('onFocusLostMethodID', lost);
      fixture.detectChanges();
      component.lastServerValueAsSeenByTinyMCEContent = 'same';
      component.tinyValue = 'same';
      component.blur();
      expect(lost).toHaveBeenCalled();
    });
  });

  describe('click/contextMenu', () => {
    it('click fires onActionMethodID', () => {
      const action = vi.fn();
      fixture.componentRef.setInput('onActionMethodID', action);
      fixture.detectChanges();
      component.click({ event: new MouseEvent('click') });
      expect(action).toHaveBeenCalled();
    });

    it('contextMenu fires onRightClickMethodID and prevents default', () => {
      const right = vi.fn();
      fixture.componentRef.setInput('onRightClickMethodID', right);
      fixture.detectChanges();
      const prevent = vi.fn();
      component.contextMenu({ event: { preventDefault: prevent } });
      expect(right).toHaveBeenCalled();
      expect(prevent).toHaveBeenCalled();
    });
  });

  describe('editor text operations', () => {
    it('selectAll selects the whole body', () => {
      const editor = mockEditor();
      component.selectAll();
      expect(editor.selection.select).toHaveBeenCalledWith(editor.getBody(), true);
    });

    it('getSelectedText returns the editor selection content', () => {
      mockEditor();
      expect(component.getSelectedText()).toBe('selected');
    });

    it('getAsPlainText returns the text format content', () => {
      const editor = mockEditor();
      editor.getContent.mockReturnValue('plain');
      expect(component.getAsPlainText()).toBe('plain');
      expect(editor.getContent).toHaveBeenCalledWith({ format: 'text' });
    });

    it('getScrollX/Y read from the editor window', () => {
      mockEditor();
      expect(component.getScrollX()).toBe(10);
      expect(component.getScrollY()).toBe(20);
    });

    it('setScroll scrolls the editor window', () => {
      const editor = mockEditor();
      component.setScroll(3, 4);
      expect(editor.getWin().scrollTo).toHaveBeenCalledWith(3, 4);
    });

    it('replaceSelectedText sets content and pushes an update when changed', () => {
      const editor = mockEditor();
      editor.getContent.mockReturnValue('<p>new</p>');
      component.lastServerValueAsSeenByTinyMCEContent = '<p>old</p>';
      const pushSpy = vi.spyOn(component, 'pushUpdate');
      component.replaceSelectedText('new');
      expect(editor.selection.setContent).toHaveBeenCalledWith('new');
      expect(pushSpy).toHaveBeenCalled();
    });
  });

  describe('requestFocus', () => {
    it('focuses the editor immediately when one is present', () => {
      const editor = mockEditor();
      component.requestFocus(false);
      expect(editor.focus).toHaveBeenCalled();
      expect(component.mustExecuteOnFocus).toBe(true);
    });
  });

  describe('svyOnChanges', () => {
    it('toggles the editor to readonly when not editable', () => {
      const editor = mockEditor();
      fixture.componentRef.setInput('editable', false);
      fixture.componentRef.setInput('enabled', true);
      fixture.componentRef.setInput('readOnly', false);
      fixture.detectChanges();
      component.svyOnChanges({ editable: { currentValue: false, previousValue: true, firstChange: false, isFirstChange: () => false } } as any);
      expect(editor.mode.set).toHaveBeenCalledWith('readonly');
    });

    it('updates tinyValue when the dataProviderID changes', () => {
      fixture.detectChanges();
      component.dataProviderID.set('<p>updated</p>');
      component.svyOnChanges({ dataProviderID: { currentValue: '<p>updated</p>', previousValue: '', firstChange: false, isFirstChange: () => false } } as any);
      expect(component.tinyValue).toBe('<p>updated</p>');
      expect(component.lastServerValueAsSeenByTinyMCEContent).toBe('<p>updated</p>');
    });

    it('adds and removes style classes', () => {
      fixture.detectChanges();
      const addSpy = vi.spyOn(component.getRenderer(), 'addClass');
      const removeSpy = vi.spyOn(component.getRenderer(), 'removeClass');
      component.svyOnChanges({ styleClass: { currentValue: 'a b', previousValue: 'c', firstChange: false, isFirstChange: () => false } } as any);
      expect(removeSpy).toHaveBeenCalledWith(component.getNativeElement(), 'c');
      expect(addSpy).toHaveBeenCalledWith(component.getNativeElement(), 'a');
      expect(addSpy).toHaveBeenCalledWith(component.getNativeElement(), 'b');
    });
  });

});
