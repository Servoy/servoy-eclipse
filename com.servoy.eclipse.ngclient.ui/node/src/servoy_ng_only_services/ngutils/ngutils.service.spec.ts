import { TestBed, inject } from '@angular/core/testing';

import { SessionStorageService } from 'angular-web-storage';

import { NGUtilsService } from './ngutils.service';

import {WindowRefService} from '../../sablo/util/windowref.service'

import {ServiceChangeHandler} from '../../sablo/util/servicechangehandler'

import { SabloService } from '../../sablo/sablo.service';

import { WebsocketService } from '../../sablo/websocket.service';
import { ConverterService } from '../../sablo/converter.service';
import { LoggerFactory } from '../../sablo/logger.service'
import { ServicesService } from '../../sablo/services.service'
import { ServoyService } from "../../ngclient/servoy.service";

describe('NGUtilsService', () => {
  let windowRef: any;
  let servoyServiceRef: any;
  beforeEach(() => {
     windowRef =  {};
     servoyServiceRef =  {};
     windowRef.nativeWindow = {};
     // we use a useFactory because when using useValue that will be cloned, so you can adjust windowRef later on.
    TestBed.configureTestingModule({
      providers: [NGUtilsService, {provide: WindowRefService, useFactory:()=> windowRef }, 
                  ServiceChangeHandler, SabloService, WebsocketService, SessionStorageService, ConverterService, LoggerFactory, ServicesService, {provide: ServoyService, useFactory:()=> servoyServiceRef }]
    });
  });

  it('should be created', inject([NGUtilsService], (service: NGUtilsService) => {
    expect(service).toBeTruthy();
  }));
  
  it('it should return the user agent', inject([NGUtilsService], (service: NGUtilsService) => {
      windowRef.nativeWindow = {navigator:{userAgent:"test"}};
      expect(service.getUserAgent()).toBe("test"); // test
    }));
});
