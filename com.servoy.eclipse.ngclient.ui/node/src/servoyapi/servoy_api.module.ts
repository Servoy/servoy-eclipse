import { NgModule } from '@angular/core';


import {FormatFilterPipe,MnemonicletterFilterPipe} from './pipes/pipes'

/**
 * This module should export all the public api like Pipes and Services that components or services want to use.
 */
@NgModule({
    declarations: [
                   FormatFilterPipe,
                   MnemonicletterFilterPipe
                 ],
  exports: [
    FormatFilterPipe,
    MnemonicletterFilterPipe
  ]
})
export class ServoyApiModule { }
