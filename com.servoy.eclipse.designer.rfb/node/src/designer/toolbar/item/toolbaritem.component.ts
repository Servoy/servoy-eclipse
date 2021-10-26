import { Directive, Input } from '@angular/core';
import { ToolbarItem } from '../toolbar.component';

@Directive()
export class ToolbarItemComponent {
    @Input() item: ToolbarItem;

    onselection(selection: string) {
        const text = this.item.onselection(selection);
        if(text) this.item.text = text;
    }

    isDisabled(): boolean {
        return typeof(this.item.enabled) == 'function' ? !this.item.enabled() : !this.item.enabled;
    }
}