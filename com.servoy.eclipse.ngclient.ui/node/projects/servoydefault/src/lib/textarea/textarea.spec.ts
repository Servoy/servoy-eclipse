import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TooltipService, TooltipDirective, SabloTabseq, StartEditDirective,
         FormattingService, ServoyPublicService } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';

import { ServoyDefaultTextArea } from './textarea';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';

import { runOnPushChangeDetection } from '../testingutils';

describe('ServoyDefaultTextArea', () => {
  let component: ServoyDefaultTextArea;
  let fixture: ComponentFixture<ServoyDefaultTextArea>;
  let textArea: DebugElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ServoyDefaultTextArea, TooltipDirective, SabloTabseq, StartEditDirective, FormsModule],
      providers: [FormattingService, TooltipService,
                  { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultTextArea);
    textArea = fixture.debugElement.query(By.css('textarea'));
    component = fixture.componentInstance;
    component.servoyApi =  { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), startEdit: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have called servoyApi.getMarkupId', () => {
      expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });

  it('should use start edit directive', () => {
      textArea.triggerEventHandler('focus', null);
      expect(component.servoyApi.startEdit).toHaveBeenCalled();
  });

  it('should have value test', async () => {
    component.dataProviderID.set('test');
    runOnPushChangeDetection(fixture);
    fixture.whenStable().then(() =>
      expect(component.getNativeElement().value).toBe('test'));
  });

  it('should call update method', () => {
    vi.spyOn(component, 'pushUpdate');
    textArea.nativeElement.dispatchEvent(new Event('change'));
    expect(component.pushUpdate).toHaveBeenCalled();
  });

  it('should have class: svy-textarea form-control form-control-sm input-sm svy-padding-xs ng-untouched ng-pristine ng-valid', () => {
      expect( textArea.nativeElement.getAttribute('class')).toBe('svy-textarea form-control form-control-sm input-sm svy-padding-xs ng-untouched ng-pristine ng-valid');
  });

});
