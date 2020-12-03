import { Component, ViewChild, SimpleChanges, Input, Renderer2, ElementRef, EventEmitter, Output, OnDestroy, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent, SvyUtilsService } from '../../ngclient/servoy_public';
import { LoggerFactory, LoggerService } from '../../sablo/logger.service';
import { FileUploadModule, FileSelectDirective, FileUploader } from 'ng2-file-upload';

@Component( {
    selector: 'servoyextra-fileupload',
    templateUrl: './fileupload.html',
    styleUrls: ['./fileupload.css']
} )
export class ServoyExtraFileUpload extends ServoyBaseComponent {

    @Input() onDataChangeMethodID;
    @Input() onFileUploadedMethodID;
    @Input() onFileTransferFinishedMethodID;

    @Output() dataProviderIDChange = new EventEmitter();
    @Input() dataProviderID;
    @Input() displaysTags;
    @Input() accept;
    @Input() enabled;
    @Input() location;
    @Input() name;
    @Input() size;
    @Input() styleClass;
    @Input() styleClassExpression;
    @Input() iconStyleClass;
    @Input() resultDisplayTimeout;
    @Input() successIconStyleClass;
    @Input() showFileName;
    @Input() showProgress;
    @Input() multiFileUpload;
    @Input() uploadText;
    @Input() uploadProgressText;
    @Input() uploadSuccessText;
    @Input() uploadCancelText;
    @Input() uploadNotSupportedText;
    @Input() uploadNotSupportedFileText;
    @Input() toolTipText;

    uploader: FileUploader;
    hasBaseDropZoneOver: boolean;

    private log: LoggerService;

    constructor( renderer: Renderer2, cdRef: ChangeDetectorRef, logFactory: LoggerFactory, private utilsService: SvyUtilsService ) {
        super( renderer, cdRef );
        this.log = logFactory.getLogger( 'FileUpload' );
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

