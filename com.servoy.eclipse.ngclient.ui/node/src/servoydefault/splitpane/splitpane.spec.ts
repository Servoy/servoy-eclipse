import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyDefaultSplitpane } from './splitpane';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { BGSplitter } from './bg_splitter/bg_splitter.component';
import { BGPane } from './bg_splitter/bg_pane.component';
import { SabloModule } from '../../sablo/sablo.module';
import { FormattingService, I18NProvider, LocaleService, TooltipService } from '../../ngclient/servoy_public';

describe('ServoyDefaultSplitpane', () => {
  let component: ServoyDefaultSplitpane;
  let fixture: ComponentFixture<ServoyDefaultSplitpane>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultSplitpane, BGPane, BGSplitter ],
      imports: [SabloModule, ServoyPublicModule],
      providers: [I18NProvider, FormattingService, TooltipService, LocaleService ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultSplitpane);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
