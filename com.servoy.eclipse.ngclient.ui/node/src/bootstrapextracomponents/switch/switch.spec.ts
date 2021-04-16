import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { JwBootstrapSwitchNg2Module } from 'jw-bootstrap-switch-ng2';
import { ServoyPublicModule } from '@servoy/public';
import { ServoyTestingModule } from '../../testing/servoytesting.module';

import { ServoyBootstrapExtraSwitch } from './switch';

describe('BootstrapExtraSwitch', () => {
  let component: ServoyBootstrapExtraSwitch;
  let fixture: ComponentFixture<ServoyBootstrapExtraSwitch>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapExtraSwitch ],
      imports: [ JwBootstrapSwitchNg2Module, FormsModule, ServoyPublicModule, ServoyTestingModule ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapExtraSwitch);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
