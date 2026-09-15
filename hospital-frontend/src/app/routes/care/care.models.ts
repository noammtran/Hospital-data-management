export interface CarePoint {
  id: number;
  code: string;
  name: string;
  kind: 'CLINIC' | 'SERVICE' | 'WARD';
  departmentId: number;
  active: boolean;
}
export interface CareServiceItem {
  id: number;
  code: string;
  name: string;
  pointId: number;
  active: boolean;
}
export interface CareStaff {
  id: number;
  name: string;
  role: string;
  doctorId: number | null;
  active: boolean;
}
export interface CareAssignment {
  id: number;
  userId: number;
  pointId: number;
  perform: boolean;
  verify: boolean;
  active: boolean;
}
export interface CareReference {
  id: number;
  name: string;
}
export interface CareOptions {
  currentUserId: number;
  points: CarePoint[];
  services: CareServiceItem[];
  staff: CareStaff[];
  assignments: CareAssignment[];
  departments: CareReference[];
  doctors: CareReference[];
  rooms: { id: number; name: string; departmentId: number; availableBeds: number }[];
}
export interface CareVisit {
  id: string;
  patientId: string;
  patientName: string;
  patientStatus: number;
  status: string;
  progress: string;
  pointId: number;
  pointName: string;
  locationPointId: number | null;
  locationName: string | null;
  ownerId: number | null;
  ownerName: string | null;
  priority: number;
  createdAt: string;
  closedAt: string | null;
  orderCount: number;
  publishedCount: number;
  unreadCount: number;
  critical: boolean;
  admissionId: string | null;
  wardPointId: number | null;
}
export interface CareResult {
  id: number;
  revision: number;
  body: string;
  critical: boolean;
  correctionReason: string | null;
  createdBy: string;
  createdAt: string;
  publishedBy: string | null;
  publishedAt: string | null;
  readBy: string | null;
  readAt: string | null;
}
export interface CareTask {
  id: number;
  visitId: string;
  patientId: string;
  patientName: string;
  patientStatus: number;
  pointId: number;
  pointName: string;
  kind: string;
  serviceId: number | null;
  serviceName: string | null;
  status: string;
  arrival: string;
  reason: string | null;
  createdBy: number;
  orderedBy: string;
  createdAt: string;
  arrivedAt: string | null;
  startedAt: string | null;
  performedBy: string | null;
  resolution: string | null;
  resolvedAt: string | null;
  results: CareResult[];
  actions: string[];
}
export interface CareNote {
  id: number;
  kind: string;
  body: string;
  createdBy: string;
  createdAt: string;
}
export interface CareHandoff {
  id: number;
  kind: string;
  status: string;
  fromUser: string;
  toUserId: number;
  toUser: string;
  pointId: number;
  pointName: string;
  reason: string;
  response: string | null;
  createdAt: string;
  respondedAt: string | null;
  canRespond: boolean;
}
export interface CareDiagnosis {
  id: string;
  description: string;
  icdCode: string | null;
  doctor: string;
  prescriptions: {
    id: string;
    medicineName: string;
    dosage: string;
    frequency: string;
    duration: string;
    instructions: string | null;
  }[];
}
export interface CareDetail {
  visit: CareVisit;
  complaint: string | null;
  conclusion: string | null;
  plan: string | null;
  outcome: string | null;
  followupPlan: string | null;
  followupAt: string | null;
  followupOwner: string | null;
  tasks: CareTask[];
  notes: CareNote[];
  handoffs: CareHandoff[];
  diagnoses: CareDiagnosis[];
  actions: string[];
}
export interface CareInpatient {
  admissionId: string;
  patientId: string;
  patientName: string;
  patientStatus: number;
  departmentName: string;
  doctorName: string;
  roomName: string | null;
  departmentId: number;
  visitId: string | null;
}
export interface CarePage<T> {
  items: T[];
  pagination: {
    page: number;
    pageSize: number;
    totalItems: number;
    totalPages: number;
    hasNext: boolean;
    hasPrev: boolean;
  };
}
export type CareQuery = Record<string, string | number | boolean | null | undefined>;
const LABELS: Record<string, string> = {
  WAITING: 'Waiting for examination',
  ACTIVE: 'Examination in progress',
  EXAMINING: 'With the doctor',
  AWAITING_ADMISSION: 'Awaiting inpatient reception',
  INPATIENT: 'Inpatient',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
  QUEUED: 'Waiting to start',
  IN_PROGRESS: 'In progress',
  WAITING_RESULTS: 'Waiting for services / results',
  AWAITING_VALIDATION: 'Result awaiting validation',
  RESULT_READY: 'Result ready',
  RESULTS_TO_REVIEW: 'Results to review',
  CRITICAL_RESULT: 'Critical result to review',
  FOLLOWUP_PENDING: 'Closed visit · follow-up pending',
  CANCEL_REQUESTED: 'Cancellation requested',
  DECLINED: 'Not performed / declined',
  EXPECTED: 'Expected · not arrived',
  ARRIVED: 'Arrived',
  CALLED: 'Called',
  ABSENT: 'Absent',
  LEFT: 'Left this point',
  CLINIC: 'Examination point',
  SERVICE: 'Service point',
  WARD: 'Inpatient ward',
  EXAM: 'Examination',
  ORDER: 'Service order',
  VITALS: 'Vitals / nursing observation',
  REVIEW: 'Clinical review',
  COORDINATION: 'Coordination note',
  CONCLUSION: 'Conclusion',
  TRANSFER: 'Doctor handoff',
  CONSULT: 'Consultation',
  PENDING: 'Awaiting response',
  ACCEPTED: 'Accepted',
  OUTPATIENT: 'Outpatient treatment',
  FOLLOW_UP: 'Follow-up appointment',
  LEFT_INCOMPLETE: 'Left before completion',
  REFERRED: 'Referred',
  DISCHARGED: 'Discharged',
};
export function careLabel(value: string): string {
  return LABELS[value] ?? value;
}
export function patientStatusLabel(value: number): string {
  return ['Waiting', 'Examining', 'Admitted', 'Discharged'][value] ?? 'Unknown';
}
export function careError(error: unknown): string {
  const response = error as { error?: { message?: string; errors?: Record<string, string> } };
  const fields = Object.values(response.error?.errors ?? {});
  return fields.length
    ? fields.join('. ')
    : (response.error?.message ?? 'Unable to load or save care data. Please try again.');
}
