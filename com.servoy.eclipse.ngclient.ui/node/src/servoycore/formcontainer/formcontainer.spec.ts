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

    function formCacheWithParts(parts: PartCache[]): FormCache {
      const formCache = new FormCache('testform', { width: 640, height: 490 }, false, '', {} as any);
      parts.forEach((part) => formCache.addPart(part));
      return formCache;
    }

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
  });
});
