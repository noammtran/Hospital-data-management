/** Minimal patient identity returned by the statistics endpoints. The id is kept
 *  for drill-down; the identity card number is what is shown to the user. */
export interface PatientRef {
  id: string;
  name: string;
  identityNo: string | null;
}

/** Result of a statistics query: how many patients matched and who they are. */
export interface StatisticsResult {
  total: number;
  patients: PatientRef[];
}

/** A selectable diagnosis filter (ICD code + readable label). */
export interface DiagnosisOption {
  icdCode: string;
  label: string;
}

/** A selectable medicine filter (medicine id + name). */
export interface MedicineOption {
  medicineId: number;
  label: string;
}
