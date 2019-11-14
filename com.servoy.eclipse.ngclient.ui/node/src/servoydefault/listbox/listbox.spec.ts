import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import {SabloModule} from '../../sablo/sablo.module'

import { ServoyDefaultListBox } from './listbox';
import {FormatFilterPipe,SvyFormat, FormattingService, StartEditDirective, TooltipDirective, TooltipService} from '../../ngclient/servoy_public'

import { FormsModule } from '@angular/forms';


describe("ServoyDefaultTextField", () => {
  let component: ServoyDefaultListBox;
  let fixture: ComponentFixture<ServoyDefaultListBox>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultListBox, FormatFilterPipe, SvyFormat, StartEditDirective,TooltipDirective],
      imports: [SabloModule,FormsModule],
      providers: [FormatFilterPipe,FormattingService,TooltipService]
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
