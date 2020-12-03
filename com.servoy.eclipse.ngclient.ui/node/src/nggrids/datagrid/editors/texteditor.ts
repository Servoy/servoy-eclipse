import { Component, HostListener, Input } from '@angular/core';
import { ICellEditorParams } from 'ag-grid-community';
import { DatagridEditor } from './datagrideditor';

@Component({
    selector: 'datagrid-texteditor',
    template: `
    <div class="ag-input-wrapper">
      <input class="ag-cell-edit-input" [value]="initialDisplayValue" [svyDecimalKeyConverter]="format" [maxLength]="maxLength" #element>
    </div>
    `
})
export class TextEditor extends DatagridEditor {

    @Input() initialDisplayValue;
    @Input() format;
    @Input() maxLength = 524288;


    agInit(params: ICellEditorParams): void {
        super.agInit(params);

        if(this.initialValue && this.initialValue.displayValue != undefined) {
            this.initialValue = this.initialValue.displayValue;
        }
        let v = this.initialValue;
        const column = this.dataGrid.getColumn(params.column.getColId());
        if(column && column.format) {
            this.format = column.format;
            if (this.format.maxLength) {
                this.maxLength = this.format.maxLength;
            }
            if(this.format.edit) {
                v = this.dataGrid.formattingService.format(v, this.format, true);
            }
            else if(this.format.display) {
                v = this.dataGrid.formattingService.format(v, this.format, false);
            }
        }
        this.initialDisplayValue = v;
    }

    @HostListener('keydown',['$event']) onKeyDown(e: KeyboardEvent) {
        if(this.dataGrid.arrowsUpDownMoveWhenEditing && this.dataGrid.arrowsUpDownMoveWhenEditing != 'NONE') {
            const isNavigationLeftRightKey = e.keyCode === 37 || e.keyCode === 39;
            const isNavigationUpDownEntertKey = e.keyCode === 38 || e.keyCode === 40 || e.keyCode === 13;

            if (isNavigationLeftRightKey || isNavigationUpDownEntertKey) {

                if(isNavigationUpDownEntertKey) {
                    let newEditingNode = null;
                    const columnToCheck = this.params.column;
                    const mustBeEditable = this.dataGrid.arrowsUpDownMoveWhenEditing == 'NEXTEDITABLECELL';
                    if( e.keyCode == 38) { // UP
                        if(this.params.rowIndex > 0) {
                            const _this = this;
                            this.dataGrid.agGrid.api.forEachNode( function(node) {
                                if (node.rowIndex <= (_this.params.rowIndex - 1) &&
                                    (!mustBeEditable || columnToCheck.isCellEditable(node))) {
                                    newEditingNode = node;
                                }
                            });
                        }
                    } else if (e.keyCode == 13 || e.keyCode == 40) { // ENTER/DOWN
                        if( this.params.rowIndex < this.dataGrid.agGrid.api.getModel().getRowCount() - 1) {
                            const _this = this;
                            this.dataGrid.agGrid.api.forEachNode( function(node) {
                                if (node.rowIndex >= (_this.params.rowIndex + 1) &&
                                    !newEditingNode && (!mustBeEditable || columnToCheck.isCellEditable(node))) {
                                    newEditingNode = node;
                                }
                            });
                        }
                    }
                    this.dataGrid.agGrid.api.stopEditing();
                    if (newEditingNode) {
                        this.dataGrid.selectionEvent = { type: 'key', event: e };
                        newEditingNode.setSelected(true, true);

                        if(columnToCheck.isCellEditable(newEditingNode)) {
                            this.dataGrid.agGrid.api.startEditingCell({
                                rowIndex: newEditingNode.rowIndex,
                                colKey: columnToCheck.getColId()
                            });
                        } else {
                            this.dataGrid.agGrid.api.setFocusedCell(newEditingNode.rowIndex, columnToCheck.getColId());
                        }
                    }
                    e.preventDefault();
                }
                e.stopPropagation();
            }
        }
    }

    @HostListener('keypress',['$event']) onKeyPress(e: KeyboardEvent) {
        const isNavigationLeftRightKey = e.keyCode === 37 || e.keyCode === 39;
        const isNavigationUpDownEntertKey = e.keyCode === 38 || e.keyCode === 40 || e.keyCode === 13;

        if(!(isNavigationLeftRightKey || isNavigationUpDownEntertKey) && this.format) {
            return this.dataGrid.formattingService.testForNumbersOnly(e, null, this.elementRef.nativeElement, false, true, this.format, false);
        }
        else return true;
    }

    // focus and select can be done after the gui is attached
    ngAfterViewInit(): void {
        this.elementRef.nativeElement.focus();
        this.elementRef.nativeElement.select();

        const editFormat = this.format.edit ? this.format.edit : this.format.display;
        if(this.format && editFormat && this.format.isMask) {
            const settings = {};
            settings['placeholder'] = this.format.placeHolder ? this.format.placeHolder : ' ';
            if (this.format.allowedCharacters)
                settings['allowedCharacters'] = this.format.allowedCharacters;

            //TODO: jquery mask
            //$(this.eInput).mask(editFormat, settings);
        }
    }

    // returns the new value after editing
    getValue(): any {
        let displayValue = this.elementRef.nativeElement.value;

        if(this.format) {
            const editFormat = this.format.edit ? this.format.edit : this.format.display;
            if(editFormat) {
                displayValue = this.dataGrid.formattingService.unformat(displayValue, editFormat, this.format.type, this.initialValue);
            }
            if (this.format.type == 'TEXT' && (this.format.uppercase || this.format.lowercase)) {
                if (this.format.uppercase) displayValue = displayValue.toUpperCase();
                else if (this.format.lowercase) displayValue = displayValue.toLowerCase();
            }
        }
        const realValue = displayValue;

        return {displayValue, realValue};
    }
}
