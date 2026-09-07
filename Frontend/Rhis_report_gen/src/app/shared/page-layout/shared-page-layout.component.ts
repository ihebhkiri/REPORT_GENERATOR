import {ChangeDetectionStrategy, Component, computed, inject, signal, viewChild} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {ActivatedRoute, Router, RouterLink, RouterOutlet} from '@angular/router';
import {catchError, finalize, of} from 'rxjs';
import {MenuModule} from 'primeng/menu';
import {MenuItem} from 'primeng/api';

import {AuthService} from '../../features/auth/services/auth.service';
import {confirmPendingChanges} from '../../features/administration/dataset-exposure/pending-dataset-exposure-changes.guard';

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
  imports: [RouterLink, RouterOutlet, MenuModule],
  templateUrl: './shared-page-layout.component.html',
  styleUrl: './shared-page-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SharedPageLayoutComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly outlet = viewChild(RouterOutlet);
  readonly loggingOut = signal(false);
  readonly logoutError = signal<string | null>(null);
  readonly profileMenuItems: MenuItem[] = [
    {label: 'Déconnexion', icon: 'pi pi-sign-out', command: () => this.logout()},
  ];
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

  logout(): void {
    const outlet = this.outlet();
    if (this.loggingOut() || !confirmPendingChanges(outlet?.isActivated ? outlet.component : null)) {
      return;
    }
    this.loggingOut.set(true);
    this.logoutError.set(null);
    this.auth.logout().pipe(finalize(() => this.loggingOut.set(false))).subscribe({
      next: () => void this.router.navigateByUrl('/login', {
        replaceUrl: true, state: {logoutConfirmed: true},
      }),
      error: () => this.logoutError.set('La déconnexion a échoué. Veuillez réessayer.'),
    });
  }
}
