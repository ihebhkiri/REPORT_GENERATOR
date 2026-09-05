import {ChangeDetectionStrategy, Component, computed, inject, signal} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {ActivatedRoute, RouterLink, RouterOutlet} from '@angular/router';
import {catchError, of} from 'rxjs';

import {AuthService} from '../../features/auth/services/auth.service';

type LayoutPage = 'reports' | 'datasets' | 'assistant' | 'configuration' | 'export';

const PAGE_COPY: Record<LayoutPage, {breadcrumb: string; title: string; description: string}> = {
  reports: {
    breadcrumb: 'Rapports · Source de données',
    title: 'Créer un rapport dynamique',
    description: 'Sélectionnez une source de données, configurez les colonnes et exportez votre rapport.',
  },
  datasets: {
    breadcrumb: 'Administration · Données',
    title: 'Exposition des données',
    description: 'Choisissez les tables et les champs proposés lors de la création des rapports.',
  },
  assistant: {
    breadcrumb: 'Rapports · Assistant',
    title: 'Assistant de rapports',
    description: 'Décrivez le rapport souhaité en français, puis vérifiez ce que l’assistant a compris.',
  },
  configuration: {
    breadcrumb: 'Rapports · Configuration',
    title: 'Configurer le rapport',
    description: 'Configurez les champs, les filtres et le tri de votre rapport avant de générer l’aperçu final.',
  },
  export: {
    breadcrumb: 'Rapports · Export',
    title: 'Préparer et exporter le rapport',
    description: 'Suivez les étapes jusqu’au téléchargement de votre rapport.',
  },
};

@Component({
  selector: 'app-shared-page-layout',
  imports: [RouterLink, RouterOutlet],
  templateUrl: './shared-page-layout.component.html',
  styleUrl: './shared-page-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SharedPageLayoutComponent {
  readonly isCollapsed = signal(true);
  readonly page = (inject(ActivatedRoute).snapshot.data['page'] ?? 'reports') as LayoutPage;
  readonly pageCopy = PAGE_COPY[this.page];
  readonly isDatasets = this.page === 'datasets';
  readonly isReports = this.page === 'reports' || this.page === 'configuration' || this.page === 'export';
  readonly isAssistant = this.page === 'assistant';
  readonly user = toSignal(
    inject(AuthService).me().pipe(catchError(() => of(null))),
    {initialValue: null},
  );
  readonly isAdmin = computed(() => this.user()?.roles.includes('ROLE_ADMIN') ?? false);
}
