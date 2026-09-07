import {inject} from '@angular/core';
import {CanDeactivateFn, Router} from '@angular/router';

import type {DatasetExposureComponent} from './dataset-exposure.component';

const PENDING_CHANGES_MESSAGE =
  'Des modifications ne sont pas enregistrées. Voulez-vous vraiment quitter cette page ?';

export function confirmPendingChanges(component: unknown): boolean {
  return !(component && typeof component === 'object' &&
    'hasUnsavedChanges' in component && typeof component.hasUnsavedChanges === 'function' &&
    component.hasUnsavedChanges()) || window.confirm(PENDING_CHANGES_MESSAGE);
}

export const pendingDatasetExposureChangesGuard: CanDeactivateFn<DatasetExposureComponent> = (
  component,
) => inject(Router).getCurrentNavigation()?.extras.state?.['logoutConfirmed'] === true ||
  confirmPendingChanges(component);
