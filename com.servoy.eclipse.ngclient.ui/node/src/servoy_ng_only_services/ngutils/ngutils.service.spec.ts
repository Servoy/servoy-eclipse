import { TestBed, inject } from '@angular/core/testing';

import { NGUtilsService } from './ngutils.service';

import {WindowRefService} from '../../sablo/util/windowref.service'


describe('NGUtilsService', () => {
  let windowRef;
  beforeEach(() => {
     windowRef =  {}
     // we use a useFactory because when using useValue that will be cloned, so you can adjust windowRef later on.
    TestBed.configureTestingModule({
      providers: [NGUtilsService, {provide: WindowRefService, useFactory:()=> windowRef }]
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
