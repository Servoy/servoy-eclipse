import { computed, Injectable, signal, Signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class PopupStateService {
    private activePopups = new Set<string>();
    private activePopupCount = signal(0);
    readonly isPopupActive: Signal<boolean> = computed(() => this.activePopupCount() > 0);

    isAnyPopupActive(): boolean {
        return this.activePopups.size > 0;
    }

    activatePopup(id: string) {
        this.activePopups.add(id);
        this.activePopupCount.set(this.activePopups.size);
    }

    deactivatePopup(id: string) {
        setTimeout(() => {
            this.activePopups.delete(id);
            this.activePopupCount.set(this.activePopups.size);
        }, 200); // a delay is required to allow the window.service.ts keyListener to complete execution
    }
}