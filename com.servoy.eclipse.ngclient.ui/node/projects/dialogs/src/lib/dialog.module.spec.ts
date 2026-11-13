import { describe, it, expect } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { ServoyPublicService } from '@servoy/public';

import { DialogModule } from './dialog.module';
import { DialogService } from './dialogservice/dialogs.service';

describe('DialogModule', () => {
  it('should provide the DialogService', () => {
    TestBed.configureTestingModule({
      imports: [DialogModule],
      providers: [{ provide: ServoyPublicService, useValue: {} }]
    });

    const service = TestBed.inject(DialogService);
    expect(service).toBeInstanceOf(DialogService);
  });
});
