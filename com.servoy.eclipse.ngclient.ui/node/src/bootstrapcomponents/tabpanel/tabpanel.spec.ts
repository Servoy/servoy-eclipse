import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapTabpanel } from './tabpanel';
import { ServoyPublicModule, WindowRefService } from 'servoy-public';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { ServoyApi } from 'servoy-public';

describe('TabpanelComponent', () => {
  let component: ServoyBootstrapTabpanel;
  let fixture: ComponentFixture<ServoyBootstrapTabpanel>;
  const servoyApi: jasmine.SpyObj<ServoyApi> = jasmine.createSpyObj<ServoyApi>('ServoyApi', ['getMarkupId', 'trustAsHtml','registerComponent','unRegisterComponent']);

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapTabpanel ],
      imports: [NgbModule, ServoyTestingModule, ServoyPublicModule],
      providers: [WindowRefService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapTabpanel);
    component = fixture.componentInstance;
    component.servoyApi =  servoyApi;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
