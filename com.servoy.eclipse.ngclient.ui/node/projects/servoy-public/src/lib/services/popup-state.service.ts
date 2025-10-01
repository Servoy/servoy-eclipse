import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class PopupStateService {
    private activePopups = new Set<string>();

    isAnyPopupActive(): boolean {
        return this.activePopups.size > 0;
    }

    activatePopup(id: string) {
        this.activePopups.add(id);
    }

    deactivatePopup(id: string) {
        setTimeout(() => {
            this.activePopups.delete(id);
        }, 200); // a delay is required to allow the window.service.ts keyListener to complete execution
    }
}