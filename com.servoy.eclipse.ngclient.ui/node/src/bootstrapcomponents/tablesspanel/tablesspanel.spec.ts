import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { FormService } from '../../ngclient/form.service';
import { ServoyPublicModule } from 'servoy-public';
import { ServoyTestingModule } from '../../testing/servoytesting.module';

import { ServoyBootstrapTablesspanel } from './tablesspanel';

describe('TablesspanelComponent', () => {
  let component: ServoyBootstrapTablesspanel;
  let fixture: ComponentFixture<ServoyBootstrapTablesspanel>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapTablesspanel ],
      imports: [NgbModule, ServoyTestingModule, ServoyPublicModule],
      providers: [ { provide: FormService, useValue: {getFormCacheByName: () => {} }} ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapTablesspanel);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
