import { TestBed } from '@angular/core/testing';

import { WindowService } from './window.service';
import { ShortcutService } from './shortcut.service';
import {PopupMenuService} from './popupmenu.service';
import { ServoyPublicTestingModule} from '@servoy/public';

describe('WindowService', () => {
  let service: WindowService;

  beforeEach(() => {
    TestBed.configureTestingModule({
        imports: [ ServoyPublicTestingModule],
        providers:[WindowService, ShortcutService, PopupMenuService]
    });
    service = TestBed.inject(WindowService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
