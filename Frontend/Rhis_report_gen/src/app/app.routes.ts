import { Routes } from '@angular/router';
import {ReportStepsComponent} from './shared/report-steps/report-steps.component';
import {adminGuard} from './features/auth/guards/admin.guard';
import {pendingDatasetExposureChangesGuard} from './features/administration/dataset-exposure/pending-dataset-exposure-changes.guard';

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
    data: {page: 'datasets'},
    loadComponent: () =>
      import('./shared/page-layout/shared-page-layout.component').then(
        (m) => m.SharedPageLayoutComponent,
      ),
    children: [
      {
        path: '',
        canDeactivate: [pendingDatasetExposureChangesGuard],
        loadComponent: () =>
          import('./features/administration/dataset-exposure/dataset-exposure.component').then(
            (m) => m.DatasetExposureComponent,
          ),
      },
    ],
  },
  {
    path: 'test',
component : ReportStepsComponent  },
];
