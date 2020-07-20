import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapSelect } from './select';
import { ShowDisplayValuePipe } from "../lib/showDisplayValue.pipe";

describe('SelectComponent', () => {
  let component: ServoyBootstrapSelect;
  let fixture: ComponentFixture<ServoyBootstrapSelect>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapSelect ], 
      providers: [ShowDisplayValuePipe]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapSelect);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
