import { describe, it, expect, beforeEach, vi } from 'vitest';
import { BehaviorSubject } from 'rxjs';

import { StatusBarComponent } from './statusbar.component';

describe('StatusBarComponent', () => {
  let component: StatusBarComponent;
  let stateListener: BehaviorSubject<string>;
  let editorSession: any;

  beforeEach(() => {
    stateListener = new BehaviorSubject<string>('');
    editorSession = {
      stateListener,
      getState: vi.fn().mockReturnValue({ statusText: '' }),
    };

    component = Object.create(StatusBarComponent.prototype);
    (component as any).cdr = { markForCheck: vi.fn() };
    (component as any).editorSession = editorSession;
    component.statusText = '';
  });

  describe('ngAfterViewInit', () => {
    it('should subscribe to stateListener', () => {
      component.ngAfterViewInit();
      expect(component.editorStateSubscription).toBeDefined();
    });

    it('should update statusText when stateListener emits "statusText"', () => {
      editorSession.getState.mockReturnValue({ statusText: 'Button [myBtn]' });
      component.ngAfterViewInit();
      stateListener.next('statusText');
      expect(component.statusText).toBe('Button [myBtn]');
      expect((component as any).cdr.markForCheck).toHaveBeenCalled();
    });

    it('should not update statusText for other state changes', () => {
      component.ngAfterViewInit();
      (component as any).cdr.markForCheck.mockClear();
      stateListener.next('dragging');
      expect(component.statusText).toBe('');
      expect((component as any).cdr.markForCheck).not.toHaveBeenCalled();
    });
  });

  describe('ngOnDestroy', () => {
    it('should unsubscribe from stateListener', () => {
      component.ngAfterViewInit();
      const spy = vi.spyOn(component.editorStateSubscription, 'unsubscribe');
      component.ngOnDestroy();
      expect(spy).toHaveBeenCalled();
    });
  });
});
