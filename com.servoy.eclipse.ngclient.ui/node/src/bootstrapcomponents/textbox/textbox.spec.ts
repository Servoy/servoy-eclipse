import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapTextbox } from './textbox';
import { FormattingService } from "../../ngclient/servoy_public";

describe('TextboxComponent', () => {
  let component: ServoyBootstrapTextbox;
  let fixture: ComponentFixture<ServoyBootstrapTextbox>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapTextbox ], 
      providers: [FormattingService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapTextbox);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});
