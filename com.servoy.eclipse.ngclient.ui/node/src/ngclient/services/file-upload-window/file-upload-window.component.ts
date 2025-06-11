import { Component, Input } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpEventType, HttpResponse } from '@angular/common/http';
import { I18NProvider } from '../i18n_provider.service';

@Component({
    selector: 'servoycore-file-upload-window',
    templateUrl: './file-upload-window.component.html',
    styleUrls: ['./file-upload-window.component.css'],
    standalone: false
})
export class FileUploadWindowComponent {

    @Input() url: string;
    @Input() title: string;
    @Input() multiselect: boolean;
    @Input() filter: string;


    i18n_upload = 'Upload';
    i18n_chooseFiles = 'Select a file';
    i18n_cancel = 'Cancel';
    i18n_selectedFiles = 'Selected files';
    i18n_nothingSelected = 'Nothing selected, yet';
    i18n_remove = 'Remove';
    i18n_name = 'Name';
    genericError = 'File upload error';

    uploadFiles: File[] = [];
    progress = 0;
    errorText = '';
    isUploading = false;
    onCloseCallback: () => void;

    constructor(private http: HttpClient, i18nProvider: I18NProvider) {
        i18nProvider.listenForI18NMessages(
            'servoy.filechooser.button.upload',
            'servoy.filechooser.upload.addFile',
            'servoy.filechooser.upload.addFiles',
            'servoy.filechooser.selected.files',
            'servoy.filechooser.nothing.selected',
            'servoy.filechooser.button.remove',
            'servoy.filechooser.label.name',
            'servoy.button.cancel',
            'servoy.filechooser.error').messages((val) => {
                this.i18n_upload = val.get('servoy.filechooser.button.upload');
                if (this.isMultiselect())
                    this.i18n_chooseFiles = val.get('servoy.filechooser.upload.addFiles');
                else
                    this.i18n_chooseFiles = val.get('servoy.filechooser.upload.addFile');
                this.i18n_cancel = val.get('servoy.button.cancel');
                this.i18n_selectedFiles = val.get('servoy.filechooser.selected.files');
                this.i18n_nothingSelected = val.get('servoy.filechooser.nothing.selected');
                this.i18n_remove = val.get('servoy.filechooser.button.remove');
                this.i18n_name = val.get('servoy.filechooser.label.name');
                this.genericError = val.get('servoy.filechooser.error');
                if (!this.title) this.title = this.i18n_chooseFiles;
            });
    }

    isMultiselect(): boolean {
        return this.multiselect === true;
    }

    isFileSelected(): boolean {
        return this.uploadFiles.length > 0;
    }

    getUploadFiles(): File[] {
        return this.uploadFiles;
    }

    getFileIndex(f: File): number {
        for (let i = 0; i < this.uploadFiles.length; i++) {
            if (f.name === this.uploadFiles[i].name) {
                return i;
            }
        }
        return -1;
    }

    doRemove(f: File): void {
        const fileIdx = this.getFileIndex(f);
        if (fileIdx > -1) this.uploadFiles.splice(fileIdx, 1);
    }

    // Add a property to track files that exceed the size limit
    oversizedFiles: Set<string> = new Set<string>();
    
    fileChange($event: Event): void {
        if (!this.isMultiselect()) {
            this.uploadFiles.length = 0;
            this.oversizedFiles.clear();
        }
        
        const target = $event.target as HTMLInputElement;
        const fileList: FileList = target.files;
        
        for (const key of Object.keys(fileList)) {
            const file = fileList[key];
            const fileSizeKB = file.size / (1024); // bytes to kilobytes
            
            // Check if file exceeds max size (if a max size is set)
            if (this.maxUploadFileSize > 0 && fileSizeKB > this.maxUploadFileSize) {
                this.oversizedFiles.add(file.name);
            }
            
            // Add the file to the list regardless of size (for display purposes)
            if (this.getFileIndex(file) === -1) {
                this.uploadFiles.push(file);
            }
        }
        
        target.value = ''; 
    }

    // Add a property to store the max upload file size
    maxUploadFileSize: number = 0;
    
    getAcceptFilter(): string {
        // If filter contains maxUploadFileSize information, extract it
        if (this.filter && this.filter.includes('maxUploadFileSize=')) {
            const filters = this.filter.split(',');
            let cleanedFilter: string[] = [];
            
            for (const filter of filters) {
                if (filter.includes('maxUploadFileSize=')) {
                    const parts = filter.split('maxUploadFileSize=');
                    const sizeValue = parts[1]; // Get the part after 'maxUploadFileSize='
                    if (sizeValue && !isNaN(Number(sizeValue))) {
                        this.maxUploadFileSize = Number(sizeValue);
                    }
                } else {
                    cleanedFilter.push(filter);
                }
            }
            
            // Update filter without the maxUploadFileSize entry
            this.filter = cleanedFilter.join(',');
        }
		
        return this.filter;
    }

    // Helper method to get display name (with asterisk for oversized files)
    getDisplayName(file: File): string {
        return this.oversizedFiles.has(file.name) ? file.name + ' ( > ' + this.maxUploadFileSize + ' KB )'  : file.name;
    }
    
    // Helper method to check if a file is valid for upload
    isFileValidForUpload(file: File): boolean {
        return !this.oversizedFiles.has(file.name);
    }
    
    doUpload(): void {
        // Check if there are any valid files to upload
        const validFiles = this.uploadFiles.filter(file => this.isFileValidForUpload(file));
        
        if (validFiles.length === 0) {
            console.log('No valid files to upload');
            // Optionally show a message to the user
            return;
        }
        
        this.isUploading = true;
        this.progress = 0;
        this.errorText = '';
        const formData = new FormData();
        
        // Only include valid files (not oversized) in the upload
        for (const file of validFiles) {
            formData.append('uploads[]', file, file.name);
        }

        const headers = this.maxUploadFileSize >= 0 ? { 'X-Max-Upload-Size': this.maxUploadFileSize.toString() } : {};

        this.http.post(this.url, formData, {
            headers,
            reportProgress: true,
            observe: 'events'
        })
            .subscribe(
                data => {
                    const r: any = data as any;
                    if (r.type === HttpEventType.UploadProgress) {
                        const current = 100.0 * r.loaded / r.total;
                        if (current < this.progress) {
                            // unsubscribe ?
                            //$scope.upload.abort();
                        } else this.progress = current;
                    } else if (data instanceof HttpResponse) {
                        setTimeout(() => {
                            this.isUploading = false;
                            this.dismiss();
                        }, 2000);
                    }
                },
                (err: HttpErrorResponse) => {
                    this.errorText = err.message ? err.message : this.genericError;
                }
            );
        //    .add(() => this.uploadBtn.nativeElement.disabled = false);//teardown
    }

    getProgress(postFix: string): string {
        if (this.progress) return Math.round(this.progress) + postFix;
        return '';
    }

    dismiss(): void {
        if (!this.isUploading && this.onCloseCallback) this.onCloseCallback();
    }

    public setOnCloseCallback(callback) {
        this.onCloseCallback = callback;
    }
}
