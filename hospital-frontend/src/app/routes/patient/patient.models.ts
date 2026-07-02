/** Generic response envelope returned by the backend (`ApiResponse<T>`). */
export interface ApiResponse<T> {
  success: boolean;
  statusCode: number;
  message: string;
  data: T;
  errors?: Record<string, string>;
  timestamp: string;
}

export interface PaginationMeta {
  page: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrev: boolean;
}

export interface PageResponse<T> {
  items: T[];
  pagination: PaginationMeta;
}

/** Row shape of `GET /api/v1/patients` (PatientListResponse). */
export interface PatientListItem {
  id: string;
  name: string;
  gender: number | null;
  birthDate: string | null;
  phoneNo: string | null;
  status: number;
  hasHistory: boolean;
}

/** Full shape of `GET /api/v1/patients/{id}` (PatientDetailResponse). */
export interface PatientDetail {
  id: string;
  name: string;
  birthDate: string | null;
  gender: number | null;
  address: string | null;
  phoneNo: string | null;
  email: string | null;
  identityNo: string | null;
  bloodType: number | null;
  allergies: string | null;
  medicalHistory: string | null;
  insuranceNo: string | null;
  insuranceExpiry: string | null;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  emergencyContactRelation: string | null;
  status: number;
  note: string | null;
  createdBy: string;
  createdDate: string;
  lastUpdatedBy: string;
  lastUpdatedDate: string;
}

/** Body of POST / PUT (CreatePatientRequest / UpdatePatientRequest). */
export interface PatientPayload {
  name: string;
  birthDate: string | null;
  gender: number | null;
  address: string | null;
  phoneNo: string | null;
  email: string | null;
  identityNo: string | null;
  bloodType: number | null;
  allergies: string | null;
  medicalHistory: string | null;
  insuranceNo: string | null;
  insuranceExpiry: string | null;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  emergencyContactRelation: string | null;
  status: number;
  note: string | null;
}

/** Row shape of `GET /api/v1/patients/{id}/history` (PatientHistoryResponse). */
export interface PatientHistoryItem {
  fieldName: string;
  oldValue: string | null;
  newValue: string | null;
  changedBy: string;
  changedDate: string;
}

/** Row shape of `GET /api/v1/patients/history` (PatientHistoryEntryResponse). */
export interface PatientHistoryEntry extends PatientHistoryItem {
  patientId: string;
  patientName: string;
}

export interface LookupOption {
  value: number;
  label: string;
}

// The backend has no master-data endpoint yet, so the LOOKUP_VALUES groups
// used by the patient screens are mirrored here as constants.
export const GENDER_OPTIONS: LookupOption[] = [
  { value: 1, label: 'Male' },
  { value: 2, label: 'Female' },
  { value: 3, label: 'Other' },
];

export const PATIENT_STATUS_OPTIONS: LookupOption[] = [
  { value: 0, label: 'Waiting' },
  { value: 1, label: 'Examining' },
  { value: 2, label: 'Admitted' },
  { value: 3, label: 'Discharged' },
];

export const BLOOD_TYPE_OPTIONS: LookupOption[] = [
  { value: 1, label: 'A+' },
  { value: 2, label: 'A-' },
  { value: 3, label: 'B+' },
  { value: 4, label: 'B-' },
  { value: 5, label: 'AB+' },
  { value: 6, label: 'AB-' },
  { value: 7, label: 'O+' },
  { value: 8, label: 'O-' },
];

export const ADMISSION_TYPE_OPTIONS: LookupOption[] = [
  { value: 1, label: 'Emergency' },
  { value: 2, label: 'Urgent' },
  { value: 3, label: 'Voluntary' },
];

export const ADMISSION_STATUS_OPTIONS: LookupOption[] = [
  { value: 1, label: 'In treatment' },
  { value: 2, label: 'Discharged' },
  { value: 3, label: 'Transferred' },
];

export const SEVERITY_OPTIONS: LookupOption[] = [
  { value: 1, label: 'Mild' },
  { value: 2, label: 'Moderate' },
  { value: 3, label: 'Severe' },
  { value: 4, label: 'Critical' },
];

/** One diagnosis inside a visit (GET /patients/{id}/medical-record). */
export interface VisitDiagnosis {
  date: string;
  icdCode: string | null;
  description: string;
  severity: number | null;
  doctorName: string;
}

/** One prescription inside a visit. */
export interface VisitPrescription {
  medicineName: string;
  dosage: string;
  frequency: string;
  duration: string;
  instructions: string | null;
}

/** One treatment episode (visit) with its diagnoses and prescriptions. */
export interface Visit {
  admissionId: string;
  admissionDate: string;
  dischargeDate: string | null;
  departmentName: string;
  doctorName: string;
  roomName: string | null;
  reason: string | null;
  type: number | null;
  status: number | null;
  notes: string | null;
  diagnoses: VisitDiagnosis[];
  prescriptions: VisitPrescription[];
}

export function lookupLabel(options: LookupOption[], value: number | null | undefined): string {
  return options.find(o => o.value === value)?.label ?? '';
}

/** Display names for the audited PATIENTS fields. */
export const FIELD_LABELS: Record<string, string> = {
  name: 'Full name',
  birthDate: 'Date of birth',
  gender: 'Gender',
  address: 'Address',
  phoneNo: 'Phone number',
  email: 'Email',
  identityNo: 'ID card number',
  bloodType: 'Blood type',
  allergies: 'Allergies',
  medicalHistory: 'Medical history',
  insuranceNo: 'Insurance number',
  insuranceExpiry: 'Insurance expiry',
  emergencyContactName: 'Emergency contact name',
  emergencyContactPhone: 'Emergency contact phone',
  emergencyContactRelation: 'Emergency contact relation',
  status: 'Status',
  note: 'Note',
};

// Some audited fields store a lookup code (gender, blood type, status), so the
// change history would otherwise show a bare number. Map those back to labels.
const HISTORY_VALUE_LOOKUPS: Record<string, LookupOption[]> = {
  gender: GENDER_OPTIONS,
  bloodType: BLOOD_TYPE_OPTIONS,
  status: PATIENT_STATUS_OPTIONS,
};

// Date fields are stored in the history as ISO strings (yyyy-MM-dd); show them
// as dd/MM/yyyy to match the rest of the app.
const HISTORY_DATE_FIELDS = new Set(['birthDate', 'insuranceExpiry']);

export function formatHistoryValue(fieldName: string, value: string | null): string {
  if (value === null || value === '') {
    return value ?? '';
  }
  const options = HISTORY_VALUE_LOOKUPS[fieldName];
  if (options) {
    return lookupLabel(options, Number(value)) || value;
  }
  if (HISTORY_DATE_FIELDS.has(fieldName)) {
    const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);
    if (m) {
      return `${m[3]}/${m[2]}/${m[1]}`;
    }
  }
  return value;
}
