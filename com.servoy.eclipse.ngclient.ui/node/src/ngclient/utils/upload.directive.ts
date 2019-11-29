import { Directive , Input, OnInit, ViewContainerRef, HostListener} from '@angular/core';
import { ApplicationService } from "../services/application.service";
import { FormService, ComponentCache } from "../form.service";
import { SvyUtilsService } from "../servoy_public";

@Directive({
  selector: '[svyUpload]'
})

export class UploadDirective implements OnInit {
    @Input('formname') formname : string;
    @Input('componentName') componentName;
    
    private url: string;
    private propertyName: string = "dataProviderID";
    
    constructor(private appService: ApplicationService,
                private utilsService: SvyUtilsService) {
    }
    
    ngOnInit(): void {
        this.url = this.utilsService.generateUploadUrl(this.formname, this.componentName, this.propertyName);
    }
    
    @HostListener('click') click(e: Event) {
        this.appService.showFileOpenDialog(this.url, "Please select a file", false, null);
    }
}