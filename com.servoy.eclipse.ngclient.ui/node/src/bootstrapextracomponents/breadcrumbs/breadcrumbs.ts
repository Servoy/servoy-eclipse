import { Component, Input, ChangeDetectorRef, Renderer2, Output, EventEmitter } from '@angular/core';
import { ServoyBaseComponent } from '../../ngclient/servoy_public';

@Component({
    selector: 'bootstrapextracomponents-breadcrumbs',
    templateUrl: './breadcrumbs.html'
})
export class ServoyBootstrapExtraBreadcrumbs extends ServoyBaseComponent<HTMLElement> {

    @Input() styleClass: string;
    @Input() crumbStyleClass: string;
    @Input() lastCrumbStyleClass: string;
    @Input() breadcrumbs: Array<Crumb>;
    @Output() breadcrumbsChange = new EventEmitter();
    @Input() autoRemoveWhenClicked: boolean;

    @Input() onCrumbClicked: (event: MouseEvent, crumb: Crumb, index: number) => Promise<boolean>;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    crumbClicked(event, crumb, index) {
        if (this.autoRemoveWhenClicked == true) {
            if (this.breadcrumbs) {
                this.breadcrumbs.splice(index + 1, this.breadcrumbs.length - index - 1);
                this.breadcrumbsChange.emit(this.breadcrumbs);
            }
        }
        if (this.onCrumbClicked) {
            this.onCrumbClicked(event, crumb, index);
        }
    }

}

class Crumb {
    public crumbId: string;
    public displayName: string;
}
