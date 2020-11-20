import { Directive, ElementRef, ViewChild } from '@angular/core';
import { ICellEditorAngularComp } from 'ag-grid-angular/public-api';
import { ICellEditorParams } from 'ag-grid-community';
import { DataGrid } from '../datagrid';

@Directive()
export class DatagridEditor implements ICellEditorAngularComp {

    @ViewChild('element') elementRef: ElementRef;
    dataGrid: DataGrid;
    params: ICellEditorParams;
    initialValue;

    agInit(params: ICellEditorParams): void {
        // create the cell
        this.params = params;
        this.dataGrid = params.context.componentParent;
        this.initialValue = params.value;
    }

    getValue() {
        throw new Error('Method not implemented.');
    }
}
