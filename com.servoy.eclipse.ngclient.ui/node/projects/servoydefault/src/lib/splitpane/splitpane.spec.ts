import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyDefaultSplitpane } from './splitpane';
import { ServoyPublicTestingModule , FormattingService, TooltipService } from '@servoy/public';
import { BGSplitter } from './bg_splitter/bg_splitter.component';
import { BGPane } from './bg_splitter/bg_pane.component';

describe('ServoyDefaultSplitpane', () => {
  let component: ServoyDefaultSplitpane;
  let fixture: ComponentFixture<ServoyDefaultSplitpane>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultSplitpane, BGPane, BGSplitter ],
      imports: [ServoyPublicTestingModule],
      providers: [FormattingService, TooltipService ]
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
