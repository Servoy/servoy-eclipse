import { Component, Input, Output, EventEmitter, ChangeDetectorRef, SimpleChanges,
        Renderer2, ElementRef, ViewChild, Directive } from '@angular/core';

import {PropertyUtils, FormattingService} from '../ngclient/servoy_public';

import {ServoyDefaultBaseComponent} from './basecomponent';

@Directive()
export class ServoyDefaultBaseLabel extends  ServoyDefaultBaseComponent {

    @Input() hideText;
    @Input() imageMediaID;
    @Input() mediaOptions;
    @Input() mnemonic;
    @Input() rolloverCursor;
    @Input() rolloverImageMediaID;
    @Input() showFocus;
    @Input() textRotation;
    @Input() verticalAlignment;

    @ViewChild('child') child: ElementRef;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    protected attachHandlers() {
        super.attachHandlers();
        if ( this.onActionMethodID ) {
            if (this.onDoubleClickMethodID) {
                const innerThis: ServoyDefaultBaseComponent = this;
                this.renderer.listen( this.getNativeElement(), 'click', e => {
                    if (innerThis.timeoutID) {
                        window.clearTimeout(innerThis.timeoutID);
                        innerThis.timeoutID = null;
                        // double click, do nothing
                    } else {
                        innerThis.timeoutID = window.setTimeout(function() {
                            innerThis.timeoutID = null;
                            innerThis.onActionMethodID( e );
                        }, 250); }
                 });
            } else {
                this.renderer.listen( this.getNativeElement(), 'click', e => this.onActionMethodID( e ));
            }
        }
        if ( this.onDoubleClickMethodID ) {
            this.renderer.listen( this.elementRef.nativeElement, 'dblclick', ( e ) => {
                this.onDoubleClickMethodID( e );
            } );
        }
    }

    svyOnChanges( changes: SimpleChanges ) {
        for ( const property of Object.keys(changes) ) {
            const change = changes[property];
            switch ( property ) {
                case 'rolloverCursor':
                    this.renderer.setStyle(this.elementRef.nativeElement,  'cursor', change.currentValue == 12 ? 'pointer' : 'default' );
                    break;
                case 'mnemonic':
                    if ( change.currentValue ) this.renderer.setAttribute(this.elementRef.nativeElement,   'accesskey', change.currentValue );
                    else  this.renderer.removeAttribute(this.elementRef.nativeElement,  'accesskey' );
                    break;
                case 'textRotation':
                    if (change.currentValue) PropertyUtils.setRotation(this.getNativeElement(), this.renderer, change.currentValue, this.size);
                    break;
            }
        }
        super.svyOnChanges(changes);
    }

    public getNativeChild() {
        return this.child.nativeElement;
    }
}
