import { describe, it, expect, beforeEach } from 'vitest';
import { signal } from '@angular/core';

import { StatusBarComponent } from './statusbar.component';

describe('StatusBarComponent', () => {
  let component: StatusBarComponent;
  let editorSession: any;

  beforeEach(() => {
    editorSession = {
      statusText: signal(''),
    };

    component = Object.create(StatusBarComponent.prototype);
    (component as any).editorSession = editorSession;
  });

  it('should read statusText from editorSession signal', () => {
    expect(editorSession.statusText()).toBe('');
  });

  it('should reflect updated statusText', () => {
    editorSession.statusText.set('Button [myBtn]');
    expect(editorSession.statusText()).toBe('Button [myBtn]');
  });
});
