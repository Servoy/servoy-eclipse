import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { ServoyFormTemplateComponent } from './formtemplate.component';


const routes: Routes = [
  {
    path: '',
    component: ServoyFormTemplateComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FormTemplateRoutingModule { }
