import {
  AUDIT_LOG_ACTIONS,
  AUDIT_LOG_ENTITY_TYPES,
  AuditLogItem,
  auditActor,
  auditChangeValue,
  auditChangeSummary,
  auditEventLabel,
  auditFieldLabel,
  auditTarget,
  readableAuditCode,
} from './audit-log.models';

describe('audit-log presentation helpers', () => {
  const legacyItem: AuditLogItem = {
    id: 1,
    occurredAt: '2026-08-14T08:30:00Z',
    actor: 'admin',
    action: 'UPDATE',
    entityType: 'PATIENT',
    entityId: 'P26AA001',
    targetLabel: 'Nguyen Van Test',
    details: null,
  };

  it('turns an entity and action pair into a meaningful event label', () => {
    expect(auditEventLabel(legacyItem)).toBe('Patient updated');
    expect(auditEventLabel({ ...legacyItem, entityType: 'USER', action: 'PASSWORD_RESET' })).toBe(
      'Password reset'
    );
    expect(auditEventLabel({ ...legacyItem, sampleData: true })).toBe('Sample · Patient updated');
  });

  it('keeps a legacy target readable without inventing unavailable names', () => {
    expect(auditTarget(legacyItem)).toBe('Nguyen Van Test (P26AA001)');
    expect(auditTarget({ ...legacyItem, entityId: null })).toBe('Nguyen Van Test');
    expect(auditTarget({ ...legacyItem, entityId: null, targetLabel: null })).toBe('Not recorded');
  });

  it('does not repeat an identifier already included in a V2 snapshot label', () => {
    expect(
      auditTarget({
        ...legacyItem,
        eventVersion: 2,
        legacy: false,
        targetLabel: 'Nguyen Van Test · P26AA001',
      })
    ).toBe('Nguyen Van Test · P26AA001');
  });

  it('uses the frozen subject context for an admission target', () => {
    expect(
      auditTarget({
        ...legacyItem,
        entityType: 'ADMISSION',
        targetLabel: 'Admission 14/08/2026 · 7f103f90',
        subjectLabel: 'Nguyen Van Test · P26AA001',
      })
    ).toBe('Admission 14/08/2026 · 7f103f90 (Nguyen Van Test · P26AA001)');
  });

  it('keeps the legacy admission ID and patient reference in the right order', () => {
    expect(
      auditTarget({
        ...legacyItem,
        entityType: 'ADMISSION',
        entityId: 'admission-1',
        targetLabel: 'P26AA001',
      })
    ).toBe('admission-1 (P26AA001)');
  });

  it('shows the actor snapshot and falls back to the legacy username', () => {
    expect(
      auditActor({ ...legacyItem, actorDisplayName: 'System Administrator', actorRole: 'ADMIN' })
    ).toBe('System Administrator (admin · Admin)');
    expect(auditActor(legacyItem)).toBe('admin');
  });

  it('prefers the structured summary and uses a clear legacy fallback', () => {
    expect(auditChangeSummary({ ...legacyItem, summary: '3 fields changed' })).toBe(
      '3 fields changed'
    );
    expect(auditChangeSummary({ ...legacyItem, details: 'Changed assigned room' })).toBe(
      'Changed assigned room'
    );
    expect(auditChangeSummary(legacyItem)).toBe('Record updated');
  });

  it('uses structured counts instead of repeating the target in update and bulk summaries', () => {
    expect(
      auditChangeSummary({
        ...legacyItem,
        eventVersion: 2,
        legacy: false,
        summary: 'Updated patient Nguyen Van Test',
        changeCount: 3,
      })
    ).toBe('3 fields changed');
    expect(
      auditChangeSummary({
        ...legacyItem,
        action: 'BULK_DELETE',
        eventVersion: 2,
        legacy: false,
        summary: 'Deleted selected patients',
        affectedTargetCount: 2,
      })
    ).toBe('2 records deleted');
  });

  it('provides readable codes and field names', () => {
    expect(readableAuditCode('BULK_DELETE')).toBe('Bulk Delete');
    expect(auditFieldLabel('departmentId')).toBe('Department');
    expect(auditFieldLabel('customFieldName')).toBe('Custom Field Name');
  });

  it('distinguishes a protected value from a genuinely unset general value', () => {
    const change = {
      fieldKey: 'medicalHistory',
      valueType: 'TEXT' as const,
      oldValue: null,
      newValue: null,
      oldDisplayValue: null,
      newDisplayValue: null,
      sensitivity: 'MEDICAL' as const,
      displayOrder: 1,
    };

    expect(auditChangeValue(change, 'old')).toBe('Protected value');
    expect(auditChangeValue({ ...change, sensitivity: 'GENERAL' }, 'old')).toBe('Not set');
    expect(
      auditChangeValue({ ...change, oldValue: '[REDACTED]', oldDisplayValue: '[REDACTED]' }, 'old')
    ).toBe('Protected value');
  });

  it('offers every delivered action and entity type as a filter value', () => {
    expect(AUDIT_LOG_ACTIONS).toEqual([
      'CREATE',
      'UPDATE',
      'DELETE',
      'BULK_DELETE',
      'STATUS_UPDATE',
      'PASSWORD_RESET',
    ]);
    expect(AUDIT_LOG_ENTITY_TYPES).toEqual(['PATIENT', 'ADMISSION', 'USER', 'CARE_VISIT', 'CARE_TASK', 'CARE_RESULT', 'CARE_ASSIGNMENT']);
  });
});
