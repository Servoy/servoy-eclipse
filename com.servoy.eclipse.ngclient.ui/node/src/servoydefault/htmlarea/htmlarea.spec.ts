import {async, ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import { ServoyDefaultHtmlarea  } from './htmlarea';
import {FormattingService, ServoyApi, TooltipDirective, TooltipService} from "../../ngclient/servoy_public";
import {SabloModule} from "../../sablo/sablo.module";
import {FormsModule} from "@angular/forms";
import {By} from "@angular/platform-browser";
import { AngularEditorModule } from '@kolkov/angular-editor';
import {HttpClientModule} from '@angular/common/http';

describe('HtmlareaComponent', () => {
  let component: ServoyDefaultHtmlarea;
  let fixture: ComponentFixture<ServoyDefaultHtmlarea>;

  let servoyApi;

    beforeEach(async(() => {
    servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","isInDesigner"]);

    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultHtmlarea,TooltipDirective],
      imports: [SabloModule, FormsModule,AngularEditorModule,HttpClientModule],
      providers: [FormattingService,TooltipService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultHtmlarea);

    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;
    component = fixture.componentInstance;
    component.dataProviderID = "WhatArea";
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
