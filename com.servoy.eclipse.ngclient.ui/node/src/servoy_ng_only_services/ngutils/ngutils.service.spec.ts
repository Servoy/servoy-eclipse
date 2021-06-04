import { TestBed, inject } from '@angular/core/testing';

import { NGUtilsService } from './ngutils.service';

import {SessionStorageService, WindowRefService} from '@servoy/public';

import { SabloService } from '../../sablo/sablo.service';

import { WebsocketService } from '../../sablo/websocket.service';
import { ConverterService } from '../../sablo/converter.service';
import { LoggerFactory } from '@servoy/public';
import { ServicesService } from '../../sablo/services.service';
import { ServoyService } from '../../ngclient/servoy.service';
import { PlatformLocation } from '@angular/common';
import { LoadingIndicatorService } from '../../sablo/util/loading-indicator/loading-indicator.service';
import { TestabilityService } from '../../sablo/testability.service';

describe('NGUtilsService', () => {
  let windowRef: any;
  let servoyServiceRef: any;
  let platformLocation: any;
  let sabloService : any;
  beforeEach(() => {
     windowRef =  {};
     servoyServiceRef =  {};
     windowRef.nativeWindow = {};
     windowRef.nativeWindow.location = {};
     platformLocation = jasmine.createSpyObj('PlatformLocation', ['onPopState']);
     sabloService = jasmine.createSpyObj('SabloService', ['connect']);
     // we use a useFactory because when using useValue that will be cloned, so you can adjust windowRef later on.
    TestBed.configureTestingModule({
      providers: [NGUtilsService, {provide: WindowRefService, useFactory:()=> windowRef },
                  {provide: SabloService, useValue:sabloService}, WebsocketService, TestabilityService, SessionStorageService,
                  ConverterService, LoggerFactory, ServicesService, LoadingIndicatorService,
                  {provide: ServoyService, useFactory:()=> servoyServiceRef},
                  {provide: PlatformLocation, useValue: platformLocation}]
    });
  });

  it('should be created', inject([NGUtilsService], (service: NGUtilsService) => {
    expect(service).toBeTruthy();
  }));

  it('it should return the user agent', inject([NGUtilsService], (service: NGUtilsService) => {
      windowRef.nativeWindow = {navigator:{userAgent:'test'}};
      expect(service.getUserAgent()).toBe('test'); // test
  }));

  it('it should call the setBackActionCallback method', inject([NGUtilsService], (service: NGUtilsService) => {
      service.backActionCB = '';
      expect(platformLocation.onPopState).toHaveBeenCalled();
  }));
});
