import {ActivatedRouteSnapshot, RouterStateSnapshot} from '@angular/router';

import {DatasetExposureComponent} from './dataset-exposure.component';
import {pendingDatasetExposureChangesGuard} from './pending-dataset-exposure-changes.guard';

describe('pendingDatasetExposureChangesGuard', () => {
  it('allows navigation without asking when the draft is clean', () => {
    const component = componentWithUnsavedChanges(false);
    const confirmSpy = spyOn(window, 'confirm');

    expect(runGuard(component)).toBeTrue();
    expect(confirmSpy).not.toHaveBeenCalled();
  });

  it('uses the user confirmation when the draft is dirty', () => {
    const component = componentWithUnsavedChanges(true);
    const confirmSpy = spyOn(window, 'confirm').and.returnValues(false, true);

    expect(runGuard(component)).toBeFalse();
    expect(runGuard(component)).toBeTrue();
    expect(confirmSpy).toHaveBeenCalledTimes(2);
  });

  function componentWithUnsavedChanges(value: boolean): DatasetExposureComponent {
    return {
      hasUnsavedChanges: jasmine.createSpy('hasUnsavedChanges').and.returnValue(value),
    } as unknown as DatasetExposureComponent;
  }

  function runGuard(component: DatasetExposureComponent): boolean {
    return pendingDatasetExposureChangesGuard(
      component,
      {} as ActivatedRouteSnapshot,
      {} as RouterStateSnapshot,
      {} as RouterStateSnapshot,
    ) as boolean;
  }
});
