import { Component, ViewChild, SimpleChanges, Input, Renderer2, ElementRef, EventEmitter, Output, OnDestroy, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent, PropertyUtils } from '../../ngclient/servoy_public';
import { LoggerFactory, LoggerService } from '../../sablo/logger.service';
import { AngularEditorComponent, AngularEditorConfig } from '@kolkov/angular-editor';

@Component( {
    selector: 'servoyextra-htmlarea',
    templateUrl: './htmlarea.html',
} )
export class ServoyExtraHtmlarea extends ServoyBaseComponent {

    @Input() onActionMethodID;
    @Input() onRightClickMethodID;
    @Input() onDataChangeMethodID;
    @Input() onFocusGainedMethodID;
    @Input() onFocusLostMethodID;

    @Output() dataProviderIDChange = new EventEmitter();
    @Input() dataProviderID;
    @Input() enabled;
    @Input() editable;
    @Input() placeholderText;
    @Input() readOnly;
    @Input() responsiveHeight;
    @Input() styleClass;
    @Input() tabSeq;
    @Input() text;
    @Input() toolTipText;
    @Input() scrollbars;

    @ViewChild( AngularEditorComponent ) editor: AngularEditorComponent;

    private log: LoggerService;

    config: AngularEditorConfig = {
        editable: true,
        enableToolbar: true,
        spellcheck: true,
        translate: 'no',
        defaultParagraphSeparator: 'p'
    };

    constructor( renderer: Renderer2, cdRef: ChangeDetectorRef, logFactory: LoggerFactory ) {
        super( renderer, cdRef );
        this.log = logFactory.getLogger( 'Htmlarea' );
    }

    svyOnInit() {
        super.svyOnInit();

        this.attachHandlers();
        this.attachFocusListeners( this.getFocusElement() );

        if ( this.dataProviderID === undefined ) {
            this.dataProviderID = null;
        }
        // ugly hack to fix the height
        const nativeElement = this.getNativeElement();
        const componentHeight = nativeElement.offsetHeight;
        // let toolBarHeight = nativeElement.childNodes[0].childNodes[0].childNodes[1].childNodes[1].offsetHeight;
        const initialContentHeight = nativeElement.childNodes[0].childNodes[0].childNodes[2].childNodes[0].offsetHeight;
        const initialEditorHeight = nativeElement.childNodes[0].childNodes[0].offsetHeight;

        this.renderer.setStyle( nativeElement.childNodes[0].childNodes[0].childNodes[2].childNodes[0], 'height', ( initialContentHeight + componentHeight - initialEditorHeight ) + 'px' );

    }

    public getScrollX(): number {
        return this.getNativeElement().scrollLeft;
    }

    public getScrollY(): number {
        return this.getNativeElement().scrollTop;
    }

    public setScroll( x: number, y: number ) {
        this.getNativeElement().scrollLeft = x;
        this.getNativeElement().scrollTop = y;
    }

    svyOnChanges( changes: SimpleChanges ) {
        if ( changes ) {
            for ( const property of Object.keys( changes ) ) {
                const change = changes[property];
                switch ( property ) {
                    case 'styleClass':
                        if ( change.previousValue )
                            this.renderer.removeClass( this.getNativeElement(), change.previousValue );
                        if ( change.currentValue )
                            this.renderer.addClass( this.getNativeElement(), change.currentValue );
                        break;
                    case 'scrollbars':
                        if ( change.currentValue ) {
                            const element = this.getNativeChild().getElementsByClassName( 'angular-editor-textarea' );
                            PropertyUtils.setScrollbars( element, this.renderer, change.currentValue );
                        }
                        break;
                    case 'editable':
                        this.config.editable = this.editable;
                        break;
                    case 'enabled':
                        this.config.enableToolbar = this.enabled;
                        break;
                }
            }
        }
        super.svyOnChanges( changes );
    }

    getFocusElement(): any {
        return this.getNativeElement();
    }

    requestFocus() {
        this.editor.focus();
    }

    pushUpdate() {
        this.dataProviderIDChange.emit( this.dataProviderID );
    }

    attachFocusListeners( nativeElement: any ) {
        if ( this.onFocusGainedMethodID ) {
            this.editor.focusEvent.subscribe(() => {
                this.onFocusGainedMethodID( new CustomEvent( 'focus' ) );
            } );
        }

        this.editor.blurEvent.subscribe(() => {
            this.pushUpdate();
            if ( this.onFocusLostMethodID ) this.onFocusLostMethodID( new CustomEvent( 'blur' ) );
        } );
    }

    protected attachHandlers() {
        if ( this.onActionMethodID ) {

            if ( this.getNativeElement().tagName === 'TEXTAREA' || this.getNativeElement().type === 'text' ) {
                this.renderer.listen( this.getNativeElement(), 'keydown', e => {
                    if ( e.keyCode === 13 ) this.onActionMethodID( e );
                } );
            } else {
                this.renderer.listen( this.getNativeElement(), 'click', e => this.onActionMethodID( e ) );
            }
        }
        if ( this.onRightClickMethodID ) {
            this.renderer.listen( this.getNativeElement(), 'contextmenu', e => {
                this.onRightClickMethodID( e ); return false;
            } );
        }
    }
}
