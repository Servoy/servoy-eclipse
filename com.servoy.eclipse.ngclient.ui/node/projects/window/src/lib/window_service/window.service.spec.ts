import { TestBed } from '@angular/core/testing';

import { WindowPluginService } from './window.service';
import { ShortcutService } from './shortcut.service';
import {PopupMenuService} from './popupmenu.service';
import { ServoyPublicTestingModule} from '@servoy/public';

describe('WindowService', () => {
  let service: WindowPluginService;

  beforeEach(() => {
    TestBed.configureTestingModule({
        imports: [ ServoyPublicTestingModule],
        providers:[WindowPluginService, ShortcutService, PopupMenuService]
    });
    service = TestBed.inject(WindowPluginService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
