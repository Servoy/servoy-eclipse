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
import { PlatformLocation } from '@angular/common';

describe('NGUtilsService', () => {
  let windowRef: any;
  let servoyServiceRef: any;
  let platformLocation: any;
  beforeEach(() => {
     windowRef =  {};
     servoyServiceRef =  {};
     windowRef.nativeWindow = {};
     platformLocation = jasmine.createSpyObj("PlatformLocation", ["pushState", "onPopState"]);
     // we use a useFactory because when using useValue that will be cloned, so you can adjust windowRef later on.
    TestBed.configureTestingModule({
      providers: [NGUtilsService, {provide: WindowRefService, useFactory:()=> windowRef }, 
                  ServiceChangeHandler, SabloService, WebsocketService, SessionStorageService, 
                  ConverterService, LoggerFactory, ServicesService, 
                  {provide: ServoyService, useFactory:()=> servoyServiceRef},
                  {provide: PlatformLocation, useValue: platformLocation}]
    });
  });

  it('should be created', inject([NGUtilsService], (service: NGUtilsService) => {
    expect(service).toBeTruthy();
  }));
  
  it('it should return the user agent', inject([NGUtilsService], (service: NGUtilsService) => {
      windowRef.nativeWindow = {navigator:{userAgent:"test"}};
      expect(service.getUserAgent()).toBe("test"); // test
  }));
  
  it('it should call the setBackActionCallback method', inject([NGUtilsService], (service: NGUtilsService) => {
      service.backActionCB = "";
      expect(platformLocation.pushState).toHaveBeenCalledWith('captureBack', null, null);
      expect(platformLocation.pushState).not.toHaveBeenCalledWith('wrongArgument', null, null);
      expect(platformLocation.onPopState).toHaveBeenCalled();
  }));
});
