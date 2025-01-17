import { Pipe, PipeTransform } from '@angular/core';

@Pipe( {
    name: 'emptyValue',
    standalone: false
} )
export class EmptyValueFilterPipe implements PipeTransform {

    transform( input: any): any {
        // eslint-disable-next-line eqeqeq
        if (input == '') return '\u00A0';
        return input;
    }
}
