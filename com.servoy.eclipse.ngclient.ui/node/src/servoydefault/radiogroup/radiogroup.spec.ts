import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultRadiogroup } from './radiogroup';
import { SabloModule } from "../../sablo/sablo.module";
import { ServoyPublicModule } from '../../ngclient/servoy_public.module'
import { FormsModule } from "@angular/forms";
import { FormattingService, TooltipService } from "../../ngclient/servoy_public";

describe('ServoyDefaultRadiogroup', () => {
  let component: ServoyDefaultRadiogroup;
  let fixture: ComponentFixture<ServoyDefaultRadiogroup>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultRadiogroup],
      imports: [SabloModule, FormsModule, ServoyPublicModule],
      providers: [FormattingService, TooltipService]
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
