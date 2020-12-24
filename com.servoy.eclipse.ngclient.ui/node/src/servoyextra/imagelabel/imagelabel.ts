import { Component, ViewChild, SimpleChanges, Input, Renderer2, ElementRef, EventEmitter, Output, OnDestroy, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent } from '../../ngclient/servoy_public';
import { LoggerFactory, LoggerService } from '../../sablo/logger.service';

@Component( {
    selector: 'servoyextra-imagelabel',
    templateUrl: './imagelabel.html'
} )
export class ServoyExtraImageLabel extends ServoyBaseComponent<HTMLImageElement> {

    @Input() onActionMethodID;
    @Input() onRightClickMethodID;

    @Input() enabled;
    @Input() styleClass;
    @Input() tabSeq;
    @Input() media;

    imageURL = 'bootstrapcomponents/imagemedia/images/empty.gif';

    private log: LoggerService;

    constructor( renderer: Renderer2, cdRef: ChangeDetectorRef, logFactory: LoggerFactory ) {
        super( renderer, cdRef );
        this.log = logFactory.getLogger( 'ImageLabel' );
    }

    svyOnInit() {
        super.svyOnInit();
        this.attachHandlers();
    }

    svyOnChanges( changes: SimpleChanges ) {
        if ( changes ) {
            for ( const property of Object.keys( changes ) ) {
                const change = changes[property];
                switch ( property ) {
                    case 'enabled':
                        if ( change.currentValue )
                            this.renderer.removeAttribute( this.getFocusElement(), 'disabled' );
                        else
                            this.renderer.setAttribute( this.getFocusElement(), 'disabled', 'disabled' );
                        break;
                    case 'media':
                        this.updateImageURL( change.currentValue );
                        break;
                    case 'styleClass':
                        if ( change.previousValue )
                            this.renderer.removeClass( this.getNativeElement(), change.previousValue );
                        if ( change.currentValue )
                            this.renderer.addClass( this.getNativeElement(), change.currentValue );
                        break;
                }
            }
        }
        super.svyOnChanges( changes );
    }

    getFocusElement(): any {
        return this.getNativeElement();
    }

    private updateImageURL( media: any ) {
        if ( media ) this.imageURL = media;
    }

    protected attachHandlers() {
        if ( this.onActionMethodID ) {
            this.renderer.listen( this.getNativeElement(), 'click', e => this.onActionMethodID( e ) );
        }
        if ( this.onRightClickMethodID ) {
            this.renderer.listen( this.getNativeElement(), 'contextmenu', e => {
 this.onRightClickMethodID( e ); return false;
} );
        }
    }
}

