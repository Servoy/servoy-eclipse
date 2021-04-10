import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapAccordion } from './accordion';
import { WindowRefService } from 'servoy-public'
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ServoyApi } from 'servoy-public'
import { ServoyTestingModule } from '../../testing/servoytesting.module';

describe('AccordionComponent', () => {
  let component: ServoyBootstrapAccordion;
  let fixture: ComponentFixture<ServoyBootstrapAccordion>;
  const servoyApi: jasmine.SpyObj<ServoyApi> = jasmine.createSpyObj<ServoyApi>('ServoyApi', ['getMarkupId', 'trustAsHtml','registerComponent','unRegisterComponent']);

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapAccordion ],
      imports: [NgbModule, ServoyTestingModule],
      providers: [WindowRefService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
//    fixture = TestBed.createComponent(ServoyBootstrapAccordion);
//    component = fixture.componentInstance;
//    component.servoyApi =  servoyApi;
//    const tabs = [];
//    let tab = new Tab();
//    tab.name = "tab1";
//    tab.containedForm = "form1";
//    tab.text = "tab1";
//    tabs[0] = tab;
//    component.tabs = tabs;
//    fixture.detectChanges();
  });

  // it('should create', () => {
  //   expect(component).toBeTruthy();
  // });
});
