import { Directive, input } from '@angular/core';
import { ToolbarItem } from '../toolbar.component';

@Directive()
export class ToolbarItemComponent {
    item = input<ToolbarItem>();

    onselection(selection: string) {
        const text = this.item()!.onselection(selection);
        if(text) this.item()!.text = text;
        return false;
    }

    isDisabled(): boolean {
        const enabled = this.item()!.enabled;
        return typeof(enabled) == 'function' ? !enabled() : !enabled;
    }
}