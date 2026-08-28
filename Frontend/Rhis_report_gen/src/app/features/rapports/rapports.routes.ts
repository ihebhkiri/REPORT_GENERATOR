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
    loadComponent: () =>
      import('./pages/configuration/configuration.component').then(
        (m) => m.ConfigurationComponent,
      ),
  },
  {
    path: 'export/:generationId',
    loadComponent: () =>
      import('./pages/export/export.component').then((m) => m.ExportComponent),
  },
];
