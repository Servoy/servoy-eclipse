import { inject, Injectable, DOCUMENT } from '@angular/core';

import { WindowRefService } from '@servoy/public';

@Injectable()
export class ClientUtilsService {
  private readonly windowRef = inject(WindowRefService);
  private readonly document = inject(DOCUMENT) as Document;

  constructor() {}

  public getBounds(component: string, selector: string): { x: number; y: number; width: number; height: number } {
    let el = this.document.getElementById(component);
    if (el) {
      if (selector) {
        el = el.querySelector(selector);
      }
      if (el) {
        const rect = el.getBoundingClientRect();
        return { x: rect.left + this.windowRef.nativeWindow.scrollX, y: rect.top + this.windowRef.nativeWindow.scrollY, width: rect.width, height: rect.height };
      }
    }
    return null!;
  }
}
