import { Injectable, NgModule, NgZone } from '@angular/core';
import { ConverterService } from '../sablo/converter.service';
import { IDeferedState, SabloDeferHelper } from '../sablo/defer.service';
import { ReconnectingWebSocket } from '../sablo/io/reconnecting.websocket';
import { LoggerFactory, ServoyPublicService, SessionStorageService, IDeferred, WindowRefService } from '@servoy/public';
import { LoadingIndicatorService } from '../sablo/util/loading-indicator/loading-indicator.service';
import { WebsocketService, WebsocketSession } from '../sablo/websocket.service';
import { ServoyPublicServiceImpl } from '../ngclient/services/servoy_public_impl.service';
import { FormService } from '../ngclient/form.service';
import { LocaleService } from '../ngclient/locale.service';
import { ApplicationService } from '../ngclient/services/application.service';
import { ServoyService } from '../ngclient/servoy.service';
import { I18NProvider } from '../ngclient/services/i18n_provider.service';
import { SvyUtilsService } from '../ngclient/utils.service';
import { SabloService } from '../sablo/sablo.service';
import { PopupFormService } from '../ngclient/services/popupform.service';

class TestDeferred implements IDeferred<any> {
  promise: Promise<any> = {
      then: (_value) => null, catch: (_err) => null,
      finally:() => null,
      [Symbol.toStringTag]: ''
  };
  reject(_reason: any) {
  }
  resolve(_value: any) {
  }
}

@Injectable()
class TestSabloDeferHelper extends SabloDeferHelper {
  public getNewDeferId(state: IDeferedState) {
      state.deferred['1'] = { defer: new TestDeferred(), timeoutId: 1 };
      return 1;
  }
}

@Injectable()
export class TestWebsocketService extends WebsocketService {
  constructor(private _windowRef: WindowRefService,
        private _converterService: ConverterService<unknown>,
        private _logFactory: LoggerFactory,
        private _loadingIndicatorService: LoadingIndicatorService,
        private _ngZone: NgZone) {
     super(_windowRef, _converterService, _logFactory, _loadingIndicatorService, _ngZone);
    }

  connect(): WebsocketSession {
      return new WebsocketSession({} as ReconnectingWebSocket, this,
        this._windowRef, this._converterService, this._loadingIndicatorService, this._ngZone, this._logFactory );
  }
  disconnect() {
  }
}

@Injectable()
export class TestSabloService extends SabloService {
        constructor(private wService: WebsocketService, sessionStorage: SessionStorageService,windowRefService: WindowRefService, logFactory: LoggerFactory) {
            super(wService, sessionStorage,windowRefService, logFactory);
             sessionStorage.remove('svy_session_lock');
        }
}

@NgModule({
  declarations: [

  ],
  imports: [
  ],
  exports: [


  ],
  providers: [
    { provide: SabloDeferHelper, useClass: TestSabloDeferHelper },
    { provide: WebsocketService, useClass: TestWebsocketService },
    { provide: SabloService, useClass: TestSabloService },
    { provide: FormService, useValue: {} },
    { provide: ApplicationService, useValue: {} },
    { provide: ServoyService, useValue: {} },
    ServoyPublicServiceImpl, { provide: ServoyPublicService, useExisting: ServoyPublicServiceImpl },
    LocaleService, I18NProvider, SvyUtilsService,PopupFormService
             ],
  schemas: [

  ]
})
export class ServoyTestingModule { }
