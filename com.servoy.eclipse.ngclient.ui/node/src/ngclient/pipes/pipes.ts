import { Pipe, PipeTransform } from '@angular/core';

@Pipe( { name: 'mnemonicletterFilter' } )
export class MnemonicletterFilterPipe implements PipeTransform {
    transform( input: string, letter: string): string{
        if(letter && input) return input.replace(letter, '<u>'+letter+'</u>');
        return input
    } 
}