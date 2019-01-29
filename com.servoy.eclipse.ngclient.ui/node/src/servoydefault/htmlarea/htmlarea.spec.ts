import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { ServoyDefaultHtmlarea  } from './htmlarea';
// import {HtmlFilterPipe} from "../../ngclient/pipes/pipes";
import {FormattingService, ServoyApi} from "../../ngclient/servoy_public";
import {SabloModule} from "../../sablo/sablo.module";
import {FormsModule} from "@angular/forms";

describe('HtmlareaComponent', () => {
  let component: ServoyDefaultHtmlarea;
  let fixture: ComponentFixture<ServoyDefaultHtmlarea>;
  let servoyApi;

  beforeEach(async(() => {
    servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId"]);

    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultHtmlarea ],
      imports: [SabloModule, FormsModule],
      providers: [FormattingService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultHtmlarea);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
