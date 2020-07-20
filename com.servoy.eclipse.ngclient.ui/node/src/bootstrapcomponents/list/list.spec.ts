import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapList } from './list';
import { DatalistPolyFill } from './lib/purejs-datalist-polyfill/datalist.polyfill';
import { ShowDisplayValuePipe } from "../lib/showDisplayValue.pipe";

describe('ListComponent', () => {
  let component: ServoyBootstrapList;
  let fixture: ComponentFixture<ServoyBootstrapList>;
  let datalistPolyfill;

  beforeEach(async(() => {
      
    datalistPolyfill = jasmine.createSpyObj("DatalistPolyFill", ["apply"]);
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapList ],  
      providers: [{provide: DatalistPolyFill, useValue: datalistPolyfill}, ShowDisplayValuePipe] 
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});
