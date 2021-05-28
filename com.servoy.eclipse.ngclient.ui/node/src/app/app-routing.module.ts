import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';


const routes: Routes = [
  {
    path: 'designer/solution/:solutionname',
    loadChildren: () => import('../designer/servoydesigner.module').then(m => m.ServoyDesignerModule)
  },
  {
    path: '**',
    loadChildren: () => import('../ngclient/servoy.module').then(m => m.ServoyModule)
  }
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes)
  ],
  exports: [RouterModule],
  providers: []
})
export class AppRoutingModule { }