import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapTabpanel } from './tabpanel';
import { WindowRefService } from '../../sablo/util/windowref.service';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { SabloModule } from '../../sablo/sablo.module';
import { ServoyApi } from '../../ngclient/servoy_public';

describe('TabpanelComponent', () => {
  let component: ServoyBootstrapTabpanel;
  let fixture: ComponentFixture<ServoyBootstrapTabpanel>;
  const servoyApi: jasmine.SpyObj<ServoyApi> = jasmine.createSpyObj<ServoyApi>('ServoyApi', ['getMarkupId', 'trustAsHtml','registerComponent','unRegisterComponent']);

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapTabpanel ],
      imports: [NgbModule, SabloModule],
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
