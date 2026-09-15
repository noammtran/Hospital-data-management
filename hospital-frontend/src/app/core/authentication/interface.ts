export interface User {
  [prop: string]: any;

  id?: number | string | null;
  username?: string;
  name?: string;
  displayName?: string;
  email?: string | null;
  avatar?: string;
  role?: UserRole;
  roles?: UserRole[];
  permissions?: AppPermission[];
  mustChangePassword?: boolean;
}

export type UserRole = 'RECEPTIONIST' | 'DOCTOR' | 'NURSE' | 'TECHNICIAN' | 'ADMIN' | 'PATIENT';

export type AppPermission =
  | 'CARE_VIEW' | 'CARE_REGISTER' | 'CARE_COORDINATE' | 'CARE_EXAMINE' | 'CARE_PERFORM' | 'CARE_VERIFY' | 'CARE_ADMIT' | 'CARE_MANAGE'
  | 'VIEW_PATIENT'
  | 'CREATE_PATIENT'
  | 'UPDATE_PATIENT'
  | 'DELETE_PATIENT'
  | 'VIEW_PATIENT_HISTORY'
  | 'VIEW_AUDIT_LOG'
  | 'VIEW_MEDICAL_RECORD'
  | 'VIEW_STATISTICS'
  | 'MANAGE_USERS'
  | 'PATIENT_CHECK_IN'
  | 'PATIENT_START_EXAMINATION'
  | 'PATIENT_COMPLETE_EXAMINATION'
  | 'ADMISSION_CREATE'
  | 'ADMISSION_UPDATE'
  | 'ADMISSION_DISCHARGE'
  | 'ADMISSION_DELETE'
  | 'VIEW_OWN_RECORD'
  | 'CHANGE_OWN_PASSWORD';

export interface ApiResponse<T> {
  success: boolean;
  statusCode: number;
  message: string;
  data: T;
  errors?: Record<string, string>;
  timestamp: string;
}

export interface Token {
  [prop: string]: any;

  access_token: string;
  token_type?: string;
  expires_in?: number;
  exp?: number;
  refresh_token?: string;
}
