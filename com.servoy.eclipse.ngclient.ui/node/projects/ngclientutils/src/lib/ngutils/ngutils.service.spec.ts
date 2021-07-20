import { TestBed, inject } from '@angular/core/testing';

import { NGUtilsService } from './ngutils.service';

import { WindowRefService, ServoyPublicTestingModule, LoggerFactory} from '@servoy/public';

import { PlatformLocation } from '@angular/common';

describe('NGUtilsService', () => {
  let windowRef: any;
  let platformLocation: any;
  beforeEach(() => {
     windowRef =  {};
     windowRef.nativeWindow = {};
     windowRef.nativeWindow.location = {};
     platformLocation = jasmine.createSpyObj('PlatformLocation', ['onPopState']);
     // we use a useFactory because when using useValue that will be cloned, so you can adjust windowRef later on.
    TestBed.configureTestingModule({
      providers: [NGUtilsService, {provide: WindowRefService, useFactory:()=> windowRef },
                   LoggerFactory,
                  {provide: PlatformLocation, useValue: platformLocation}],
       imports: [ ServoyPublicTestingModule],            
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
