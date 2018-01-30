import { TestBed, inject } from '@angular/core/testing';

import { ConverterService } from './converter.service';

describe('ConverterService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConverterService]
    });
  });

  it('should be created', inject([ConverterService], (service: ConverterService) => {
    expect(service).toBeTruthy();
  }));
});
