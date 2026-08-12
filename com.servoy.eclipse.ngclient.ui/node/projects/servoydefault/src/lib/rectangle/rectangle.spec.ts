import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultRectangle } from './rectangle';

import {FormsModule} from '@angular/forms';
import { ServoyApi, TooltipService, FormattingService, ServoyPublicService,
         TooltipDirective } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import { SimpleChange } from '@angular/core';
import { By } from '@angular/platform-browser';

describe('ServoyDefaultRectangle', () => {
  let component: ServoyDefaultRectangle;
  let fixture: ComponentFixture<ServoyDefaultRectangle>;
  let servoyApi: any;
  let rectangle: any;

  beforeEach(async () => {
      servoyApi =  { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;
      TestBed.configureTestingModule({
        imports: [ServoyDefaultRectangle, TooltipDirective, FormsModule],
      providers: [FormattingService, TooltipService,
                    { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
      })
      .compileComponents();
    });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultRectangle);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;
    component = fixture.componentInstance;
    rectangle = fixture.debugElement.query(By.css('.svy-rectangle'));
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it ('should check border styles from ngOnChanges (linesize)', () => {
      fixture.componentRef.setInput('lineSize', 5);
      component.ngOnChanges({
          lineSize: new SimpleChange(null, component.lineSize(), true)
      });
      fixture.detectChanges();
      expect(rectangle.nativeElement.style.borderWidth).toBe('5px');
      expect(rectangle.nativeElement.style.borderStyle).toBe('solid');
  });

  it ('should check border styles from ngOnChanges (roundedRadius)', () => {
      fixture.componentRef.setInput('roundedRadius', 6);
      component.ngOnChanges({
          roundedRadius: new SimpleChange(null, component.roundedRadius(), false)
      });
      fixture.detectChanges();
      expect(rectangle.nativeElement.style.borderRadius).toBe('3px');
  });

  it ('should check border styles from ngOnChanges (foreground)', () => {
      fixture.componentRef.setInput('foreground', '#ffffff');
      component.ngOnChanges({
          foreground: new SimpleChange(null, component.foreground(), false)
      });
      fixture.detectChanges();
      expect(rectangle.nativeElement.style.borderColor).toBe('rgb(255, 255, 255)');
  });

  it ('should check border styles from ngOnChanges (shapetype)', () => {
      fixture.componentRef.setInput('shapeType', 3);
      fixture.componentRef.setInput('size', {width: 4, height: 0});
      component.ngOnChanges({
          shapeType: new SimpleChange(null, component.shapeType(), true),
          size: new SimpleChange(null, component.size(), true)
      });
      fixture.detectChanges();
      expect(rectangle.nativeElement.style.borderRadius).toBe('2px');
  });
});
