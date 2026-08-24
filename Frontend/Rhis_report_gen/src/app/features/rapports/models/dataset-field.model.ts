export type DatasetFieldType =
  | 'TEXT'
  | 'INTEGER'
  | 'DECIMAL'
  | 'BOOLEAN'
  | 'DATE'
  | 'TIME'
  | 'DATE_TIME'
  | 'OFFSET_DATE_TIME'
  | 'UUID'
  | 'UNSUPPORTED';

export type FilterOperator =
  | 'EQUALS'
  | 'NOT_EQUALS'
  | 'IS_NULL'
  | 'IS_NOT_NULL'
  | 'CONTAINS'
  | 'GREATER_THAN'
  | 'GREATER_THAN_OR_EQUAL'
  | 'LESS_THAN'
  | 'LESS_THAN_OR_EQUAL'
  | 'BETWEEN';

export interface DatasetField {
  readonly id: number;
  readonly displayName: string;
  readonly sourceName: string;
  readonly dataType: DatasetFieldType;
  readonly nullable: boolean;
  readonly supported: boolean;
  readonly supportedOperators: readonly FilterOperator[];
}
