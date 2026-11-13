import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { FormService } from '../../ngclient/form.service';
import { ServoyPublicModule } from '@servoy/public';
import { ServoyTestingModule } from '../../testing/servoytesting.module';

import { ServoyCoreFormContainer } from './formcontainer';
import { FormCache, PartCache } from '../../ngclient/types';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('ServoyCoreFormContainer', () => {
  let component: ServoyCoreFormContainer;
  let fixture: ComponentFixture<ServoyCoreFormContainer>;
  const formServiceMock = { getFormCacheByName: vi.fn() };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServoyCoreFormContainer, ServoyTestingModule, ServoyPublicModule, NoopAnimationsModule],
      providers: [{ provide: FormService, useValue: formServiceMock }],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyCoreFormContainer);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('servoyApi', { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('getContainerStyle overflow (SVY-21655)', () => {
    const bodyLayout = { position: 'absolute', 'overflow-x': 'hidden', 'overflow-y': 'hidden' };
    const otherLayout = { position: 'absolute', overflow: 'hidden' };

    const formCacheWithParts = (parts: PartCache[]): FormCache => {
      const formCache = new FormCache('testform', { width: 640, height: 490 }, false, '', {} as any);
      parts.forEach((part) => formCache.addPart(part));
      return formCache;
    };

    it('should apply the scrollbars of the body part when it is the only part', () => {
      formServiceMock.getFormCacheByName.mockReturnValue(formCacheWithParts([new PartCache('body', [], bodyLayout)]));

      const style = component.getContainerStyle('testform');

      expect(style['overflowX']).toBe('hidden');
      expect(style['overflowY']).toBe('hidden');
    });

    it('should apply the scrollbars of the body part when header and footer parts exist', () => {
      formServiceMock.getFormCacheByName.mockReturnValue(
        formCacheWithParts([
          new PartCache('titleheader', [], otherLayout),
          new PartCache('header', [], otherLayout),
          new PartCache('body', [], bodyLayout),
          new PartCache('footer', [], otherLayout),
        ]),
      );

      const style = component.getContainerStyle('testform');

      expect(style['overflowX']).toBe('hidden');
      expect(style['overflowY']).toBe('hidden');
    });

    it('should use the explicit height input as minHeight when given in pixels', () => {
      fixture.componentRef.setInput('height', '200');
      const style = component.getContainerStyle('testform');
      expect(style['minHeight']).toBe('200px');
    });

    it('should apply a percentage height to the style and the native element', () => {
      fixture.componentRef.setInput('height', '50%');
      const style = component.getContainerStyle('testform');
      expect(style['height']).toBe('50%');
    });

    it('should use the absolute form design height as minHeight when no height input is given', () => {
      const formCache = new FormCache('testform', { width: 640, height: 490 }, false, '', {} as any);
      formServiceMock.getFormCacheByName.mockReturnValue(formCache);
      fixture.componentRef.setInput('containedForm', 'testform');
      const style = component.getContainerStyle('testform');
      expect(style['minHeight']).toBe('490px');
    });
  });

  describe('switchForm', () => {
    // On the very first call form1 and realContainedForm are both undefined, so the
    // (form1 === realContainedForm) branch is taken and the form goes into form2.
    it('should put the first form into form2 and show it', () => {
      component.switchForm('formA');
      expect(component.getForm2()).toBe('formA');
      expect(component.form2_visible).toBe(true);
      expect(component.form1_visible).toBe(false);
    });

    it('should alternate to form1 on the next switch', () => {
      component.switchForm('formA');
      component.switchForm('formB');
      expect(component.getForm1()).toBe('formB');
      expect(component.form1_visible).toBe(true);
      expect(component.form2_visible).toBe(false);
    });

    it('should track the latest form in realContainedForm via getForm2/getForm1', () => {
      component.switchForm('formA');
      component.switchForm('formB');
      component.switchForm('formC');
      // third switch goes back into form2 because form1 now holds the current real form (formB)
      expect(component.getForm2()).toBe('formC');
    });
  });

  describe('animationDone', () => {
    it('should keep form2 visible when it still holds the real contained form', () => {
      component.switchForm('formA'); // formA goes into form2, realContainedForm = formA
      component.animationDone({ toState: 'hide' } as any, 'form2');
      expect(component.form2_visible).toBe(true);
    });

    it('should ignore a non-hide animation event', () => {
      component.switchForm('formA');
      component.form1_visible = true;
      component.animationDone({ toState: 'slide-left' } as any, 'form1');
      expect(component.form1_visible).toBe(true);
    });
  });
});
