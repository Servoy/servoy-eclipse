import { Component, Input, AfterViewInit, Renderer2, Pipe, PipeTransform,ChangeDetectorRef } from '@angular/core';
import { ServoyBootstrapBaseLabel } from '../bts_baselabel';

@Component({
    selector: 'bootstrapcomponents-datalabel',
    templateUrl: './datalabel.html',
    styleUrls: ['./datalabel.scss']
})
export class ServoyBootstrapDatalabel extends ServoyBootstrapBaseLabel<HTMLSpanElement> {

    @Input() dataProviderID;
    @Input() styleClassExpression;
    @Input() valuelistID;
    @Input() format;

    constructor(renderer: Renderer2,cdRef: ChangeDetectorRef) {
        super(renderer,cdRef);
    }

    attachHandlers() {
        if ( this.onActionMethodID ) {
            if (this.onDoubleClickMethodID) {
                const innerThis: ServoyBootstrapDatalabel = this;
                this.renderer.listen( this.getNativeElement(), 'click', e => {
                    if (innerThis.timeoutID) {
                        window.clearTimeout(innerThis.timeoutID);
                        innerThis.timeoutID = null;
                        // double click, do nothing
                    } else {
                        innerThis.timeoutID = window.setTimeout(function() {
                            innerThis.timeoutID = null;
                            innerThis.onActionMethodID( e );
                        }, 250);
}
                 });
            } else {
                this.renderer.listen( this.getNativeElement(), 'click', e => {
                    this.onActionMethodID(e);
                });
            }
        }
        if ( this.onRightClickMethodID ) {
          this.renderer.listen( this.getNativeElement(), 'contextmenu', e => {
              this.onRightClickMethodID( e ); return false;
            });
        }
        if ( this.onDoubleClickMethodID ) {
            this.renderer.listen( this.elementRef.nativeElement, 'dblclick', ( e ) => {
                this.onDoubleClickMethodID(e);
            });
        }
    }
}

@Pipe( { name: 'designFilter'} )
export class DesignFilterPipe implements PipeTransform {
    transform(input: any, inDesigner: boolean) {
        if (inDesigner) {
            return 'DataLabel';
        }
        return input;
    }

}
