import { TestBed } from '@angular/core/testing';

import { WindowService } from './window.service';
import { ShortcutService } from './shortcut.service';
import {PopupMenuService} from './popupmenu.service';
import { LocaleService } from '../ngclient/locale.service';
import { SabloModule } from '../sablo/sablo.module';
import { ServoyPublicModule } from '@servoy/public';
import { FormService } from '../ngclient/form.service';
import { ServoyService } from '../ngclient/servoy.service';
import { ViewportService } from '../ngclient/services/viewport.service';
import { ClientFunctionService } from '../ngclient/services/clientfunction.service';
import { ServoyTestingModule } from '../testing/servoytesting.module';
import { PopupFormService } from '../ngclient/services/popupform.service';

describe('WindowService', () => {
  let service: WindowService;

  beforeEach(() => {
    TestBed.configureTestingModule({
        imports: [ServoyTestingModule, SabloModule, ServoyPublicModule],
        providers:[WindowService, ShortcutService, PopupMenuService, PopupFormService, ViewportService, ClientFunctionService,
                    FormService, ServoyService, { provide: LocaleService, useValue: {getLocale: () => 'en' }}]
    });
    service = TestBed.inject(WindowService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
