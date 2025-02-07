import { Component } from '@angular/core';

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

    refresh(params: ICellRendererParams): boolean {
        // nop
        return true;
    }

    agInit(params: ICellRendererParams): void {
        this.lfc = params.context['componentParent'];
        this.foundsetRows = params.data;
        this.startIndex =(params.node.rowIndex - this.lfc.foundset.viewPort.startIndex) * this.lfc.numberOfColumns;

    }

    afterGuiAttached?(params?: IAfterGuiAttachedParams): void {
        // nop
    }

    getFoundsetRowIndex(i: number): number {
        return this.startIndex + i;
    }
}
