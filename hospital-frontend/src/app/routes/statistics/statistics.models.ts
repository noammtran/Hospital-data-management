/** Compact patient context returned for statistics drill-down. */
export interface PatientRef {
  id: string;
  name: string;
  identityNo: string | null;
  age: number | null;
  gender: number | null;
  status: number;
  admissionCount: number;
}

export interface StatisticsBreakdown {
  key: string;
  label: string;
  count: number;
}

export interface StatisticsSummary {
  admissions: number;
  diagnoses: number;
  prescriptions: number;
  patientStatuses: StatisticsBreakdown[];
  genders: StatisticsBreakdown[];
  ageGroups: StatisticsBreakdown[];
}

/** Aggregates and drill-down rows for one filtered patient cohort. */
export interface StatisticsResult {
  total: number;
  patients: PatientRef[];
  summary: StatisticsSummary;
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
