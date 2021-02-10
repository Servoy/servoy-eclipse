import { Component, ViewChild, SimpleChanges, Input, Renderer2, EventEmitter, Output, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent, PropertyUtils } from '../../ngclient/servoy_public';
import { AngularEditorComponent, AngularEditorConfig } from '@kolkov/angular-editor';

@Component( {
    selector: 'servoyextra-htmlarea',
    templateUrl: './htmlarea.html',
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyExtraHtmlarea extends ServoyBaseComponent<HTMLDivElement> {

    @Input() onActionMethodID: ( e: Event ) => void;
    @Input() onRightClickMethodID: ( e: Event ) => void;
    @Input() onDataChangeMethodID: ( e: Event ) => void;
    @Input() onFocusGainedMethodID: ( e: Event ) => void;
    @Input() onFocusLostMethodID: ( e: Event ) => void;

    @Output() dataProviderIDChange = new EventEmitter();
    @Input() dataProviderID: any;
    @Input() enabled: boolean;
    @Input() editable: boolean;
    @Input() placeholderText: string;
    @Input() readOnly: boolean;
    @Input() responsiveHeight: any;
    @Input() styleClass: string;
    @Input() tabSeq: number;
    @Input() text: string;
    @Input() toolTipText: string;
    @Input() scrollbars: any;

    mustExecuteOnFocus = true;

    @ViewChild( AngularEditorComponent ) editor: AngularEditorComponent;

    config: AngularEditorConfig = {
        editable: true,
        enableToolbar: true,
        spellcheck: true,
        translate: 'no',
        defaultParagraphSeparator: 'p'
    };

    constructor( renderer: Renderer2, cdRef: ChangeDetectorRef ) {
        super( renderer, cdRef );
    }

    svyOnInit() {
        super.svyOnInit();

        this.attachHandlers();
        this.attachFocusListeners();

        if ( this.dataProviderID === undefined ) {
            this.dataProviderID = null;
        }
        // ugly hack to fix the height
        const nativeElement = this.getNativeElement();
        const componentHeight = nativeElement.offsetHeight;
        // let toolBarHeight = nativeElement.childNodes[0].childNodes[0].childNodes[1].childNodes[1].offsetHeight;
        const initialContentHeight = ( nativeElement.childNodes[0].childNodes[0].childNodes[2].childNodes[0] as HTMLElement ).offsetHeight;
        const initialEditorHeight = ( nativeElement.childNodes[0].childNodes[0] as HTMLElement ).offsetHeight;

        this.renderer.setStyle( nativeElement.childNodes[0].childNodes[0].childNodes[2].childNodes[0], 'height', ( initialContentHeight + componentHeight - initialEditorHeight ) + 'px' );

        // work around for https://github.com/kolkov/angular-editor/issues/341
        setTimeout(() => {
            this.cdRef.detectChanges();
        }, 5);

    }

    public getScrollX(): number {
        return this.getFocusElement().scrollLeft;
    }

    public getScrollY(): number {
        return this.getFocusElement().scrollTop;
    }

    public setScroll( x: number, y: number ) {
        this.getFocusElement().scrollLeft = x;
        this.getFocusElement().scrollTop = y;
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
                            const element = this.getNativeChild().textarea;
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

    getFocusElement() {
        return this.editor.textArea.nativeElement;
    }

    requestFocus( mustExecuteOnFocusGainedMethod: boolean ) {
        this.mustExecuteOnFocus = mustExecuteOnFocusGainedMethod;
        this.getFocusElement().focus();
    }

    pushUpdate() {
        this.dataProviderIDChange.emit( this.dataProviderID );
    }

    attachFocusListeners() {
        if ( this.onFocusGainedMethodID ) {
            this.editor.focusEvent.subscribe(() => {
                if ( this.mustExecuteOnFocus === true ) {
                    this.onFocusGainedMethodID( new CustomEvent( 'focus' ) );
                }
                this.mustExecuteOnFocus = true;
            } );
        }

        this.editor.blurEvent.subscribe(() => {
            this.pushUpdate();
            if ( this.onFocusLostMethodID ) this.onFocusLostMethodID( new CustomEvent( 'blur' ) );
        } );
    }

    public selectAll() {
        const range = document.createRange();
        range.selectNodeContents( this.getFocusElement() );
        const sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange( range );
    }

    protected attachHandlers() {
        if ( this.onActionMethodID ) {

            if ( this.getNativeElement().tagName === 'TEXTAREA' /*|| this.getNativeElement().type === 'text' */ ) {
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

    public getSelectedText(): string {
        const selection = window.getSelection();
        let node = selection.anchorNode;
        while ( node ) {
            if ( node === this.getFocusElement() || node === this.getFocusElement().parentNode ) {
                return selection.toString();
            }
            node = node.parentNode;
        }
        return '';
    }

    public replaceSelectedText( text: string ) {
        var sel: any, range: any;
        if ( window.getSelection ) {
            sel = window.getSelection();
            if ( sel.rangeCount ) {
                range = sel.getRangeAt( 0 );
                range.deleteContents();
                range.insertNode( document.createTextNode( text ) );
            }
        }
    }

    public getAsPlainText(): string {
        if ( this.dataProviderID ) {
            return this.dataProviderID.replace( /<[^>]*>/g, '' );
        }
        return this.dataProviderID;
    }
}
