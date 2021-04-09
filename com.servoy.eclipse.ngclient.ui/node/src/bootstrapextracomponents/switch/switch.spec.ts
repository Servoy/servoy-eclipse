import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { JwBootstrapSwitchNg2Module } from 'jw-bootstrap-switch-ng2';

import { ServoyBootstrapExtraSwitch } from './switch';

describe('BootstrapExtraSwitch', () => {
  let component: ServoyBootstrapExtraSwitch;
  let fixture: ComponentFixture<ServoyBootstrapExtraSwitch>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapExtraSwitch ],
      imports: [ JwBootstrapSwitchNg2Module ]
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
