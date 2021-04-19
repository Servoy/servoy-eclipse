import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { ServoyDefaultHtmlarea  } from './htmlarea';
import { LocaleService, FormattingService, ServoyApi, TooltipService, SvyUtilsService } from '../../ngclient/servoy_public';
import { SabloModule } from '../../sablo/sablo.module';
import { FormsModule } from '@angular/forms';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { ApplicationService } from '../../ngclient/services/application.service';
import {HttpClientModule} from '@angular/common/http';
import { ServoyService } from '../../ngclient/servoy.service';
import { ClientFunctionService } from '../../ngclient/services/clientfunction.service';
import { ViewportService } from '../../ngclient/services/viewport.service';
import { ServerDataService } from '../../ngclient//services/serverdata.service';
import { I18NProvider } from '../../ngclient/services/i18n_provider.service';
import { FormService } from '../../ngclient/form.service';
import { EditorModule, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';

describe('HtmlareaComponent', () => {
  let component: ServoyDefaultHtmlarea;
  let fixture: ComponentFixture<ServoyDefaultHtmlarea>;

  const servoyApi: jasmine.SpyObj<ServoyApi> = jasmine.createSpyObj<ServoyApi>('ServoyApi', ['getMarkupId', 'isInDesigner','registerComponent','unRegisterComponent', 'getClientProperty']);

    beforeEach(waitForAsync(() => {

    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultHtmlarea],
      imports: [EditorModule, SabloModule, FormsModule, HttpClientModule, ServoyPublicModule],
      providers: [FormattingService, TooltipService, { provide: LocaleService, useValue: {getLocale: () => 'en' } }, ServoyService,ClientFunctionService,
        , I18NProvider, SvyUtilsService, FormService, { provide:ServerDataService, useValue: {init: ()=>{}}} , ViewportService, ApplicationService,  
        { provide: TINYMCE_SCRIPT_SRC, useValue: 'tinymce/tinymce.min.js' }]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultHtmlarea);

    fixture.componentInstance.servoyApi = servoyApi;
    component = fixture.componentInstance;
    component.dataProviderID = 'WhatArea';
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });

});
