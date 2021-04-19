import { Component, Input } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpEventType, HttpResponse } from '@angular/common/http';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { I18NProvider } from '../../../ngclient/servoy_public';

@Component({
  selector: 'servoycore-file-upload-window',
  templateUrl: './file-upload-window.component.html',
  styleUrls: ['./file-upload-window.component.css']
})
export class FileUploadWindowComponent {

  @Input() url: string;
  @Input() title: string;
  @Input() multiselect: boolean;
  @Input() filter: string;


  i18n_upload = 'Upload';
	i18n_chooseFiles = 'Select a file';
	i18n_cancel = 'Cancel';
	i18n_selectedFiles =	'Selected files';
	i18n_nothingSelected = 'Nothing selected, yet';
	i18n_remove = 'Remove';
	i18n_name = 'Name';
	genericError = 'File upload error';

  uploadFiles: File[] = [];
  progress = 0;
  errorText = '';
  isUploading = false;

  constructor(private activeModal: NgbActiveModal, private http: HttpClient, i18nProvider: I18NProvider) {
    i18nProvider.getI18NMessages(
      'servoy.filechooser.button.upload',
      'servoy.filechooser.upload.addFile',
      'servoy.filechooser.upload.addFiles',
      'servoy.filechooser.selected.files',
      'servoy.filechooser.nothing.selected',
      'servoy.filechooser.button.remove',
      'servoy.filechooser.label.name',
      'servoy.button.cancel',
      'servoy.filechooser.error').then((val)=> {
        this.i18n_upload = val['servoy.filechooser.button.upload'];
        if (this.isMultiselect())
          this.i18n_chooseFiles = val['servoy.filechooser.upload.addFiles'];
        else
          this.i18n_chooseFiles = val['servoy.filechooser.upload.addFile'];
        this.i18n_cancel = val['servoy.button.cancel'];
        this.i18n_selectedFiles = val['servoy.filechooser.selected.files'];
        this.i18n_nothingSelected = val['servoy.filechooser.nothing.selected'];
        this.i18n_remove = val['servoy.filechooser.button.remove'];
        this.i18n_name = val['servoy.filechooser.label.name'];
        this.genericError = val['servoy.filechooser.error'];
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
    for(let i = 0; i < this.uploadFiles.length; i++) {
      if(f.name === this.uploadFiles[i].name) {
        return i;
      }
    }
    return -1;
  }

  doRemove(f: File): void {
    const fileIdx = this.getFileIndex(f);
    if(fileIdx > -1) this.uploadFiles.splice(fileIdx, 1);
  }

  fileChange($event: Event): void {
    if(!this.isMultiselect()) this.uploadFiles.length = 0;
    const target = $event.target as HTMLInputElement;
    const fileList: FileList = target.files;
    for (const key of Object.keys(fileList)) {
      if(this.getFileIndex(fileList[key]) === -1) this.uploadFiles.push(fileList[key]);
    }
    target.value = '';
  }

  getAcceptFilter(): string {
    return this.filter;
  }

  doUpload(): void {
    this.isUploading = true;
    this.progress = 0;
    this.errorText = '';
    const formData = new FormData();
    for(const key of Object.keys(this.uploadFiles)) {
      formData.append('uploads[]', this.uploadFiles[key], this.uploadFiles[key].name);
    }

    this.http.post(this.url, formData, { reportProgress: true, observe: 'events' })
    .subscribe(
        data => {
          const r: any = data as any;
          if (r.type === HttpEventType.UploadProgress) {
            const current = 100.0 * r.loaded / r.total;
            if (current < this.progress) {
              // unsubscribe ?
              //$scope.upload.abort();
            } else this.progress  = current;
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
    if(!this.isUploading) this.activeModal.close();
  }
}
