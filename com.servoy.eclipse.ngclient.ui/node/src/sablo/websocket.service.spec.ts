import { TestBed, inject , fakeAsync,flush,discardPeriodicTasks} from '@angular/core/testing';

import { WebsocketService } from './websocket.service';

import {WindowRefService} from './util/windowref.service'
import { ServicesService } from './services.service'
import { ConverterService } from './converter.service'
import {LoggerService} from './logger.service'

describe('WebsocketService', () => {
    let windowRef;
    let normalWebSocket = null;
  beforeEach(() => {
      normalWebSocket =  window["WebSocket"];
      window["WebSocket"] = WebSocketMock;
      
      windowRef =  {nativeWindow:{}}
     const servicesService = jasmine.createSpyObj('ServicesService', ['callServiceApi','updateServiceScopes']);
      const converterService = jasmine.createSpyObj('ConverterService', ['convertFromServerToClient','convertFromClientToServer','convertClientObject']);
    TestBed.configureTestingModule({
      providers: [WebsocketService, 
                  {provide: WindowRefService, useFactory:()=>windowRef},
                  {provide: ServicesService, useFactory:()=>servicesService },
                  {provide: ConverterService, useFactory:()=>converterService }, LoggerService]
    });
  });
  
  afterEach(() => {
      window["WebSocket"] = normalWebSocket;
  })

  it('should be created', inject([WebsocketService], (service: WebsocketService) => {
    expect(service).toBeTruthy();
  }));
  it('should be make a connection', inject([WebsocketService], fakeAsync((service: WebsocketService) => {
      windowRef.nativeWindow = { location: {protocol:'http', host:"localhost",pathname:"/"}}
     const session = service.connect({}, {}, {}, null);
      session.onopen((event)=> {console.log(event)})
       flush(2);
     expect( session.isConnected()).toBeTruthy();
     discardPeriodicTasks();
    })));
});


class WebSocketMock {
    data: any;
    public static instance:WebSocketMock;
    public url:string;
    public closed = false;
    constructor(url:string) {
        this.url = url;
        WebSocketMock.instance = this;
        setTimeout(() => {
            WebSocketMock.instance["onopen"](new CustomEvent("open"));
        },1);
    }
    
    public close() {
        this.closed = true;
        WebSocketMock.instance["onclose"](new CustomEvent("close"));
    }
    
    public send(data) {
        this.data = data;
    }
}