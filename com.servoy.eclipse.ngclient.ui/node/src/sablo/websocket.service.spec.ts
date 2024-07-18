import { TestBed, inject , fakeAsync, flush, discardPeriodicTasks} from '@angular/core/testing';

import { WebsocketService } from './websocket.service';

import {WindowRefService} from '@servoy/public';
import { ServicesService } from './services.service';
import { ConverterService } from './converter.service';
import { LoggerFactory } from '@servoy/public';
import { LoadingIndicatorService } from './util/loading-indicator/loading-indicator.service';


describe('WebsocketService', () => {
    let windowRef;
    let normalWebSocket = null;
  beforeEach(() => {
      normalWebSocket =  window['WebSocket'];
      window['Web' + 'Socket'] = WebSocketMock;

      windowRef =  {nativeWindow: {}};
      const servicesService = jasmine.createSpyObj('ServicesService', ['callServiceApi', 'updateServiceScopes']);
      const converterService = jasmine.createSpyObj('ConverterService', ['convertFromServerToClient', 'convertFromClientToServer', 'convertClientObject']);
      const loadingIndicatorService = jasmine.createSpyObj('SabloLoadingIndicator', ['showLoading', 'hideLoading', 'isShowing']);
      TestBed.configureTestingModule({
      providers: [WebsocketService,
                  {provide: WindowRefService, useFactory: () => windowRef},
                  {provide: ServicesService, useFactory: () => servicesService },
                  {provide: ConverterService, useFactory: () => converterService },
                  {provide: LoadingIndicatorService, useFactory: () => loadingIndicatorService}, LoggerFactory]
    });
  });

  afterEach(() => {
      window['WebSocket'] = normalWebSocket;
  });

  it('should be created', inject([WebsocketService], (service: WebsocketService) => {
    expect(service).toBeTruthy();
  }));
  it('should be make a connection', inject([WebsocketService], fakeAsync((service: WebsocketService) => {
      windowRef.nativeWindow = { location: {protocol: 'http', host: 'localhost', pathname: '/'}};
     const session = service.connect('', [], {}, null);
    flush(2);
     expect( session.isConnected()).toBeTruthy();
     discardPeriodicTasks();
    })));
});


class WebSocketMock {
    constructor(url: string) {
        this.url = url;
        WebSocketMock.instance = this;
        setTimeout(() => {
            WebSocketMock.instance['onopen'](new CustomEvent('open'));
        }, 1);
    }
    public static instance: WebSocketMock;
    data: any;
    public url: string;
    public closed = false;

    public close() {
        this.closed = true;
        WebSocketMock.instance['onclose'](new CustomEvent('close'));
    }

    public send(data) {
        this.data = data;
    }
}
