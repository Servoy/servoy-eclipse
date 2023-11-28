import { TestBed, inject } from '@angular/core/testing';

import { ConverterService } from './converter.service';

import {SpecTypesService, LoggerFactory} from '@servoy/public';
import {WindowRefService} from '@servoy/public';


describe('ConverterService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConverterService,SpecTypesService,LoggerFactory,WindowRefService]
    });
  });

  it('should be created', inject([ConverterService], (service: ConverterService<unknown>) => {
    expect(service).toBeTruthy();
  }));
});
