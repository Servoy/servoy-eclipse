import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultRectangle } from './rectangle';
import {SabloModule} from "../../sablo/sablo.module";
import { ServoyPublicModule } from '../../ngclient/servoy_public.module'

import {FormsModule} from "@angular/forms";
import {ServoyApi,TooltipService,FormattingService} from "../../ngclient/servoy_public";
describe('PasswordComponent', () => {
  let component: ServoyDefaultRectangle;
  let fixture: ComponentFixture<ServoyDefaultRectangle>;
  let servoyApi;

  beforeEach(async(() => {
      servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
      TestBed.configureTestingModule({
        declarations: [ ServoyDefaultRectangle ],
        imports: [SabloModule, FormsModule, ServoyPublicModule],
        providers: [FormattingService, TooltipService]
      })
      .compileComponents();
    }));
  
  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultRectangle);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
