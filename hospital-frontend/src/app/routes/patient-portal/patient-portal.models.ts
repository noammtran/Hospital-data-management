export interface PatientSelfProfile {
  id: string;
  name: string;
  birthDate: string | null;
  gender: number | null;
  address: string | null;
  phoneNo: string | null;
  email: string | null;
  identityNo: string;
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

export interface PatientAdmissionSummary {
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
}

export interface PatientSelfRecord {
  patient: PatientSelfProfile;
  records: import('../patient/patient.models').Visit[];
}

export interface ApiResponse<T> {
  data: T;
}
