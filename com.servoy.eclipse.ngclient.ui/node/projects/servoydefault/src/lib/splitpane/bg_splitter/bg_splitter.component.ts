import { Component, output, OnChanges, SimpleChanges, HostListener, AfterContentInit, QueryList, Renderer2, ViewEncapsulation, ElementRef, ChangeDetectionStrategy, input, inject, viewChild, contentChildren } from '@angular/core';

import { BGPane } from './bg_pane.component';
@Component( {
    selector: 'bg-splitter',
    template: '<div class="split-panes" #element><ng-content></ng-content></div>',
    styleUrls: ['./bg_splitter.css'],
    encapsulation: ViewEncapsulation.None,
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: true
} )
export class BGSplitter implements AfterContentInit , OnChanges {

    readonly orientation = input('vertical');
    readonly divSize = input<any>(undefined);
    readonly divLocation = input<any>(undefined);

    readonly onDividerChange = output<any>();

    private drag = false;
    private handler;
    private readonly renderer = inject(Renderer2);

    private readonly panes = contentChildren(BGPane);

    private readonly elementRef = viewChild.required<ElementRef>('element');

    constructor() {
        this.handler = this.renderer.createElement( 'div' );
        this.renderer.addClass( this.handler, 'split-handler' );

        this.handler.addEventListener( 'mousedown', ( ev: any ) => {
            ev.preventDefault();
            this.drag = true;
        } );
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes['divSize']  && changes['divSize'].currentValue >= 0 ) {
            let styleName = 'width';
            if (this.orientation() === 'vertical') styleName = 'height';
            this.renderer.setStyle(this.handler,styleName, changes['divSize'].currentValue +'px');
            this.adjustLocation(null, this.divLocation());
        }
        if (changes['divLocation'] && changes['divLocation'].currentValue >= 0) {
            this.adjustLocation(null, changes['divLocation'].currentValue);
        }
        if (changes['orientation']) {
            this.renderer.addClass( this.elementRef().nativeElement, this.orientation());
        }
    }

    ngAfterContentInit() {
        let index = 1;
        this.panes().forEach(( item ) => {
            item.index = index++;
        } );
        this.renderer.insertBefore( this.elementRef().nativeElement, this.handler, this.panes()[this.panes().length - 1].element.nativeElement );

        this.adjustLocation(null,this.divLocation());
    }

    @HostListener( 'document:mouseup', ['$event'] )
    mouseup( event: any ) {
        if ( this.drag ) {
            let dividerLocation;
            if(this.orientation() === 'vertical' ) {
                dividerLocation = this.handler.style.top;
            } else {
                dividerLocation = this.handler.style.left;
            }
            this.onDividerChange.emit( dividerLocation ? parseInt(dividerLocation.substring(0, dividerLocation.length - 2)) : 0);
        }
        this.drag = false;
    }

    @HostListener( 'mousemove', ['$event'] )
    mousemove( event: any ) {
        if ( !this.drag ) return;
        this.adjustLocation(event);
    }

    private adjustLocation(event?: any, wantedPosition?: any) {
        if (!this.panes() || this.panes().length != 2) return;
        const bounds = this.elementRef().nativeElement.getBoundingClientRect();
        const pos = this.getPosition(bounds, event, wantedPosition);
        if ( this.orientation() === 'vertical' ) {
            const height = bounds.bottom - bounds.top;

            // only check for minSize if it is adjusting because of mousemove
            if(event) {
                if ( pos! < this.panes()[0].minSize() ) return;
                if ( height - pos! < this.panes()[this.panes().length - 1].minSize() ) return;
            }

            this.renderer.setStyle( this.handler, 'top', pos + 'px' );
            this.renderer.setStyle( this.panes()[0].element.nativeElement, 'height', pos + 'px' );
            this.renderer.setStyle( this.panes()[this.panes().length - 1].element.nativeElement, 'top', (pos + this.handler.offsetHeight) + 'px' );

        } else {
            const width = bounds.right - bounds.left;

            // only check for minSize if it is adjusting because of mousemove
            if(event) {
                if ( pos! < this.panes()[0].minSize() ) return;
                if ( width - pos! < this.panes()[this.panes().length - 1].minSize() ) return;
            }

            this.renderer.setStyle( this.handler, 'left', pos + 'px' );
            this.renderer.setStyle( this.panes()[0].element.nativeElement, 'width', pos + 'px' );
            this.renderer.setStyle( this.panes()[this.panes().length - 1].element.nativeElement, 'left',  (pos + this.handler.offsetWidth) + 'px' );
        }
    }

    private getPosition(bounds: any, event?: any, wantedPosition?: number) {
        if ( this.orientation() === 'vertical' ) {
            const height = bounds.bottom - bounds.top;
            if ((wantedPosition! < 0 || wantedPosition === undefined) && !event) {
                return height / 2;
            } else if (event) {
                return event.clientY - bounds.top;
            }
            if (wantedPosition! >= 0 && wantedPosition! <= 1) {
                return Math.round(height * wantedPosition!);
            }
        } else {//horizontal
            const width = bounds.right - bounds.left;
            if ((wantedPosition! < 0 || wantedPosition === undefined) && !event) {
                return width / 2;
            } else if (event) {
                return event.clientX - bounds.left;
            }
            if (wantedPosition! >= 0 && wantedPosition! <= 1) {
                return Math.round(width * wantedPosition!);
            }
        }
        return wantedPosition;
    }
}
