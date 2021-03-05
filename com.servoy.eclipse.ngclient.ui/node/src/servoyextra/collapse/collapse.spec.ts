import { LoggerFactory } from './../../sablo/logger.service';
import { ConverterService } from './../../sablo/converter.service';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyExtraCollapse } from './collapse';
import { FormService } from './../../ngclient/form.service';
import { SabloService } from '../../sablo/sablo.service';
import { WebsocketService } from '../../sablo/websocket.service';
import { WindowRefService } from '../../sablo/util/windowref.service';
import { ServicesService } from '../../sablo/services.service';
import { LoadingIndicatorService } from '../../sablo/util/loading-indicator/loading-indicator.service';
import { SessionStorageService } from '../../sablo/webstorage/sessionstorage.service';
import { ServoyService } from '../../ngclient/servoy.service';
import { I18NProvider, LocaleService } from '../../ngclient/servoy_public';
import { ClientFunctionService } from '../../ngclient/services/clientfunction.service';
import { SpecTypesService } from '../../sablo/spectypes.service';
import { SabloDeferHelper } from '../../sablo/defer.service';
import { ViewportService } from '../../ngclient/services/viewport.service';

describe('ServoyExtraCollapse', () => {
  let component: ServoyExtraCollapse;
  let fixture: ComponentFixture<ServoyExtraCollapse>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyExtraCollapse ],
      providers: [FormService,
        SabloService,
        WebsocketService,
        WindowRefService,
        ServicesService,
        ConverterService,
        LoggerFactory,
        SessionStorageService,
        ServoyService,
        LocaleService,
        I18NProvider,
        ClientFunctionService,
        SpecTypesService,
        SabloDeferHelper,
        ViewportService,
        LoadingIndicatorService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyExtraCollapse);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);  
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});