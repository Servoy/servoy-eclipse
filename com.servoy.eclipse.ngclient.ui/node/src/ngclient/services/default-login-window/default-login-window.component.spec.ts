import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { LocalStorageService } from '@servoy/public';
import { SabloService } from '../../../sablo/sablo.service';
import { DefaultLoginWindowComponent } from './default-login-window.component';
describe('DefaultLoginWindowComponent', () => {
  let component: DefaultLoginWindowComponent;
  let fixture: ComponentFixture<DefaultLoginWindowComponent>;
  let sabloService: any;
  let localStorageService: any;
  let ngbActiveModal: any;
  beforeEach(async () => {
    sabloService = { callService: vi.fn() } as any;
    localStorageService = { set: vi.fn() } as any;
    ngbActiveModal = { close: vi.fn() } as any;
    TestBed.configureTestingModule({
      imports: [FormsModule, DefaultLoginWindowComponent],
      providers: [
        { provide: SabloService, useValue: sabloService },
        { provide: LocalStorageService, useValue: localStorageService },
      ],
    }).compileComponents();
  });
  beforeEach(() => {
    fixture = TestBed.createComponent(DefaultLoginWindowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });
  it('should create', () => {
    expect(component).toBeTruthy();
  });
  it('should login', async () => {
    const loginData = { username: 'test', password: 'test', remember: true };
    const promise = Promise.resolve(loginData);
    sabloService.callService.mockReturnValue(promise);
    component.username = 'test';
    component.password = 'test';
    component.doLogin();
    await promise;
    expect(sabloService.callService).toHaveBeenCalledWith(
      'applicationServerService',
      'login',
      loginData,
      false,
    );
    expect(localStorageService.set).toHaveBeenCalledTimes(2);
    expect(component.message).toBeUndefined();
  });
  it('should not login', async () => {
    const loginData = { username: 'test', password: 'test', remember: true };
    const promise = Promise.resolve(false);
    sabloService.callService.mockReturnValue(promise);
    component.username = 'test';
    component.password = 'test';
    component.doLogin();
    await promise;
    await Promise.resolve();
    expect(sabloService.callService).toHaveBeenCalledWith(
      'applicationServerService',
      'login',
      loginData,
      false,
    );
    expect(ngbActiveModal.close).toHaveBeenCalledTimes(0);
    expect(component.message).toBe('Invalid username or password, try again');
  });
});
