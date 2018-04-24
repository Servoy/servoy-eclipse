import { TestBed, inject } from '@angular/core/testing';

import { ConverterService } from './converter.service';

import {SpecTypesService} from '../sablo/spectypes.service'

import {LoggerService} from './logger.service'
import {WindowRefService} from './util/windowref.service'


describe('ConverterService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConverterService,SpecTypesService,LoggerService,WindowRefService]
    });
  });

  it('should be created', inject([ConverterService], (service: ConverterService) => {
    expect(service).toBeTruthy();
  }));
});
