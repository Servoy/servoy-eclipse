import { Component, Output, ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent, SvyUtilsService } from '../../ngclient/servoy_public';
import { LightboxModule } from 'ngx-lightbox';

@Component( {
    selector: 'servoyextra-lightboxgallery',
    templateUrl: './lightboxgallery.html',
    styleUrls: ['./lightboxgallery.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyExtraLightboxGallery extends ServoyBaseComponent<HTMLDivElement> {

}

