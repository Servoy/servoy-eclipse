import { Component, ViewChild, SimpleChanges, Input, Renderer2, ChangeDetectorRef } from '@angular/core';
import { ServoyBaseComponent, SvyUtilsService } from '../../ngclient/servoy_public';
import { UppyConfig, UppyAngularComponent } from "uppy-angular";

@Component({
    selector: 'servoyextra-multifileupload',
    templateUrl: './multifileupload.html'
})
export class ServoyExtraMultiFileUpload extends ServoyBaseComponent<HTMLDivElement> {

    @Input() autoProceed;
    @Input() allowMultipleUploads;
    @Input() hideUploadButton;
    @Input() restrictions;
    @Input() note;
    @Input() metaFields;
    @Input() size;
    @Input() cssPosition;
    @Input() disableStatusBar;
    @Input() inline;
    @Input() closeAfterFinish;
    @Input() sources: string[];
    @Input() options: object;
    @Input() localeStrings: object;

    @Input() onFileUploaded;
    @Input() onFileAdded;
    @Input() onBeforeFileAdded;
    @Input() onFileRemoved;
    @Input() onUploadComplete;
    @Input() onModalOpened
    @Input() onModalClosed;
    @Input() onRestrictionFailed;

    @ViewChild('element', { static: false }) uppyRef: UppyAngularComponent;

