import { TestBed } from '@angular/core/testing';

import { WindowService } from './window.service';
import { ShortcutService } from './shortcut.service';
import {PopupMenuService} from './popupmenu.service';
import { SvyUtilsService, LocaleService } from '../ngclient/servoy_public';
import { SabloModule } from '../sablo/sablo.module';
import { ServoyPublicModule } from '../ngclient/servoy_public.module';
import { FormService } from '../ngclient/form.service';
import { ServoyService } from '../ngclient/servoy.service';
import { ViewportService } from '../ngclient/services/viewport.service';
import {ServiceChangeHandler} from '../sablo/util/servicechangehandler';

describe('WindowService', () => {
  let service: WindowService;

  beforeEach(() => {
    TestBed.configureTestingModule({
        imports: [SabloModule, ServoyPublicModule],
        providers:[WindowService, ShortcutService, PopupMenuService, SvyUtilsService,ViewportService, FormService, ServoyService, ServiceChangeHandler, { provide: LocaleService, useValue: {getLocale: () => 'en' }}]
    });
    service = TestBed.inject(WindowService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
