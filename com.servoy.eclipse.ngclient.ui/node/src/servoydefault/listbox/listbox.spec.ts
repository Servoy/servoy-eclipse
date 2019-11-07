import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import {SabloModule} from '../../sablo/sablo.module'

import { ServoyDefaultListBox } from './textfield';
import {FormatFilterPipe,SvyFormat, FormattingService, StartEditDirective} from '../../ngclient/servoy_public'


describe("ServoyDefaultTextField", () => {
  let component: ServoyDefaultListBox;
  let fixture: ComponentFixture<ServoyDefaultListBox>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultListBox, FormatFilterPipe, SvyFormat, StartEditDirective],
      imports: [SabloModule],
      providers: [FormatFilterPipe,FormattingService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultListBox);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
