import { Component, ElementRef, HostListener, ViewChild } from '@angular/core';

import { AgRendererComponent } from 'ag-grid-angular';
import { ICellRendererParams, IAfterGuiAttachedParams } from 'ag-grid-community';
import { ListFormComponent } from './listformcomponent';

@Component({
    selector: 'svy-row-renderer-component',
    templateUrl: './row-renderer.component.html',
    standalone: false
})
export class RowRenderer implements AgRendererComponent {

    lfc: ListFormComponent;
    foundsetRows: any[];
    startIndex: number;

    @HostListener('registerCSTS', ['$event'])
    registerCSTS(event: Event) {
        // Cast the event to CustomEvent to access the detail property
        const customEvent = event as CustomEvent;
        const newEvent = new CustomEvent('registerCSTS', {
            bubbles: true,
            detail: customEvent.detail
        });
        this.lfc.element().nativeElement.children[0].dispatchEvent(newEvent);
    }

    refresh(params: ICellRendererParams): boolean {
        // nop
        return true;
    }

    agInit(params: ICellRendererParams): void {
        this.lfc = params.context['componentParent'];
        this.foundsetRows = params.data;
        this.startIndex =(params.node.rowIndex - this.lfc._foundset().viewPort.startIndex) * this.lfc.numberOfColumns;

    }

    afterGuiAttached?(params?: IAfterGuiAttachedParams): void {
        // nop
    }

    getFoundsetRowIndex(i: number): number {
        return this.startIndex + i;
    }
}
