import { Component, ChangeDetectorRef, SimpleChanges, ViewChild, Directive, ElementRef, OnInit, EventEmitter, Output, Renderer2, Input, ChangeDetectionStrategy } from '@angular/core';
import { BaseCustomObject, ServoyBaseComponent, ServoyPublicService } from '@servoy/public';
import { Format } from '@servoy/public';

@Component( {
    selector: 'bootstrapextracomponents-input-group',
    templateUrl: './inputgroup.html',
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyBootstrapExtraInputGroup extends ServoyBaseComponent<HTMLDivElement> {

    @ViewChild( 'input', { static: false } ) input: ElementRef<HTMLInputElement>;

    @Input() onAction: ( e: Event, data?: any ) => void;
    @Input() onRightClick: ( e: Event, data?: any ) => void;
    @Input() onDataChangeMethodID: ( e: Event ) => void;
    @Input() onFocusGainedMethodID: ( e: Event ) => void;
    @Input() onFocusLostMethodID: ( e: Event ) => void;

    @Output() dataProviderChange = new EventEmitter();
    @Input() dataProvider: any;
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
    preventSimpleClick = false;
    timer: any;

    constructor( renderer: Renderer2, cdRef: ChangeDetectorRef, private servoyService: ServoyPublicService ) {
        super( renderer, cdRef );
    }

    public getFocusElement(): HTMLElement {
        return this.input.nativeElement;
    }

    svyOnInit() {
        super.svyOnInit();
        this.attachFocusListeners( this.getFocusElement() );
        if (this.dataProvider === undefined) {
            this.dataProvider = null;
        }
        if ( this.onAction ) {
            this.renderer.listen( this.getFocusElement(), 'click', e => this.onAction( e ) );
        }
        if ( this.onRightClick ) {
            this.renderer.listen( this.getFocusElement(), 'contextmenu', e => {
                this.onRightClick( e ); return false;
            } );
        }
    }

    svyOnChanges( changes: SimpleChanges ) {
        if ( changes ) {
            for ( const property of Object.keys( changes ) ) {
                const change = changes[property];
                switch ( property ) {
                    case 'placeholderText':
                        if ( change.currentValue ) this.renderer.setAttribute( this.getFocusElement(), 'placeholder', change.currentValue );
                        else this.renderer.removeAttribute( this.getFocusElement(), 'placeholder' );
                        break;
                }
            }
            super.svyOnChanges( changes );
        }
    }

    pushUpdate() {
        this.dataProviderChange.emit(this.dataProvider);
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
        return this.addOnButtons.filter(( addOnBtn: any ) => addOnBtn.position === position );
    }


    buttonClicked( event: any, btnText: string, btnIndex: number ) {
        const addOnButton = this.addOnButtons[btnIndex];
        this.timer = 0;
        this.preventSimpleClick = false;

        if ( addOnButton && addOnButton.onAction && event.type === 'click' ) {
            if ( addOnButton.onDoubleClick ) {
                this.timer = setTimeout(() => {
                    if ( !this.preventSimpleClick ) {
                        const jsEvent = this.servoyService.createJSEvent( event, 'action' );
                        this.servoyService.executeInlineScript( addOnButton.onAction.formname, addOnButton.onAction.script, [jsEvent, addOnButton.name, btnText, btnIndex] );
                    }
                }, 250 );

            } else {
                const jsEvent = this.servoyService.createJSEvent( event, 'action' );
                this.servoyService.executeInlineScript( addOnButton.onAction.formname, addOnButton.onAction.script, [jsEvent, addOnButton.name, btnText, btnIndex] );
            }
        }
    }

    buttonDoubleClicked( event: any, btnText: string, btnIndex: number ) {
        const addOnButton = this.addOnButtons[btnIndex];

        if ( addOnButton && event.type === 'dblclick' && addOnButton.onDoubleClick ) {
            this.preventSimpleClick = true;
            clearTimeout( this.timer );
            const jsEvent = this.servoyService.createJSEvent( event, 'doubleclick' );
            this.servoyService.executeInlineScript( addOnButton.onDoubleClick.formname, addOnButton.onDoubleClick.script, [jsEvent, addOnButton.name, btnText, btnIndex] );

        }

    }

    buttonRightClicked( event: any, btnText: string, btnIndex: number ) {
        const addOnButton = this.addOnButtons[btnIndex];
        if ( addOnButton && event.type === 'contextmenu' && addOnButton.onRightClick ) {
            event.preventDefault();
            const jsEvent = this.servoyService.createJSEvent( event, 'rightclick' );
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
