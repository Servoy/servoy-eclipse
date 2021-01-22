import { Component, SimpleChanges, Input, Renderer2, EventEmitter, Output, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent, SvyUtilsService } from '../../ngclient/servoy_public';
import { FileUploader } from 'ng2-file-upload';

@Component( {
    selector: 'servoyextra-fileupload',
    templateUrl: './fileupload.html',
    styleUrls: ['./fileupload.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyExtraFileUpload extends ServoyBaseComponent<HTMLDivElement> {

    @Input() onDataChangeMethodID: ( e: Event ) => void;
    @Input() onFileUploadedMethodID: ( e: Event ) => void;
    @Input() onFileTransferFinishedMethodID: ( e: Event ) => void;

    @Output() dataProviderIDChange = new EventEmitter();
    @Input() dataProviderID: any;
    @Input() displaysTags: boolean;
    @Input() accept: any;
    @Input() enabled: boolean;
    @Input() location: any;
    @Input() name: string;
    @Input() size: any;
    @Input() styleClass: string;
    @Input() styleClassExpression: string;
    @Input() iconStyleClass: string;
    @Input() resultDisplayTimeout: number;
    @Input() successIconStyleClass: string;
    @Input() showFileName: boolean;
    @Input() showProgress: boolean;
    @Input() multiFileUpload: boolean;
    @Input() uploadText: string;
    @Input() uploadProgressText: string;
    @Input() uploadSuccessText: string;
    @Input() uploadCancelText: string;
    @Input() uploadNotSupportedText: string;
    @Input() uploadNotSupportedFileText: string;
    @Input() toolTipText: string;

    uploader: FileUploader;
    hasBaseDropZoneOver: boolean;

    constructor( renderer: Renderer2, cdRef: ChangeDetectorRef, private utilsService: SvyUtilsService ) {
        super( renderer, cdRef );
        this.uploader = new FileUploader( {
            url: '',
        } );
        this.hasBaseDropZoneOver = false;
    }

    public fileOverBase( e: any ): void {
        this.hasBaseDropZoneOver = e;
    }

    public fileInputClick(): void {
        const element: HTMLElement = document.getElementById( 'fileInputLabel' ) as HTMLElement;
        element.click();
    }

    svyOnInit() {
        super.svyOnInit();
        const url = this.utilsService.generateUploadUrl( this.servoyApi.getFormname(), this.name, 'dataProviderID' );
        this.uploader = new FileUploader( {
            url,
        });
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
                }
            }
        }
        super.svyOnChanges( changes );
    }

    getFocusElement(): any {
        return this.getNativeElement();
    }
}

