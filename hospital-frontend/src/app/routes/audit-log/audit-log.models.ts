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

export const AUDIT_LOG_ACTIONS = [
  'CREATE',
  'UPDATE',
  'DELETE',
  'BULK_DELETE',
  'STATUS_UPDATE',
  'PASSWORD_RESET',
] as const;

export type AuditLogAction = (typeof AUDIT_LOG_ACTIONS)[number];

export const AUDIT_LOG_ENTITY_TYPES = ['PATIENT', 'ADMISSION', 'USER', 'CARE_VISIT', 'CARE_TASK', 'CARE_RESULT', 'CARE_ASSIGNMENT'] as const;

export type AuditLogEntityType = (typeof AUDIT_LOG_ENTITY_TYPES)[number];

export interface AuditLogFilters {
  search?: string;
  actor?: string;
  action?: AuditLogAction;
  entityType?: AuditLogEntityType;
  from?: string;
  to?: string;
}

export interface AuditLogQuery extends AuditLogFilters {
  page: number;
  size: number;
}

/**
 * The original fields stay required because previously recorded events only
 * contain this compact shape. V2 snapshot fields are optional so those events
 * remain readable instead of being guessed or discarded.
 */
export interface AuditLogItem {
  id: number;
  occurredAt: string;
  actor: string;
  action: string;
  entityType: string;
  entityId: string | null;
  targetLabel: string | null;
  details: string | null;
  eventVersion?: number;
  legacy?: boolean;
  actorDisplayName?: string | null;
  actorRole?: string | null;
  subjectType?: string | null;
  subjectId?: string | null;
  subjectLabel?: string | null;
  summary?: string | null;
  changeCount?: number;
  affectedTargetCount?: number;
  sampleData?: boolean;
}

export type AuditLogValueType = 'TEXT' | 'DATE' | 'NUMBER' | 'BOOLEAN' | 'REFERENCE';

export type AuditLogSensitivity = 'GENERAL' | 'PERSONAL' | 'MEDICAL' | 'SECRET';

export interface AuditLogChange {
  fieldKey: string;
  valueType: AuditLogValueType;
  oldValue: string | null;
  newValue: string | null;
  oldDisplayValue: string | null;
  newDisplayValue: string | null;
  sensitivity: AuditLogSensitivity;
  displayOrder: number;
}

export interface AuditLogAffectedTarget {
  entityType: string;
  entityId: string | null;
  targetLabel: string | null;
  displayOrder: number;
}

export interface AuditLogDetail {
  event: AuditLogItem;
  changes: AuditLogChange[];
  affectedTargets: AuditLogAffectedTarget[];
}

const EVENT_LABELS: Record<string, Record<string, string>> = {
  PATIENT: {
    CREATE: 'Patient created',
    UPDATE: 'Patient updated',
    DELETE: 'Patient deleted',
    BULK_DELETE: 'Patients bulk deleted',
  },
  ADMISSION: {
    CREATE: 'Admission created',
    UPDATE: 'Admission updated',
    DELETE: 'Admission deleted',
  },
  USER: {
    CREATE: 'Staff account created',
    UPDATE: 'Staff account updated',
    STATUS_UPDATE: 'Staff account status changed',
    PASSWORD_RESET: 'Password reset',
  },
};

export function readableAuditCode(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map(word => word[0]?.toUpperCase() + word.slice(1))
    .join(' ');
}

export function auditEventLabel(row: AuditLogItem): string {
  const label =
    EVENT_LABELS[row.entityType]?.[row.action] ??
    `${readableAuditCode(row.entityType)} ${readableAuditCode(row.action).toLowerCase()}`;
  return row.sampleData ? `Sample · ${label}` : label;
}

export function auditTargetPrimary(row: AuditLogItem): string {
  const label = row.targetLabel?.trim();
  const entityId = row.entityId?.trim();
  if (row.entityType === 'ADMISSION' && !row.subjectLabel?.trim()) {
    return entityId || label || 'Not recorded';
  }
  return label || entityId || 'Not recorded';
}

export function auditTargetSecondary(row: AuditLogItem): string {
  const subject = row.subjectLabel?.trim() || row.subjectId?.trim();
  if (subject && subject !== row.targetLabel?.trim()) {
    return subject;
  }
  if (!row.legacy && (row.eventVersion ?? 0) >= 2) {
    return '';
  }

  const label = row.targetLabel?.trim();
  const entityId = row.entityId?.trim();
  if (row.entityType === 'ADMISSION' && label && label !== entityId) {
    return label;
  }
  return label && entityId && label !== entityId ? entityId : '';
}

