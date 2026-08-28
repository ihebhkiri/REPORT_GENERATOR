import {ChangeDetectionStrategy, Component, computed, inject} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {ActivatedRoute, RouterLink, RouterOutlet} from '@angular/router';
import {catchError, of} from 'rxjs';

import {AuthService} from '../../features/auth/services/auth.service';

@Component({
  selector: 'app-shared-page-layout',
  imports: [RouterLink, RouterOutlet],
  templateUrl: './shared-page-layout.component.html',
  styleUrl: './shared-page-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SharedPageLayoutComponent {
  readonly isDatasets = inject(ActivatedRoute).snapshot.data['page'] === 'datasets';
  readonly user = toSignal(
    inject(AuthService).me().pipe(catchError(() => of(null))),
    {initialValue: null},
  );
  readonly isAdmin = computed(() => this.user()?.roles.includes('ROLE_ADMIN') ?? false);
}
