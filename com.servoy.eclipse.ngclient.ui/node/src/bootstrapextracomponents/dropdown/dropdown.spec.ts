import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ServoyBootstrapExtraDropdown } from './dropdown';
import { ServoyPublicModule } from '@servoy/public';
import { ClientFunctionService } from '../../ngclient/services/clientfunction.service';
import { ViewportService } from '../../ngclient/services/viewport.service';
import { ServoyTestingModule } from '../../testing/servoytesting.module';

describe('ServoyBootstrapExtraDropdown', () => {
  let component: ServoyBootstrapExtraDropdown;
  let fixture: ComponentFixture<ServoyBootstrapExtraDropdown>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapExtraDropdown ],
      imports: [ServoyTestingModule, ServoyPublicModule],
      providers: [ClientFunctionService, ViewportService ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapExtraDropdown);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
