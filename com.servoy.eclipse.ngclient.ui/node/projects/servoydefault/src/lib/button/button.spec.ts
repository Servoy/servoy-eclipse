import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultButton } from './button';

import { TooltipService, ComponentContributor, TooltipDirective, SabloTabseq,
         ImageMediaIdDirective, FormatFilterPipe, MnemonicletterFilterPipe,
         TrustAsHtmlPipe, FormattingService, ServoyPublicService } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';

import { runOnPushChangeDetection } from '../testingutils';

describe('SvyButton', () => {
  let component: ServoyDefaultButton;
  let fixture: ComponentFixture<ServoyDefaultButton>;

  const servoyApi: any = { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      declarations: [ServoyDefaultButton, TooltipDirective, SabloTabseq,
                     ImageMediaIdDirective, FormatFilterPipe, MnemonicletterFilterPipe,
                     TrustAsHtmlPipe],
      providers: [TooltipService, ComponentContributor, FormattingService,
                  { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }],
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultButton);
    component = fixture.componentInstance;
    component.servoyApi =  servoyApi;
    component.toolTipText = 'Hi';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it( 'should have called servoyApi.getMarkupId', () => {
    runOnPushChangeDetection(fixture);
    expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });

  it( 'should render html', async () => {
    servoyApi.trustAsHtml.mockReturnValue( true );
    component.dataProviderID = '<div class="myclass" onclick="javascript:test()">hallo</div>';
    runOnPushChangeDetection(fixture);
    expect( component.child.nativeElement.children[1].innerHTML ).toBe(component.dataProviderID);
  });
});
