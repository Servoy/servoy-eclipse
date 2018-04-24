import { TestBed, inject } from '@angular/core/testing';

import { ConverterService } from './converter.service';

import {SpecTypesService} from '../sablo/spectypes.service'

import {LoggerService} from './logger.service'

describe('ConverterService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConverterService,SpecTypesService,LoggerService]
    });
  });

  it('should be created', inject([ConverterService], (service: ConverterService) => {
    expect(service).toBeTruthy();
  }));
});
