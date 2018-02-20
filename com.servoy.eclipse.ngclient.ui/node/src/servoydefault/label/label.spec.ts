import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultLabel} from './label';


import {SabloModule} from '../../sablo/sablo.module'

import {ServoyApiModule} from '../../servoyapi/servoy_api.module'

describe('SvLabel', () => {
  let component: ServoyDefaultLabel;
  let fixture: ComponentFixture<ServoyDefaultLabel>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultLabel ],
      imports:[
               SabloModule,
               ServoyApiModule],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultLabel);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
