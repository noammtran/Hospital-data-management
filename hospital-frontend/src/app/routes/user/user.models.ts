export type UserRole = 'RECEPTIONIST' | 'NURSE' | 'DOCTOR' | 'TECHNICIAN' | 'ADMIN';

export interface UserRoleOption {
  value: UserRole;
  label: string;
}

export const USER_ROLE_OPTIONS: readonly UserRoleOption[] = [
  { value: 'RECEPTIONIST', label: 'Receptionist' },
  { value: 'NURSE', label: 'Nurse' },
  { value: 'DOCTOR', label: 'Doctor' },
  { value: 'TECHNICIAN', label: 'Technician' },
  { value: 'ADMIN', label: 'Administrator' },
];

export interface ApiResponse<T> {
  success: boolean;
  statusCode: number;
  message: string;
  data?: T;
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

export interface UserAccount {
  id: number;
  username: string;
  identityNo: string | null;
  email: string;
  displayName: string;
  role: UserRole;
  active: boolean;
  mustChangePassword: boolean;
  createdBy: string;
  createdDate: string;
  lastUpdatedBy: string;
  lastUpdatedDate: string;
}

export interface UserQuery {
  page: number;
  size: number;
  search?: string;
  role?: UserRole | null;
  active?: boolean | null;
}

export interface CreateUserPayload {
  identityNo: string;
  email: string;
  displayName: string;
  role: UserRole;
}

export interface UpdateUserPayload {
  identityNo: string;
  email: string;
  displayName: string;
  role: UserRole;
}

export function roleLabel(role: UserRole): string {
  return USER_ROLE_OPTIONS.find(option => option.value === role)?.label ?? role;
}

export interface StaffDoctor {
  id: number;
  code: string;
  name: string;
  specialization: string | null;
  departmentId: number;
  departmentName: string | null;
  active: boolean;
}

export interface CreateStaffDoctorPayload {
  code: string;
  name: string;
  departmentId: number;
  specialization: string | null;
  phoneNo: string | null;
  email: string | null;
}

export interface StaffWorkProfile {
  user: UserAccount;
  rolePermissions: string[];
  doctor: StaffDoctor | null;
  assignments: {
    id: number;
    pointId: number;
    pointName: string;
    pointKind: string | null;
    departmentName: string | null;
    pointActive: boolean;
    active: boolean;
    perform: boolean;
    verify: boolean;
  }[];
  setupIssues: string[];
}

export function workSetupIssue(issue: string): string {
  const labels: Record<string, string> = {
    DOCTOR_LINK_MISSING:
      'No doctor profile linked. Link an existing profile or create one in work setup.',
    DOCTOR_UNAVAILABLE: 'The linked doctor profile is unavailable for new clinical work.',
    POINT_ASSIGNMENT_MISSING: 'No active assignment at an available care point.',
    CLINICAL_CAPABILITY_MISSING:
      'No clinical execution or service validation assignment configured.',
  };
  return labels[issue] ?? issue;
}

export function staffPermissionLabel(permission: string): string {
  const labels: Record<string, string> = {
    CARE_VIEW: 'Open care workspace',
    CARE_REGISTER: 'Register care visits',
    CARE_COORDINATE: 'Coordinate care',
    CARE_EXAMINE: 'Conduct examinations',
    CARE_PERFORM: 'Perform clinical work',
    CARE_VERIFY: 'Validate service results',
    CARE_ADMIT: 'Receive inpatients',
    CARE_MANAGE: 'Configure care points and staff',
    VIEW_PATIENT: 'View patients',
    CREATE_PATIENT: 'Create patients',
    UPDATE_PATIENT: 'Update patients',
    DELETE_PATIENT: 'Permanently delete patients',
    VIEW_PATIENT_HISTORY: 'View patient field history',
    VIEW_AUDIT_LOG: 'View activity history',
    VIEW_MEDICAL_RECORD: 'View medical records',
    VIEW_STATISTICS: 'View statistics',
    MANAGE_USERS: 'Manage staff accounts',
    PATIENT_CHECK_IN: 'Check in patients without an open care visit',
    PATIENT_START_EXAMINATION: 'Start examinations without an open care visit',
    PATIENT_COMPLETE_EXAMINATION: 'Complete examinations without an open care visit',
    ADMISSION_CREATE: 'Create independent admissions',
    ADMISSION_UPDATE: 'Update admission details',
    ADMISSION_DISCHARGE: 'Complete independent admissions',
    ADMISSION_DELETE: 'Permanently delete admissions',
    VIEW_OWN_RECORD: 'View own record',
    CHANGE_OWN_PASSWORD: 'Change own password',
  };
  return labels[permission] ?? permission.replaceAll('_', ' ').toLowerCase();
}
