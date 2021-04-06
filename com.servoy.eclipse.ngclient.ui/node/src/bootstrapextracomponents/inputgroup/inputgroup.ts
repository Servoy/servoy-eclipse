import { Component, ChangeDetectorRef, SimpleChanges, ViewChild, Directive, ElementRef, OnInit, EventEmitter, Output, Renderer2, Input, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent } from '../../ngclient/servoy_public';
import { Format } from '../../ngclient/servoy_public';
import { ServoyService } from '../../ngclient/servoy.service';
import { SvyUtilsService } from '../../ngclient/servoy_public';
import { BaseCustomObject } from '../../sablo/spectypes.service';

@Component( {
    selector: 'bootstrapextracomponents-input-group',
    templateUrl: './inputgroup.html',
    styleUrls: ['./inputgroup.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyBootstrapExtraInputGroup extends ServoyBaseComponent<HTMLDivElement> {

    @ViewChild( 'input', { static: false } ) input: ElementRef<HTMLInputElement>;

    @Input() onAction: ( e: Event, data?: any ) => void;
    @Input() onRightClick: ( e: Event, data?: any ) => void;
    @Input() onDataChangeMethodID: ( e: Event ) => void;
    @Input() onFocusGainedMethodID: ( e: Event ) => void;
    @Input() onFocusLostMethodID: ( e: Event ) => void;

    @Output() dataProviderIDChange = new EventEmitter();
    @Input() dataProviderID: any;
    @Input() enabled: boolean;
    @Input() editable: boolean;
    @Input() format: Format;
    @Input() inputType: string;
    @Input() placeholderText: string;
    @Input() readOnly: boolean;
    @Input() styleClass: string;
    @Input() tabSeq: number;
    @Input() visible: boolean;
    @Input() addOns: AddOn[];
    @Input() addOnButtons: AddOnButton[];
    @Input() size: { width: number; height: number };
    @Input() toolTipText: string;

    mustExecuteOnFocus = true;

    constructor( renderer: Renderer2, cdRef: ChangeDetectorRef, private utils: SvyUtilsService, private servoyService: ServoyService ) {
        super( renderer, cdRef );
    }

    public getFocusElement(): HTMLElement {
        return this.getNativeElement();
    }

    svyOnInit() {
        super.svyOnInit();
        this.attachFocusListeners( this.input.nativeElement );
        if ( this.onAction ) {
            this.renderer.listen( this.input.nativeElement, 'click', e => this.onAction( e ) );
        }
        if ( this.onRightClick ) {
            this.renderer.listen( this.input.nativeElement, 'contextmenu', e => {
                this.onRightClick( e ); return false;
            } );
        }
    }

    svyOnChanges( changes: SimpleChanges ) {
        if ( changes ) {
            super.svyOnChanges( changes );
        }
    }

    requestFocus( mustExecuteOnFocusGainedMethod: boolean ) {
        this.mustExecuteOnFocus = mustExecuteOnFocusGainedMethod;
        this.getFocusElement().focus();
    }

    attachFocusListeners( nativeElement: HTMLElement ) {
        if ( this.onFocusGainedMethodID )
            this.renderer.listen( nativeElement, 'focus', ( e ) => {
                if ( this.mustExecuteOnFocus === true ) {
                    this.onFocusGainedMethodID( e );
                }
                this.mustExecuteOnFocus = true;
            } );
        if ( this.onFocusLostMethodID )
            this.renderer.listen( nativeElement, 'blur', ( e ) => {
                this.onFocusLostMethodID( e );
            } );
    }
    hasLeftButtons() {
        return this.filterButtons( 'LEFT' ).length > 0;
    }

    hasRightButtons() {
        return this.filterButtons( 'RIGHT' ).length > 0;
    }

    filterButtons( position: string ) {
        if ( !this.addOnButtons ) {
            return [];
        }
        function filterButtons( addOnBtn: any ) {
            return addOnBtn.position === position;
        }
        return this.addOnButtons.filter( filterButtons );
    }

    buttonClicked( event: any, btnText: string, btnIndex: number ) {
        const addOnButton = this.addOnButtons[btnIndex];
        if ( addOnButton && addOnButton.onAction ) {
            const jsEvent = this.utils.createJSEvent( event, 'action' );
            this.servoyService.executeInlineScript( addOnButton.onAction.formname, addOnButton.onAction.script, [jsEvent, addOnButton.name, btnText, btnIndex] );
        }
        else if ( addOnButton && event.type == 'dblclick' && addOnButton.onDoubleClick ) {
            const jsEvent = this.utils.createJSEvent( event, 'doubleclick' );
            this.servoyService.executeInlineScript( addOnButton.onDoubleClick.formname, addOnButton.onDoubleClick.script, [jsEvent, addOnButton.name, btnText, btnIndex] );

        }
        else if ( addOnButton && event.type == 'contextmenu' && addOnButton.onRightClick ) {
            const jsEvent = this.utils.createJSEvent( event, 'rightclick' );
            this.servoyService.executeInlineScript( addOnButton.onRightClick.formname, addOnButton.onRightClick.script, [jsEvent, addOnButton.name, btnText, btnIndex] );
        }
    }
}

export class AddOn extends BaseCustomObject {
    public attributes: Array<{ key: string; value: string }>;
    public text: string;
    public position: string;
}

export class AddOnButton extends AddOn {
    public name: string;
    public onAction: { formname: string; script: string };
    public onDoubleClick: { formname: string; script: string };
    public onRightClick: { formname: string; script: string };
    public styleClass: string;
    public imageStyleClass: string;
}

@Directive( {
    selector: '[svyAttributesInputGroup]'
} )
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export class SvyAttributesInputGroup implements OnInit {
    @Input( 'svyAttributesInputGroup' ) attributes: Array<{ key: string; value: string }>;

    constructor( private el: ElementRef, private renderer: Renderer2 ) {

    }

    ngOnInit(): void {
        if ( this.attributes ) {
            const nativeElem = this.el.nativeElement;
            Array.from( this.attributes ).forEach( attribute => this.renderer.setAttribute( nativeElem, attribute.key, attribute.value ) );
        }
    }
}