import { Pipe, PipeTransform } from '@angular/core';
import {PropertyUtils} from '../utils/property_utils'

@Pipe( { name: 'mnemonicletterFilter' } )
export class MnemonicletterFilterPipe implements PipeTransform {
    transform( input: string, letter: string): string{
        if(letter && input) return input.replace(letter, '<u>'+letter+'</u>');
        return input
    } 
}
@Pipe({name: 'notNullOrEmpty'})
export class NotNullOrEmptyPipe implements PipeTransform {
  transform(value: any[], args?: any): any {
      if(value)
    return value.filter(a => {
      return PropertyUtils.getPropByStringPath(a, 'realValue')!==null;
    });
  }
}