import { Component, Input } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders, HttpErrorResponse, HttpEventType, HttpResponse } from '@angular/common/http';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-file-upload-window',
  templateUrl: './file-upload-window.component.html',
  styleUrls: ['./file-upload-window.component.css']
})
export class FileUploadWindowComponent {

  @Input() url: string;
  @Input() title: string;
  @Input() multiselect: boolean;
  @Input() filter: string;


  i18n_upload: string = "Upload";
	i18n_chooseFiles: string = "Select a file";
	i18n_cancel: string = "Cancel";
	i18n_selectedFiles: string =	"Selected files"
	i18n_nothingSelected: string = "Nothing selected, yet"
	i18n_remove: string = "Remove" 
	i18n_name: string = "Name" 
	genericError: string = "File upload error";

  uploadFiles: File[] = [];
  progress: number = 0;
  errorText: string = "";
  isUploading: boolean = false;

  constructor(private activeModal: NgbActiveModal, private http: HttpClient) { }

  isMultiselect(): boolean {
    return this.multiselect == true;
  }

  isFileSelected(): boolean {
    return this.uploadFiles.length > 0;
  }

  getUploadFiles(): File[] {
    return this.uploadFiles;
  }

  getFileIndex(f: File): number {
    for(let i = 0; i < this.uploadFiles.length; i++) {
      if(f.name == this.uploadFiles[i].name) {
        return i;
      }
    }
    return -1;
  }

  doRemove(f: File): void {
    let fileIdx = this.getFileIndex(f);
    if(fileIdx > -1) this.uploadFiles.splice(fileIdx, 1);
  }

  fileChange($event): void {
    if(!this.isMultiselect()) this.uploadFiles.length = 0;
    let fileList: FileList = $event.target.files;
    for(let i = 0; i < fileList.length; i++) {
      if(this.getFileIndex(fileList[i]) == -1) this.uploadFiles.push(fileList[i]);
    }
    $event.target.value = '';
  }

  getAcceptFilter(): string {
    return this.filter;
  }

  doUpload(): void {
    this.isUploading = true;
    this.progress = 0;
    this.errorText = "";
    let formData = new FormData();
    for(let i = 0; i < this.uploadFiles.length; i++) {
      formData.append("uploads[]", this.uploadFiles[i], this.uploadFiles[i].name);
    }

    this.http.post(this.url, formData, { reportProgress: true, observe: 'events' })
    .subscribe(
        data => {
          let r: any = data as any;
          if (r.type == HttpEventType.UploadProgress) {
            let current = 100.0 * r.loaded / r.total;
            if (current < this.progress) {
              // unsubscribe ?
              //$scope.upload.abort();
            }
            else this.progress  = current;
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
    )
//    .add(() => this.uploadBtn.nativeElement.disabled = false);//teardown
  }

  getProgress = function(postFix): string {
    if (this.progress) return Math.round(this.progress) + postFix;
    return "";
  }

  dismiss(): void {
    if(!this.isUploading) this.activeModal.close();
  }
}
