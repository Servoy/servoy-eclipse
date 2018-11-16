import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultPassword } from './password';
import {SabloModule} from "../../sablo/sablo.module";
import {FormsModule} from "@angular/forms";
import {FormattingService, ServoyApi,TooltipService,TooltipDirective} from "../../ngclient/servoy_public";
describe('PasswordComponent', () => {
  let component: ServoyDefaultPassword;
  let fixture: ComponentFixture<ServoyDefaultPassword>;
  let servoyApi;

  beforeEach(async(() => {
      servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
      TestBed.configureTestingModule({
        declarations: [ ServoyDefaultPassword, TooltipDirective ],
        imports: [SabloModule, FormsModule],
        providers: [FormattingService, TooltipService]
      })
      .compileComponents();
    }));
  
  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultPassword);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
