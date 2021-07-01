import { TestBed } from '@angular/core/testing';

import { WindowService } from './window.service';
import { ShortcutService } from './shortcut.service';
import {PopupMenuService} from './popupmenu.service';
import { ServoyPublicModule , ServoyPublicTestingModule} from '@servoy/public';
import { PopupFormService } from './popupform.service';

describe('WindowService', () => {
  let service: WindowService;

  beforeEach(() => {
    TestBed.configureTestingModule({
        imports: [ ServoyPublicTestingModule, ServoyPublicModule],
        providers:[WindowService, ShortcutService, PopupMenuService, PopupFormService]
    });
    service = TestBed.inject(WindowService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
