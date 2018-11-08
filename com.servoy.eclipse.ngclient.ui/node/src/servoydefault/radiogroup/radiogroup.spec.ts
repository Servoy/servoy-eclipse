import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultRadiogroup } from './radiogroup';
import {SabloModule} from "../../sablo/sablo.module";
import {FormsModule} from "@angular/forms";
import {FormattingService, SvyFormat} from "../../ngclient/servoy_public";
import {NotNullOrEmptyPipe} from "../../ngclient/pipes/pipes";

describe('ServoyDefaultRadiogroup', () => {
  let component: ServoyDefaultRadiogroup;
  let fixture: ComponentFixture<ServoyDefaultRadiogroup>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultRadiogroup, NotNullOrEmptyPipe, SvyFormat],
      imports: [SabloModule, FormsModule],
      providers: [NotNullOrEmptyPipe,FormattingService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultRadiogroup);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
