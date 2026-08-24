export type DatasetExposureMode =
  | 'NONE'
  | 'MAIN_ONLY'
  | 'RELATED_ONLY'
  | 'MAIN_AND_RELATED';

export interface FieldExposure {
  readonly id: number;
  readonly displayName: string;
  readonly active: boolean;
  readonly visible: boolean;
}

export interface DatasetExposure {
  readonly id: number;
  readonly displayName: string;
  readonly active: boolean;
  readonly displayMain: boolean;
  readonly displayRelated: boolean;
  readonly visibleFieldCount: number;
  readonly fields: readonly FieldExposure[];
}

export interface DatasetExposureConfiguration {
  readonly datasets: readonly DatasetExposure[];
}

export interface FieldExposureUpdate {
  readonly id: number;
  readonly visible: boolean;
}

export interface DatasetExposureUpdate {
  readonly id: number;
  readonly displayMain: boolean;
  readonly displayRelated: boolean;
  readonly fields: readonly FieldExposureUpdate[];
}

export interface UpdateDatasetExposureRequest {
  readonly datasets: readonly DatasetExposureUpdate[];
}

export function toExposureMode(
  displayMain: boolean,
  displayRelated: boolean,
): DatasetExposureMode {
  if (displayMain && displayRelated) {
    return 'MAIN_AND_RELATED';
  }
  if (displayMain) {
    return 'MAIN_ONLY';
  }
  if (displayRelated) {
    return 'RELATED_ONLY';
  }
  return 'NONE';
}

export function fromExposureMode(mode: DatasetExposureMode): {
  readonly displayMain: boolean;
  readonly displayRelated: boolean;
} {
  switch (mode) {
    case 'NONE':
      return {displayMain: false, displayRelated: false};
    case 'MAIN_ONLY':
      return {displayMain: true, displayRelated: false};
    case 'RELATED_ONLY':
      return {displayMain: false, displayRelated: true};
    case 'MAIN_AND_RELATED':
      return {displayMain: true, displayRelated: true};
  }
}
