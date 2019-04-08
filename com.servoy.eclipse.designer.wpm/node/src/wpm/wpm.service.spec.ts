import { TestBed } from '@angular/core/testing';

import { WpmService } from './wpm.service';

describe('WpmService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: WpmService = TestBed.get(WpmService);
    expect(service).toBeTruthy();
  });
});
