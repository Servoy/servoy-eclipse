import { AfterViewInit, Component, ElementRef, ChangeDetectionStrategy, inject } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';

import { AgRendererComponent } from 'ag-grid-angular';
import { ICellRendererParams, IRowNode } from 'ag-grid-community';
import { ListFormComponent } from './listformcomponent';
import { SabloTabseq } from '@servoy/public';
import { AddAttributeDirective } from '../addattribute.directive';

@Component({
  selector: 'svy-row-renderer-component',
  templateUrl: './row-renderer.component.html',
  host: { '(registerCSTS)': 'registerCSTS($event)' },
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [SabloTabseq, NgTemplateOutlet, AddAttributeDirective],
})
export class RowRenderer implements AgRendererComponent, AfterViewInit {
  lfc!: ListFormComponent;
  foundsetRows!: any[];
  startIndex!: number;
  private node!: IRowNode;
  private rowId!: string;

  private elementRef = inject(ElementRef);

  registerCSTS(event: Event) {
    // Cast the event to CustomEvent to access the detail property
    const customEvent = event as CustomEvent;
    const newEvent = new CustomEvent('registerCSTS', {
      bubbles: true,
      detail: customEvent.detail,
    });
    this.lfc.element()!.nativeElement.children[0].dispatchEvent(newEvent);
  }

  refresh(params: ICellRendererParams): boolean {
    // nop
    return true;
  }

  agInit(params: ICellRendererParams): void {
    this.lfc = params.context['componentParent'];
    this.foundsetRows = params.data;
    this.startIndex = (params.node.rowIndex! - this.lfc._foundset()!.viewPort.startIndex) * this.lfc.numberOfColumns;
    this.node = params.node;
    this.rowId = params.node.id!;
  }

  ngAfterViewInit(): void {
    // NOTE: AG Grid only invokes the optional `afterGuiAttached` hook on filters, cell
    // editors and date components - NOT on cell renderers - so it never fires here. Angular's
    // own `ngAfterViewInit` is the reliable "this component's view is now in the DOM" hook for
    // a cell renderer (SVY-21457).
    this.measureAndApplyRowHeight();
  }

  getFoundsetRowIndex(i: number): number {
    return this.startIndex + i;
  }

  /**
   * Per-row auto-height for SVY-21457 (see ListFormComponent.applyMeasuredRowHeight for why
   * this is a single explicit measurement rather than AG Grid's native colDef.autoHeight).
   * The nested responsive form content can still be reflowing right after attach (bootstrap
   * 12grid float columns), so the measurement is taken after a double requestAnimationFrame,
   * which reliably runs after the browser has completed layout for the frame following attach.
   */
  private measureAndApplyRowHeight(): void {
    if (!this.lfc.isPerRowAutoHeight()) {
      return;
    }
    const rowId = this.rowId;
    const node = this.node;
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        // `node.isFullWidthCell()` is deprecated since AG Grid v32.2.0 (warning #61); the
        // LFC never uses master/detail or full-width rows, so `node.detail` is always
        // falsy here, but check it via the non-deprecated field instead of the method.
        if (!this.elementRef?.nativeElement || node.detail) {
          return;
        }
        const measuredHeight = this.measureContentHeight(this.elementRef.nativeElement);
        this.lfc.applyMeasuredRowHeight(rowId, node, measuredHeight);
      });
    });
  }

  /**
   * The row wrapper (`row-renderer.component.html`, responsive branch) contains the nested
   * form's own bootstrap `12grid` `.row`/`.col-*` structure, which is `float: left` at every
   * level with no clearfix - each floated element's own `offsetHeight` collapses (does not
   * grow to contain its floated children), and that collapse compounds at every nesting
   * level. A single level of `offsetTop + offsetHeight` on direct children under-reports the
   * true height because those children are themselves collapsed. Recurse to the deepest
   * elements (using `getBoundingClientRect` so we compare in the same coordinate space) and
   * take the lowest bottom edge relative to the row wrapper (SVY-21457).
   */
  private measureContentHeight(cellGui: HTMLElement): number {
    const rowEl = cellGui.querySelector(':first-child') as HTMLElement;
    if (!rowEl) {
      return cellGui.scrollHeight;
    }
    const rowTop = rowEl.getBoundingClientRect().top;
    let maxBottom = 0;
    const visit = (el: Element) => {
      for (const childEl of Array.from(el.children)) {
        const child = childEl as HTMLElement;
        const rect = child.getBoundingClientRect();
        if (rect.height > 0 || rect.width > 0) {
          maxBottom = Math.max(maxBottom, rect.bottom - rowTop);
        }
        visit(child);
      }
    };
    visit(rowEl);
    return maxBottom > 0 ? maxBottom : rowEl.scrollHeight;
  }
}
