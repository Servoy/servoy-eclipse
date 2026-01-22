import { Directive , Input, OnInit, HostListener} from '@angular/core';
import { ServoyPublicService } from '../services/servoy_public.service';

@Directive({
    selector: '[svyUpload]',
    standalone: false
})
export class UploadDirective implements OnInit {
    @Input() formname: string;
    @Input() componentName: string;

    private url: string;
    private propertyName = 'dataProviderID';

    constructor(private servoyService: ServoyPublicService) {
    }

    @HostListener('click',['$event']) click(e: Event) {
        this.servoyService.showFileOpenDialog('Please select a file', false, null, this.url);
    }

    ngOnInit(): void {
        this.url = this.servoyService.generateUploadUrl(this.formname, this.componentName, this.propertyName);
    }

}
