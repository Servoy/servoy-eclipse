import { Pipe, PipeTransform } from '@angular/core';
import {PropertyUtils} from '../utils/property_utils';

@Pipe( { name: 'mnemonicletterFilter' } )
export class MnemonicletterFilterPipe implements PipeTransform {
    transform( input: string, letter: string): string{
        if(letter && input) return input.replace(letter, '<u>'+letter+'</u>');
        return input;
    }
}
@Pipe({name: 'notNullOrEmpty'})
export class NotNullOrEmptyPipe implements PipeTransform {
  transform(value: any[], args?: any): any {
      if(value)
    return value.filter(a => PropertyUtils.getPropByStringPath(a, 'realValue')!=='' && PropertyUtils.getPropByStringPath(a, 'realValue')!==null );
  }
}

@Pipe( { name: 'htmlFilter' } )
export class HtmlFilterPipe implements PipeTransform {
    transform( input: string): string{
      if (input && input.indexOf && input.indexOf('<body') >=0 && input.lastIndexOf('</body') >=0) {
        input = input.substring(input.indexOf('<body')+6,input.lastIndexOf('</body'));
      }
      return input;
    }
}
