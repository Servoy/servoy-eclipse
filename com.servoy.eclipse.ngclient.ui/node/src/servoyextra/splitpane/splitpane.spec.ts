import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyExtraSplitpane } from './splitpane';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { BGSplitter } from './bg_splitter/bg_splitter.component';
import { BGPane } from './bg_splitter/bg_pane.component';
import { SabloModule } from '../../sablo/sablo.module';
import { FormattingService, I18NProvider, LocaleService, TooltipService } from '../../ngclient/servoy_public';


describe('ServoyExtraSplitpane', () => {
  let component: ServoyExtraSplitpane;
  let fixture: ComponentFixture<ServoyExtraSplitpane>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyExtraSplitpane, BGPane, BGSplitter ],
      imports: [SabloModule, ServoyPublicModule],
      providers: [I18NProvider, FormattingService, TooltipService, LocaleService ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyExtraSplitpane);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
