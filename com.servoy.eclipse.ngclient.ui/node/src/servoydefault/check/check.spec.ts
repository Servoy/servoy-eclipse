import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultCheck } from './check';
import {SabloModule} from "../../sablo/sablo.module";
import {
  DecimalkeyconverterDirective,
  FormatFilterPipe,
  FormattingService, StartEditDirective,ServoyApi,
  SvyFormat
} from "../../ngclient/servoy_public";
import {FormsModule} from "@angular/forms";

describe('CheckComponent', () => {
  let component: ServoyDefaultCheck;
  let fixture: ComponentFixture<ServoyDefaultCheck>;
  let servoyApi;
  beforeEach(async(() => {
  servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultCheck ],
      imports: [SabloModule, FormsModule],
      providers: [FormattingService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultCheck);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
