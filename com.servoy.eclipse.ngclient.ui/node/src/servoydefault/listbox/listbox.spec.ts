import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SabloModule } from '../../sablo/sablo.module'
import { ServoyPublicModule } from '../../ngclient/servoy_public.module'

import { ServoyDefaultListBox } from './listbox';
import {  FormattingService, TooltipService} from '../../ngclient/servoy_public'

import { FormsModule } from '@angular/forms';


describe("ServoyDefaultListBox", () => {
  let component: ServoyDefaultListBox;
  let fixture: ComponentFixture<ServoyDefaultListBox>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultListBox],
      imports: [SabloModule,FormsModule, ServoyPublicModule],
      providers: [FormattingService,TooltipService]
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
