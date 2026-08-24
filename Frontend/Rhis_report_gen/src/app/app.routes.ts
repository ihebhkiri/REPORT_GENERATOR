import { Routes } from '@angular/router';
import {ReportStepsComponent} from './shared/report-steps/report-steps.component';
import {adminGuard} from './features/auth/guards/admin.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login',
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'rapports',
    loadChildren: () => import('./features/rapports/rapports.routes').then((m) => m.routes),
  },
  {
    path: 'administration/datasets',
    canActivate: [adminGuard],
    loadComponent: () =>
      import(
        './features/administration/dataset-exposure/dataset-exposure.component'
      ).then((m) => m.DatasetExposureComponent),
  },
  {
    path: 'test',
component : ReportStepsComponent  },
];
