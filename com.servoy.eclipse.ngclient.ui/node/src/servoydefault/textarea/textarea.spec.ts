import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import {SabloModule} from '../../sablo/sablo.module'

import { ServoyDefaultTextArea } from './textarea';
import {FormatFilterPipe,SvyFormat, FormattingService,DecimalkeyconverterDirective, StartEditDirective, TooltipDirective, TooltipService} from '../../ngclient/servoy_public'


describe("ServoyDefaultTextArea", () => {
  let component: ServoyDefaultTextArea;
  let fixture: ComponentFixture<ServoyDefaultTextArea>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultTextArea, FormatFilterPipe, SvyFormat,StartEditDirective,TooltipDirective],
      imports: [SabloModule],
      providers: [FormatFilterPipe,FormattingService,TooltipService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultTextArea);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
