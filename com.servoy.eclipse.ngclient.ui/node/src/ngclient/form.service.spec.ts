import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import {WebsocketService} from '../sablo/websocket.service';
import {SabloService} from '../sablo/sablo.service';
import {ConverterService} from '../sablo/converter.service';
import {LoggerFactory} from '@servoy/public';
import {WindowRefService} from '@servoy/public';

import { FormService } from './form.service';
import {ServoyService} from './servoy.service';
import { ClientFunctionService } from '../sablo/clientfunction.service';

describe('FormService', () => {
  let websocketService: any;
  let sabloService;
  let converterService;
  let servoyService;
  beforeEach(() => {
      websocketService = { getSession: vi.fn().mockReturnValue(Promise.resolve({ onMessageObject: vi.fn() })) } as any;
      sabloService = { connect: vi.fn() } as any;
      converterService = { convertFromClientToServer: vi.fn() } as any;
      servoyService = { setFindMode: vi.fn() } as any;
    TestBed.configureTestingModule({
      providers: [FormService,
                          LoggerFactory,
                          WindowRefService,
                          ClientFunctionService,
                          {provide: WebsocketService, useValue:websocketService},
                          {provide: SabloService, useValue:sabloService},
                          {provide: ConverterService, useValue:converterService},
                          {provide: ServoyService, useValue: servoyService}],
    });
  });

  it('should be created', () => {
      const service = TestBed.inject(FormService);
      expect(service).toBeTruthy();
      expect(websocketService.getSession).toHaveBeenCalled();
  });
});

class WebsocketServiceMock {
}
