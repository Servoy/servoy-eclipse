import { ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { ICellEditorParams } from 'ag-grid-community';
import { DatagridEditor } from './datagrideditor';

@Component({
    selector: 'datagrid-formeditor',
    template: `
      <div id="nggridformeditor" [style.width.px]="width" [style.height.px]="height">
        <ng-template [ngTemplateOutlet]="getTemplate()" [ngTemplateOutletContext]="{name:getForm()}"></ng-template>
      </div>
    `
})
export class FormEditor extends DatagridEditor implements OnDestroy {

    editForm;
    width: number = 300;
    height: number = 200;

    constructor(private cdRef: ChangeDetectorRef) {
        super();
    }

    agInit(params: ICellEditorParams): void {
        super.agInit(params);
        this.dataGrid._internalFormEditorValue = this.params.value;
        if(this.dataGrid.onColumnFormEditStarted) {
            this.dataGrid.onColumnFormEditStarted(
                this.dataGrid.getFoundsetIndexFromEvent(this.params), this.dataGrid.getColumnIndex(this.params.column.getColId()), this.params.value);
        }

        const column = this.dataGrid.getColumn(params.column.getColId());
        if(column.editFormSize) {
            this.width = column.editFormSize.width;
            this.height = column.editFormSize.height;
        }

        this.editForm = column.editForm;
        this.dataGrid.servoyApi.formWillShow(this.editForm).finally(() => this.cdRef.markForCheck());
    }

    ngAfterViewInit(): void {
        this.dataGrid.agGrid.api.setFocusedCell(this.params.node.rowIndex, this.params.column.getColId());
    }

    isPopup?(): boolean {
        return true;
    }

    getValue(): any {
        return this.dataGrid._internalFormEditorValue;
    }

    ngOnDestroy(): void {
        const column = this.dataGrid.getColumn(this.params.column.getColId());
        this.dataGrid.servoyApi.hideForm(column.editForm);
    }

    getForm() {
        return this.editForm;
    }

    getTemplate() {
        return this.dataGrid.templateRef;
    }
}