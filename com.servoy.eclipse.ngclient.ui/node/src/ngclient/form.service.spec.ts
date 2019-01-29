import { TestBed, inject } from '@angular/core/testing';

import {EventEmitter } from '@angular/core';

import {WebsocketService} from '../sablo/websocket.service';
import {SabloService} from '../sablo/sablo.service';
import {ConverterService} from '../sablo/converter.service';
import {LoggerFactory} from '../sablo/logger.service';
import {WindowRefService} from '../sablo/util/windowref.service';

import { FormService } from './form.service';
import {ServoyService} from "./servoy.service";

describe('FormService', () => {
  let websocketService;
  let sabloService;
  let converterService;
  let servoyService;
  beforeEach(() => {
      websocketService = jasmine.createSpyObj("WebsocketService", {getSession: new Promise<any>((e)=>{})});
      sabloService = jasmine.createSpyObj("SabloService", ["connect"]);
      converterService = jasmine.createSpyObj("SabloService", ["convertFromClientToServer"]);
      servoyService = jasmine.createSpyObj("ServoyService", ["setFindMode"]);
    TestBed.configureTestingModule({
      providers: [FormService, 
                          LoggerFactory,
                          WindowRefService,
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
