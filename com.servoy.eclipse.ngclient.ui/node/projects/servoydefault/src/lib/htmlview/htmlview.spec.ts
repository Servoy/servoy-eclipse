import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultHTMLView } from './htmlview';

import { FormattingService, TooltipService, ServoyApi, ServoyPublicService,
         TooltipDirective, SabloTabseq, TrustAsHtmlPipe } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import { By } from '@angular/platform-browser';

import { runOnPushChangeDetection } from '../testingutils';

describe('ServoyDefaultHTMLView', () => {
  let component: ServoyDefaultHTMLView;
  let fixture: ComponentFixture<ServoyDefaultHTMLView>;
  const servoyApi: any = { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ServoyDefaultHTMLView, TooltipDirective, SabloTabseq, TrustAsHtmlPipe],
      providers: [FormattingService, TooltipService,
                  { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultHTMLView);
    component = fixture.componentInstance;
    component.servoyApi =  servoyApi;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it( 'should render markupid ', () => {
      servoyApi.getMarkupId.mockReturnValue( 'myid');
      runOnPushChangeDetection(fixture);
      const div = fixture.debugElement.query(By.css('div')).nativeElement;
      expect(div.id).toBe('myid');
    });

  it( 'should have called servoyApi.getMarkupId', () => {
      expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });

  it ('should test innerhtml', () => {
      component.dataProviderID.set('<p>some text herre</p>');
       runOnPushChangeDetection(fixture);
      const spanEl = fixture.debugElement.query(By.css('span'));
      expect(spanEl.nativeElement.innerHTML).toBe('<p>some text herre</p>');
      expect(component.servoyApi.trustAsHtml).toHaveBeenCalled();
  });
});