export function auditTarget(row: AuditLogItem): string {
  const primary = auditTargetPrimary(row);
  const secondary = auditTargetSecondary(row);
  return secondary ? `${primary} (${secondary})` : primary;
}

export function auditActorPrimary(row: AuditLogItem): string {
  return row.actorDisplayName?.trim() || row.actor;
}

export function auditActorSecondary(row: AuditLogItem): string {
  const role = row.actorRole?.trim();
  if (!row.actorDisplayName?.trim()) {
    return role || '';
  }
  return role ? `${row.actor} · ${readableAuditCode(role)}` : row.actor;
}

export function auditActor(row: AuditLogItem): string {
  const primary = auditActorPrimary(row);
  const secondary = auditActorSecondary(row);
  return secondary ? `${primary} (${secondary})` : primary;
}

export function auditChangeSummary(row: AuditLogItem): string {
  const structuredEvent = !row.legacy && (row.eventVersion ?? 0) >= 2;
  if (structuredEvent && row.action === 'UPDATE') {
    const count = row.changeCount ?? 0;
    return count === 0
      ? 'No field changes'
      : `${count} ${count === 1 ? 'field' : 'fields'} changed`;
  }
  if (structuredEvent && row.action === 'BULK_DELETE') {
    const count = row.affectedTargetCount ?? 0;
    if (count > 0) {
      return `${count} ${count === 1 ? 'record' : 'records'} deleted`;
    }
  }

  const summary = row.summary?.trim();
  if (summary) {
    return summary;
  }
  if (row.changeCount && row.changeCount > 0) {
    return `${row.changeCount} ${row.changeCount === 1 ? 'field' : 'fields'} changed`;
  }
  if (row.details?.trim()) {
    return row.details;
  }

  switch (row.action) {
    case 'CREATE':
      return 'Record created';
    case 'UPDATE':
      return 'Record updated';
    case 'DELETE':
      return 'Permanently deleted';
    case 'BULK_DELETE':
      return 'Records permanently deleted';
    case 'STATUS_UPDATE':
      return 'Status changed';
    case 'PASSWORD_RESET':
      return 'Credentials reset';
    default:
      return 'Not recorded';
  }
}

export function auditValue(value: string | null | undefined): string {
  return value == null || value === '' ? 'Not set' : value;
}

const FIELD_LABELS: Record<string, string> = {
  active: 'Account status',
  address: 'Address',
  admissionDate: 'Admission date',
  allergies: 'Allergies',
  birthDate: 'Date of birth',
  bloodType: 'Blood type',
  department: 'Department',
  departmentId: 'Department',
  dischargeDate: 'Discharge date',
  doctor: 'Doctor',
  doctorId: 'Doctor',
  email: 'Email',
  emergencyContactName: 'Emergency contact name',
  emergencyContactPhone: 'Emergency contact phone',
  emergencyContactRelation: 'Emergency contact relationship',
  fullName: 'Full name',
  displayName: 'Display name',
  gender: 'Gender',
  identityNo: 'ID card number',
  insuranceExpiry: 'Insurance expiry',
  insuranceNo: 'Insurance number',
  medicalHistory: 'Medical history',
  name: 'Full name',
  note: 'Note',
  notes: 'Notes',
  phoneNo: 'Phone number',
  reason: 'Admission reason',
  role: 'Role',
  room: 'Room',
  roomId: 'Room',
  status: 'Status',
  type: 'Admission type',
  username: 'Username',
};

export function auditFieldLabel(fieldKey: string): string {
  const knownLabel = FIELD_LABELS[fieldKey];
  if (knownLabel) {
    return knownLabel;
  }
  return fieldKey
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .replace(/^./, first => first.toUpperCase());
}

export function auditChangeValue(change: AuditLogChange, side: 'old' | 'new'): string {
  const display = side === 'old' ? change.oldDisplayValue : change.newDisplayValue;
  const raw = side === 'old' ? change.oldValue : change.newValue;
  if (display == null && raw == null && change.sensitivity !== 'GENERAL') {
    return 'Protected value';
  }
  if (display === '[REDACTED]' || raw === '[REDACTED]') {
    return 'Protected value';
  }
  return auditValue(display ?? raw);
}
