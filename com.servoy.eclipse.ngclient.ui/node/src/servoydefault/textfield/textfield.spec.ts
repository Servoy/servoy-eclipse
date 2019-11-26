import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import {SabloModule} from '../../sablo/sablo.module'

import { ServoyDefaultTextField } from './textfield';
import {FormatFilterPipe,SvyFormat, FormattingService,DecimalkeyconverterDirective, StartEditDirective,TooltipDirective,TooltipService} from '../../ngclient/servoy_public'


describe("ServoyDefaultTextField", () => {
  let component: ServoyDefaultTextField;
  let fixture: ComponentFixture<ServoyDefaultTextField>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultTex tField, FormatFilterPipe, SvyFormat,DecimalkeyconverterDirective, StartEditDirective,TooltipDirective],
      imports: [SabloModule],
      providers: [FormatFilterPipe,FormattingService,TooltipService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultTextField);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
