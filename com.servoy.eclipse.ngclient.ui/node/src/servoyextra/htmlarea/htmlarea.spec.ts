import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ServoyExtraHtmlarea  } from './htmlarea';
import { LocaleService, FormattingService, ServoyApi, TooltipService, SvyUtilsService } from '../../ngclient/servoy_public';
import { SabloModule } from '../../sablo/sablo.module';
import { FormsModule } from '@angular/forms';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { HttpClientModule } from '@angular/common/http';
import { ApplicationService } from '../../ngclient/services/application.service';
import { ServoyService } from '../../ngclient/servoy.service';
import { ClientFunctionService } from '../../ngclient/services/clientfunction.service';
import { ViewportService } from '../../ngclient/services/viewport.service';
import { ServerDataService } from '../../ngclient//services/serverdata.service';
import { I18NProvider } from '../../ngclient/services/i18n_provider.service';
import { FormService } from '../../ngclient/form.service';
import { EditorModule } from '@tinymce/tinymce-angular';

describe('HtmlareaComponent', () => {
  let component: ServoyExtraHtmlarea;
  let fixture: ComponentFixture<ServoyExtraHtmlarea>;

  const servoyApi: jasmine.SpyObj<ServoyApi> = jasmine.createSpyObj<ServoyApi>('ServoyApi', ['getMarkupId', 'isInDesigner','registerComponent','unRegisterComponent', 'getClientProperty']);

    beforeEach(waitForAsync(() => {

    TestBed.configureTestingModule({
      declarations: [ ServoyExtraHtmlarea ],
      imports: [EditorModule, SabloModule, FormsModule, HttpClientModule, ServoyPublicModule],
      providers: [FormattingService, TooltipService, { provide: LocaleService, useValue: {getLocale: () => 'en' } },ServoyService,ClientFunctionService, 
      ViewportService,I18NProvider, SvyUtilsService, FormService, { provide:ServerDataService, useValue: {init: ()=>{}}}, ApplicationService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyExtraHtmlarea);

    fixture.componentInstance.servoyApi = servoyApi;
    component = fixture.componentInstance;
    component.dataProviderID = 'WhatArea';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
