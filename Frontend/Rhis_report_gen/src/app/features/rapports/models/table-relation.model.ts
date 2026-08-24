export interface TableRelation {
  readonly sourceDatasetId: number;
  readonly sourceTable: string;
  readonly sourceDisplayName: string;
  readonly sourceColumn: string;
  readonly targetDatasetId: number;
  readonly targetTable: string;
  readonly targetDisplayName: string;
  readonly targetColumn: string;
}
