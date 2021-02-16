import { Directive , Input, OnInit, HostListener} from '@angular/core';
import { ApplicationService } from '../services/application.service';
import { SvyUtilsService } from '../servoy_public';

@Directive({
  selector: '[svyUpload]'
})

export class UploadDirective implements OnInit {
    @Input() formname: string;
    @Input() componentName: string;

    private url: string;
    private propertyName = 'dataProviderID';

    constructor(private appService: ApplicationService,
                private utilsService: SvyUtilsService) {
    }

    @HostListener('click') click(e: Event) {
        this.appService.showFileOpenDialog('Please select a file', false, null, this.url);
    }

    ngOnInit(): void {
        this.url = this.utilsService.generateUploadUrl(this.formname, this.componentName, this.propertyName);
    }

}
