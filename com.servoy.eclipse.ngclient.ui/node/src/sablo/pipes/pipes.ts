import { Pipe, PipeTransform } from '@angular/core';

import { DomSanitizer } from '@angular/platform-browser'


@Pipe( { name: 'trustAsHtml' } )
export class TrustAsHtmlPipe implements PipeTransform {
    constructor( private domSanitizer: DomSanitizer ) {
    }

    transform( input: string, trustAsHtml: boolean ): any {
        if ( trustAsHtml ) {
            return this.domSanitizer.bypassSecurityTrustHtml(input);
        }
        return input;
    }
}