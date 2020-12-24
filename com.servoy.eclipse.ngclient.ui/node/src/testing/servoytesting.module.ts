import { Injectable, NgModule, NgZone } from '@angular/core';
import { ConverterService } from '../sablo/converter.service';
import { IDeferedState, SabloDeferHelper } from '../sablo/defer.service';
import { ReconnectingWebSocket } from '../sablo/io/reconnecting.websocket';
import { LoggerFactory } from '../sablo/logger.service';
import { ServicesService } from '../sablo/services.service';
import { IDeferred } from '../sablo/util/deferred';
import { LoadingIndicatorService } from '../sablo/util/loading-indicator/loading-indicator.service';
import { WindowRefService } from '../sablo/util/windowref.service';
import { WebsocketService, WebsocketSession } from '../sablo/websocket.service';


class TestDeferred implements IDeferred<any> {
  promise: Promise<any>;
  reject(reason: any) {
  }
  resolve(value: any) {
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
  constructor(private ref: WindowRefService,
      private serv: ServicesService,
      private conv: ConverterService,
      private logFac: LoggerFactory,
      private loading: LoadingIndicatorService,
      private zone: NgZone) {
     super(ref, serv, conv, logFac, loading,zone);
    }

  connect(): WebsocketSession {
      return new WebsocketSession({} as ReconnectingWebSocket, this, this.serv, this.ref, this.conv, this.logFac, this.loading, this.zone);
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
    { provide: WebsocketService, useClass: TestWebsocketService }
             ],
  schemas: [

  ]
})
export class ServoyTestingModule { }
