import { Routes } from '@angular/router';

export const APP_ROUTES: Routes = [
  {
    path: 'designer/solution/:solutionname/form/:formname/clientnr/:clientnr',
    loadChildren: () => import('../designer/servoydesigner.module').then(m => m.ServoyDesignerModule)
  },
  {
    path: '**',
    loadChildren: () => import('../ngclient/servoy.module').then(m => m.ServoyModule)
  }
];
