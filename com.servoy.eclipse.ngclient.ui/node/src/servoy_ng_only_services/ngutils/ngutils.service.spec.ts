import { TestBed, inject } from '@angular/core/testing';

import { NGUtilsService } from './ngutils.service';

import {WindowRefService} from '../../app/util/windowref.service'


describe('NGUtilsService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [NGUtilsService, {provide: WindowRefService, useValue: new WindowRefServiceStub() }]
    });
  });

  it('should be created', inject([NGUtilsService], (service: NGUtilsService) => {
    expect(service).toBeTruthy();
  }));
  
  it('it should return the user agent', inject([NGUtilsService], (service: NGUtilsService) => {
      expect(service.getUserAgent()).toBe("test");
    }));
});

class  WindowRefServiceStub {
    get nativeWindow (): any {
        return {navigator:{userAgent:"test"}};
    }
}