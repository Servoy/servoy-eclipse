import { Component, ViewChild, SimpleChanges, Input, Renderer2, ElementRef, EventEmitter, Output, OnDestroy, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent } from '../../ngclient/servoy_public'
import { LoggerFactory, LoggerService } from '../../sablo/logger.service';
import { FileUploadModule, FileUploader } from 'ng2-file-upload';

//const URL = '/api/';
const URL = 'https://evening-anchorage-3159.herokuapp.com/api/';

@Component( {
    selector: 'servoyextra-fileupload',
    templateUrl: './fileupload.html'
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
    @Input() visible;
    
    uploader:FileUploader;
    hasBaseDropZoneOver:boolean;
    
    private log: LoggerService;

    constructor( renderer: Renderer2, cdRef: ChangeDetectorRef, logFactory: LoggerFactory ) {
        super( renderer, cdRef );
        this.log = logFactory.getLogger( 'FileUpload' );
        this.uploader = new FileUploader( {
            url: URL,
            disableMultipart: true, // 'DisableMultipart' must be 'true' for formatDataFunction to be called.
            formatDataFunctionIsAsync: true,
            formatDataFunction: async ( item ) => {
                return new Promise(( resolve, reject ) => {
                    resolve( {
                        name: this.name,
                        length: this.size,
                        contentType: item._file.type,
                        date: new Date()
                    } );
                } );
            }
        } );

        this.hasBaseDropZoneOver = true;
    }

    public fileOverBase( e: any ): void {
        this.hasBaseDropZoneOver = e;
    }

    svyOnInit() {
        super.svyOnInit();
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

