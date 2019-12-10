import {async, ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import { ServoyDefaultHtmlarea  } from './htmlarea';
import { FormattingService, ServoyApi, TooltipService } from "../../ngclient/servoy_public";
import { SabloModule } from "../../sablo/sablo.module";
import { FormsModule } from "@angular/forms";
import { ServoyPublicModule } from '../../ngclient/servoy_public.module'
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
      declarations: [ ServoyDefaultHtmlarea],
      imports: [SabloModule, FormsModule,AngularEditorModule,HttpClientModule, ServoyPublicModule],
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
