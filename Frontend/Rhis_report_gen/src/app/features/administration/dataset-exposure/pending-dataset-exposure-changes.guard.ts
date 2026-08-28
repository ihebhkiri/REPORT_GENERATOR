import {CanDeactivateFn} from '@angular/router';

import type {DatasetExposureComponent} from './dataset-exposure.component';

const PENDING_CHANGES_MESSAGE =
  'Des modifications ne sont pas enregistrées. Voulez-vous vraiment quitter cette page ?';

export const pendingDatasetExposureChangesGuard: CanDeactivateFn<DatasetExposureComponent> = (
  component,
) => !component.hasUnsavedChanges() || window.confirm(PENDING_CHANGES_MESSAGE);
