import { Component, HostListener } from '@angular/core';
import { DatagridEditorDirective } from './datagrideditor';
import { ICellEditorParams } from '@ag-grid-community/core';

@Component({
    selector: 'aggrid-datagrid-selecteditor',
    template: `
        <div class="ag-cell-edit-input">
            <select class="ag-cell-edit-input" #element></select>
        </div>
    `
})
export class SelectEditor extends DatagridEditorDirective {

    @HostListener('keydown', ['$event']) onKeyDown(e: KeyboardEvent) {
        const isNavigationKey = e.keyCode === 38 || e.keyCode === 40;
        if (isNavigationKey) {
            e.stopPropagation();
        }
    }

    @HostListener('mousedown', ['$event']) onMouseDown(e: MouseEvent) {
        e.stopPropagation();
    }

    agInit(params: ICellEditorParams): void {
        super.agInit(params);

        const vl = this.dataGrid.getValuelist(params);
        if (vl) {
            let v = params.value;
            if (v && v.displayValue !== undefined) {
                v = v.displayValue;
            }
            vl.filterList('').subscribe(valuelistValues => {
                valuelistValues.forEach((value) => {
                    const option = document.createElement('option');
                    option.value = value.realValue == null ? '_SERVOY_NULL' : value.realValue;
                    option.text = value.displayValue;
                    if (v != null && v.toString() === value.displayValue) {
                        option.selected = true;
                        //TODO: init node data with select editor
                        // if(value.realValue != undefined && params.value["realValue"] == undefined) {
                        //     params.node["data"][params.column.colDef["field"]] = {realValue: value.realValue, displayValue: v};
                        // }
                    }
                    this.elementRef.nativeElement.appendChild(option);
                });
            });
        }
    }

    ngAfterViewInit(): void {
        this.elementRef.nativeElement.focus();
    }

    // returns the new value after editing
    getValue(): any {
        const displayValue = this.elementRef.nativeElement.selectedIndex > -1 ? this.elementRef.nativeElement.options[this.elementRef.nativeElement.selectedIndex].text : '';
        const realValue = this.elementRef.nativeElement.value === '_SERVOY_NULL' ? null : this.elementRef.nativeElement.value;
        return displayValue !== realValue ? { displayValue, realValue } : realValue;
    }
}