    settings: UppyConfig = null;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, private utilsService: SvyUtilsService) {
        super(renderer, cdRef);
    }

    ngOnInit() {
        super.ngOnInit();
        this.internalInit();
    }

    svyOnInit() {
        super.svyOnInit();
        if (this.onFileAdded) {
            this.uppyRef.uppyInstance.on('file-added', (file) => {
                this.onFileAdded(this.createUppyFile(file));
            });
        }

        if (this.onFileRemoved) {
            this.uppyRef.uppyInstance.on('file-removed', (file) => {
                this.onFileRemoved(this.createUppyFile(file));
            });
        }

        if (this.onRestrictionFailed) {
            this.uppyRef.uppyInstance.on('restriction-failed', (file, error) => {
                this.onRestrictionFailed(this.createUppyFile(file), error.message);
            });
        }

        if (this.onModalOpened) {
            this.uppyRef.uppyInstance.on('dashboard:modal-open', () => {
                this.onModalOpened();
            });
        }

        if (this.onModalClosed) {
            this.uppyRef.uppyInstance.on('dashboard:modal-closed', () => {
                this.onModalOpened();
            });
        }

        if (this.onUploadComplete) {
            this.uppyRef.uppyInstance.on('complete', (result) => {
                var filesSuccess = [];
                if (result.successful) {
                    for (var o = 0; o < result.successful.length; o++) {
                        filesSuccess.push(this.createUppyFile(result.successful[o]));
                    }
                }
                var filesFailed = [];
                if (result.failed) {
                    for (var f = 0; f < result.failed.length; f++) {
                        filesFailed.push(this.createUppyFile(result.failed[f]));
                    }
                }
                this.onUploadComplete(filesSuccess, filesFailed);
            });
        }
        this.uppyRef.uppyInstance.setOptions({
            onBeforeFileAdded: (currentFile, files) => this.onBeforeFileAddedEvent(currentFile, files)
        });
        let locale = null;
        if (this.localeStrings) {
            for (var key in this.localeStrings) {
                var localeString = this.localeStrings[key];
                if (key.indexOf('.') !== -1) {
                    var keyParts = key.split('.');
                    if (!locale.strings.hasOwnProperty(keyParts[0])) {
                        locale.strings[keyParts[0]] = {};
                    }
                    locale.strings[keyParts[0]][keyParts[1]] = localeString;
                } else {
                    locale.strings[key] = localeString;
                }
            }
        }

        let dashBoardOptions = {
            disableStatusBar: this.disableStatusBar,
            inline: this.inline,
            closeAfterFinish: this.closeAfterFinish,
            locale: locale
        };

        if (this.options) {
            for (var x in this.options) {
                dashBoardOptions[x] = this.options[x];
            }
        }
        this.uppyRef.uppyInstance.getPlugin('Dashboard').setOptions(dashBoardOptions);
    }

    svyOnChanges(changes: SimpleChanges) {
        super.svyOnChanges(changes);
        this.internalInit();
    }

    reset(): void {
        this.uppyRef.uppyInstance.reset();
    }

    upload(): void {
        this.uppyRef.uppyInstance.upload();
    }

    retryAll(): void {
        this.uppyRef.uppyInstance.retryAll();
    }

    cancelAll(): void {
        this.uppyRef.uppyInstance.cancelAll();
    }

    retryUpload(fileID: string): void {
        this.uppyRef.uppyInstance.retryUpload(fileID);
    }

    removeFile(fileID: string): void {
        this.uppyRef.uppyInstance.removeFile(fileID);
    }

    info(message: any, type?: string, duration?: number): void {
        this.uppyRef.uppyInstance.info(message, type, duration);
    }

    initialize(): void {
        this.uppyRef.uppyInstance.close();
        this.internalInit();
    }

    openModal(): void {
        this.uppyRef.uppyInstance.getPlugin('Dashboard').openModal();
    }

    closeModal(): void {
        this.uppyRef.uppyInstance.getPlugin('Dashboard').closeModal();
    }

    internalInit(): void {
        let uppyPlugins = {};
        if (this.sources) {
            this.sources.forEach((value) => { uppyPlugins[value] = true });
        }
        this.settings = {
            uploadAPI: {
                endpoint: this.utilsService.generateUploadUrl(this.servoyApi.getFormname(), this.name, 'onFileUploaded')
            },
            plugins: uppyPlugins,
            restrictions: this.restrictions,
            statusBarOptions: {
                hideUploadButton: this.hideUploadButton
            },
            uploaderLook: {
                note: this.note,
                width: this.cssPosition.width,
                height: this.cssPosition.height
            },
            options: {
                autoProceed: this.autoProceed,
                allowMultipleUploads: this.allowMultipleUploads,
                meta: this.metaFields
            }
        };
    }

    filesToBeAdded: Array<String> = [];

    onBeforeFileAddedEvent(currentFile: any, files: any): boolean {
        if (!this.onBeforeFileAdded) {
            return true;
        }
        let currentFiles = this.getFiles();

        if (this.filesToBeAdded.indexOf(currentFile.name) !== -1) {
            return true;
        }

        this.filesToBeAdded.push(currentFile.name);

        this.onBeforeFileAdded(this.createUppyFile(currentFile), currentFiles).then(function(result) {
            if (result === true) {
                this.uppyRef.uppyInstance.addFile(currentFile);
            }
            this.filesToBeAdded.splice(this.filesToBeAdded.indexOf(currentFile.name), 1);
        });
        return false;
    }

    getFile(fileID: string): UploadFile {
        let file = this.uppyRef.uppyInstance.getFile(fileID);
        if (file != null) {
            return this.createUppyFile(file);
        }
        return null;
    }

    getFiles(): UploadFile[] {
        let files = this.uppyRef.uppyInstance.getFiles();
        let result = [];
        if (files) {
            for (var f = 0; f < files.length; f++) {
                result.push(this.createUppyFile(files[f]));
            }
        }
        return result;
    }

    createUppyFile(file: any): UploadFile {
        let result: UploadFile = {
            id: file.id,
            name: file.name,
            extension: file.extension,
            type: file.type,
            size: file.size,
            metaFields: {},
            error: null
        };

        if (this.metaFields && file.meta) {
            for (var m = 0; m < this.metaFields.length; m++) {
                let fieldName = this.metaFields[m].id
                result.metaFields[fieldName] = file.meta[fieldName] || null;
            }
        }

        if (!file.progress) {
            result.progress = {
                bytesTotal: file.size,
                bytesUploaded: 0,
                percentage: 0,
                uploadComplete: false,
                uploadStarted: null
            }
        } else {
            result.progress = file.progress;
            if (result.progress.uploadStarted) {
                result.progress.uploadStarted = new Date(result.progress.uploadStarted);
            }
        }

        if (file.error) {
            result.error = file.error;
        }

        return result;
    }
}

class UploadFile {
    id: string;
    name: string;
    extension: string;
    type: string;
    size: number;
    metaFields: any;
    progress?: Progress;
    error: string;
}

class Progress {
    bytesTotal: number;
    bytesUploaded: number;
    percentage: number;
    uploadComplete: boolean;
    uploadStarted: Date;
}