import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MessageDialogWindowComponent } from './message-dialog-window.component';

describe('MessageDialogWindowComponent', () => {
  let component: MessageDialogWindowComponent;
  let fixture: ComponentFixture<MessageDialogWindowComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MessageDialogWindowComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(MessageDialogWindowComponent);
    component = fixture.componentInstance;
    component.onCloseCallback = vi.fn();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should default the OK button text when it is unset', () => {
      component.okButtonText.set(undefined!);
      component.ngOnInit();
      expect(component.okButtonText()).toBe('OK');
    });

    it('should seed the buttonsText array from the OK button text', () => {
      component.ngOnInit();
      expect(component.buttonsText()).toEqual(['OK']);
    });

    it('should preserve explicitly provided buttonsText', () => {
      component.buttonsText.set(['Yes', 'No']);
      component.ngOnInit();
      expect(component.buttonsText()).toEqual(['Yes', 'No']);
    });

    it('should seed the return value from the first value for an input dialog', () => {
      component.styleClass.set('type-input');
      component.values.set(['prefill']);
      component.ngOnInit();
      expect(component.retValue).toBe('prefill');
    });
  });

  describe('getType', () => {
    it('should default to text', () => {
      expect(component.getType()).toBe('text');
    });

    it('should return the configured input type', () => {
      component.inputType.set('password');
      expect(component.getType()).toBe('password');
    });
  });

  describe('getButtonClass', () => {
    it('should mark the default button as primary for a plain dialog', () => {
      component.defaultButtonIndex.set(1);
      expect(component.getButtonClass(1)).toBe('svy-btn-primary');
      expect(component.getButtonClass(0)).toBe('');
    });

    it('should mark button index 1 as primary for an input dialog', () => {
      component.styleClass.set('type-input');
      expect(component.getButtonClass(1)).toBe('svy-btn-primary');
      expect(component.getButtonClass(0)).toBe('');
    });
  });

  describe('dismiss', () => {
    it('should set the return value and invoke the close callback for a plain dialog', () => {
      component.dismiss('Yes');
      expect(component.retValue).toBe('Yes');
      expect(component.onCloseCallback).toHaveBeenCalledWith('Yes');
    });

    it('should null out the return value for an input dialog when the value is not the OK button', () => {
      component.styleClass.set('type-input');
      component.okButtonText.set('OK');
      component.retValue = 'typed';
      component.dismiss('Cancel');
      expect(component.retValue).toBeNull();
      expect(component.onCloseCallback).toHaveBeenCalledWith(null);
    });

    it('should keep the typed value for an input dialog when OK is pressed', () => {
      component.styleClass.set('type-input');
      component.okButtonText.set('OK');
      component.retValue = 'typed';
      component.dismiss('OK');
      expect(component.retValue).toBe('typed');
      expect(component.onCloseCallback).toHaveBeenCalledWith('typed');
    });
  });

  describe('handleKeyboardEvent', () => {
    it('should dismiss with null on Escape', () => {
      const spy = vi.spyOn(component, 'dismiss');
      component.handleKeyboardEvent({ key: 'Escape' } as KeyboardEvent);
      expect(spy).toHaveBeenCalledWith(null);
    });

    it('should dismiss with OK on Enter for an input dialog', () => {
      component.styleClass.set('type-input');
      const spy = vi.spyOn(component, 'dismiss');
      component.handleKeyboardEvent({ key: 'Enter' } as KeyboardEvent);
      expect(spy).toHaveBeenCalledWith('OK');
    });

    it('should ignore Enter for a plain dialog', () => {
      const spy = vi.spyOn(component, 'dismiss');
      component.handleKeyboardEvent({ key: 'Enter' } as KeyboardEvent);
      expect(spy).not.toHaveBeenCalled();
    });
  });
});
