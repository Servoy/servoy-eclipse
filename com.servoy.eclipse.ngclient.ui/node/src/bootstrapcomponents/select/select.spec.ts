import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapSelect } from './select';
import { ShowDisplayValuePipe } from '../lib/showDisplayValue.pipe';

describe('SelectComponent', () => {
  let component: ServoyBootstrapSelect;
  let fixture: ComponentFixture<ServoyBootstrapSelect>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapSelect ],
      providers: [ShowDisplayValuePipe]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapSelect);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});
