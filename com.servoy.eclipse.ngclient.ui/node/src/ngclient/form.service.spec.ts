import { TestBed, inject } from '@angular/core/testing';

import {EventEmitter } from '@angular/core';

import {WebsocketService} from '../sablo/websocket.service';
import {SabloService} from '../sablo/sablo.service';
import {ConverterService} from '../sablo/converter.service';


import { FormService } from './form.service';

describe('FormService', () => {
  let websocketService;
  let sabloService;
  let converterService;
  beforeEach(() => {
      websocketService = jasmine.createSpyObj("WebsocketService", {getSession: new Promise<any>((e)=>{})});
      sabloService = jasmine.createSpyObj("SabloService", ["connect"]);
      converterService = jasmine.createSpyObj("SabloService", ["convertFromClientToServer"]);
    TestBed.configureTestingModule({
      providers: [FormService, {provide: WebsocketService, useValue:websocketService},
                                                   {provide: SabloService, useValue:sabloService},
                                                   {provide: ConverterService, useValue:converterService}],
    });
  });

  it('should be created', inject([FormService], (service: FormService) => {
      expect(service).toBeTruthy();
      expect(websocketService.getSession).toHaveBeenCalled();
  }));
});

class WebsocketServiceMock {
}