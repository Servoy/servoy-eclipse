import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer } from '@angular/platform-browser';
import { of } from 'rxjs';

import { SessionView } from './session-view';
import { ServoyService } from '../../ngclient/servoy.service';

describe('SessionView', () => {
  let component: SessionView;
  let httpClient: any;
  let servoyService: any;
  let sessionProblem: any;

  const setup = () => {
    httpClient = { get: vi.fn() };
    servoyService = { getSolutionSettings: () => ({ sessionProblem: () => sessionProblem }) };
    TestBed.configureTestingModule({
      imports: [SessionView],
      providers: [
        { provide: HttpClient, useValue: httpClient },
        { provide: ServoyService, useValue: servoyService },
        { provide: DomSanitizer, useValue: { bypassSecurityTrustHtml: (h: string) => `trusted:${h}` } },
      ],
    });
    const fixture = TestBed.createComponent(SessionView);
    component = fixture.componentInstance;
  };

  beforeEach(() => {
    sessionProblem = { viewUrl: 'view.html', redirectUrl: 'http://redirect', redirectTimeout: -1, nonce: 'nonce1' };
    setup();
  });

  afterEach(() => {
    vi.useRealTimers();
    document.body.querySelectorAll('script').forEach((s) => s.remove());
  });

  it('should create', () => {
    httpClient.get.mockReturnValue(of('<div></div>'));
    expect(component).toBeTruthy();
  });

  it('ngOnInit should process placeholders and bind trusted html', () => {
    httpClient.get.mockReturnValue(of('<a ng-href="x">{{redirectUrl}}</a>'));
    component.ngOnInit();
    expect(component.htmlString).toBe('trusted:<a href="x">http://redirect</a>');
  });

  it('ngOnInit should strip external scripts from the bound html and re-inject them into the body', () => {
    httpClient.get.mockReturnValue(of('<div>hi</div><script src="app.js"></script>'));
    component.ngOnInit();
    expect(component.htmlString).toBe('trusted:<div>hi</div>');
    const injected = document.body.querySelector('script[src="app.js"]');
    expect(injected).not.toBeNull();
    expect(injected!.getAttribute('nonce')).toBe('nonce1');
  });

  it('ngOnInit should set the defer flag on a deferred external script', () => {
    httpClient.get.mockReturnValue(of('<script defer src="d.js"></script>'));
    component.ngOnInit();
    const injected = document.body.querySelector('script[src="d.js"]') as HTMLScriptElement;
    expect(injected.defer).toBe(true);
  });

  it('ngOnInit should schedule a redirect when redirectTimeout is non-negative', () => {
    vi.useFakeTimers();
    sessionProblem.redirectTimeout = 0;
    httpClient.get.mockReturnValue(of('<div></div>'));
    const setTimeoutSpy = vi.spyOn(window, 'setTimeout');
    component.ngOnInit();
    expect(setTimeoutSpy).toHaveBeenCalled();
  });

  it('ngOnInit should not schedule a redirect when redirectTimeout is negative', () => {
    vi.useFakeTimers();
    sessionProblem.redirectTimeout = -1;
    httpClient.get.mockReturnValue(of('<div></div>'));
    const setTimeoutSpy = vi.spyOn(window, 'setTimeout');
    component.ngOnInit();
    expect(setTimeoutSpy).not.toHaveBeenCalled();
  });
});
