import { TestBed, inject } from '@angular/core/testing';

import {WebsocketService} from '../sablo/websocket.service';
import {SabloService} from '../sablo/sablo.service';
import {ConverterService} from '../sablo/converter.service';
import {LoggerFactory} from '@servoy/public';
import {WindowRefService} from '@servoy/public';

import { FormService } from './form.service';
import {ServoyService} from './servoy.service';
import { ClientFunctionService } from '../sablo/clientfunction.service';

describe('FormService', () => {
  let websocketService;
  let sabloService;
  let converterService;
  let servoyService;
  beforeEach(() => {
      websocketService = jasmine.createSpyObj('WebsocketService', {getSession: new Promise<any>((e)=>{})});
      sabloService = jasmine.createSpyObj('SabloService', ['connect']);
      converterService = jasmine.createSpyObj('SabloService', ['convertFromClientToServer']);
      servoyService = jasmine.createSpyObj('ServoyService', ['setFindMode']);
    TestBed.configureTestingModule({
      providers: [FormService,
                          LoggerFactory,
                          WindowRefService,
                          ClientFunctionService,
                          {provide: WebsocketService, useValue:websocketService},
                          {provide: SabloService, useValue:sabloService},
                          {provide: ConverterService, useValue:converterService},
                          {provide: ServoyService, useValue: servoyService}],
    });
  });

  it('should be created', inject([FormService], (service: FormService) => {
      expect(service).toBeTruthy();
      expect(websocketService.getSession).toHaveBeenCalled();
  }));
});

class WebsocketServiceMock {
}
