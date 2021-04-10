import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyDefaultSplitpane } from './splitpane';
import { ServoyPublicModule } from 'servoy-public';
import { BGSplitter } from './bg_splitter/bg_splitter.component';
import { BGPane } from './bg_splitter/bg_pane.component';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { FormattingService, TooltipService } from 'servoy-public';
import { LocaleService } from '../../ngclient/locale.service';
import { I18NProvider } from '../../ngclient/services/i18n_provider.service';

describe('ServoyDefaultSplitpane', () => {
  let component: ServoyDefaultSplitpane;
  let fixture: ComponentFixture<ServoyDefaultSplitpane>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultSplitpane, BGPane, BGSplitter ],
      imports: [ServoyTestingModule, ServoyPublicModule],
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
