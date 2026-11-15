import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { I18NProvider } from '../i18n_provider.service';
import { AlertWindowComponent } from './alert-window.component';

describe('AlertWindowComponent', () => {
  let component: AlertWindowComponent;
  let fixture: ComponentFixture<AlertWindowComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlertWindowComponent],
      providers: [
        { provide: I18NProvider, useValue: { getI18NMessages: vi.fn(), getI18NMessage: vi.fn() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(AlertWindowComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders the title and message signals into the template', () => {
    component.title.set('My title');
    component.message.set('My message');
    fixture.detectChanges();

    const host: HTMLElement = fixture.nativeElement;
    expect(host.querySelector('.window-title')!.textContent).toContain('My title');
    expect(host.querySelector('.svy-alert-message pre')!.textContent).toContain('My message');
  });

  describe('dismiss', () => {
    it('invokes the registered close callback', () => {
      const callback = vi.fn();
      component.setOnCloseCallback(callback);

      component.dismiss();

      expect(callback).toHaveBeenCalledTimes(1);
    });

    it('is triggered by clicking the OK button', () => {
      const callback = vi.fn();
      component.setOnCloseCallback(callback);
      fixture.detectChanges();

      const okButton: HTMLButtonElement = fixture.nativeElement.querySelector('.svy-alert-button');
      okButton.click();

      expect(callback).toHaveBeenCalledTimes(1);
    });
  });

  describe('setOnCloseCallback', () => {
    it('stores the callback so dismiss uses the latest one', () => {
      const first = vi.fn();
      const second = vi.fn();
      component.setOnCloseCallback(first);
      component.setOnCloseCallback(second);

      component.dismiss();

      expect(first).not.toHaveBeenCalled();
      expect(second).toHaveBeenCalledTimes(1);
    });
  });
});
