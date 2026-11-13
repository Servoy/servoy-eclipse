import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { SessionService } from './session.service';
import { ServoyService } from '../servoy.service';
import { WindowService } from './window.service';

describe('SessionService', () => {
  let service: SessionService;
  let windowService: any;
  let sessionProblemSet: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    sessionProblemSet = vi.fn();
    windowService = { switchToSessionProblemPage: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        SessionService,
        { provide: ServoyService, useValue: { getSolutionSettings: () => ({ sessionProblem: { set: sessionProblemSet } }) } },
        { provide: WindowService, useValue: windowService },
      ],
    });
    service = TestBed.inject(SessionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('expireSession should use the defaults and switch to the problem page', () => {
    service.expireSession({ viewUrl: '' } as any);
    const arg = sessionProblemSet.mock.calls[0][0];
    expect(arg.viewUrl).toBe('templates/sessionExpiredView.html');
    expect(windowService.switchToSessionProblemPage).toHaveBeenCalled();
  });

  it('expireSession should honor the supplied viewUrl and redirectUrl', () => {
    service.expireSession({ viewUrl: 'custom.html', redirectUrl: 'http://x' } as any);
    const arg = sessionProblemSet.mock.calls[0][0];
    expect(arg.viewUrl).toBe('custom.html');
    expect(arg.redirectUrl).toBe('http://x');
  });

  it('setNoLicense should use the server-too-busy defaults', () => {
    service.setNoLicense({} as any);
    const arg = sessionProblemSet.mock.calls[0][0];
    expect(arg.viewUrl).toBe('templates/serverTooBusyView.html');
    expect(arg.redirectTimeout).toBe(-1);
  });

  it('setNoLicense should honor the supplied values', () => {
    service.setNoLicense({ viewUrl: 'v.html', redirectUrl: 'r', redirectTimeout: 5 } as any);
    const arg = sessionProblemSet.mock.calls[0][0];
    expect(arg.viewUrl).toBe('v.html');
    expect(arg.redirectUrl).toBe('r');
    expect(arg.redirectTimeout).toBe(5);
  });

  it('setMaintenanceMode should use the maintenance defaults', () => {
    service.setMaintenanceMode({} as any);
    const arg = sessionProblemSet.mock.calls[0][0];
    expect(arg.viewUrl).toBe('templates/maintenanceView.html');
  });

  it('setInternalServerError should use the default view and pass the stack', () => {
    service.setInternalServerError({ stack: 'trace' } as any);
    const arg = sessionProblemSet.mock.calls[0][0];
    expect(arg.viewUrl).toBe('templates/serverInternalErrorView.html');
    expect(arg.stack).toBe('trace');
  });
});
