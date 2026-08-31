import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    data: {page: 'reports'},
    loadComponent: () =>
      import('../../shared/page-layout/shared-page-layout.component').then(
        (m) => m.SharedPageLayoutComponent,
      ),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/source_de_donnes/rapports.component').then((m) => m.RapportsComponent),
      },
    ],
  },
  {
    path: 'configuration/:datasetId',
    data: {page: 'configuration'},
    loadComponent: () =>
      import('../../shared/page-layout/shared-page-layout.component').then(
        (m) => m.SharedPageLayoutComponent,
      ),
    children: [{
      path: '',
      loadComponent: () =>
        import('./pages/configuration/configuration.component').then(
          (m) => m.ConfigurationComponent,
        ),
    }],
  },
  {
    path: 'export/:generationId',
    data: {page: 'export'},
    loadComponent: () =>
      import('../../shared/page-layout/shared-page-layout.component').then(
        (m) => m.SharedPageLayoutComponent,
      ),
    children: [{
      path: '',
      loadComponent: () =>
        import('./pages/export/export.component').then((m) => m.ExportComponent),
    }],
  },
];
