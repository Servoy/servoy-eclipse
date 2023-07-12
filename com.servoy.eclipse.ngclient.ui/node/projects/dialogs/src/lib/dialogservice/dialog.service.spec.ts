import { TestBed, inject } from '@angular/core/testing';

import { ServoyPublicTestingModule } from '@servoy/public';

import { PlatformLocation } from '@angular/common';
import { DialogService } from './dialogs.service';

describe('NGUtilsService', () => {
  beforeEach(() => {
     // we use a useFactory because when using useValue that will be cloned, so you can adjust windowRef later on.
    TestBed.configureTestingModule({
      providers: [DialogService],
       imports: [ ServoyPublicTestingModule],
    });
  });

  it('should be created', inject([DialogService], (service: DialogService) => {
    expect(service).toBeTruthy();
  }));

});
