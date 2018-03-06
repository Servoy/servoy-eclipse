import { TestBed, inject } from '@angular/core/testing';

import { ConverterService } from './converter.service';

import {SpecTypesService} from '../sablo/spectypes.service'

describe('ConverterService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConverterService,SpecTypesService]
    });
  });

  it('should be created', inject([ConverterService], (service: ConverterService) => {
    expect(service).toBeTruthy();
  }));
});
